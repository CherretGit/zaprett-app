package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getHostList

class HostRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> = getAllLists()
    override fun getRepoList(callback: (List<RepoItemInfo>?) -> Unit) = getHostList(sharedPreferences, callback)
}