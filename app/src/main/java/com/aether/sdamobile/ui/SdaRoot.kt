package com.aether.sdamobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.sdamobile.ui.theme.Bg

private data class WizardLaunch(val mode: WizardMode, val accountId: String? = null)

@Composable
fun SdaRoot(
    viewModel: MainViewModel = viewModel(),
    wizardViewModel: WizardViewModel = viewModel()
) {
    val vault by viewModel.vault.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var wizard by remember { mutableStateOf<WizardLaunch?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        when {
            viewModel.repository.initError != null -> StorageErrorScreen(
                message = viewModel.repository.initError.orEmpty(),
                onRetry = { viewModel.repository.reload() }
            )

            vault.accounts.isEmpty() -> WelcomeScreen(
                onAdd = { showAddSheet = true },
                onImport = { showImport = true }
            )

            else -> MainScreen(
                viewModel = viewModel,
                accounts = vault.accounts,
                onAdd = { showAddSheet = true },
                onImport = { showImport = true },
                onSignIn = { account -> wizard = WizardLaunch(WizardMode.LOGIN, account.localId) },
                onFinishActivation = { account ->
                    wizard = WizardLaunch(WizardMode.FINALIZE, account.localId)
                }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 88.dp)
        )
    }

    if (showAddSheet) {
        AddAccountSheet(
            onDismiss = { showAddSheet = false },
            onAddToSteam = {
                showAddSheet = false
                wizard = WizardLaunch(WizardMode.ADD)
            },
            onImport = {
                showAddSheet = false
                showImport = true
            }
        )
    }

    if (showImport) {
        ImportDialog(
            repository = viewModel.repository,
            onDismiss = { showImport = false },
            onImported = { account ->
                viewModel.selectedAccountId = account.localId
                viewModel.emit("Аккаунт ${account.accountName} добавлен")
                showImport = false
            }
        )
    }

    wizard?.let { launch ->
        WizardDialog(
            viewModel = wizardViewModel,
            mode = launch.mode,
            accountId = launch.accountId,
            onClose = {
                val pending = wizardViewModel.state.value.pendingAccountId
                wizardViewModel.cancel()
                wizardViewModel.reset()
                wizard = null
                if (pending != null) viewModel.selectedAccountId = pending
            }
        )
    }
}
