package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.utils.getAllExcludeLists
import com.cherret.zaprett.utils.getAllLists
import com.cherret.zaprett.utils.getHostListMode

class HostRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> =
        if (getHostListMode(sharedPreferences) == ListType.whitelist) getAllLists() else getAllExcludeLists()
}