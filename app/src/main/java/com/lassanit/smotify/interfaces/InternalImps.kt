package com.lassanit.smotify.interfaces

import android.graphics.Bitmap
import android.view.View
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppHeader
import com.lassanit.smotify.display.data.AppMessage
import com.lassanit.smotify.display.data.SmartMedia

interface InternalImps {

    interface ToolBarImp {
        fun onMenuExtend(view: View)
    }

    interface HomeAppImp {
        fun onAppClick(pos: Int, view: View, app: App)
        fun onAppLongClick(pos: Int, view: View, app: App)
    }

    interface HomeAppItemImp {
        fun onItem(on: Boolean)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    interface HomeNavExtImp : HomeNavMenuFragImp, HomeNavMenuActImp {}

    interface HomeNavMenuFragImp {
        fun onNavigationAllRead()
        fun onNavigationVisibilityToggle()
    }

    interface HomeNavMenuActImp {
        fun onSettings()
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////


    interface AppNavigationImp {
        fun onNavigationModeToggle(state: Boolean)
    }

    interface ExportImportImp {
        fun onComplete()
        fun onFailure()
        fun onLog(str: String)
    }

    interface AppDataAdapterImp : AppMessageAdapterImp, AppHeaderAdapterImp {

    }

    interface AppMessageAdapterImp {
        fun onItemClick(view: View, data: AppMessage, media: SmartMedia?)
        fun onItemLongClick(view: View, data: AppMessage)
        fun onItemIconClick(view: View, data: AppMessage, bitmap: Bitmap?)
        fun onItemMediaClick(view: View, data: AppMessage, media: SmartMedia?)
    }

    interface AppHeaderAdapterImp {
        fun onItemClick(view: View, data: AppHeader)
        fun onItemLongClick(view: View, data: AppHeader)
        fun onItemSelected(view: View, data: AppHeader)
        fun onItemSelectionActions(
            isActionAllowed:Boolean,
            hasUnread: Boolean,
            isSecretAllowed: Boolean,
            isNormalVisibility: Boolean
        )
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

}