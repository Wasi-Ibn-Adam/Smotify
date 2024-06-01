package com.lassanit.smotify.bases

import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase.Ads.DEFAULTS.TRAIL_TIME
import com.lassanit.smotify.bases.SharedBase.Ads.removeTempId
import com.lassanit.smotify.classes.SortWith.Chooser

class SharedBase {

    companion object {
        private const val TAG_USER = "last-uid"

        fun onLogOut() {
            runCatching {
                CloudService.reset()
                Ads.reset()
                PhoneService.reset()
                Visibility.reset()
                Settings.Edit.reset()
            }
        }

        fun setUser(uid: String?) {
            SmartNotify.getPref().edit().putString(TAG_USER, uid).apply()
        }

        fun getLastUser(): String? {
            return SmartNotify.getPref().getString(TAG_USER, null)
        }

        private fun getState(tag: String, default: Boolean): Boolean {
            return SmartNotify.getPref().getBoolean("shared_base <<>> $tag", default)
        }

        private fun setState(tag: String, active: Boolean) {
            SmartNotify.getPref().edit().putBoolean("shared_base <<>> $tag", active).apply()
        }

        private fun toggleState(tag: String, default: Boolean) {
            setState(tag, getState(tag, default).not())
        }
    }

    object PhoneService {
        private const val TAG = "phone_service::active"

        private object DEFAULT {
            const val ACTIVE = true
        }

        fun isActive(): Boolean {
            return getState(TAG, DEFAULT.ACTIVE)
        }

        fun setActive(active: Boolean) {
            setState(TAG, active)
        }

        fun toggleState() {
            Companion.toggleState(TAG, DEFAULT.ACTIVE)
        }

        fun reset() {
            setState(TAG, DEFAULT.ACTIVE)
        }
    }

    object CloudService {
        private const val TAG = "cloud_service::active"
        private const val TAG_IDS = "cloud_service::temp_ids"

        private object DEFAULT {
            const val SYNC_RESET_TIME = 450000 //  7.5 min
            const val SYNC_WRITE_TIME = 1050000 // 17.5 min
        }


        fun reset() {
            setTime(0)
            WriteSync.setLastMsgSyncedTime(0)
            removeTempId()
        }

        fun isActive(): Boolean {
            val curr = System.currentTimeMillis()
            return SmartNotify.getPref().getLong(TAG, curr) > curr
        }

        fun getTime(): Long {
            return SmartNotify.getPref().getLong(TAG, System.currentTimeMillis())
        }

        fun setTime(time: Long) {
            SmartNotify.getPref().edit().putLong(TAG, time).apply()
        }


        ///////////////////////////////////////////////////////////////////////////////////////

        object WriteSync {
            private const val TAG_SYNC = "cloud_service::WriteSync::sync"
            private const val TAG_SYNC_TIME = "cloud_service::WriteSync::sync_time"
            private const val TAG_SYNC_MSG_TIME = "cloud_service::WriteSync::sync_msg_time"
            private const val TAG_SYNC_MEDIA_ID = "cloud_service::WriteSync::sync_media_id"
            fun isSyncAllowed(): Boolean {
                val cur = System.currentTimeMillis()
                val expected = getExpectedSyncTime(cur)
                return (expected <= cur).and(isSyncing().not())
            }

            fun syncStarted() {
                SmartNotify.getPref().edit().putBoolean(TAG_SYNC, true).apply()
            }

            fun isSyncing(): Boolean {
                return SmartNotify.getPref().getBoolean(TAG_SYNC, false)
            }

            fun allowedSyncReset(): Boolean {
                val def = System.currentTimeMillis()
                val ex = getExpectedSyncTime(def)
                if (def == ex)
                    return true
                return ((ex.plus(DEFAULT.SYNC_RESET_TIME)) < def)
            }


            fun syncFailed() {
                SmartNotify.getPref().edit().putBoolean(TAG_SYNC, false).apply()
            }

            fun syncComplete() {
                SmartNotify.getPref().edit()
                    .putBoolean(TAG_SYNC, false)
                    .putLong(
                        TAG_SYNC_TIME,
                        System.currentTimeMillis().plus(DEFAULT.SYNC_WRITE_TIME)
                    )
                    .apply()
            }

            fun getExpectedSyncTime(def: Long = System.currentTimeMillis()): Long {
                return SmartNotify.getPref().getLong(TAG_SYNC_TIME, def)
            }

            fun getLastMsgSyncedTime(): Long {
                return SmartNotify.getPref().getLong(TAG_SYNC_MSG_TIME, 0L)
            }

            fun setLastMsgSyncedTime(time: Long) {
                SmartNotify.getPref().edit().putLong(TAG_SYNC_MSG_TIME, time).apply()
            }

