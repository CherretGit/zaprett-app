package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.RepoTab

class BinRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> = emptyArray()
    override val repoTab = RepoTab.bins
}