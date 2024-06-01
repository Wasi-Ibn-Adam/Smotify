package com.lassanit.smotify.services_receivers

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat.*
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.SmartNotify.Companion.log
import com.lassanit.smotify.bases.CloudBase
import com.lassanit.smotify.bases.EditBaseHelper
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.handlers.ServiceManager
import com.lassanit.smotify.handlers.SmartMessageManager
import com.lassanit.smotify.handlers.SmartMessageManager.Companion.FOREGROUND_NOTIFICATION_ID
import com.lassanit.smotify.interfaces.ExternalImps


class SmartPhoneService : NotificationListenerService() {
    private lateinit var maker: SmartMessageManager
    private lateinit var cloudStore: CloudBase.Store

    companion object {
        private var imp: ExternalImps.SmartPhoneServiceImp? = null
        private var impCloud: ExternalImps.SmartCloudServiceImp? = null
        private var hasMsg: Boolean = false
        private val actionMap = HashMap<Int, PendingIntent?>()
        private val idMap = HashMap<Pair<String, Int>, Pair<Int, Int>>()
        private var makeStack: Boolean = false

        fun connect(handler: ExternalImps.SmartPhoneServiceImp, stack: Boolean = false) {
            makeStack = stack
            imp = handler
            if (hasMsg) {
                imp?.hasOldMsg()
                hasMsg = false
            }
        }

        fun disconnect() {
            imp = null
        }

        fun getCloud(): ExternalImps.SmartCloudServiceImp? {
            return impCloud
        }

        fun connectCloud(handler: ExternalImps.SmartCloudServiceImp) {
            impCloud = handler
        }

        fun disconnectCloud() {
            impCloud = null
        }

        private fun addAction(mid: Int, action: PendingIntent?) {
            actionMap[mid] = action
        }

        private fun removeAction(intent: PendingIntent) {
            runCatching {
                for (item in actionMap.entries) {
                    if (item.value == intent) {
                        actionMap.remove(item.key)
                        break
                    }
                }
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        fun getAction(mid: Int): PendingIntent? {
            return actionMap[mid]
        }

        private fun addNotificationId(pkg: String, id: Int, type: Int, count: Int) {
            idMap[Pair(pkg, id)] = Pair(type, count)
        }

        private fun removeNotificationId(pkg: String, id: Int) {
            runCatching { idMap.remove(Pair(pkg, id)) }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        private fun getNotificationIdType(pkg: String, id: Int): Pair<Int, Int> {
            return idMap[Pair(pkg, id)] ?: Pair(-1, -1)
        }

        private fun hasNotificationId(pkg: String, id: Int): Boolean {
            return idMap.contains(Pair(pkg, id))
        }

    }

    override fun onCreate() {
        super.onCreate()
        log("onCreate")
        runCatching {
            maker = SmartMessageManager(this)
            maker.createNotificationChannel()
            maker.updateForegroundNotification()
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
        runCatching {
            Firebase.auth.currentUser?.uid?.let {
                if (::cloudStore.isInitialized.not())
                    cloudStore = CloudBase.Store(applicationContext, it)
                cloudStore.requestReadLink()
                cloudStore.requestWriteLink()
            }
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        log("onListenerConnected")
        runCatching {
            startForeground(FOREGROUND_NOTIFICATION_ID, maker.getForegroundNotification())
            for (sbn in activeNotifications) {
                onNotificationReceived(sbn)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) for (sbn in snoozedNotifications) {
                onNotificationReceived(sbn)
            }
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }

        runCatching {
            if (SmartNotify.isUser()) {
                if (SharedBase.CloudService.isActive()) Firebase.auth.currentUser?.uid?.let {
                    CloudBase.Store.set(
                        it
                    )
                }
            }
        }.onFailure {
            it.printStackTrace()
            runCatching {
                Firebase.crashlytics.recordException(it)
            }
        }

    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        log("onListenerDisconnected")
        runCatching {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching { ServiceManager.slowSetup(applicationContext) }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching { onNotificationReceived(sbn) }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
        runCatching {
            cloudStore.requestReadLink()
            cloudStore.requestWriteLink()
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        runCatching {
            sbn?.notification?.contentIntent?.let { removeAction(it) }
            sbn?.id?.let { removeNotificationId(sbn.packageName, it) }
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun onNotificationReceived(sbn: StatusBarNotification) {
        runCatching {
            // if notification service/app is active (within smart notify)

            if (!SharedBase.PhoneService.isActive() || !SharedBase.Apps.isAppActive(sbn.packageName)) return

            // if notification is system app (ignore system app)
            if (!Utils.isSystemApp(applicationContext, sbn.packageName)) return

            // if notification is ongoing notifications (music / download / calls ) are not being cater
            if (sbn.isOngoing) return

            //   if (shouldStop(sbn)) return
            val template =
                sbn.notification.extras.getString(Notification.EXTRA_TEMPLATE).toString().trim()

            val ret = handleTemplate(template, sbn)
            if (ret.first.not()) {
                Firebase.crashlytics.recordException(
                    Exception("This Style is Not Handled ::$template ::detected temp ${ret.second} :: $sbn")
                )
                handleTemplate("repeat", sbn)
            }
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }
    }

    private fun handleTemplate(template: String, sbn: StatusBarNotification): Pair<Boolean, Int> {
        return runCatching {
            log("${template.replace("android.app.Notification", "")} $sbn", "-sbn-")
            val ret = when (template) {
                MessagingStyle::class.java.name, Notification.MessagingStyle::class.java.name -> {
                    val ret = MessageStyleHandler.handle(applicationContext, maker, sbn)
                    Pair(ret, AppMessage.Types.TYPE_MESSAGE)
                }

                InboxStyle::class.java.name, Notification.InboxStyle::class.java.name -> {
                    val ret =
                        InboxStyleHandler.handle(applicationContext, maker, sbn)
                    Pair(ret, AppMessage.Types.TYPE_INBOX)
                }

                BigPictureStyle::class.java.name, Notification.BigPictureStyle::class.java.name -> {
                    val ret = BigPictureStyleHandler.handle(
                        applicationContext,
                        maker,
                        sbn
                    )
                    Pair(ret, AppMessage.Types.TYPE_BIG_PICTURE)
                }

                BigTextStyle::class.java.name, Notification.BigTextStyle::class.java.name -> {
                    val ret = BigTextStyleHandler.handle(
                        applicationContext, maker, sbn
                    )
                    Pair(ret, AppMessage.Types.TYPE_BIG_TEXT)
                }

                CallStyle::class.java.name -> {
                    val ret = DefaultStyleHandler.handle(
                        applicationContext, maker, sbn, AppMessage.Types.TYPE_CALL
                    )
                    Pair(ret, AppMessage.Types.TYPE_CALL)
                }

                Notification.MediaStyle::class.java.name -> {

                    Pair(false, AppMessage.Types.TYPE_MEDIA)
                }

                DecoratedCustomViewStyle::class.java.name, Notification.DecoratedCustomViewStyle::class.java.name -> {
                    val ret = DefaultStyleHandler.handle(
                        applicationContext, maker, sbn, AppMessage.Types.TYPE_DECORATE_CUSTOM
                    )
                    Pair(ret, AppMessage.Types.TYPE_DECORATE_CUSTOM)
                }

                CarExtender::javaClass.name, Notification.CarExtender::javaClass.name -> {
                    val ret = DefaultStyleHandler.handle(
                        applicationContext, maker, sbn, AppMessage.Types.TYPE_CAR
                    )
                    Pair(ret, AppMessage.Types.TYPE_CAR)
                }

                WearableExtender::javaClass.name, Notification.WearableExtender::javaClass.name -> {
                    val ret = DefaultStyleHandler.handle(
                        applicationContext, maker, sbn, AppMessage.Types.TYPE_WEARABLE
                    )
                    Pair(ret, AppMessage.Types.TYPE_WEARABLE)
                }

                "repeat" -> {
                    val ret = DefaultStyleHandler.handle(
                        applicationContext, maker, sbn, AppMessage.Types.TYPE_REPEAT
                    )
                    Pair(ret, AppMessage.Types.TYPE_REPEAT)
                }

                else -> {
                    val ret = DefaultStyleHandler.handle(applicationContext, maker, sbn)
                    Pair(ret, AppMessage.Types.TYPE_NULL)
                }
            }
            if (ret.first)
                addNotificationId(
                    sbn.packageName,
                    sbn.id,
                    ret.second,
                    sbn.notification.number
                )
            ret
        }.onFailure {
            it.printStackTrace()
            Firebase.crashlytics.recordException(it)
        }.getOrElse { Pair(false, -3) }
    }

    object Utils {

        fun isSystemApp(context: Context, pkg: String): Boolean {
            return "android" != pkg && !context.packageName.equals(pkg)
        }

        fun isEmpty(str: String?): Boolean {
            return (str.isNullOrEmpty() || str.equals("null", true))
        }

        fun isSame(s1: String?, s2: String?, ignore: Boolean = true): Boolean {
            return ((isEmpty(s1).and(isEmpty(s2))) || (s1.equals(s2, ignore)))
        }

        @Suppress("DEPRECATION")
        fun getBitmap(context: Context, extras: Bundle): Bitmap? {
            return runCatching {
                var map: Parcelable? = extras.getParcelable(EXTRA_PICTURE)
                if (map == null) {
                    map = extras.getParcelable(EXTRA_PICTURE_ICON)
                }
                if (map != null) {
                    if (map is Icon) {
                        return iconToBitmap(context, map)
                    }
                    if (map is Bitmap) {
                        return map
                    }
                    if (map is Drawable) {
                        return map.toBitmap()
                    }
                }
                return null
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrNull()
        }

        fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
            return runCatching {
                val drawable = icon.loadDrawable(context)
                val bitmap = drawable?.let {
                    Bitmap.createBitmap(
                        it.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                    )
                }
                val canvas = bitmap?.let { Canvas(it) }
                canvas?.let { drawable.setBounds(0, 0, it.width, canvas.height) }
                canvas?.let { drawable.draw(it) }
                bitmap
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrNull()
        }

        fun getIcon(context: Context, notification: Notification): Bitmap? {
            return runCatching {
                if (notification.getLargeIcon() != null) return iconToBitmap(
                    context,
                    notification.getLargeIcon()
                )
                else if (notification.publicVersion != null) return getIcon(
                    context,
                    notification.publicVersion
                )
                return null
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrNull()

        }

        fun injectionClearance(str: String): String {
            return str.replace(":", "").replace("|", "").replace(",", "").replace("@", "")
                .replace("~", "").replace("*", "").trim()
        }
    }

    object StyleHandler {
        private var lastMsg: AppMessage? = null
        fun isDuplicate(msg: AppMessage): Boolean {
            return runCatching {
                val b = (lastMsg != null).and(Utils.isSame(lastMsg?.title, msg.title))
                    .and(Utils.isSame(lastMsg?.text, msg.text))
                    .and(Utils.isSame(lastMsg?.pkg, msg.pkg)).and(lastMsg?.time == msg.time)

                lastMsg = msg
                return b
            }.onFailure { it.printStackTrace() }.getOrElse { true }
        }

        fun isDuplicateMessaging(msg: AppMessage): Boolean {
            return runCatching {
                val b = (lastMsg != null).and(Utils.isSame(lastMsg?.title, msg.title))
                    .and(Utils.isSame(lastMsg?.text, msg.text))
                    .and(Utils.isSame(lastMsg?.pkg, msg.pkg, false))
                    .and(lastMsg?.hasMedia == msg.hasMedia).and(lastMsg?.time == msg.time)
                    .and(lastMsg?.type != msg.type)
                lastMsg = msg
                return b
            }.onFailure { it.printStackTrace() }.getOrElse { true }
        }

        fun isDuplicateInbox(p0: Pair<String, Int>, count: Int): Boolean {
            val idType = getNotificationIdType(p0.first, p0.second)
            return if (idType.first == AppMessage.Types.TYPE_INBOX) (idType.second == count)
            else true
        }

        fun isDuplicateNull(p0: Pair<String, Int>, count: Int): Boolean {
            log("${p0.first} ${p0.second}  $count ", "counter")
            return hasNotificationId(p0.first, p0.second).and(1 == count)
        }

        fun styleFound(
            maker: SmartMessageManager,
            pkg: String,
            title: String,
            mid: Int = -1,
            action: PendingIntent? = null
        ) {
            runCatching {
                imp?.onMsgAdded(pkg)
                if (imp == null || makeStack) {
                    hasMsg = true
                }
                maker.addApp(pkg)
                maker.updateForegroundNotification()
                if (mid != -1) addAction(mid, action)
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }
    }

    object MessageStyleHandler {
        private var style: MessagingStyle? = null
        private var time = 0L
        private var added: Boolean = false
        private var pkg: String? = null
        private var icon: Bitmap? = null
        private var title: String? = null
        private var action: PendingIntent? = null
        private fun validMessage(): Boolean {
            return !(Utils.isEmpty(pkg) || Utils.isEmpty(title) || style == null || style?.messages == null || style?.messages?.size == 0)
        }

        private fun filterTitle(title: String?, subTitle: String?): String {
            if (Utils.isEmpty(title)) return ""
            if (Utils.isEmpty(subTitle)) return title!!.trim()

            val i = title!!.lastIndexOf(subTitle!!)
            if (title != subTitle && i != -1) return title.substring(0, i).trim()
            return title.trim()
        }

        // in message-style
        // title, subText
        private fun filterMessageTitle(title: String, subTitle: String?): Pair<String, String?> {
            return runCatching {
                var newTitle: String = filterTitle(title, subTitle)
                val regex = Regex("\\(\\d+\\s?(new\\s?)?messages?\\)")
                var subText: String? = null
                if (newTitle.contains(regex)) {
                    subText = regex.findAll(newTitle).first().value.trim()
                    newTitle = newTitle.replaceFirst(regex, "").trim()
                }
                return Pair(newTitle, subText)
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { Pair(title, null) }
        }

        private fun iconGeneration(context: Context) {
            runCatching {
                icon = style?.user?.icon?.toIcon(context)?.loadDrawable(context)?.toBitmap()
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        private fun handleMessages(
            context: Context,
            sbnData: EditBaseHelper.SbnData,
        ) {
            runCatching {
                val last = style?.messages?.last()!!
                val pair = filterMessageTitle(title!!, last.person?.name.toString())
                if (title != null && pair.first.equals("you", ignoreCase = true)) {
                    title = style?.messages?.first()?.person?.name.toString()
                    log("you message: $title ,  ${pair.first} ")
                } else title = Utils.injectionClearance(pair.first).trim()
                val subtext = pair.second?.let { Utils.injectionClearance(it) }

                val db = EditBaseHelper.get(context)

                for ((cur, msg) in style!!.messages.withIndex()) {
                    val subtitle = Utils.injectionClearance(msg.person?.name.toString())
                    val text = msg.text.toString()

                    if (msg.person != null && msg.person!!.icon != null) {
                        icon = msg.person?.icon?.toIcon(context)?.loadDrawable(context)?.toBitmap()
                    }
                    time = msg.timestamp
                    val smartMessage = AppMessage(
                        -1,
                        AppMessage.Types.TYPE_MESSAGE,
                        pkg!!,
                        title!!,
                        subtitle.trim(),
                        text.trim(),
                        subtext?.trim(),
                        time,
                        icon = icon
                    )
                    log(
                        "${msg.timestamp}, ${msg.dataMimeType}, ${msg.dataUri}, ", "SERVICE"
                    )

                    if (StyleHandler.isDuplicateMessaging(smartMessage)) {
                        added = false
                        continue
                    }

                    val mid = db.addMessagingStyle(
                        smartMessage,
                        sbnData,
                        msg.dataUri,
                        msg.dataMimeType,
                        Utils.getBitmap(context, msg.extras),
                        imp
                    )

                    log("${style!!.messages.size}, ${cur.plus(1)}, $mid", "SERVICE")

                    if (mid > -1) {
                        added = true
                        addAction(mid, action)
                    }
                }
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        private fun detail(extras: Bundle): String {
            val keySet = extras.keySet()
            val builder = StringBuilder()
            for (key in keySet) {
                builder.append(key).append("::").append(extras.get(key)).appendLine()
            }
            builder.append("isByteArray: ")
            runCatching {
                val parcelableArray = extras.getParcelableArray("android.messages")
                if (parcelableArray != null) {
                    for (f in parcelableArray) {
                        builder.append(
                            when (f) {
                                is Bundle -> {
                                    "{" + detail(f) + "}\n"
                                }

                                else -> f.javaClass.name + " " + f.describeContents() + "\n"
                            }
                        )
                    }
                }
            }
            return builder.toString()
        }

        private fun handleSnapChatMessages(
            context: Context,
            maker: SmartMessageManager,
            sbn: StatusBarNotification
        ): Boolean {
            return runCatching {
                val style =
                    sbn.notification.extras.getParcelableArray("android.messages") ?: return false
                title = Utils.injectionClearance(title.toString()).trim()
                val db = EditBaseHelper.get(context)
                for (msg in style) {
                    if (msg is Bundle) {
                        val uri = msg.get("uri") as Uri?
                        val type = msg.getString("type")
                        val time = msg.getLong("time")
                        val text = msg.getString("text")
                        if (uri.toString().isBlank().or(type.isNullOrBlank())
                                .or(text.isNullOrBlank())
                        ) {
                            continue
                        }

                        val smartMessage = AppMessage(
                            -1,
                            AppMessage.Types.TYPE_MESSAGE,
                            pkg!!,
                            title!!,
                            null,
                            text!!.trim(),
                            null,
                            time,
                            icon = Utils.getIcon(context, sbn.notification)
                        )
                        log(
                            "$smartMessage", "SERVICE"
                        )
                        if (StyleHandler.isDuplicateMessaging(smartMessage)) {
                            added = false
                            continue
                        }
                        val mid = db.addMessagingStyle(smartMessage, null, uri, type, null, imp)

                        if (mid > -1) {
                            added = true
                            addAction(mid, sbn.notification.contentIntent)
                        }
                        if (added) {
                            StyleHandler.styleFound(maker, pkg!!, title!!)
                        }
                        return true
                    }
                }
                return false
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }

        fun handle(
            context: Context, maker: SmartMessageManager, sbn: StatusBarNotification
        ): Boolean {
            return runCatching {
                pkg = sbn.packageName
                style = MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
                time = sbn.notification.`when`
                if (time == 0L) time = sbn.postTime
                title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
                if (!validMessage()) {
                    return handleSnapChatMessages(context, maker, sbn)
                }

                iconGeneration(context)
                action = sbn.notification.contentIntent
                handleMessages(context, EditBaseHelper.SbnData(sbn.id.toString(), sbn.key))
                if (added) {
                    StyleHandler.styleFound(maker, pkg!!, title!!)
                }
                return true
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }
    }

    object InboxStyleHandler {
        private var time = 0L
        private var added: Boolean = false
        private var lines: Array<CharSequence>? = null
        private var pkg: String? = null
        private var icon: Bitmap? = null
        private var title: String? = null
        private var action: PendingIntent? = null

        private fun validMessage(): Boolean {
            return !(Utils.isEmpty(pkg) || Utils.isEmpty(title) || lines == null || lines?.size == 0)
        }

        // in inbox-style
        // text, subTitle
        private fun filterMessageText(text: String): Pair<String, String?> {
            return runCatching {
                var newText: String = text
                var subTitle: String? = null
                if (text.contains(":")) {
                    val i = text.indexOf(":")
                    if (text.contains("(") && text.contains(")")) {
                        val s = text.indexOf("(")
                        val e = text.indexOf(")")
                        if (i in s until e) {
                            return Pair(newText, null)
                        }
                    }
                    subTitle = text.substring(0, i).trim()
                    newText = text.substring(i + 1)
                }
                return Pair(newText, subTitle)
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { Pair(text, null) }
        }


        private fun oneMsg(
            context: Context,
            sbnData: EditBaseHelper.SbnData,
            image: Bitmap?,
            line: String,
            id: Int,
            number: Int
        ) {
            runCatching {
                var subTitle: String? = null
                var text: String?
                if (line.contains("@")) {  // whatsapp @
                    val tokens = line.split("@")
                    subTitle = Utils.injectionClearance(tokens[0])
                    text = if (tokens[1].indexOf(title!!) == 0) tokens[1].replaceFirst(title!!, "")
                    else tokens[1]
                    val pair = filterMessageText(text)
                    text = pair.first
                    if (pair.second != null) {
                        subTitle = Utils.injectionClearance(pair.second!!)
                        if (Utils.isEmpty(subTitle).not()) {
                            val appName = EditBaseHelper.get(context).getAppName(pkg!!)?.trim()
                            if (Utils.isEmpty(appName).not() && title == appName) {
                                title = subTitle
                            }
                        }
                    }
                } else {
                    text = line
                }
                val smartMessage = AppMessage(
                    -1,
                    AppMessage.Types.TYPE_INBOX,
                    pkg!!,
                    title!!,
                    subTitle?.trim(),
                    text.trim(),
                    null,
                    time,
                    true,
                    image != null,
                    icon
                )

                if (StyleHandler.isDuplicate(smartMessage)) {
                    added = false
                    return
                }
                if (StyleHandler.isDuplicateInbox(
                        Pair(smartMessage.pkg, id), number
                    )
                ) {
                    added = false
                    return
                }
                val mid = EditBaseHelper.get(context).add(smartMessage, sbnData, image, imp)
                addAction(mid, action)
                added = (mid >= 0)
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        private fun handleMessages(
            context: Context, sbnData: EditBaseHelper.SbnData, extra: Bundle, id: Int, number: Int
        ) {
            runCatching {
                val image = Utils.getBitmap(context, extra)
                oneMsg(context, sbnData, image, lines!!.last().toString(), id, number)
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }
        }

        fun handle(
            context: Context, maker: SmartMessageManager, sbn: StatusBarNotification
        ): Boolean {
            return runCatching {
                pkg = sbn.packageName
                title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString()
                if (Utils.isEmpty(title)) title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()

                lines = sbn.notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

                time = sbn.notification.`when`
                if (time == 0L) time = sbn.postTime

                if (!validMessage()) return false

                icon = Utils.getIcon(context, sbn.notification)
                action = sbn.notification.contentIntent

                handleMessages(
                    context,
                    EditBaseHelper.SbnData(sbn.id.toString(), sbn.key),
                    sbn.notification.extras,
                    sbn.id,
                    sbn.notification.number
                )

                if (added) {
                    StyleHandler.styleFound(maker, pkg!!, title!!)
                }
                return true
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }

    }

    object BigPictureStyleHandler {
        private var time = 0L
        private var text: String? = null
        private var pkg: String? = null
        private var icon: Bitmap? = null
        private var title: String? = null
        private fun validMessage(): Boolean {
            return !(Utils.isEmpty(pkg) || Utils.isEmpty(title) || Utils.isEmpty(text))
        }

        fun handle(
            context: Context, maker: SmartMessageManager, sbn: StatusBarNotification
        ): Boolean {
            return runCatching {
                pkg = sbn.packageName
                title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString()
                if (Utils.isEmpty(title)) title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()

                text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

                time = sbn.notification.`when`
                if (time == 0L) time = sbn.postTime

                if (!validMessage()) return false

                icon = Utils.getIcon(context, sbn.notification)
                val image = Utils.getBitmap(context, sbn.notification.extras)

                val hasMedia = (image == null).not()

                val smartMessage = AppMessage(
                    -1,
                    AppMessage.Types.TYPE_BIG_PICTURE,
                    pkg!!,
                    title!!,
                    null,
                    text!!.trim(),
                    null,
                    time,
                    true,
                    hasMedia,
                    icon
                )
                if (StyleHandler.isDuplicate(smartMessage)) return true

                val mid = EditBaseHelper.get(context)
                    .add(smartMessage, EditBaseHelper.SbnData(sbn.tag, sbn.key), image, imp)
                if (mid >= 0) {
                    StyleHandler.styleFound(
                        maker, pkg!!, title!!, mid, sbn.notification.contentIntent
                    )
                }
                return true
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }
    }

    object BigTextStyleHandler {
        private var time = 0L
        private var text: String? = null
        private var subtitle: String? = null
        private var subtxt: String? = null
        private var pkg: String? = null
        private var icon: Bitmap? = null
        private var title: String? = null
        private fun validMessage(): Boolean {
            return !(Utils.isEmpty(pkg) || Utils.isEmpty(title) || Utils.isEmpty(text))
        }


        fun handle(
            context: Context, maker: SmartMessageManager, sbn: StatusBarNotification
        ): Boolean {
            return runCatching {

                pkg = sbn.packageName
                title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString()
                if (Utils.isEmpty(title)) title =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()

                subtitle =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                text =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
                subtxt =
                    sbn.notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString()

                time = sbn.notification.`when`
                if (time == 0L) time = sbn.postTime

                if (!validMessage()) return false

                icon = Utils.getIcon(context, sbn.notification)

                var image = Utils.getBitmap(context, sbn.notification.extras)
                if (image == null) {
                    val barry = sbn.notification.extras.getBundle("system_notification_extras")
                    if (barry != null) image = Utils.getBitmap(context, barry)
                }

                val hasMedia = (image == null).not()
                val smartMessage = AppMessage(
                    -1,
                    AppMessage.Types.TYPE_BIG_TEXT,
                    pkg!!,
                    title!!,
                    subtitle,
                    text!!.trim(),
                    subtxt?.trim(),
                    time,
                    true,
                    hasMedia,
                    icon
                )
                if (StyleHandler.isDuplicate(smartMessage)) {
                    return true
                }
                val mid = EditBaseHelper.get(context)
                    .add(smartMessage, EditBaseHelper.SbnData(sbn.tag, sbn.key), image, imp)


                if (mid > -1) {
                    StyleHandler.styleFound(
                        maker, pkg!!, title!!, mid, sbn.notification.contentIntent
                    )
                }
                true
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }
    }

    object DefaultStyleHandler {
        private var time = 0L
        private var text: String? = null
        private var pkg: String? = null
        private var icon: Bitmap? = null
        private var title: String? = null
        private fun validMessage(): Boolean {
            return !(Utils.isEmpty(pkg) || Utils.isEmpty(title) || Utils.isEmpty(text))
        }

        fun handle(
            context: Context,
            maker: SmartMessageManager,
            sbn: StatusBarNotification,
            type: Int = AppMessage.Types.TYPE_NULL
        ): Boolean {
            return runCatching {
                pkg = sbn.packageName

                title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
                text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
                time = sbn.notification.`when`
                if (time == 0L) time = sbn.postTime

                if (!validMessage()) return false
                icon = Utils.getIcon(context, sbn.notification)
                val image =  Utils.getBitmap(context, sbn.notification.extras)
                val smartMessage = AppMessage(
                    -1,
                    type,
                    pkg!!,
                    title!!,
                    null,
                    text!!.trim(),
                    null,
                    time,
                    true,
                    (image != null),
                    icon
                )
                if (StyleHandler.isDuplicate(smartMessage)) {
                    return true
                }
                if (StyleHandler.isDuplicateNull(
                        Pair(smartMessage.pkg, sbn.id), sbn.notification.number
                    )
                ) {
                    return true
                }
                val mid = EditBaseHelper.get(context)
                    .add(smartMessage, EditBaseHelper.SbnData(sbn.tag, sbn.key), image, imp)
                if (mid > -1) {
                    StyleHandler.styleFound(
                        maker, pkg!!, title!!, mid, sbn.notification.contentIntent
                    )
                }

                true
            }.onFailure {
                it.printStackTrace()
                Firebase.crashlytics.recordException(it)
            }.getOrElse { false }
        }

    }

}