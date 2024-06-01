package com.lassanit.smotify.classes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.bases.roomie.RoomBase
import com.lassanit.smotify.display.data.App
import com.lassanit.smotify.display.data.AppData
import com.lassanit.smotify.services_receivers.PhoneAppReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


open class Resource {
    fun resize(context: Context, drawable: Drawable, w: Int, h: Int): Drawable {
        // Convert dp values to pixels
        val density = context.resources.displayMetrics.density
        val desiredWidth = (w * density).toInt()
        val desiredHeight = (h * density).toInt()

        // Create a Bitmap from the original Drawable
        val originalBitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(originalBitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        // Create a scaled Bitmap with the desired width and height
        val scaledBitmap =
            Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, false)
        return BitmapDrawable(context.resources, scaledBitmap)
    }

    companion object {


        fun getTimeOrDate(long: Long): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val epsec = Date().toInstant().minusMillis(long).epochSecond
                if (epsec < 0) {
                    val time = Date(long)
                    return SimpleDateFormat("hh:mm aa, MM/dd/yyyy", Locale("en")).format(time)
                } else if (epsec < 3600) {
                    if (epsec < 1) {
                        return "now"
                    }

                    if (epsec < 60) {
                        return "$epsec sec"
                    }
                    val min = epsec / 60
                    return "$min minute${if (min > 1L) "s" else ""}"
                } else if (epsec < 86400) {
                    val hour = epsec / 3600
                    return "$hour hour${if (hour > 1L) "s" else ""}"
                }
            }
            val time = Date(long)
            return SimpleDateFormat("MM/dd/yyyy ", Locale("en")).format(time)
        }

        fun getTime(long: Long): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val epsec = Date().toInstant().minusMillis(long).epochSecond
                if (epsec < 3600) {
                    if (epsec < 1) {
                        return "now"
                    }

                    if (epsec < 60) {
                        return "$epsec sec"
                    }
                    val min = epsec / 60
                    return "$min minute${if (min > 1L) "s" else ""}"
                } else if (epsec < 86400) {
                    val hour = epsec / 3600
                    return "$hour hour${if (hour > 1L) "s" else ""}"
                }
            }
            val time = Date(long)
            return SimpleDateFormat("hh:mm aa, MM/dd/yyyy ", Locale("en")).format(time)
        }

        fun getTimeDetail(long: Long): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val epsec = Date().toInstant().minusMillis(long).epochSecond
                if (epsec < 3600) {
                    if (epsec < 1) {
                        return "now"
                    }
                    if (epsec < 60) {
                        return "$epsec sec"
                    }
                    val min = epsec / 60
                    return "$min minute${if (min > 1L) "s" else ""}"
                }
            }
            val time = Date(long)
            return SimpleDateFormat("hh:mm aa, MM/dd/yyyy ", Locale("en")).format(time)
        }

        fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }

        fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
            return runCatching {
                if (byteArray == null) return null
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }.getOrNull()
        }

        fun getAppNameFromPackageName(context: Context, packageName: String): String? {
            return runCatching {
                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrNull()
        }

        fun saveAppList(context: Context) {
            val task = suspend {
                runCatching {
                    val mainIntent = Intent(Intent.ACTION_MAIN, null)
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val pkgAppsList = context.packageManager.queryIntentActivities(mainIntent, 0)
                    val handler = SmartBase(context).editHelper
                    SmartNotify.log("room making", "Room")
                    val room = RoomBase.build(context).app()
                    for (packageInfo in pkgAppsList) {
                        runCatching {
                            val pkg = (packageInfo.activityInfo.packageName)
                            val name =
                                (context.packageManager.getApplicationLabel(packageInfo.activityInfo.applicationInfo)
                                    .toString())
                            val app = App(pkg, name)
                            if (handler.addApp(app) >= 0) {
                                PhoneAppReceiver.getImp()?.onPhoneAppAdded(pkg)
                            }
                            runCatching {
                                SmartNotify.log("room insert", "Room")
                                if (room.insert(app.toRoom()) >= 0) {
                                    PhoneAppReceiver.getImp()?.onPhoneAppAdded(pkg)
                                }
                            }.onFailure { it.printStackTrace() }
                        }.onFailure { it.printStackTrace() }
                    }
                }.onFailure { it.printStackTrace() }
            }
            CoroutineScope(IO).launch {
                runCatching { task() }.onFailure { it.printStackTrace() }
            }
        }

        fun storeLink(pkg: String): String {
            return "https://play.google.com/store/apps/details?id=".plus(pkg)
        }

        fun policyLink(): Intent {
            val privacyPolicyUrl =
                "https://onestop-peerless.blogspot.com/p/privacy-policy-smart-notify-privacy.html"
            val privacyPolicyUri = Uri.parse(privacyPolicyUrl)
            return Intent(Intent.ACTION_VIEW, privacyPolicyUri)
        }

        fun termsLink(): Intent {
            val termsAndConditionsUrl =
                "https://onestop-peerless.blogspot.com/p/terms-conditions-smart-notify-terms.html"
            val termsAndConditionsUri = Uri.parse(termsAndConditionsUrl)
            return Intent(Intent.ACTION_VIEW, termsAndConditionsUri)
        }

        fun copyToClipBoard(context: Context, string: String) {
            runCatching {
                val clipboard =
                    context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", string)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied.", Toast.LENGTH_SHORT)
                    .show()
            }.onFailure { it.printStackTrace() }
        }

        private fun getFile(
            name: String,
            ext: String,
            parent: String? = null
        ): File {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "/".plus(parent ?: "Smart Notify")
            )
            dir.mkdirs()
            return File(dir, name.plus('.').plus(ext.replace(".", "")))
        }

        fun shareViewAsImage(view: View, name: String, onResult: (Intent?) -> Unit) {
            runCatching {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.draw(canvas)

                // 2. Create a temporary file to store the image
                val imageFile = getFile(name, "png")

                // 3. Save the Bitmap to the file
                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                MediaScannerConnection.scanFile(view.context,
                    arrayOf(imageFile.path),
                    arrayOf("image/png"),
                    object : MediaScannerConnection.OnScanCompletedListener {
                        override fun onScanCompleted(p0: String?, p1: Uri?) {
                            runCatching {
                                if (p1 == null) {
                                    onResult(null)
                                    return
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND)
                                shareIntent.type = "image/png"
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                shareIntent.putExtra(Intent.EXTRA_STREAM, p1)
                                onResult(shareIntent)
                            }.onFailure {
                                it.printStackTrace()
                                onResult(null)
                            }

                        }
                    })
            }.onFailure {
                it.printStackTrace()
                onResult(null)
            }
        }

        fun getAppDetails(context: Context, app: App): AppData {
            var version = ""
            var install = 0L
            var update = 0L
            try {
                val manager = context.packageManager
                val packageInfo = manager.getPackageInfo(app.pkg, 0)
                version = packageInfo.versionName
                install = packageInfo.firstInstallTime
                update = packageInfo.lastUpdateTime
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return AppData(app, SharedBase.Apps.isAppActive(app.pkg), install, update, version)
        }

        fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
            var cursor: Cursor? = null
            return try {
                val proj = arrayOf(
                    MediaStore.Images.Media.DATA
                )
                cursor = context.contentResolver.query(contentUri, proj, null, null, null)
                val column_index = cursor!!.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DATA
                )
                cursor.moveToFirst()
                cursor.getString(column_index)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                SmartNotify.log("getRealPathFromURI Exception : $e", "uri")
                ""
            } finally {
                cursor?.close()
            }
        }

        fun drawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap: Bitmap =
                if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                    // Create an empty bitmap with a default size if the drawable has no intrinsic size
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                } else {
                    // Create a bitmap with the same size as the drawable
                    Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
    }


}