            fun getLastMediaId(): Long {
                return SmartNotify.getPref().getLong(TAG_SYNC_MEDIA_ID, -1L)
            }

            fun setLastMediaId(id: Long) {
                SmartNotify.getPref().edit().putLong(TAG_SYNC_MEDIA_ID, id).apply()
            }
        }

        object ReadSync {
            private const val TAG_SYNC = "cloud_service::ReadSync::sync"

            fun syncStarted() {
                SmartNotify.getPref().edit().putBoolean(TAG_SYNC, true).apply()
            }

            fun isSyncing(): Boolean {
                return SmartNotify.getPref().getBoolean(TAG_SYNC, false)
            }

            fun syncCompleted() {
                SmartNotify.getPref().edit().putBoolean(TAG_SYNC, false).apply()
            }

        }

        object Purchase {
            fun setTempId(planId: String?) {
                runCatching {
                    SmartNotify.getPref().edit().putString(TAG_IDS, planId).apply()
                }
            }

            fun getTempId(): String? {
                return runCatching {
                    SmartNotify.getPref().getString(TAG_IDS, null)
                }.getOrNull()
            }

            fun removeTempId() {
                setTempId(null)
            }
        }
    }

    object Apps {
        private const val TAG = "apps"

        private object DEFAULT {
            const val ACTIVE = true
        }

        private fun getTag(pkg: String): String {
            return TAG.plus(' ').plus(pkg.replace(".", "_"))
        }

        fun isAppActive(pkg: String): Boolean {
            return getState(getTag(pkg), DEFAULT.ACTIVE)
        }

        fun setAppActive(pkg: String, active: Boolean) {
            setState(getTag(pkg), active)
        }

        fun toggleAppState(pkg: String) {
            toggleState(getTag(pkg), DEFAULT.ACTIVE)
        }
    }

    object Ads {
        private const val TAG = "ads::active"
        private const val TAG_TRAIL = "ads::trail"
        private const val TAG_IDS = "ads::temp_id"

        private object DEFAULTS {
            const val TRAIL_TIME = 24 * 3600_000L
        }

        fun reset() {
            setTime(0)
            setTrialTime(0)
            removeTempId()
        }

        fun isActive(): Boolean {
            val curr = System.currentTimeMillis()
            return SmartNotify.getPref().getLong(TAG, curr) > curr
        }

        fun getTime(): Long {
            return SmartNotify.getPref().getLong(TAG, System.currentTimeMillis())
        }

        fun setTime(time: Long) {
            SmartNotify.getPref().edit().putLong(TAG, time).apply()
        }

        fun setTempId(planId: String?) {
            SmartNotify.getPref().edit().putString(TAG_IDS, planId).apply()
        }

        fun getTempId(): String? {
            return SmartNotify.getPref().getString(TAG_IDS, null)
        }

        fun removeTempId() {
            setTempId(null)
        }

        fun isTrailActive(): Boolean {
            val curr = System.currentTimeMillis()
            return SmartNotify.getPref().getLong(TAG_TRAIL, curr) > curr
        }

        fun getTrailTime(): Long {
            return SmartNotify.getPref().getLong(TAG_TRAIL, System.currentTimeMillis())
        }

        private fun setTrialTime(time: Long) {
            SmartNotify.getPref().edit().putLong(TAG_TRAIL, time).apply()
        }

        fun setTrail() {
            setTrialTime(System.currentTimeMillis() + TRAIL_TIME)
        }

        fun earnOneHour() {
            val time = Chooser.greater(getTime(),System.currentTimeMillis())
            setTime(time.plus(3600_000))
        }

    }

    object Visibility {
        private const val TAG_TYPE = "<<>>  TYPE  <<>>"

        const val NORMAL = 1
        const val SECRET = 0

        private const val PHONE = 0
        private const val CLOUD = 1

        private object DEFAULT {
            const val CLOUD_PHONE = true
            const val NORMAL_SECRET = true
        }

        fun reset() {
            Phone.setNormalVsSecretState(DEFAULT.NORMAL_SECRET)
            setCloudVsPhoneState(DEFAULT.CLOUD_PHONE)
        }

        fun toggleCloudVsPhoneState() {
            toggleState(TAG_TYPE, DEFAULT.CLOUD_PHONE)
        }

        private fun setCloudVsPhoneState(phone: Boolean) {
            setState(TAG_TYPE, phone)
        }

        fun isCloud(): Boolean {
            return isCloudOrPhone() == CLOUD
        }

        fun isPhone(): Boolean {
            return isCloudOrPhone() == PHONE
        }

        fun isCloudOrPhone(): Int {
            return if (getState(TAG_TYPE, DEFAULT.CLOUD_PHONE) == DEFAULT.CLOUD_PHONE)
                PHONE
            else
                CLOUD
        }

        object Phone {

            private const val TAG_APPS = "<<>>  APP  <<>>"

