package com.vayunmathur.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.vayunmathur.crypto.MAIN_NAVBAR_PAGES
import com.vayunmathur.crypto.MaximizedRow
import com.vayunmathur.crypto.NavigationBottomBar
import com.vayunmathur.crypto.PORTFOLIO_NAVBAR_PAGES
import com.vayunmathur.crypto.PortfolioPage
import com.vayunmathur.crypto.PortfolioViewModel
import com.vayunmathur.crypto.PredictionMarket
import com.vayunmathur.crypto.PredictionMarketPage
import java.text.NumberFormat
import kotlin.collections.filter

@Composable
fun PredictionMarketScreen(viewModel: PortfolioViewModel, backStack: NavBackStack<NavKey>) {

    val markets by viewModel.predictionMarkets.collectAsState()

    Scaffold(bottomBar = {
        NavigationBottomBar(MAIN_NAVBAR_PAGES, PortfolioPage, backStack)
    }) { paddingValues ->
            Scaffold(bottomBar = {
                NavigationBottomBar(PORTFOLIO_NAVBAR_PAGES, PredictionMarketPage, backStack)
            }, modifier = Modifier.padding(paddingValues)) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues).padding(horizontal = 16.dp)
            ) {
                Text("Prediction Market viewing is available, but trading currently is unavailable until our provider increases liquidity", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(markets.filter { it.anyMarketOpen() }) { market ->
                        PredictionMarketCard(market, backStack)
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionMarketCard(market: PredictionMarket.Event, backStack: NavBackStack<NavKey>) {
    Card(
        Modifier.fillMaxWidth().clickable { backStack.add(PredictionMarketDetailPage(market.seriesTicker)) },
    ) {
        Column(Modifier.padding(12.dp)) {
            MaximizedRow {
                Text(market.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("● Live", color = Color.Green, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            market.markets.filter{it.closeTime > System.currentTimeMillis() / 1000 }.sortedByDescending { it.chance }.take(3).forEach { marketItem ->
                MaximizedRow {
                    Text(marketItem.subtitle, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${(marketItem.chance * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.background(Color(0x3325D366), RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Yes", color = Color(0xFF25D366), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(" / ", color = Color.Gray, fontSize = 12.sp)
                            Text("No", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Show More >", style = MaterialTheme.typography.labelSmall)
                Text("$${NumberFormat.getNumberInstance().format(market.volume)} vol",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
