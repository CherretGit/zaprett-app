package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.RepoTab
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.utils.getAllBin

class BinRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalled(): Array<StorageData> = getAllBin()
    override val repoTab = RepoTab.bins
}