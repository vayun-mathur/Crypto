package com.vayunmathur.crypto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.vayunmathur.crypto.PortfolioViewModel
import com.vayunmathur.crypto.token.JupiterLendRepository
import com.vayunmathur.crypto.token.TokenInfo

@Composable
fun TokenListDialog(tokenList: List<TokenInfo>, viewModel: PortfolioViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Add a new Token to your portfolio") },
        text = {
            Column {
                tokenList.forEach { tokenInfo ->
                    val onClick = { viewModel.createTokenAccount(tokenInfo); onDismiss() }
                    if(tokenInfo.category == TokenInfo.Companion.Category.JUPITER_LEND) {
                        val apy = JupiterLendRepository[tokenInfo]?.apy ?: 0.0
                        TextButton(onClick) {
                            Text("${tokenInfo.name} - ${String.format("%.2f", apy * 100)}% APY")
                        }
                    } else {
                        TextButton(onClick) {
                            Text(tokenInfo.name)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } }
    )
}