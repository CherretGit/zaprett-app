package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.utils.getAllIpsets
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getHostList
import com.cherret.zaprett.utils.getIpsetList

class IpsetRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> = getAllIpsets()
    override fun getRepoList(callback: (Result<List<RepoItemInfo>>) -> Unit) =
        getIpsetList(sharedPreferences, callback)
}