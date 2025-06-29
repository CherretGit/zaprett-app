package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.cherret.zaprett.RepoItemInfo
import com.cherret.zaprett.download
import com.cherret.zaprett.getAllLists
import com.cherret.zaprett.getHostList
import com.cherret.zaprett.getZaprettPath
import com.cherret.zaprett.registerDownloadListenerHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class HostRepoViewModel(application: Application): BaseRepoViewModel(application) {
    override fun getInstalledLists(): Array<String> = getAllLists()
    override fun getRepoList(callback: (List<RepoItemInfo>?) -> Unit) = getHostList(sharedPreferences, callback)
}