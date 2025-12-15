package com.vayunmathur.crypto.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.vayunmathur.crypto.JupiterAPI
import com.vayunmathur.crypto.LendPage
import com.vayunmathur.crypto.MAIN_NAVBAR_PAGES
import com.vayunmathur.crypto.NavigationBottomBar
import com.vayunmathur.crypto.PORTFOLIO_NAVBAR_PAGES
import com.vayunmathur.crypto.PortfolioPage
import com.vayunmathur.crypto.PortfolioViewModel
import com.vayunmathur.crypto.displayAmount
import com.vayunmathur.crypto.token.JupiterLendRepository
import com.vayunmathur.crypto.token.TokenInfo
import com.vayunmathur.crypto.token.TokenPriceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.pow

@Composable
fun LendDetailScreen(viewModel: PortfolioViewModel, backStack: NavBackStack<NavKey>, jlTokenMint: String) {
    val lendTokens by viewModel.lendTokens.collectAsState()
    val jlToken = lendTokens.find { it.tokenInfo.mintAddress == jlTokenMint }!!
    val jlTokenData = JupiterLendRepository[jlToken.tokenInfo]!!
    val underlyingToken = jlTokenData.underlyingToken

    Scaffold(bottomBar = {
        NavigationBottomBar(MAIN_NAVBAR_PAGES, PortfolioPage, backStack)
    }) { paddingValues ->
        Scaffold(bottomBar = {
            NavigationBottomBar(PORTFOLIO_NAVBAR_PAGES, LendPage, backStack)
        }, modifier = Modifier.padding(paddingValues)) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = jlToken.tokenInfo.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${String.format("%.2f", jlToken.totalValue)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "${jlToken.amount.displayAmount()} ${jlToken.tokenInfo.symbol}")

                Spacer(modifier = Modifier.height(32.dp))

                LendActionCard(viewModel, underlyingToken, jlToken.tokenInfo)
            }
        }
    }
}

@Composable
private fun LendActionCard(viewModel: PortfolioViewModel, underlyingToken: TokenInfo, jlToken: TokenInfo) {
    var isDeposit by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }

    val fromToken = if (isDeposit) underlyingToken else jlToken
    val toToken = if (isDeposit) jlToken else underlyingToken

    var pendingOrder by remember { mutableStateOf<JupiterAPI.PendingOrder?>(null) }
    val progress = remember { Animatable(0f) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        val fromAmountFromOrder = pendingOrder?.let { it.inAmount / 10.0.pow(fromToken.decimals) } ?: 0.0
        val toAmountFromOrder = pendingOrder?.let { it.outAmount / 10.0.pow(toToken.decimals) } ?: 0.0

        ConfirmationDialog(
            onConfirm = { pendingOrder?.let { viewModel.swap(it) } },
            onDismiss = { showDialog = false },
            title = if (isDeposit) "Confirm Deposit" else "Confirm Withdraw",
            content = "You are about to swap ${String.format("%.2f", fromAmountFromOrder)} ${fromToken.symbol} for ${String.format("%.2f", toAmountFromOrder)} ${toToken.symbol}."
        )
    }

    LaunchedEffect(amount, isDeposit) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        pendingOrder = null
        if (amountDouble > 0) {
            val fromAmountForApi = if (isDeposit) {
                amountDouble
            } else { // Withdraw
                val underlyingTokenPrice = TokenPriceRepository[underlyingToken]?.price ?: 0.0
                val jlTokenPrice = TokenPriceRepository[jlToken]?.price ?: 0.0
                if (jlTokenPrice > 0) {
                    amountDouble * (underlyingTokenPrice / jlTokenPrice)
                } else {
                    0.0
                }
            }

            if(fromAmountForApi > 0) {
                while (isActive) {
                    try {
                        pendingOrder = JupiterAPI.createOrder(fromToken, toToken, fromAmountForApi, viewModel.wallet)
                        lastUpdateTime = System.currentTimeMillis()
                    } catch (e: Exception) {
                        println("Error creating order: $e")
                    }
                    delay(15000)
                }
            }
        }
    }

    LaunchedEffect(lastUpdateTime) {
        if (lastUpdateTime == 0L) return@LaunchedEffect
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 15000, easing = LinearEasing)
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                val depositColors = if (isDeposit) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                val withdrawColors = if (!isDeposit) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                OutlinedButton(onClick = { isDeposit = true }, colors = depositColors) { Text("Deposit") }
                OutlinedButton(onClick = { isDeposit = false }, colors = withdrawColors) { Text("Withdraw") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount in ${underlyingToken.symbol}") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val rate = pendingOrder?.let {
        val outAmount = it.outAmount / 10.0.pow(toToken.decimals)
        val inAmount = it.inAmount / 10.0.pow(fromToken.decimals)
        outAmount / inAmount
    }

    if (rate != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top=16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.size(24.dp)
            )
            Text(
                "1 ${fromToken.symbol} = ${String.format(Locale.US, "%.6f", rate)} ${toToken.symbol}",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(onClick = { showDialog = true }, enabled = pendingOrder != null, modifier = Modifier.fillMaxWidth()) {
        Text(if (isDeposit) "Deposit" else "Withdraw")
    }
}
