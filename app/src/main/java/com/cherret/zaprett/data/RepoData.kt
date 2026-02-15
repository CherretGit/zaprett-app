package com.cherret.zaprett.data

import kotlinx.serialization.Serializable

@Serializable
data class RepoIndex (
    val schema: Int,
    val items: List<RepoIndexItem>
)

@Serializable
data class RepoIndexItem (
   val name: String,
   val type: ItemType,
   val manifest: String,
)

@Serializable
data class RepoManifest(
    val scheme: Int,
    val name: String,
    val author: String,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val artifact: Artifact
)

@Serializable
data class Artifact (
    val url: String,
    val sha256: String
)

data class RepoItemFull(
    val index: RepoIndexItem,
    val manifest: RepoManifest
)

data class DependencyEntry (
    val manifest: RepoManifest,
    val dependencies: MutableSet<String> = mutableSetOf()
)

data class ResolveResult(
    val roots: List<RepoItemFull>,
    val dependencies: List<DependencyEntry>
)

data class RepoItemUI(
    val name: String,
    val author: String,
    val description: String
)

data class DependencyUI(
    val name: String
)