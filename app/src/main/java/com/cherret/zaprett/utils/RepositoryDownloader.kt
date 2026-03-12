package com.cherret.zaprett.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.cherret.zaprett.data.DependencyEntry
import com.cherret.zaprett.data.RepoIndex
import com.cherret.zaprett.data.RepoIndexItem
import com.cherret.zaprett.data.RepoItemFull
import com.cherret.zaprett.data.RepoManifest
import com.cherret.zaprett.data.ResolveResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

private val client = HttpClient(OkHttp)

private val json = Json { ignoreUnknownKeys = true }

fun getRepo(url: String, filter: (RepoIndexItem) -> Boolean): Flow<List<RepoItemFull>> = flow {
    val index = client.get(url).bodyAsText()
    val indexJson = json.decodeFromString<RepoIndex>(index)
    val filtered = indexJson.items.filter(filter)
    val manifest = coroutineScope {
        filtered.map { item ->
            async {
                Semaphore(15).withPermit {
                    val manifest =
                        json.decodeFromString<RepoManifest>(client.get(item.manifest).bodyAsText())
                    RepoItemFull(item, manifest)
                }
            }
        }.awaitAll()
    }
    emit(manifest)
}

fun resolveDependencies(items: List<RepoItemFull>): Flow<ResolveResult> = flow {
    val resolved = mutableSetOf<String>()
    val depsMap = mutableMapOf<String, DependencyEntry>()
    val manifestCache = mutableMapOf<String, RepoManifest>()
    suspend fun collect(manifest: RepoManifest, rootName: String) {
        manifest.dependencies.forEach { depUrl ->
            val dep = manifestCache.getOrPut(depUrl) {
                json.decodeFromString<RepoManifest>(
                    client.get(depUrl).bodyAsText()
                )
            }
            val entry = depsMap.getOrPut(dep.name) {
                DependencyEntry(dep)
            }
            entry.dependencies += rootName
            if (resolved.add(dep.name)) {
                collect(dep, dep.name)
            }
        }
    }
    items.forEach { item ->
        collect(item.manifest, item.index.id)
    }
    emit(
        ResolveResult(
            roots = items,
            dependencies = depsMap.values.toList()
        )
    )
}

fun registerDownloadListener(context: Context, downloadId: Long, onDownloaded: (Uri) -> Unit, onError: (String) -> Unit) {// AI Generated
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
                    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            if (uriString != null) {
                                val uri = uriString.toUri()
                                context.unregisterReceiver(this)
                                onDownloaded(uri)
                            }
                        }

                        DownloadManager.STATUS_FAILED -> {
                            context.unregisterReceiver(this)
                            val errorMessage = when (reason) {
                                DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
                                DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
                                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
                                DownloadManager.ERROR_FILE_ERROR -> "File error"
                                DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
                                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
                                DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
                                DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
                                DownloadManager.ERROR_UNKNOWN -> "Unknown error"
                                else -> "Download failed: reason=$reason"
                            }
                            onError(errorMessage)
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