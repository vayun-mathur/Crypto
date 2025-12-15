package com.vayunmathur.crypto.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.vayunmathur.crypto.BackButton
import com.vayunmathur.crypto.MAIN_NAVBAR_PAGES
import com.vayunmathur.crypto.MaximizedRow
import com.vayunmathur.crypto.NavigationBottomBar
import com.vayunmathur.crypto.PORTFOLIO_NAVBAR_PAGES
import com.vayunmathur.crypto.PortfolioViewModel
import com.vayunmathur.crypto.PredictionMarket
import com.vayunmathur.crypto.PredictionMarketPage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import kotlin.math.floor

@Serializable
data class PredictionMarketDetailPage(val marketId: String): NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionMarketDetailScreen(viewModel: PortfolioViewModel, backStack: NavBackStack<NavKey>, marketId: String) {
    val markets by viewModel.predictionMarkets.collectAsState()
    val market = markets.find { it.seriesTicker == marketId }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMarket by remember { mutableStateOf<Pair<PredictionMarket.Event.Market, Boolean>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },
                navigationIcon = { BackButton(backStack) }
            )
        },
        bottomBar = {
            NavigationBottomBar(MAIN_NAVBAR_PAGES,PredictionMarketPage, backStack)
        }
    ) { paddingValues ->
        Scaffold(bottomBar = {
            NavigationBottomBar(PORTFOLIO_NAVBAR_PAGES, PredictionMarketPage, backStack)
        }, modifier = Modifier.padding(paddingValues)) { paddingValues ->
            if (market != null) {
                LazyColumn(
                    Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
                ) {
                    item {
                        MaximizedRow {
                            Text(
                                market.title,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("● Live", color = Color.Green, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    itemsIndexed(market.markets.sortedByDescending { it.yesPrice }) { idx, marketItem ->
                        if (idx > 0) HorizontalDivider()
                        MaximizedRow(Modifier.padding(vertical = 8.dp)) {
                            Text(
                                marketItem.subtitle,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(Modifier, Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                                Text(
                                    "${(marketItem.chance * 100).toInt()}%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Button(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(35.dp),
                                    onClick = {
                                        selectedMarket = marketItem to true
                                        showBottomSheet = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0x3325D366
                                        )
                                    )
                                ) {
                                    val yesNum = (marketItem.yesPrice * 100).toInt()
                                    Text(
                                        "Yes ${yesNum}¢",
                                        color = Color(0xFF25D366),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Button(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(35.dp),
                                    onClick = {
                                        selectedMarket = marketItem to false
                                        showBottomSheet = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0x33F44336
                                        )
                                    )
                                ) {
                                    val noNum = (marketItem.noPrice * 100).toInt()
                                    Text(
                                        "No ${noNum}¢",
                                        color = Color(0xFFF44336),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedMarket != null) {
        val (marketItem, isYes) = selectedMarket!!
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            OrderSheet(viewModel, marketItem, isYes) {
                showBottomSheet = false
            }
        }
    }
}

@Composable
fun OrderSheet(
    viewModel: PortfolioViewModel,
    market: PredictionMarket.Event.Market,
    isYes: Boolean,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("5") }
    val contractPrice by remember { derivedStateOf { if(isYes) market.yesPrice else market.noPrice } }
    val numContracts by remember { derivedStateOf { floor((amount.toDoubleOrNull() ?: 0.0) / contractPrice).toInt() } }
    val actualSpend by remember { derivedStateOf { numContracts * contractPrice } }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Row {
            Text(
                "Buy ${if (isYes) "Yes" else "No"}",
                color = if (isYes) Color(0xFF25D366) else Color(0xFFF44336)
            )
            Text(" — ${market.subtitle}")
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount to spend") },
            prefix = { Text("USDC") }
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("You will spend:")
            Text(NumberFormat.getCurrencyInstance().format(actualSpend))
        }

        Spacer(Modifier.height(24.dp))
        Text("If your prediction is correct, the payout is expected within 24 hours after the market closes, and the result is finalized.", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    Toast.makeText(context, "Prediction market trading not yet supported", Toast.LENGTH_LONG).show()
                    //PredictionMarket.makeOrder(market, isYes, actualSpend, viewModel.owner)
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = actualSpend > 0
        ) {
            val formattedWin = NumberFormat.getCurrencyInstance().format(numContracts)
            Text("Buy ${if(isYes) "Yes" else "No"} → Win $formattedWin", fontSize = 16.sp)
        }
    }
}
