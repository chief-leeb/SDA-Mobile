package io.github.chiefleeb.sdamobile

import android.app.Application
import io.github.chiefleeb.sdamobile.data.AccountRepository
import io.github.chiefleeb.sdamobile.data.VaultStore
import io.github.chiefleeb.sdamobile.i18n.LanguageState

class SdaApp : Application() {

    lateinit var repository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()
        LanguageState.init(this)
        repository = AccountRepository(VaultStore(filesDir))
        repository.initialize()
    }
}
