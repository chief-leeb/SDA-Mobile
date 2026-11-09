package com.aether.sdamobile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aether.sdamobile.data.SdaAccount
import com.aether.sdamobile.ui.theme.Bg
import com.aether.sdamobile.ui.theme.Card
import com.aether.sdamobile.ui.theme.Line
import com.aether.sdamobile.ui.theme.Primary
import com.aether.sdamobile.ui.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    accounts: List<SdaAccount>,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onSignIn: (SdaAccount) -> Unit,
    onFinishActivation: (SdaAccount) -> Unit
) {
    val vault by viewModel.vault.collectAsStateWithLifecycle()
    val confirmations by viewModel.confirmations.collectAsStateWithLifecycle()

    val selected = accounts.firstOrNull { it.localId == viewModel.selectedAccountId }
        ?: accounts.firstOrNull()

    LaunchedEffect(accounts.map { it.localId }, viewModel.selectedAccountId) {
        if (selected != null && viewModel.selectedAccountId != selected.localId) {
            viewModel.selectedAccountId = selected.localId
        }
    }

    LaunchedEffect(
        viewModel.tab,
        selected?.localId,
        selected?.accessToken,
        selected?.refreshToken
    ) {
        if (viewModel.tab == 1 && selected != null) {
            viewModel.refreshConfirmations(selected.localId)
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Column {
                HorizontalDivider(color = Line)
                NavigationBar(
                    containerColor = Card,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = viewModel.tab == 0,
                        onClick = { viewModel.tab = 0 },
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text("Коды", fontSize = 11.sp) },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = viewModel.tab == 1,
                        onClick = { viewModel.tab = 1 },
                        icon = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text("Подтверждения", fontSize = 11.sp) },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = viewModel.tab == 2,
                        onClick = { viewModel.tab = 2 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text("Настройки", fontSize = 11.sp) },
                        colors = navColors()
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewModel.tab == 0) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = Primary,
                    contentColor = Color(0xFF04131F),
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, Primary, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить аккаунт")
                }
            }
        }
    ) { padding ->
        when (viewModel.tab) {
            0 -> CodesScreen(
                accounts = accounts,
                timeOffset = vault.timeOffset,
                onMessage = viewModel::emit,
                onFinishActivation = onFinishActivation,
                modifier = Modifier.padding(padding)
            )
            1 -> ConfirmationsScreen(
                accounts = accounts,
                selected = selected,
                state = confirmations,
                onSelectAccount = viewModel::selectAccount,
                onRefresh = {
                    selected?.let { viewModel.refreshConfirmations(it.localId) }
                },
                onAccept = { confirmation ->
                    selected?.let { viewModel.accept(it.localId, confirmation) }
                },
                onDeny = { confirmation ->
                    selected?.let { viewModel.deny(it.localId, confirmation) }
                },
                onSignIn = onSignIn,
                modifier = Modifier.padding(padding)
            )
            else -> SettingsScreen(
                viewModel = viewModel,
                accounts = accounts,
                onAdd = onAdd,
                onImport = onImport,
                onSignIn = onSignIn,
                onFinishActivation = onFinishActivation,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF04131F),
    selectedTextColor = Primary,
    indicatorColor = Primary,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)
