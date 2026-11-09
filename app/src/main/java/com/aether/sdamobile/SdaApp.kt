package com.aether.sdamobile

import android.app.Application
import com.aether.sdamobile.data.AccountRepository
import com.aether.sdamobile.data.VaultStore

class SdaApp : Application() {

    lateinit var repository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AccountRepository(VaultStore(filesDir))
        repository.initialize()
    }
}
