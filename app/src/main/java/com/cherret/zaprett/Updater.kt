package com.cherret.zaprett

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.io.File

private val client = OkHttpClient()

fun getUpdate(context: Context, callback: (UpdateInfo?) -> Unit) {
    val request = Request.Builder().url("https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json").build()
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val jsonAdapter = moshi.adapter(UpdateInfo::class.java)
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
                val jsonString = response.body!!.string()
                val updateInfo = jsonAdapter.fromJson(jsonString)
                if (updateInfo != null) {
                    val packageVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
                    updateInfo.versionCode?.let { versionCode ->
                        if (versionCode > packageVersionCode)
                            callback(updateInfo)
                    }
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
                val changelogText = response.body!!.string()
                callback(changelogText)
            }
        }
    })
}

fun download(context: Context, url: String): Long {
    val fileName = url.substringAfterLast("/")
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    if (Environment.isExternalStorageManager()) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }
    }
    val request = DownloadManager.Request(url.toUri()).apply {
        setTitle(fileName)
        setDescription("Загрузка $fileName")
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

data class UpdateInfo(
    val version: String?,
    val versionCode: Int?,
    val downloadUrl: String?,
    val changelogUrl: String?
)