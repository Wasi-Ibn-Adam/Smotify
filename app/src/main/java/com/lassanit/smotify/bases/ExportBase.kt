package com.lassanit.smotify.bases

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStream
import java.security.MessageDigest
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class ExportBase(private val context: Context) {
    companion object {
        const val TAG = "ExportHelper"
    }

    private val device = Helper.get(context)
    private val cloud = Helper.getCloud(context)

    class Helper(
        private val context: Context,
        private val dbApp: AppBase,
        private val dbAppMessage: AppMessageBase,
        private val dbAppMedia: AppMediaBase
    )
    {
        companion object {
            const val TAG = "ExportHelper"
            fun get(context: Context): Helper {
                return SmartBase(context).getExportBase()
            }

            fun getCloud(context: Context): Helper {
                return CloudBaseHelper.get(context).getExportBase()
            }
        }


        private fun getAppBaseFile(): File? {
            return runCatching {
                val apps = dbApp.getApps()
                val list = StringBuilder()
                for (app in apps) list.append(app.name).append("__").append(app.pkg).appendLine()
                val file = File(context.cacheDir, "apps")
                val zipOutputStream = FileOutputStream(file)
                zipOutputStream.write(list.toString().encodeToByteArray())
                zipOutputStream.close()
                file
            }.onFailure { it.printStackTrace() }.getOrNull()
        }

        private fun setAppBaseFile(file: File) {
            runCatching {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val data = line?.split("__") ?: continue
                        val app = App(data[1], data[0])
                        dbApp.addApp(app)
                    }
                }
            }.onFailure { it.printStackTrace() }
        }

        private fun getAppMediaBaseFile(): File? {
            return CloudBase.Storage.zipIt(context, dbAppMedia.getMedia())
        }

        private fun setAppMediaBaseFile(file: File): ArrayList<SmartMedia> {
            return CloudBase.Storage.unzip(context, file)
        }

        private fun getAppMessageBaseFile(): File? {
            return runCatching {
                val apps = dbAppMessage.getAllAppMessages(0)
                val file = File(context.cacheDir, "messages")
                val zipOutputStream = ZipOutputStream(FileOutputStream(file))
                for (msg in apps) {
                    val audioEntry = ZipEntry(msg.id.toString())
                    zipOutputStream.putNextEntry(audioEntry)
                    zipOutputStream.write(AppMessage.toByteArray(msg))
                    zipOutputStream.closeEntry()
                }
                zipOutputStream.close()
                file
            }.onFailure { it.printStackTrace() }.getOrNull()
        }

        private fun setAppMessageBaseFile(file: File): HashMap<Int, Int> {
            return runCatching {
                val zipInputStream = ZipInputStream(FileInputStream(file))
                var zipEntry: ZipEntry? = zipInputStream.nextEntry
                val map = HashMap<Int, Int>()
                while (zipEntry != null) {
                    val msg = AppMessage.toAppMessage(zipInputStream.readBytes())
                    map[msg.id] = dbAppMessage.addUniqueAppMessage(msg, null).first
                    zipEntry = zipInputStream.nextEntry
                }
                map
            }.onFailure { it.printStackTrace() }.getOrElse { hashMapOf() }
        }

        private fun joinMediaMessages(ids: HashMap<Int, Int>, medias: ArrayList<SmartMedia>) {
            for (media in medias) {
                val newId = ids[media.mid] ?: continue
                dbAppMedia.addMedia(newId, media)
            }
        }

        fun exportDB(
            name: String,
            onProgressDetail: ((String) -> Unit)? = null,
        ): File? {
            return runCatching {
                val file = File(context.cacheDir, name)
                val zipOutputStream = ZipOutputStream(FileOutputStream(file))
                runCatching {
                    onProgressDetail?.let { it("$name Apps Loading.") }
                    val inputStream: InputStream? =
                        getAppBaseFile()?.let { context.contentResolver.openInputStream(it.toUri()) }
                    inputStream?.use {
                        onProgressDetail?.let { it("$name Apps Backup.") }
                        val entry = ZipEntry(Params.FILE_APP)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                runCatching {
                    onProgressDetail?.let { it("$name Notifications Media Loading.") }
                    val inputStream: InputStream? =
                        getAppMediaBaseFile()?.let { context.contentResolver.openInputStream(it.toUri()) }
                    inputStream?.use {
                        onProgressDetail?.let { it("$name Notifications Media Backup.") }
                        val entry = ZipEntry(Params.FILE_MEDIA)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                runCatching {
                    onProgressDetail?.let { it("$name Notifications Loading.") }
                    val inputStream: InputStream? =
                        getAppMessageBaseFile()?.let { context.contentResolver.openInputStream(it.toUri()) }
                    inputStream?.use {
                        onProgressDetail?.let { it("$name Notifications Media Backup.") }
                        val entry = ZipEntry(Params.FILE_MESSAGE)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                zipOutputStream.close()
                file
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }

        fun importDB(
            file: File,
            title: String,
            onProgressDetail: ((String) -> Unit)? = null,
        ) {
            runCatching {
                onProgressDetail?.let { it("$title Notifications Checking") }
                val zipInputStream = ZipInputStream(FileInputStream(file))
                var zipEntry: ZipEntry? = zipInputStream.nextEntry
                var idsMap: HashMap<Int, Int>? = null
                var mediaList: ArrayList<SmartMedia>? = null
                while (zipEntry != null) {
                    onProgressDetail?.let { it("$title Backup Reading.") }
                    val bufferedInputStream = BufferedInputStream(zipInputStream)
                    val outputFile = File(context.cacheDir, zipEntry.name)
                    val bufferedOutputStream =
                        BufferedOutputStream(FileOutputStream(outputFile))
                    onProgressDetail?.let { it("$title Backup Reading..") }
                    bufferedOutputStream.use { bos ->
                        val bytes = ByteArray(1024)
                        var read: Int
                        while ((bufferedInputStream.read(bytes).also { read = it }) != -1) {
                            bos.write(bytes, 0, read)
                        }
                    }
                    onProgressDetail?.let { it("$title Backup Reading...") }

                    if (zipEntry.name.contains(Params.FILE_APP)) {
                        onProgressDetail?.let { it("$title App List Adding.") }
                        setAppBaseFile(outputFile)
                    } else if (zipEntry.name.contains(Params.FILE_MEDIA)) {
                        onProgressDetail?.let { it("$title Notification Media Loading.") }
                        mediaList = setAppMediaBaseFile(outputFile)
                    } else if (zipEntry.name.contains(Params.FILE_MESSAGE)) {
                        onProgressDetail?.let { it("$title Notifications Loading.") }
                        idsMap = setAppMessageBaseFile(outputFile)
                    }
                    zipEntry = zipInputStream.nextEntry
                }
                runCatching {
                    if (mediaList.isNullOrEmpty().not() && idsMap.isNullOrEmpty().not()) {
                        onProgressDetail?.let { it("$title Notification Media Adding.") }
                        joinMediaMessages(idsMap!!, mediaList!!)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
                onProgressDetail?.let { it("$title Notifications Added") }
            }.onFailure {
                it.printStackTrace()
            }
        }

    }


    private object Params {
        const val FILE_APP = "apps.le"
        const val FILE_MEDIA = "media.le"
        const val FILE_MESSAGE = "message.le"

        const val FILE_SECURITY = "security.le"
        const val DB_EXT = ".le"
        const val FOLDER_DEVICE = "Device"
        const val FOLDER_CLOUD = "Cloud"
    }

    private fun getDBFile(): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "/Smart Notify/Database"
        )
        directory.mkdirs()
        val file = File(directory, "/${Date().time}${Params.DB_EXT}")
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    private fun getTempFiles(name: String): File {
        val directory = File(context.cacheDir, "/")
        directory.mkdirs()
        val file = File(directory, "/${name}${Params.DB_EXT}")
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    private fun getSecurityFile(uid: String): File? {
        return runCatching {
            val list = StringBuilder()
            list.append(uid)
            val file = getTempFiles("security")
            val zipOutputStream = FileOutputStream(file)
            zipOutputStream.write(list.toString().encodeToByteArray())
            zipOutputStream.close()
            file
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    private fun isValidSecurityFile(uid: String, file: File): Boolean {
        return runCatching {
            var end = false
            BufferedReader(FileReader(file)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.equals(uid) == true) {
                        end = true
                        break
                    }
                }
            }
            end
        }.onFailure { it.printStackTrace() }.getOrElse { false }
    }

    fun export(
        uid: String,
        onStart: Runnable? = null,
        onComplete: Runnable? = null,
        onSuccess: ((String?) -> Unit)? = null,
        onFailure: ((Exception?) -> Unit)? = null,
        onProgressDetail: ((String) -> Unit)? = null,
    ) {
        val task = suspend {
            runCatching {
                onStart?.run()
                onProgressDetail?.let { it("Backup Started..") }
                val file = getDBFile()
                val zipOutputStream = ZipOutputStream(FileOutputStream(file))
                runCatching {
                    val inputStream: InputStream? =
                        getSecurityFile(uid)?.let { it1 ->
                            context.contentResolver.openInputStream(
                                it1.toUri()
                            )
                        }
                    inputStream?.use {
                        val entry = ZipEntry(Params.FILE_SECURITY)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                runCatching {
                    val inputStream: InputStream? =
                        device.exportDB(Params.FOLDER_DEVICE, onProgressDetail = onProgressDetail)
                            ?.let { context.contentResolver.openInputStream(it.toUri()) }
                    inputStream?.use {
                        val entry = ZipEntry(Params.FOLDER_DEVICE)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                runCatching {
                    val inputStream: InputStream? =
                        cloud.exportDB(Params.FOLDER_CLOUD, onProgressDetail = onProgressDetail)
                            ?.let {
                                context.contentResolver.openInputStream(it.toUri())
                            }
                    inputStream?.use {
                        val entry = ZipEntry(Params.FOLDER_CLOUD)
                        zipOutputStream.putNextEntry(entry)
                        zipOutputStream.write(it.readBytes())
                        zipOutputStream.closeEntry()
                    }
                }.onFailure { it.printStackTrace() }
                onProgressDetail?.let { it("Backup Finishing...") }
                zipOutputStream.close()
                onProgressDetail?.let { it("Backup Complete.") }
                onSuccess?.let { it(null) }
                onComplete?.run()
            }.onFailure {
                it.printStackTrace()
                onFailure?.let { it1 -> it1(null) }
                onComplete?.run()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                task()
            }.onFailure { it.printStackTrace() }
        }
    }

    fun importDB(
        uid: String,
        uri: Uri,
        onStart: Runnable? = null,
        onComplete: Runnable? = null,
        onSuccess: ((String?) -> Unit)? = null,
        onFailure: ((String?, Exception?) -> Unit)? = null,
        onProgressDetail: ((String) -> Unit)? = null,
    ) {
        val task = suspend {
            runCatching {
                onStart?.run()
                onProgressDetail?.let { it("Import Started") }
                if (uri.path?.endsWith(Params.DB_EXT) != true) {
                    onFailure?.let {
                        it(
                            "This file it is not ** Smart-Notify ** generated backup file",
                            null
                        )
                    }
                    onProgressDetail?.let { it("Invalid Backup File") }
                    onComplete?.run()
                    return@runCatching
                }
                context.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        onProgressDetail?.let { it("Backup Initializing...") }
                        val zipInputStream = ZipInputStream(inputStream)
                        var zipEntry: ZipEntry? = zipInputStream.nextEntry
                        var deviceFile: File? = null
                        var cloudFile: File? = null
                        var securityBase: File? = null
                        onProgressDetail?.let { it("Checking Backup File") }
                        while (zipEntry != null) {
                            onProgressDetail?.let { it("Reading.") }
                            val bufferedInputStream = BufferedInputStream(zipInputStream)
                            val bufferedOutputStream =
                                if (zipEntry.name.contains(Params.FOLDER_DEVICE)) {
                                    deviceFile = File(context.cacheDir, zipEntry.name)
                                    BufferedOutputStream(FileOutputStream(deviceFile))
                                } else if (zipEntry.name.contains(Params.FOLDER_CLOUD)) {
                                    cloudFile = File(context.cacheDir, zipEntry.name)
                                    BufferedOutputStream(FileOutputStream(cloudFile))
                                } else if (zipEntry.name.contains(Params.FILE_SECURITY)) {
                                    securityBase = File(context.cacheDir, zipEntry.name)
                                    BufferedOutputStream(FileOutputStream(securityBase))
                                } else {
                                    null
                                }
                            onProgressDetail?.let { it("Reading..") }
                            bufferedOutputStream?.use { bos ->
                                val bytes = ByteArray(1024)
                                var read: Int
                                while ((bufferedInputStream.read(bytes)
                                        .also { read = it }) != -1
                                ) {
                                    bos.write(bytes, 0, read)
                                }
                            }
                            SmartNotify.log("Import:${zipEntry.name}", TAG)
                            onProgressDetail?.let { it("Reading...") }
                            zipEntry = zipInputStream.nextEntry
                        }
                        securityBase?.let {
                            if (isValidSecurityFile(uid, it).not()) {
                                onFailure?.let { it1 ->
                                    it1(
                                        "Database is not linked with this User-Account",
                                        null
                                    )
                                }
                                onProgressDetail?.let { it("Backup File is not linked with this User-Account.") }
                                onComplete?.run()
                                return@runCatching
                            }
                            deviceFile?.let { it1 ->
                                onProgressDetail?.let { it("Settling Device Data....") }
                                device.importDB(
                                    it1,
                                    Params.FOLDER_DEVICE,
                                    onProgressDetail = onProgressDetail
                                )
                            }
                            cloudFile?.let { it1 ->
                                onProgressDetail?.let { it("Settling Cloud Data....") }
                                cloud.importDB(
                                    it1,
                                    Params.FOLDER_CLOUD,
                                    onProgressDetail = onProgressDetail
                                )
                            }
                        }
                        onProgressDetail?.let { it("Import Complete.") }
                        onSuccess?.let { it(null) }
                        onComplete?.run()
                    }
            }.onFailure {
                it.printStackTrace()
                onFailure?.let { it1 -> it1(null, Exception(it)) }
                onComplete?.run()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                 task()
            }.onFailure { it.printStackTrace() }
        }
    }

    class Security {
        companion object {
            fun encryptFile(userId: String, file: File, encryptedFile: File): File {
                val secretKey = generateSecretKeyFromUserId(userId)

                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"))

                val fileInputStream = FileInputStream(file)
                val fileSize = fileInputStream.available()
                val fileBytes = ByteArray(fileSize)
                fileInputStream.read(fileBytes)
                fileInputStream.close()

                val encryptedFileBytes = cipher.doFinal(fileBytes)

                val encryptedFileOutputStream = FileOutputStream(encryptedFile)
                encryptedFileOutputStream.write(encryptedFileBytes)
                encryptedFileOutputStream.close()

                return encryptedFile
            }

            fun decryptFile(userId: String, inputStream: InputStream, decryptedFile: File): File {
                val secretKey = generateSecretKeyFromUserId(userId)

                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"))

                val fileSize = inputStream.available()
                val encryptedFileBytes = ByteArray(fileSize)
                inputStream.read(encryptedFileBytes)
                inputStream.close()

                val decryptedFileBytes = cipher.doFinal(encryptedFileBytes)

                val decryptedFileOutputStream = FileOutputStream(decryptedFile)
                decryptedFileOutputStream.write(decryptedFileBytes)
                decryptedFileOutputStream.close()

                return decryptedFile
            }

            private fun generateSecretKeyFromUserId(userId: String): ByteArray {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(userId.toByteArray())
                return digest.digest()
            }

        }
    }

}