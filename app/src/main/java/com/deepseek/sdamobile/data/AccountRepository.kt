package com.deepseek.sdamobile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AccountRepository(private val store: VaultStore) {

    private val _vault = MutableStateFlow(VaultData())
    val vault: StateFlow<VaultData> = _vault.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveMutex = Mutex()
    @Volatile
    private var version = 0L
    @Volatile
    private var savedVersion = 0L

    var initError by mutableStateOf<String?>(null)
        private set

    fun initialize() {
        initError = try {
            _vault.value = store.load()
            null
        } catch (e: Exception) {
            "Не удалось открыть хранилище: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun reload() {
        initialize()
    }

    fun update(transform: (VaultData) -> VaultData) {
        if (initError != null) return
        val next = transform(_vault.value)
        _vault.value = next
        val snapshotVersion = ++version
        scope.launch {
            runCatching { persistVersion(snapshotVersion) }
        }
    }

    /** Waits until the current state is durably written to the vault. */
    suspend fun flush() = withContext(Dispatchers.IO) {
        persistVersion(version)
    }

    private suspend fun persistVersion(targetVersion: Long) {
        saveMutex.withLock {
            while (savedVersion < targetVersion) {
                val observedVersion = version
                val snapshot = _vault.value
                store.save(snapshot)
                savedVersion = maxOf(savedVersion, observedVersion)
            }
        }
    }

    fun addAccount(account: SdaAccount) {
        update { vault ->
            val exists = vault.accounts.any { it.localId == account.localId }
            if (exists) {
                vault.copy(accounts = vault.accounts.map { if (it.localId == account.localId) account else it })
            } else {
                vault.copy(accounts = vault.accounts + account)
            }
        }
    }

    fun updateAccount(localId: String, transform: (SdaAccount) -> SdaAccount) {
        update { vault ->
            vault.copy(accounts = vault.accounts.map { if (it.localId == localId) transform(it) else it })
        }
    }

    fun removeAccount(localId: String) {
        update { vault -> vault.copy(accounts = vault.accounts.filterNot { it.localId == localId }) }
    }

    fun setTimeOffset(offset: Long) {
        update { vault ->
            vault.copy(timeOffset = offset, timeSyncedAt = System.currentTimeMillis() / 1000)
        }
    }

    fun account(localId: String): SdaAccount? = _vault.value.accounts.firstOrNull { it.localId == localId }

    fun serverTime(): Long = System.currentTimeMillis() / 1000 + _vault.value.timeOffset
}
