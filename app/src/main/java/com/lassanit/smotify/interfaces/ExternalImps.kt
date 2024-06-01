package com.lassanit.smotify.interfaces

interface ExternalImps {
    interface AppsImp {
        fun onPhoneAppAdded(pkg: String)
        fun onPhoneAppRemoved(pkg: String)
        fun onPhoneAppUpdated(pkg: String)
        fun hasPreviousData()
    }

    interface SmartPhoneServiceImp {
        fun onMsgAdded(pkg: String)
        fun onMsgUpdated(pkg: String,mid:Int)
        fun hasOldMsg()
    }

    interface SmartCloudServiceImp {
        fun onCloudMsgReceived(pkg: String)
    }

    interface ServiceManagerImp {
        fun serviceState(active:Boolean)
    }
}