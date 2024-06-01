package com.lassanit.smotify.bases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.AppMessageBase.Params.KEY_TIME
import com.lassanit.smotify.bases.SharedBase.CloudService
import com.lassanit.smotify.bases.SharedBase.CloudService.ReadSync
import com.lassanit.smotify.bases.SharedBase.CloudService.WriteSync
import com.lassanit.smotify.classes.Resource
import com.lassanit.smotify.classes.SortWith
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import com.lassanit.smotify.services_receivers.SmartPhoneService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface CloudTaskImp {
    fun onSuccess(any: Any)
    fun onFailure(e: Exception? = null)
}

interface StorageUploadImp {
    fun onComplete(downloadUrl: Uri?)
}

interface StorageDownloadImp {
    fun onComplete(file: File?)
}

class CloudBase {

    class Store(context: Context, uid: String) {
        companion object {
            private const val DOC_PHONES = "phones"
            private const val DOC_MESSAGES = "messages"
            private const val DOC_APPS = "apps"

            private const val COL_DATA = "data"

            private const val KEY_TIME_READ = "last_read_time_1.1"
            private const val KEY_TIME_WRITE = "last_write_time_1.1"
            private const val KEY_STATUS = "status"
            private const val KEY_WRITE_ON = "root_device_action"
            private const val KEY_CLOUD_MESSAGES = "cloud_messages"
            private const val KEY_CLOUD_MEDIA = "cloud_media"

            private fun getStatusMap(allowWrite: Boolean): HashMap<String, Any> {
                return hashMapOf(
                    KEY_WRITE_ON to allowWrite, if (allowWrite) KEY_TIME_READ to Date()
                    else KEY_TIME_WRITE to Date()
                )
            }

            fun set(uid: String, first: Boolean = false) {
                if (SmartNotify.isUser()
                        .and(first.or(CloudService.isActive()))
                ) setDeviceStatus(uid, true)
            }

            fun reset(uid: String) {
                if (SmartNotify.isUser().and(CloudService.isActive())) setDeviceStatus(uid, false)
            }

            private fun setDeviceStatus(uid: String, active: Boolean) {
                val map = hashMapOf<String, Any>()
                map[KEY_TIME] = FieldValue.serverTimestamp()
                map[KEY_STATUS] = active
                getStore(uid).document(DOC_PHONES)
                    .set(hashMapOf(Phone.getKey() to map), SetOptions.merge())
            }

            private fun packageEnc(pkg: String): String {
                return pkg.replace(".", "_")
            }

            private fun packageDec(pkg: String): String {
                return pkg.replace("_", ".")
            }

            private fun getStore(uid: String): CollectionReference {
                return FirebaseFirestore.getInstance().collection(uid)
            }
        }

        private val store = getStore(uid)
        private val read = Read(context, store)
        private val write = Write1(context, store)

        fun requestReadLink() {
            runCatching {
                read.request()
            }.onFailure { it.printStackTrace() }
        }

        fun requestWriteLink() {
            runCatching {
                write.request()
                // write.requestTransaction()
            }.onFailure { it.printStackTrace() }
        }

        private class Write(private val context: Context, private val store: CollectionReference) {
            companion object {
                const val TAG = "CloudBase-WRITE-1.1"
            }

            private var registration: ListenerRegistration? = null
            private var lastMsgTime = 0L
            fun request() {
                // checkService
                // checkSyncTime(expected after every 15 min)
                // device  ... syncing/locks
                // checkWriteAllowed(app, msg)  // snapshot listener (break on success)
                // writePhone(app,msg)  // completeListener

                runCatching {
                    if (isAllowed().not()) return
                    SmartNotify.log("request:: isAllowed", TAG)
                    WriteSync.syncStarted()
                    runCatching {
                        registration =
                            store.document(Phone.getKey()).addSnapshotListener { value, _ ->
                                if (isWritingAllowed(value)) {
                                    registration?.remove()
                                    registration = null
                                    writePhone()
                                }
                            }
                    }.onFailure { it.printStackTrace(); onFailure() }
                }
            }

            private fun isAllowed(): Boolean {
                return runCatching {

                    if (SmartNotify.isUser().and(CloudService.isActive())
                            .and(SharedBase.Settings.Display.isCloudSharingAllowed()).not()
                    ) {
                        SmartNotify.log(
                            "isAllowed:: SmartNotify.isUser().and(CloudService.isActive())\n" + "                            .and(SharedBase.Settings.Display.isCloudSharingAllowed()).not()\n" + "                    ",
                            TAG
                        )
                        return false
                    }

                    if (registration != null) {
                        SmartNotify.log("isAllowed:: registration != null", TAG)
                        return false
                    }

                    if (WriteSync.isSyncAllowed().not()) {
                        if (WriteSync.allowedSyncReset()) WriteSync.syncFailed()
                        SmartNotify.log(
                            "isAllowed:: ${WriteSync.isSyncAllowed()}, ${WriteSync.isSyncing()}, ${
                                Date(
                                    WriteSync.getExpectedSyncTime()
                                )
                            }", TAG
                        )
                        SmartNotify.log("isAllowed:: WriteSync.isSyncAllowed().not()", TAG)
                        return false
                    }

                    return true
                }.getOrElse {
                    SmartNotify.log("isAllowed:: getOrElse", TAG)
                    false
                }
            }

