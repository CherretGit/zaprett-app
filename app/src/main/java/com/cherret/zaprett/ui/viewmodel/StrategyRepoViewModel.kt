package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.utils.getAllByeDPIStrategies
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getStrategiesList

class StrategyRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> =
        if (sharedPreferences.getBoolean("use_module", false)) {
            getAllNfqwsStrategies()
        } else {
            getAllByeDPIStrategies()
        }
    override fun getRepoList(callback: (Result<List<RepoItemInfo>>) -> Unit) = getStrategiesList(sharedPreferences, callback)
}