package com.lassanit.smotify.handlers

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.lassanit.smotify.SmartNotify
import com.lassanit.smotify.bases.SharedBase
import com.lassanit.smotify.popups.InfoPopup
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Callable
import java.util.concurrent.Executors


class GDriveService(private val activity: AppCompatActivity) {
    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleActivityResult(result)
        }

    private val execute = Executors.newSingleThreadExecutor()
    private var dbFile: File? = null
    fun checkUser() {
        val pop = InfoPopup(activity, "Checking Database", "Checking Please wait.....")
        pop.show()
        dbFile = runCatching {
            getFile()
        }.getOrElse { null }
        pop.dismiss()

        runCatching {
            val options = GoogleSignInOptions.Builder()
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()
            val client = GoogleSignIn.getClient(activity, options)
            launcher.launch(client.signInIntent)
        }
    }

    private fun onError(e: Exception?) {
        e?.printStackTrace()
        SmartNotify.log("error: ${e?.message}", "gDrive")
    }

    private fun handleActivityResult(result: ActivityResult) {
        if (result.resultCode != AppCompatActivity.RESULT_OK) {
            onError(Exception("G-Drive Error"))
            return
        }
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .addOnSuccessListener {
                    userFound(it)
                }
                .addOnCanceledListener { onError(Exception("addOnCanceledListener")) }
                .addOnFailureListener { onError(it) }
        }
    }

    private fun userFound(googleSignInAccount: GoogleSignInAccount?) {
        val credentials =
            GoogleAccountCredential.usingOAuth2(activity, setOf(DriveScopes.DRIVE_APPDATA))
        credentials.selectedAccountName = googleSignInAccount?.account?.name

        val service: Drive = Drive.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credentials
            //AndroidHttp.newCompatibleTransport(), GsonFactory(), credentials
        )
            .setApplicationName("Smart Notify")
            .build()
        if (SharedBase.Settings.Display.getDatabaseId().isBlank())
            upload(service)
        else
            download(service)
    }

    private fun upload(drive: Drive) {
        SmartNotify.log("upload", "gDrive")
        val pop = InfoPopup(activity, "Backup", "Please wait.....")
        pop.show()
        Tasks.call(execute, object : Callable<String> {
            override fun call(): String {
                return runCatching {
                    val fileMetadata = com.google.api.services.drive.model.File()
                    fileMetadata.name = "backup_database.db"
                    //fileMetadata.parents = listOf("databases")
                    val mediaContent = FileContent("application/vnd.sqlite3", dbFile)
                    val files = drive.files().create(fileMetadata, mediaContent).setFields("id")
                    val uploadedFile = files.execute()
                    SharedBase.Settings.Edit.setDatabaseId(uploadedFile.id)
                    return uploadedFile.id
                }.onFailure { onError(Exception(it)) }
                    .getOrElse { "result" }
            }
        }).addOnCompleteListener {
            pop.dismiss()
        }
    }

    private fun download(drive: Drive) {
        SmartNotify.log("download", "gDrive")
        val pop = InfoPopup(activity, "Backup", "Please wait.....")
        pop.show()
        Tasks.call(execute, object : Callable<String> {
            override fun call(): String {
                return runCatching {
                    val id = SharedBase.Settings.Display.getDatabaseId()
                    val file = drive.files().get(id).execute()
                    SmartNotify.log("file details: ${file.toString()} ", "gDrive")
                    return id
                }.onFailure { onError(Exception(it)) }.getOrElse { "result" }
            }
        }).addOnCompleteListener {
            pop.dismiss()
        }
    }


    private fun getFile(): File? {
        return runCatching {
            val file=File.createTempFile("tempfile", ".txt")
            val writer = BufferedWriter(FileWriter(file))
            writer.write("This is some temporary text.")
            writer.close()
            file
        }.getOrNull()

    }
}