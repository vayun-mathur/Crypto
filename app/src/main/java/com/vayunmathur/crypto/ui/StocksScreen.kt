package com.vayunmathur.crypto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.vayunmathur.crypto.MAIN_NAVBAR_PAGES
import com.vayunmathur.crypto.NavigationBottomBar
import com.vayunmathur.crypto.PORTFOLIO_NAVBAR_PAGES
import com.vayunmathur.crypto.PortfolioPage
import com.vayunmathur.crypto.PortfolioViewModel
import com.vayunmathur.crypto.R
import com.vayunmathur.crypto.StockDetailPage
import com.vayunmathur.crypto.StocksPage
import com.vayunmathur.crypto.displayAmount
import com.vayunmathur.crypto.token.Token
import com.vayunmathur.crypto.token.TokenInfo
import com.vayunmathur.crypto.token.TokenPriceRepository
import kotlin.text.format

@Composable
fun StocksScreen(viewModel: PortfolioViewModel, backStack: NavBackStack<NavKey>) {
    val stockTokens by viewModel.stockTokens.collectAsState()
    val totalValue = stockTokens.sumOf { it.totalValue }
    var showDialog by remember { mutableStateOf(false) }

    val availableStockTokens = TokenInfo.TOKEN_LIST.filter {
        it.category == TokenInfo.Companion.Category.XSTOCK && stockTokens.none { stockToken -> stockToken.tokenInfo == it }
    }

    if (showDialog) {
        TokenListDialog(TokenInfo.TOKEN_LIST.filter{it.category == TokenInfo.Companion.Category.XSTOCK} - stockTokens.map{it.tokenInfo}, viewModel = viewModel) { showDialog = false }
    }

    Scaffold(bottomBar = {
        NavigationBottomBar(MAIN_NAVBAR_PAGES, PortfolioPage, backStack)
    }) { paddingValues ->
        Scaffold(bottomBar = {
            NavigationBottomBar(PORTFOLIO_NAVBAR_PAGES, StocksPage, backStack)
        }, floatingActionButton = {
            if (availableStockTokens.isNotEmpty()) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(painterResource(R.drawable.add_24px), contentDescription = "Add")
                }
            }
        }, contentWindowInsets = WindowInsets(), modifier = Modifier.padding(paddingValues)) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Stock Positions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${String.format("%.2f", totalValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(32.dp))
                LazyColumn {
                    items(stockTokens) { token ->
                        StockTokenCard(token = token) { backStack.add(StockDetailPage(token.tokenInfo.mintAddress))}
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StockTokenCard(token: Token, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getTokenColor(token.tokenInfo))
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = token.tokenInfo.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "$${String.format("%.2f", TokenPriceRepository[token.tokenInfo]!!.price)} ${if (TokenPriceRepository[token.tokenInfo]!!.change >= 0) "+" else ""}${String.format("%.2f", TokenPriceRepository[token.tokenInfo]!!.change)}%",
                    color = if (TokenPriceRepository[token.tokenInfo]!!.change >= 0) Color.Green else Color.Red
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$${String.format("%.2f", token.totalValue)}")
                Text(text = "${token.amount.displayAmount()} ${token.tokenInfo.symbol}")
            }
        }
    }
}