            private fun onFailure() {
                runCatching {
                    registration?.remove()
                    WriteSync.syncFailed()
                }
            }

            private fun onSuccess() {
                runCatching {
                    WriteSync.setLastMsgSyncedTime(lastMsgTime)
                    registration?.remove()
                    WriteSync.syncComplete()
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun isWritingAllowed(value: DocumentSnapshot?): Boolean {
                return runCatching {
                    (value?.data?.getOrDefault(KEY_WRITE_ON, null) ?: true) as Boolean
                }.getOrElse { true }
            }

            private fun writePhone() {
                runCatching {
                    val db = DisplayBaseHelper.get(context)
                    val time = WriteSync.getLastMsgSyncedTime()
                    val msgs = db.getMessageList(time)
                    var size = 0L
                    val docMap = getStatusMap(false)
                    val msgData = hashMapOf<String, Any?>()
                    val firstMsgId = msgs.first().id
                    var lastMsgId: Int = firstMsgId
                    for (message in msgs) {
                        val name = db.getApp(message.pkg)?.name
                        val cMsg = CloudMessage.toMap(name.toString(), message)
                        size += cMsg.toString().length
                        if (size >= 999900) {
                            lastMsgTime = message.time
                            lastMsgId = message.id
                            break
                        }
                        msgData[message.id.toString()] = cMsg
                    }

                    if (lastMsgTime == 0L) {
                        lastMsgTime = msgs.lastOrNull()?.time ?: 0L
                        msgs.lastOrNull()?.id?.let { lastMsgId = it }
                    }

                    docMap[KEY_CLOUD_MESSAGES] = msgData

                    runCatching {
                        val mediaList = db.getMediaList(firstMsgId.toLong(), lastMsgId.toLong())
                        val file = Storage.zipIt(context, mediaList)
                        Storage.uploadMediaFile(
                            store.id,
                            Phone.getKey(),
                            file,
                            object : StorageUploadImp {
                                override fun onComplete(downloadUrl: Uri?) {
                                    runCatching {
                                        docMap[KEY_CLOUD_MEDIA] = downloadUrl?.toString() ?: ""
                                        store.document(Phone.getKey())
                                            .set(docMap, SetOptions.merge()).addOnCompleteListener {
                                                if (it.isSuccessful) onSuccess() else onFailure()
                                                file?.deleteRecursively()
                                            }
                                    }.onFailure { it.printStackTrace() }
                                }
                            })
                    }.onFailure { it.printStackTrace() }

                }.onFailure { it.printStackTrace();onFailure() }
            }
        }

        private class Write1(private val context: Context, private val store: CollectionReference) {
            companion object {
                const val TAG = "CloudBase-WRITE-1.2"
            }

            fun request() {
                runCatching {
                    if (isAllowed().not()) return
                    SmartNotify.log("Transaction:: isAllowed", TAG)
                    WriteSync.syncStarted()
                    val doc = store.document(Phone.getKey())
                    doc.get().addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (isWritingAllowed(it.result)) {
                                val data = getData()
                                if (data == null) {
                                    onFailure()
                                    return@addOnCompleteListener
                                } else {
                                    val map = getStatusMap(false)
                                    map[KEY_CLOUD_MESSAGES] = data.second
                                    Storage.uploadMediaFile(store.id,
                                        Phone.getKey(),
                                        getMediaFile(data.first),
                                        object : StorageUploadImp {
                                            override fun onComplete(downloadUrl: Uri?) {
                                                map[KEY_CLOUD_MEDIA] = downloadUrl?.toString() ?: ""
                                                doc.set(map, SetOptions.merge())
                                                    .addOnCompleteListener { it1 ->
                                                        if (it1.isSuccessful) {
                                                            onSuccess(data.third)
                                                        } else {
                                                            onFailure()
                                                        }
                                                    }
                                            }
                                        })
                                }
                            } else {
                                onFailure()
                            }
                        } else {
                            onFailure()
                        }
                    }
                }
            }

