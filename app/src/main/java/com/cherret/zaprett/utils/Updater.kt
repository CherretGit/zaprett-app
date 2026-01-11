package com.cherret.zaprett.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.cherret.zaprett.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.io.File

private val client = OkHttpClient()
private val json = Json { ignoreUnknownKeys = true }
// PLS EGOR-WHITE REFACTOR THIS
fun getUpdate(sharedPreferences: SharedPreferences, callback: (UpdateInfo?) -> Unit) {
    val request = Request.Builder().url(sharedPreferences.getString("update_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json")?: "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json").build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    throw IOException()
                    callback(null)
                }
                val jsonString = response.body.string()
                val updateInfo = json.decodeFromString<UpdateInfo>(jsonString)
                updateInfo?.versionCode?.let { versionCode ->
                    if (versionCode > BuildConfig.VERSION_CODE)
                        callback(updateInfo)
                }
            }
        }
    })
}

fun getChangelog(changelogUrl: String, callback: (String?) -> Unit) {
    val request = Request.Builder().url(changelogUrl).build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }
                val changelogText = response.body.string()
                callback(changelogText)
            }
        }
    })
}

fun download(context: Context, url: String): Long {
    val fileName = url.substringAfterLast("/")
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val request = DownloadManager.Request(url.toUri()).apply {
        setTitle(fileName)
        setDescription(fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
    }
    return downloadManager.enqueue(request)
}

fun installApk(context: Context, uri: Uri) {
    val file = File(uri.path!!)
    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    if (context.packageManager.canRequestPackageInstalls()) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
@Serializable
data class UpdateInfo(
    val version: String?,
    val versionCode: Int?,
    val downloadUrl: String?,
    val changelogUrl: String?
)
