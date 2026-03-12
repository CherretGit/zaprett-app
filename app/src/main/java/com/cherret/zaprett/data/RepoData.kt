package com.cherret.zaprett.data

import kotlinx.serialization.Serializable

@Serializable
data class RepoIndex (
    val schema: Int,
    val items: List<RepoIndexItem>
)

@Serializable
data class RepoIndexItem (
   val id: String,
   val type: ItemType,
   val manifest: String,
)

@Serializable
data class RepoManifest(
    val schema: Int,
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
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
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String
)

data class DependencyUI(
    val name: String,
    val version: String
)