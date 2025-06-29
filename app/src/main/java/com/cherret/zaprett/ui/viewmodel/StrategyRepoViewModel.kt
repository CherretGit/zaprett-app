package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.RepoItemInfo
import com.cherret.zaprett.getAllByeDPIStrategies
import com.cherret.zaprett.getAllNfqwsStrategies
import com.cherret.zaprett.getStrategiesList

class StrategyRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> =
        if (sharedPreferences.getBoolean("use_module", false)) {
            getAllNfqwsStrategies()
        } else {
            getAllByeDPIStrategies()
        }
    override fun getRepoList(callback: (List<RepoItemInfo>?) -> Unit) = getStrategiesList(sharedPreferences, callback)
}