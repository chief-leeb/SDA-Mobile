package com.deepseek.sdamobile

import android.app.Application
import com.deepseek.sdamobile.data.AccountRepository
import com.deepseek.sdamobile.data.VaultStore

class SdaApp : Application() {

    lateinit var repository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AccountRepository(VaultStore(filesDir))
        repository.initialize()
    }
}
