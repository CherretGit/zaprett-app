package com.cherret.zaprett

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
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
import java.security.MessageDigest

private val client = OkHttpClient()
private val json = Json { ignoreUnknownKeys = true }

fun getHostList(sharedPreferences: SharedPreferences, callback: (List<RepoItemInfo>?) -> Unit) {
    val request = Request.Builder().url(sharedPreferences.getString("hosts_repo_url","https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/hosts.json")?: "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/hosts.json").build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    callback(null)
                }
                val jsonString = response.body.string()
                val hostsInfo = json.decodeFromString<List<RepoItemInfo>>(jsonString)
                callback(hostsInfo)
            }
        }
    })
}

fun getStrategiesList(sharedPreferences: SharedPreferences, callback: (List<RepoItemInfo>?) -> Unit) {
    val request = Request.Builder().url(sharedPreferences.getString("strategies_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/strategies.json")?: "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/strategies.json").build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) {
                    callback(null)
                }
                val jsonString = response.body.string()
                val strategiesInfo = json.decodeFromString<List<RepoItemInfo>>(jsonString)
                callback(strategiesInfo)
            }
        }
    })
}

fun registerDownloadListenerHost(context: Context, downloadId: Long, onDownloaded: (Uri) -> Unit) {// AI Generated
    val receiver = object : BroadcastReceiver() {
        @SuppressLint("Range")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return
            val dm = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val query = DownloadManager.Query().setFilterById(downloadId)
            dm.query(query)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        if (uriString != null) {
                            val uri = uriString.toUri()
                            context.unregisterReceiver(this)
                            onDownloaded(uri)
                        }
                    }
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

fun getFileSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

@Serializable
data class RepoItemInfo(
    val name: String,
    val author: String,
    val description: String,
    val type: String? = null,
    val hash: String,
    val url: String
)