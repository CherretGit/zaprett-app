package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.utils.getAllExcludeLists
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getHostList
import com.cherret.zaprett.utils.getHostListMode

class HostRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> = if (getHostListMode(sharedPreferences) == "whitelist") getAllLists() else getAllExcludeLists()
    override fun getRepoList(callback: (Result<List<RepoItemInfo>>) -> Unit) = getHostList(sharedPreferences, callback)
}