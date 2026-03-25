package com.cherret.zaprett.utils

import android.content.SharedPreferences
import com.cherret.zaprett.data.DependencyEntry
import com.cherret.zaprett.data.RepoIndex
import com.cherret.zaprett.data.RepoIndexItem
import com.cherret.zaprett.data.RepoItemFull
import com.cherret.zaprett.data.RepoManifest
import com.cherret.zaprett.data.ResolveResult
import com.cherret.zaprett.data.UpdateData
import com.cherret.zaprett.data.UpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlin.collections.forEach

object NetworkUtils {
    private val client = HttpClient(OkHttp)

    private val json = Json { ignoreUnknownKeys = true }

    fun getRepo(url: String, filter: (RepoIndexItem) -> Boolean): Flow<Pair<List<RepoIndexItem>, List<RepoItemFull>>> = flow {
        val index = client.get(url).bodyAsText()
        val indexJson = json.decodeFromString<RepoIndex>(index)
        val filtered = indexJson.items.filter(filter)
        val semaphore = Semaphore(15)
        val manifest = coroutineScope {
            filtered.map { item ->
                async {
                    semaphore.withPermit {
                        val manifest =
                            json.decodeFromString<RepoManifest>(client.get(item.manifest).bodyAsText())
                        RepoItemFull(item, manifest)
                    }
                }
            }.awaitAll()
        }
        emit(indexJson.items to manifest)
    }

    fun resolveDependencies(index: List<RepoIndexItem>, items: List<RepoItemFull>): Flow<ResolveResult> = flow {
        val indexMap = index.associateBy { it.manifest }
        val depsMap = mutableMapOf<String, DependencyEntry>()
        val manifestCache = mutableMapOf<String, RepoItemFull>()
        suspend fun collect(manifest: RepoManifest, rootId: String) {
            manifest.dependencies
                .mapNotNull { depUrl ->
                    indexMap[depUrl]?.let { indexItem ->
                        depUrl to indexItem
                    }
                }
                .forEach { (depUrl, indexItem) ->
                    val dep = manifestCache.getOrPut(depUrl) {
                        val depManifest = json.decodeFromString<RepoManifest>(
                            client.get(depUrl).bodyAsText()
                        )
                        RepoItemFull(indexItem, depManifest)
                    }
                    val entry = depsMap.getOrPut(dep.manifest.id) { DependencyEntry(dep) }
                    entry.dependencies.add(rootId)
                        .takeIf { dependency -> dependency }
                        ?.let { collect(dep.manifest, rootId) }
                }
        }
        items.forEach { collect(it.manifest, it.manifest.id) }
        emit(
            ResolveResult(
                roots = items,
                dependencies = depsMap.values.toList()
            )
        )
    }

    suspend fun getUpdate(sharedPreferences: SharedPreferences): Result<UpdateData> {
        val url = sharedPreferences.getString("update_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json")?: "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json"
        return runCatching {
            val update = client.get(url).bodyAsText()
            val updateInfo = json.decodeFromString<UpdateInfo>(update)
            val changeLog = client.get(updateInfo.changelogUrl).bodyAsText()
            UpdateData(
                updateInfo,
                changeLog
            )
        }.onFailure { if (it is CancellationException) throw it }
    }
}