package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.utils.getAllLibs

class LuaLibsRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<StorageData> = getAllLibs()
    override val repoTab = RepoTab.lua_libs
}