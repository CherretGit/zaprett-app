package com.cherret.zaprett

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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

fun getUpdate(callback: (UpdateInfo?) -> Unit) {
    val request = Request.Builder().url("https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json").build()
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
    if (url.isEmpty()) {
        Log.e("Updater", "Download URL is empty")
        return -1L
    }
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (downloadManager == null) {
        Log.e("Updater", "DownloadManager is unavailable")
        return -1L
    }
    val fileName = url.substringAfterLast("/")
    if (fileName.isEmpty()) {
        Log.e("Updater", "Invalid file name derived from URL: $url")
        return -1L
    }
    val request = DownloadManager.Request(url.toUri()).apply {
        setTitle(fileName)
        setDescription("Загрузка $fileName")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                setDestinationUri(uri)
            } else {
                Log.e("Updater", "Failed to create MediaStore URI")
                return -1L
            }
        } else {
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
    }
    return try {
        downloadManager.enqueue(request)
    } catch (e: Exception) {
        Log.e("Updater", "Failed to enqueue download: ${e.message}", e)
        -1L
    }
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
    else {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri)
        context.startActivity(intent)
    }
}

fun registerDownloadListener(context: Context, downloadId: Long, onDownloaded: (Uri) -> Unit) {// AI Generated
    val receiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return
            val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                if (cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    context.unregisterReceiver(this)
                    onDownloaded(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri())
                }
            }
        }
    }
    val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
    } else {
        ContextCompat.registerReceiver(context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
    }
}

@Serializable
data class UpdateInfo(
    val version: String?,
    val versionCode: Int?,
    val downloadUrl: String?,
    val changelogUrl: String?
)