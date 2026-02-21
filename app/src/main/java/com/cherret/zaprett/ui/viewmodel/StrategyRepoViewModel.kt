package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.utils.getAllByeDPIStrategies
import com.cherret.zaprett.utils.getAllNfqwsStrategies
import com.cherret.zaprett.utils.getServiceType

class StrategyRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> =
        if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
            getAllNfqwsStrategies()
        } else {
            getAllByeDPIStrategies()
        }
}