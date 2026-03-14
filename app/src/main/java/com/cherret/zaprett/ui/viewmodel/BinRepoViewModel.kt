package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.data.StorageData

class BinRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<StorageData> = emptyArray()
    override val repoTab = RepoTab.bins
}