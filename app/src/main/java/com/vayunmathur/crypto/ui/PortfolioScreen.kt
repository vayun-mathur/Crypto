package com.vayunmathur.crypto.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.vayunmathur.crypto.PrivateKeyPage
import com.vayunmathur.crypto.R
import com.vayunmathur.crypto.displayAmount
import com.vayunmathur.crypto.token.Token
import com.vayunmathur.crypto.token.TokenInfo
import com.vayunmathur.crypto.token.TokenPriceRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel, backStack: NavBackStack<NavKey>) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    val tokens by viewModel.tokens.collectAsState()

    if (showDialog) {
        ConfirmationDialog(
            onConfirm = { viewModel.logout() },
            onDismiss = { showDialog = false },
            title = "Disconnect Wallet",
            content = "Please make sure you have your private key saved somewhere so you can restore your wallet again."
        )
    }

    if (showTokenDialog) {
        TokenListDialog(TokenInfo.TOKEN_LIST.filter{it.category == TokenInfo.Companion.Category.NORMAL} - tokens.map{it.tokenInfo}
            .toSet(), viewModel = viewModel) { showTokenDialog = false}
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text("Private Key") },
                    selected = false,
                    onClick = { backStack.add(PrivateKeyPage) }
                )
                NavigationDrawerItem(
                    label = { Text("Disconnect Wallet") },
                    selected = false,
                    onClick = { showDialog = true }
                )
            }
        }
    ) {
        Scaffold(bottomBar = {
            NavigationBottomBar(MAIN_NAVBAR_PAGES, PortfolioPage, backStack)
        }, topBar = {
            TopAppBar(title = { }, navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(painterResource(R.drawable.menu_24px), contentDescription = "Menu")
                }
            })
        }, contentWindowInsets = WindowInsets()) { paddingValues ->
            Scaffold(bottomBar = {
                NavigationBottomBar(PORTFOLIO_NAVBAR_PAGES, PortfolioPage, backStack)
            }, floatingActionButton = {
                FloatingActionButton(onClick = { showTokenDialog = true }) {
                    Icon(painterResource(id = R.drawable.add_24px), contentDescription = "Add")
                }
            }, contentWindowInsets = WindowInsets(), modifier = Modifier.padding(paddingValues)) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize().padding(paddingValues)
                ) {
                    TokenListScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun TokenListScreen(viewModel: PortfolioViewModel) {
    val tokens by viewModel.tokens.collectAsState()
    val wallet = viewModel.wallet
    val totalValue = tokens.sumOf { it.totalValue }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.clickable {
                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("text", wallet.publicKey.toBase58())
                clipboardManager.setPrimaryClip(clipData)
            }, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(R.drawable.content_copy_24px), null, Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$${String.format("%.2f", totalValue)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Token Positions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(tokens) { token ->
                TokenCard(token = token)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TokenCard(token: Token) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
