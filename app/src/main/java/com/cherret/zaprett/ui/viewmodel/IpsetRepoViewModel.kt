package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.utils.getAllExcludeIpsets
import com.cherret.zaprett.utils.getAllIpsets
import com.cherret.zaprett.utils.getHostListMode

class IpsetRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getAllIpsets() else getAllExcludeIpsets()
    override val repoTab = RepoTab.ipsets
}