            fun isNormalOrSecret(): Int {
                return if (getState(TAG_APPS, DEFAULT.NORMAL_SECRET))
                    NORMAL
                else
                    SECRET
            }

            fun isNormalNotifications(): Boolean {
                return isNormalOrSecret() == NORMAL
            }

            fun isSecretNotifications(): Boolean {
                return isNormalOrSecret() == SECRET
            }

            fun setNormalVsSecretState(normal: Boolean) {
                setState(TAG_APPS, normal)
            }

            fun toggleNormalVsSecretState() {
                toggleState(TAG_APPS, DEFAULT.NORMAL_SECRET)
            }
        }
    }

    object Settings {
        private object DEFAULT {
            const val CLOUD_SHARE = true
            const val MEDIA_SHOW = false
            const val MEDIA_CLICK = true
            const val MEDIA_SAVE = true
            const val MESSAGE_SCROLL = true
        }

        private object CONST {
            const val MEDIA_SAVE = "MEDIA_SAVE"
            const val MEDIA_SHOW_APP_HEADER = "MEDIA_SHOW_APP_HEADER"
            const val MEDIA_CLICK_APP_HEADER = "MEDIA_CLICK_APP_HEADER"
            const val MESSAGE_MIN_COUNT_APP_HEADER = "MESSAGE_MIN_COUNT_APP_HEADER"
            const val MESSAGE_ADD_SCROLL_APP_HEADER = "MESSAGE_ADD_SCROLL_APP_HEADER"
            const val DATABASE_FILE_ID = "DATABASE_FILE_ID"
            const val CLOUD_SHARE = "CLOUD_SHARE"

        }

        object Display {
            fun getMinMessagesInAppHeader(): Int {
                return SmartNotify.getPref().getInt(CONST.MESSAGE_MIN_COUNT_APP_HEADER, 3)
            }

            fun isMediaShowInAppHeaderAllowed(): Boolean {
                return getState(CONST.MEDIA_SHOW_APP_HEADER, DEFAULT.MEDIA_SHOW)
            }

            fun isMediaClickInAppHeaderAllowed(): Boolean {
                return getState(CONST.MEDIA_CLICK_APP_HEADER, DEFAULT.MEDIA_CLICK)
            }

            fun isMessageAddedScrollAllowed(): Boolean {
                return getState(CONST.MESSAGE_ADD_SCROLL_APP_HEADER, DEFAULT.MESSAGE_SCROLL)
            }

            fun isMediaSavingAllowed(): Boolean {
                return getState(CONST.MEDIA_SAVE, DEFAULT.MEDIA_SAVE)
            }

            fun isCloudSharingAllowed(): Boolean {
                return getState(CONST.CLOUD_SHARE, DEFAULT.CLOUD_SHARE)
            }

            fun getDatabaseId(): String {
                return SmartNotify.getPref().getString(CONST.DATABASE_FILE_ID, "").toString()
            }

        }

        object Edit {
            fun reset() {
                setState(CONST.MEDIA_SHOW_APP_HEADER, DEFAULT.MEDIA_SHOW)
                setState(CONST.MEDIA_CLICK_APP_HEADER, DEFAULT.MEDIA_CLICK)
                setState(CONST.MESSAGE_ADD_SCROLL_APP_HEADER, DEFAULT.MESSAGE_SCROLL)
                setState(CONST.MEDIA_SAVE, DEFAULT.MEDIA_SAVE)
                setState(CONST.CLOUD_SHARE, DEFAULT.CLOUD_SHARE)
            }

            fun toggleMediaClickInAppHeaderAllowed() {
                toggleState(CONST.MEDIA_CLICK_APP_HEADER, DEFAULT.MEDIA_CLICK)
            }

            fun toggleMediaSavingAllowed() {
                toggleState(CONST.MEDIA_SAVE, DEFAULT.MEDIA_SAVE)
            }

            fun toggleCloudSharingAllowed() {
                toggleState(CONST.CLOUD_SHARE, DEFAULT.CLOUD_SHARE)
            }

            fun toggleMediaShowInAppHeaderAllowed() {
                toggleState(CONST.MEDIA_SHOW_APP_HEADER, DEFAULT.MEDIA_SHOW)
            }

            fun toggleMessageAddedScrollAllowed() {
                toggleState(CONST.MESSAGE_ADD_SCROLL_APP_HEADER, DEFAULT.MESSAGE_SCROLL)
            }

            fun setMinMessagesInAppHeader(min: Int) {
                SmartNotify.getPref().edit().putInt(CONST.MESSAGE_MIN_COUNT_APP_HEADER, min).apply()
            }

            fun setDatabaseId(id: String) {
                SmartNotify.getPref().edit().putString(CONST.DATABASE_FILE_ID, id).apply()
            }
        }
    }
}