            fun requestTransaction() {
                runCatching {
                    if (isAllowed().not()) return
                    SmartNotify.log("Transaction:: isAllowed", TAG)
                    WriteSync.syncStarted()
                    val doc = store.document(Phone.getKey())
                    var isSuccess = false
                    var timePair: Pair<Long, Boolean>? = null
                    FirebaseFirestore.getInstance()
                        .runTransaction {
                            if (isWritingAllowed(it.get(doc))) {
                                val data = getData()
                                if (data == null) {
                                    isSuccess = false
                                } else {
                                    timePair = data.third
                                    Storage.uploadMediaFile(store.id,
                                        Phone.getKey(),
                                        getMediaFile(data.first),
                                        object : StorageUploadImp {
                                            override fun onComplete(downloadUrl: Uri?) {
                                                val map = data.second
                                                map[KEY_CLOUD_MEDIA] =
                                                    downloadUrl?.toString() ?: ""
                                                isSuccess = true
                                                it.set(doc, map, SetOptions.merge())
                                            }
                                        })
                                }
                            } else {
                                isSuccess = false
                            }
                        }.addOnCompleteListener {
                            if (it.isSuccessful && isSuccess) {
                                onSuccess(timePair)
                            } else onFailure()
                        }


                }
            }

            /**
             * @return Triple of {
             *  Pair(firstMsg-id, lastMsg-id),
             *  HashMap(msg-id, Msg),
             *  Pair(lastMessageTime, moreDataAvailable)
             * }
             * <br>
             * if return is null then there is either no data to share or error occured
             * */
            private fun getData(): Triple<Pair<Int, Int>, HashMap<String, Any?>, Pair<Long, Boolean>>? {
                return runCatching {
                    val db = DisplayBaseHelper.get(context)
                    val appMessages = db.getMessageList(WriteSync.getLastMsgSyncedTime())
                    if (appMessages.isNotEmpty()) {
                        var size = 0L
                        var lastMsgTime = 0L
                        val msgData = hashMapOf<String, Any?>()
                        val firstMsgId = appMessages.first().id
                        var lastMsgId: Int = firstMsgId
                        var more = false
                        for (message in appMessages) {
                            val name = db.getApp(message.pkg)?.name
                            val cMsg = CloudMessage.toMap(name.toString(), message)
                            size += cMsg.toString().length
                            if (size >= 999900) {
                                lastMsgTime = message.time
                                lastMsgId = message.id
                                if (appMessages.lastOrNull() != message) more = true
                                break
                            }
                            msgData[message.id.toString()] = cMsg
                        }
                        if (lastMsgTime == 0L) {
                            lastMsgTime = appMessages.lastOrNull()?.time ?: 0L
                            appMessages.lastOrNull()?.id?.let { lastMsgId = it }
                        }
                        Triple(Pair(firstMsgId, lastMsgId), msgData, Pair(lastMsgTime, more))
                    } else null
                }.onFailure { it.printStackTrace();onFailure() }.getOrNull()
            }

            private fun getMediaFile(limitIds: Pair<Int, Int>): File? {
                return runCatching {
                    val mediaList = DisplayBaseHelper.get(context).getMediaList(
                        limitIds.first.toLong(), limitIds.second.toLong()
                    )
                    Storage.zipIt(context, mediaList)
                }.getOrNull()
            }

            private fun isAllowed(): Boolean {
                return runCatching {
                    if (SmartNotify.isUser().and(CloudService.isActive())
                            .and(SharedBase.Settings.Display.isCloudSharingAllowed()).not()
                    ) {
                        SmartNotify.log(
                            "isAllowed:: SmartNotify.isUser().and(CloudService.isActive())\n" + "                            .and(SharedBase.Settings.Display.isCloudSharingAllowed()).not()\n" + "                    ",
                            TAG
                        )
                        return false
                    }

                    if (WriteSync.isSyncAllowed().not()) {
                        if (WriteSync.allowedSyncReset()) WriteSync.syncFailed()
                        SmartNotify.log(
                            "isAllowed:: ${WriteSync.isSyncAllowed()}, ${WriteSync.isSyncing()}, ${
                                Date(
                                    WriteSync.getExpectedSyncTime()
                                )
                            }", TAG
                        )
                        SmartNotify.log("isAllowed:: WriteSync.isSyncAllowed().not()", TAG)
                        return false
                    }

                    return true
                }.getOrElse {
                    SmartNotify.log("isAllowed:: getOrElse", TAG)
                    false
                }
            }

            private fun onFailure() {
                runCatching {
                    WriteSync.syncFailed()
                }
            }

            private fun onSuccess(timePair: Pair<Long, Boolean>?) {
                runCatching {
                    if (timePair != null) {
                        WriteSync.setLastMsgSyncedTime(timePair.first)
                    }
                    WriteSync.syncComplete()
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun isWritingAllowed(value: DocumentSnapshot?): Boolean {
                return runCatching {
                    (value?.data?.getOrDefault(KEY_WRITE_ON, null) ?: true) as Boolean
                }.getOrElse { true }
            }
        }

        private class Read(private val context: Context, private var store: CollectionReference) {
            private val registration = ArrayList<ListenerRegistration>()

            companion object {
                const val TAG = "CloudBase-READ-1.1"
            }

            init {
                runCatching {
                    if (ReadSync.isSyncing()) ReadSync.syncCompleted()
                }
            }

            fun request() {
                runCatching {
                    if (isAllowed()) {
                        ReadSync.syncStarted()
                        listPhones()
                    }
                }.onFailure { it.printStackTrace() }
            }

            private fun isAllowed(): Boolean {
                return runCatching {
                    if (SmartNotify.isUser().and(CloudService.isActive()).not()) return false
                    if (ReadSync.isSyncing()) {
                        return false
                    }

                    if (registration.isNotEmpty()) return false

                    return true
                }.getOrElse {
                    false
                }
            }

            private fun onFailure() {
                runCatching {
                    ReadSync.syncCompleted()
                    for (listener in registration) {
                        listener.remove()
                    }
                    registration.clear()
                }.onFailure { it.printStackTrace() }
            }

            private fun onSuccess(key: String) {
                runCatching {
                    val map = getStatusMap(true)
                    map[KEY_CLOUD_MESSAGES] = FieldValue.delete()
                    map[KEY_CLOUD_MEDIA] = FieldValue.delete()
                    store.document(key).set(map, SetOptions.merge())
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun listPhones() {
                runCatching {
                    // store = getStore("l4NAj08YiSfRZrMIHFnUv6hfu9k1")
                    store.document(DOC_PHONES).get().addOnCompleteListener {
                        if (it.isSuccessful) {
                            val data = it.result.data?.entries
                                ?: run { onFailure();return@addOnCompleteListener }
                            for (entry in data) {
                                if (Phone.isSameKey(entry.key).not()) handlePhoneKey(entry.key)
                            }
                        } else {
                            onFailure()
                        }
                    }
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun handlePhoneKey(key: String) {
                runCatching {
                    registration.add(store.document(key)
                        .addSnapshotListener(MetadataChanges.EXCLUDE) { value, e ->
                            if (e != null) {
                                e.printStackTrace()
                                onFailure()
                                return@addSnapshotListener
                            }
                            if (value?.metadata?.isFromCache == true) return@addSnapshotListener

                            val abc = isReadingAllowed(value)
                            if (abc) {
                                readPhone(value!!, key)
                            }
                        })
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun isReadingAllowed(value: DocumentSnapshot?): Boolean {
                return runCatching {
                    SmartNotify.log(
                        "isReadingAllowed:: ${
                            value?.data?.getOrDefault(
                                KEY_WRITE_ON, "default"
                            )
                        }", TAG
                    )

                    if ((value != null).and(value!!.data != null)) {
                        return (value.data!![KEY_WRITE_ON] as Boolean? ?: true).not()
                    }
                    return false
                }.getOrElse { false }
            }

            private fun readPhone(value: DocumentSnapshot, key: String) {
                runCatching {
                    val data = value.get(KEY_CLOUD_MESSAGES) as Map<*, *>?
                    if (data == null || data.entries.isEmpty()) {
                        val media = value.getString(KEY_CLOUD_MEDIA)
                        if (media.isNullOrBlank().not()) {
                            runAsync(context, media!!, key)
                        }
                        onSuccess(key)
                        return@runCatching
                    }

                    val db = CloudBaseHelper.getEditor(context)
                    for (cMsg in data.entries) {
                        val mId = cMsg.key.toString()
                        val msgBody = cMsg.value as Map<*, *>? ?: continue
                        val msg = CloudMessage.toCloudMessage(msgBody) ?: continue
                        msg.fetchApp()?.let { db.addApp(it) }
                        msg.fetchMessage()?.let {
                            val id = db.addCloudMessage(it, key, mId)
                            if (id >= 0) {
                                SmartPhoneService.getCloud()?.onCloudMsgReceived(it.pkg)
                            }
                        }
                    }

                    val media = value.getString(KEY_CLOUD_MEDIA)
                    if (media.isNullOrBlank().not()) {
                        //val path = buildString {
                        //    append("media/")
                        //    append(store.id).append('/')
                        //    append(key).append(".zip")
                        //}
                        runAsync(context, media!!, key)
                    }
                    onSuccess(key)
                }.onFailure { it.printStackTrace();onFailure() }
            }

            private fun runAsync(context: Context, mediaPath: String, key: String) {
                SmartNotify.log("mediaPath::$mediaPath", TAG)
                runCatching {
                    Storage.downloadMediaFile(mediaPath, object : StorageDownloadImp {
                        override fun onComplete(file: File?) {
                            SmartNotify.log("mediaFilePath::${file?.path}", TAG)
                            if (file == null) return
                            val mediaList = Storage.unzip(context, file)
                            SmartNotify.log("mediaSize::${mediaList.size}", TAG)
                            CloudBaseHelper.getEditor(context).addCloudMediaList(mediaList, key)
                            file.deleteRecursively()
                        }
                    })
                }.onFailure { it.printStackTrace() }

            }
        }
    }

    class Storage {
        private object Paths {
            fun profile(uid: String): String {
                return "profile/$uid.jpg"
            }

            fun media(uid: String, key: String): String {
                return "media/$uid/$key.zip"
            }
        }

        companion object {
            fun uploadProfileImage(
                context: Context,
                uid: String,
                uri: Uri,
                onResult: (Uri?) -> Unit
            ) {
                val task = suspend {
                    val icon = Icon.createWithContentUri(uri).loadDrawable(context)
                    if (icon == null) {
                        onResult(null)
                    } else {
                        val byteArray = Resource.bitmapToByteArray(Resource.drawableToBitmap(icon))
                        Firebase.storage.reference.child(Paths.profile(uid)).putBytes(byteArray)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    it.result.storage.downloadUrl.addOnCompleteListener { it1 ->
                                        if (it1.isSuccessful) {
                                            onResult(it1.result)
                                        } else {
                                            it1.exception?.printStackTrace()
                                            onResult(null)
                                        }
                                    }
                                } else {
                                    it.exception?.printStackTrace()
                                    onResult(null)
                                }
                            }
                    }
                }
                CoroutineScope(IO).launch { task() }
            }


            fun uploadProfileImage(
                uid: String,
                byteArray: ByteArray,
                taskImp: CloudTaskImp? = null,
            ) {
                runCatching {
                    Upload.request(byteArray, Paths.profile(uid), object : StorageUploadImp {
                        override fun onComplete(downloadUrl: Uri?) {
                            if (downloadUrl == null) taskImp?.onFailure()
                            else taskImp?.onSuccess(downloadUrl)
                        }
                    })
                }
            }

            fun uploadMediaFile(
                uid: String, key: String, file: File?, uploadImp: StorageUploadImp
            ) {
                runCatching {
                    if (file == null) {
                        uploadImp.onComplete(null)
                        return
                    }
                    Upload.request(file, Paths.media(uid, key), uploadImp)
                }.onFailure {
                    it.printStackTrace()
                    uploadImp.onComplete(null)
                }
            }

            fun downloadMediaFile(
                path: String, downloadImp: StorageDownloadImp
            ) {
                runCatching {
                    Download.request(path, downloadImp)
                }.onFailure {
                    it.printStackTrace()
                    downloadImp.onComplete(null)
                }
            }

            private fun reduceBitmapSizeTo100kb(bitmap: Bitmap): Bitmap {
                // Calculate the scaling factor.
                val scalingFactor = bitmap.width / bitmap.height
                val newWidth = 100 * scalingFactor
                val newHeight = 100

                // Create a scaled bitmap.
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)

                // Compress the scaled bitmap to JPEG format.
                val byteArrayOutputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)

                // Get the compressed bitmap bytes.
                val compressedBitmapBytes = byteArrayOutputStream.toByteArray()

                // Create a new bitmap from the compressed bitmap bytes.

                return BitmapFactory.decodeByteArray(
                    compressedBitmapBytes, 0, compressedBitmapBytes.size
                )
            }

            private fun reduceImageSizeByHalf(bitmap: Bitmap): Bitmap {
                // Calculate the new width and height of the image.
                val newWidth = bitmap.width / 2
                val newHeight = bitmap.height / 2

                return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
            }

            fun zipIt(context: Context, mediaList: ArrayList<SmartMedia>): File? {
                return runCatching {
                    if (mediaList.isEmpty()) return null
                    val file = File(context.cacheDir, Date().time.toString().plus(".zip"))
                    val zipOutputStream = ZipOutputStream(FileOutputStream(file))
                    for (media in mediaList) {
                        if (media.exist()) {
                            if (media.getMap() != null) {
                                runCatching {
                                    val bitmapByteStream = ByteArrayOutputStream()
                                    reduceImageSizeByHalf(media.getMap()!!).compress(
                                        Bitmap.CompressFormat.JPEG, 100, bitmapByteStream
                                    )
                                    val bitmapEntry = ZipEntry("${media.mid}.jpg")
                                    zipOutputStream.putNextEntry(bitmapEntry)
                                    zipOutputStream.write(bitmapByteStream.toByteArray())
                                    zipOutputStream.closeEntry()
                                }.onFailure { it.printStackTrace() }
                            } else {
                                runCatching {
                                    val inputStream: InputStream? =
                                        context.contentResolver.openInputStream(media.getUri()!!)
                                    if (inputStream != null) {
                                        val ext =
                                            if (media.isAudio(context)) ".mp3"
                                            else if (media.isImage(context)) ".jpg"
                                            else if (media.isVideo(context)) ".mp4"
                                            else ""
                                        val audioEntry = ZipEntry("${media.mid}$ext")
                                        zipOutputStream.putNextEntry(audioEntry)
                                        zipOutputStream.write(inputStream.readBytes())
                                        zipOutputStream.closeEntry()
                                    }
                                    inputStream?.close()
                                }.onFailure { it.printStackTrace() }
                            }
                        }
                    }
                    zipOutputStream.close()
                    file
                }.onFailure { it.printStackTrace() }.getOrElse { null }
            }

            fun unzip(context: Context, zipFile: File): ArrayList<SmartMedia> {
                return runCatching {
                    val list = ArrayList<SmartMedia>()
                    val zipInputStream = ZipInputStream(FileInputStream(zipFile))
                    var zipEntry: ZipEntry? = zipInputStream.nextEntry
                    while (zipEntry != null) {
                        val outputFile = File(context.filesDir, zipEntry.name)

                        val bufferedInputStream = BufferedInputStream(zipInputStream)
                        val bufferedOutputStream =
                            BufferedOutputStream(FileOutputStream(outputFile))

                        bufferedOutputStream.use { bos ->
                            val bytes = ByteArray(1024)
                            var read: Int
                            while ((bufferedInputStream.read(bytes).also { read = it }) != -1) {
                                bos.write(bytes, 0, read)
                            }
                        }
                        if (zipEntry.name.endsWith(".mp3")) {
                            val id = zipEntry.name.substringBefore(".mp3").toInt()

                            val audioUri = Uri.fromFile(outputFile)
                            list.add(SmartMedia(id, null, audioUri))
                        } else if (zipEntry.name.endsWith(".jpg")) {
                            val id = zipEntry.name.substringBefore(".jpg").toInt()

                            val imageUri = Uri.fromFile(outputFile)
                            list.add(SmartMedia(id, null, imageUri))

                            //val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                            //list.add(SmartMedia(id, bitmap, null))

                        }
                        zipEntry = zipInputStream.nextEntry
                    }
                    list
                }.onFailure { it.printStackTrace() }.getOrElse { arrayListOf() }
            }
        }

        object Upload {

            fun request(uri: Uri, path: String, imp: StorageUploadImp?) {
                runCatching {
                    val storageRef = Firebase.storage.reference.child(path)
                    storageRef.putFile(uri).addOnCompleteListener {
                        if (it.isSuccessful) {
                            it.result.storage.downloadUrl.addOnCompleteListener { it1 ->
                                if (it1.isSuccessful) {
                                    imp?.onComplete(it1.result)
                                } else {
                                    it1.exception?.printStackTrace()
                                    imp?.onComplete(null)
                                }
                            }
                        } else {
                            it.exception?.printStackTrace()
                            imp?.onComplete(null)
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    imp?.onComplete(null)
                }
            }

            fun request(file: File, path: String, imp: StorageUploadImp?) {
                runCatching {
                    val storageRef = Firebase.storage.reference.child(path)
                    storageRef.putFile(file.toUri()).addOnCompleteListener {
                        if (it.isSuccessful) {
                            it.result.storage.downloadUrl.addOnCompleteListener { it1 ->
                                if (it1.isSuccessful) {
                                    imp?.onComplete(it1.result)
                                } else {
                                    it1.exception?.printStackTrace()
                                    imp?.onComplete(null)
                                }
                            }
                        } else {
                            it.exception?.printStackTrace()
                            imp?.onComplete(null)
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    imp?.onComplete(null)
                }
            }

            fun request(byteArray: ByteArray, path: String, imp: StorageUploadImp?) {
                runCatching {
                    val storageRef = Firebase.storage.reference.child(path)
                    storageRef.putBytes(byteArray).addOnCompleteListener {
                        if (it.isSuccessful) {
                            it.result.storage.downloadUrl.addOnCompleteListener { it1 ->
                                if (it1.isSuccessful) {
                                    imp?.onComplete(it1.result)
                                } else {
                                    it1.exception?.printStackTrace()
                                    imp?.onComplete(null)
                                }
                            }
                        } else {
                            it.exception?.printStackTrace()
                            imp?.onComplete(null)
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    imp?.onComplete(null)
                }
            }

        }

        object Download {
            fun request(filePath: String, imp: StorageDownloadImp?) {
                runCatching {
                    val file = File.createTempFile("db_cloud", ".zip")
                    request(filePath, file, imp)
                }.onFailure {
                    it.printStackTrace()
                    imp?.onComplete(null)
                }
            }

            fun request(filePath: String, destination: File, imp: StorageDownloadImp?) {
                runCatching {
                    val storageRef = Firebase.storage.getReferenceFromUrl(filePath)
                    storageRef.getFile(destination).addOnCompleteListener {
                        if (it.isSuccessful) {
                            imp?.onComplete(destination)
                            storageRef.delete()
                        } else {
                            it.exception?.printStackTrace()
                            imp?.onComplete(null)
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    imp?.onComplete(null)
                }
            }
        }

    }

    class RealTime {
        companion object {
            private const val TAG = "FireKit-CloudRealTime-1.0"
            private const val ADS = "ads_"
            private const val CLOUD = "cloud_"
            fun restoreCloudSubscription(uid: String) {
                runCatching {
                    Firebase.database.getReference(CLOUD).child(uid).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                val time = snapshot.getValue(Long::class.java) ?: 0L
                                CloudService.setTime(time)
                            }
                        }
                }.onFailure { e: Throwable ->
                    Firebase.crashlytics.recordException(e)
                    SmartNotify.log("${e.message}", TAG)
                }
            }

            fun setCloudTimeLimit(
                time: Long,
                onComplete: Runnable? = null,
                onSuccess: Runnable? = null,
                onFailure: Runnable? = null
            ) {
                runCatching {
                    Firebase.database.getReference(CLOUD).child(Firebase.auth.currentUser!!.uid)
                        .setValue(time).addOnCompleteListener {
                            if (it.isSuccessful) onSuccess?.run()
                            else onFailure?.run()
                            onComplete?.run()
                            it.exception?.printStackTrace()
                        }
                }.onFailure { e: Throwable ->
                    Firebase.crashlytics.recordException(e)
                    e.printStackTrace()
                    SmartNotify.log("${e.message}", TAG)
                }
            }

            fun restoreAdSubscription(uid: String) {
                runCatching {
                    SmartNotify.log("restoreAdSubscription(uid: String)", TAG)
                    Firebase.database.getReference(ADS).child(uid).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                SmartNotify.log("snapshot.exists()", TAG)
                                val time = snapshot.getValue(Long::class.java)
                                    ?: return@addOnSuccessListener
                                val t = SharedBase.Ads.getTime()
                                SharedBase.Ads.setTime(SortWith.Chooser.greater(t, time))
                            } else {
                                SmartNotify.log("snapshot.exists().not()", TAG)
                            }
                        }
                }.onFailure { e: Throwable ->
                    Firebase.crashlytics.recordException(e)
                    e.printStackTrace()
                    SmartNotify.log("${e.message}", TAG)
                }
            }

            fun setAdTimeLimit(
                time: Long,
                onComplete: Runnable? = null,
                onSuccess: Runnable? = null,
                onFailure: Runnable? = null
            ) {
                runCatching {
                    Firebase.database.getReference(ADS).child(Firebase.auth.currentUser!!.uid)
                        .setValue(time).addOnCompleteListener {
                            if (it.isSuccessful) onSuccess?.run()
                            else onFailure?.run()
                            onComplete?.run()
                            it.exception?.printStackTrace()
                        }
                }.onFailure { e: Throwable ->
                    Firebase.crashlytics.recordException(e)
                    e.printStackTrace()
                    SmartNotify.log("${e.message}", TAG)
                }
            }
        }
    }

    class Phone(var id: Int = -1) {
        var manufacturer: String = Build.MANUFACTURER
        var brand: String = Build.BRAND
        var product: String = Build.PRODUCT
        var device: String = Build.DEVICE
        var board: String = Build.BOARD

        companion object {
            private const val DELI = "__"

            fun getKey(): String {
                return Phone().getKey()
            }

            fun make(key: String): Phone? {
                runCatching {
                    val list = key.split(DELI)
                    if (list.size == 5) {
                        val phone = Phone()
                        phone.manufacturer = list[0]
                        phone.brand = list[1]
                        phone.product = list[2]
                        phone.device = list[3]
                        phone.board = list[4]
                        return phone
                    }
                }
                return null
            }

            fun isSameKey(key: String): Boolean {
                return getKey() == key
            }
        }

        fun isMyPhone(): Boolean {
            return (manufacturer == Build.MANUFACTURER).and(brand == Build.BRAND)
                .and(product == Build.PRODUCT).and(device == Build.DEVICE).and(board == Build.BOARD)
        }

        override fun toString(): String {
            return "Phone(id=$id, manufacturer='$manufacturer', brand='$brand', product='$product', device='$device', board='$board')"
        }

        fun getKey(): String {
            return buildString {
                append(manufacturer).append(DELI)
                append(brand).append(DELI)
                append(product).append(DELI)
                append(device).append(DELI)
                append(board)
            }
        }
    }

    private class Message {
        var pkg: String? = null
        var type: Int = AppMessage.Types.TYPE_NULL
        var title: String? = null
        var titleDesc: String? = null
        var text: String? = null
        var textDesc: String? = null
        var time: Long = 0L
        fun fetch(): AppMessage? {
            return runCatching {
                AppMessage(
                    -1, type, pkg!!, title!!, titleDesc, text!!, textDesc, time
                )
            }.getOrNull()
        }

        companion object {
            private const val K_PKG = "pkg"
            private const val K_TYPE = "type"
            private const val K_TITLE = "title"
            private const val K_TITLE_DESC = "titleDesc"
            private const val K_TEXT = "text"
            private const val K_TEXT_DESC = "textDesc"
            private const val K_TIME = "time"

            fun get(appMessage: AppMessage): Message {
                val msg = Message()
                msg.pkg = appMessage.pkg
                msg.type = appMessage.type
                msg.title = appMessage.title
                msg.titleDesc = appMessage.titleDesc
                msg.text = appMessage.text
                msg.textDesc = appMessage.textDesc
                msg.time = appMessage.time
                return msg
            }

            fun toMessage(map: Map<*, *>): AppMessage? {
                return runCatching {
                    val msg = Message()
                    msg.pkg = map[K_PKG].toString()
                    msg.type = map[K_TYPE].toString().toIntOrNull() ?: AppMessage.Types.TYPE_NULL
                    msg.title = map[K_TITLE].toString()
                    msg.titleDesc = map[K_TITLE_DESC].toString()
                    msg.text = map[K_TEXT].toString()
                    msg.textDesc = map[K_TEXT_DESC].toString()
                    msg.time = map[K_TIME].toString().toLongOrNull() ?: 0
                    return msg.fetch()
                }.getOrNull()
            }

            fun toMap(appMessage: AppMessage): HashMap<String, Any> {
                val map = hashMapOf<String, Any>()
                map[K_PKG] = appMessage.pkg
                map[K_TYPE] = appMessage.type
                map[K_TITLE] = appMessage.title
                map[K_TITLE_DESC] = appMessage.titleDesc.toString()
                map[K_TEXT] = appMessage.text
                map[K_TEXT_DESC] = appMessage.textDesc.toString()
                map[K_TIME] = appMessage.time
                return map
            }
        }
    }

    class CloudMessage {
        companion object {
            private const val K_PKG = "pkg"
            private const val K_NAME = "name"
            private const val K_TYPE = "type"
            private const val K_TITLE = "title"
            private const val K_TITLE_DESC = "titleDesc"
            private const val K_TEXT = "text"
            private const val K_TEXT_DESC = "textDesc"
            private const val K_HAS_ICON = "hasIcon"
            private const val K_TIME = "time"

            fun get(name: String, appMessage: AppMessage): CloudMessage {
                val msg = CloudMessage()
                msg.name = name
                msg.pkg = appMessage.pkg
                msg.type = appMessage.type
                msg.title = appMessage.title
                msg.titleDesc = appMessage.titleDesc
                msg.text = appMessage.text
                msg.textDesc = appMessage.textDesc
                msg.time = appMessage.time
                msg.hasIcon = appMessage.hasMedia
                return msg
            }

            fun toCloudMessage(map: Map<*, *>): CloudMessage? {
                return runCatching {
                    val msg = CloudMessage()
                    msg.name = map[K_NAME].toString()
                    msg.pkg = map[K_PKG].toString()
                    msg.type = map[K_TYPE].toString().toIntOrNull() ?: AppMessage.Types.TYPE_NULL
                    msg.title = map[K_TITLE].toString()
                    msg.titleDesc = map[K_TITLE_DESC].toString()
                    msg.text = map[K_TEXT].toString()
                    msg.textDesc = map[K_TEXT_DESC].toString()
                    msg.time = map[K_TIME].toString().toLongOrNull() ?: 0
                    msg.hasIcon = map.getOrDefault(K_HAS_ICON, false) as Boolean

                    return msg
                }.getOrNull()
            }

            fun toMap(name: String, appMessage: AppMessage): HashMap<String, Any> {
                val map = hashMapOf<String, Any>()
                map[K_NAME] = name
                map[K_PKG] = appMessage.pkg
                map[K_TYPE] = appMessage.type
                map[K_TITLE] = appMessage.title
                map[K_TITLE_DESC] = appMessage.titleDesc.toString()
                map[K_TEXT] = appMessage.text
                map[K_TEXT_DESC] = appMessage.textDesc.toString()
                map[K_TIME] = appMessage.time
                return map
            }
        }

        var pkg: String? = null
        var name: String? = null
        var type: Int = AppMessage.Types.TYPE_NULL
        var title: String? = null
        var titleDesc: String? = null
        var text: String? = null
        var textDesc: String? = null
        var hasIcon: Boolean = false
        var time: Long = 0L
        fun fetchMessage(): AppMessage? {
            return runCatching {

                AppMessage(
                    -1,
                    type,
                    pkg!!,
                    title!!,
                    titleDesc,
                    text!!,
                    textDesc,
                    time,
                    hasMedia = hasIcon,
                )
            }.getOrNull()
        }

        fun fetchApp(): App? {
            return runCatching {
                App(
                    pkg!!, name!!
                )
            }.getOrNull()
        }
    }
}