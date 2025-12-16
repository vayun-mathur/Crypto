package com.vayunmathur.crypto

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vayunmathur.crypto.token.JupiterLendRepository
import com.vayunmathur.crypto.token.Token
import com.vayunmathur.crypto.token.TokenInfo
import com.vayunmathur.crypto.token.TokenPriceRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.sol4k.Base58
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.VersionedTransaction
import kotlin.math.pow

class PortfolioViewModel(private val application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("crypto_prefs", Application.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private var ignoreNextFetch = false

    private val _tokens = MutableStateFlow<List<Token>>(emptyList())
    val tokens: StateFlow<List<Token>> = _tokens

    private val _lendTokens = MutableStateFlow<List<Token>>(emptyList())
    val lendTokens: StateFlow<List<Token>> = _lendTokens

    private val _stockTokens = MutableStateFlow<List<Token>>(emptyList())
    val stockTokens: StateFlow<List<Token>> = _stockTokens

    private val _predictionMarkets = MutableStateFlow<List<PredictionMarket.Event>>(emptyList())
    val predictionMarkets: StateFlow<List<PredictionMarket.Event>> = _predictionMarkets

    lateinit var wallet: Keypair
        private set

    private val _walletInitialized = MutableStateFlow(false)
    val walletInitialized: StateFlow<Boolean> = _walletInitialized

    init {
        TokenPriceRepository.init(application)
        JupiterLendRepository.init(application)

        val savedPrivateKey = sharedPreferences.getString("private_key", null)
        if (savedPrivateKey != null) {
            initializeWallet(savedPrivateKey)
        }

        val cachedTokens = sharedPreferences.getString("cached_tokens", null)
        if (cachedTokens != null) {
            val decodedTokens = json.decodeFromString<List<Token>>(cachedTokens)
            _tokens.value = decodedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.NORMAL }
            _stockTokens.value = decodedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.XSTOCK }
            _lendTokens.value = decodedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.JUPITER_LEND }
        }
    }

    fun createWallet() {
        val keypair = Keypair.generate()
        initializeWallet(Base58.encode(keypair.secret))
    }

    fun initializeWallet(privateKey: String) {
        try {
            wallet = Keypair.fromSecretKey(Base58.decode(privateKey))
            with(sharedPreferences.edit()) {
                putString("private_key", privateKey)
                apply()
            }
            _walletInitialized.value = true
            startDataFetching()
        } catch (e: Exception) {
            // Handle invalid private key
            println("Invalid private key: $e")
            _walletInitialized.value = false
        }
    }

    fun logout() {
        with(sharedPreferences.edit()) {
            remove("private_key")
            remove("cached_tokens")
            apply()
        }
        _walletInitialized.value = false
    }

    private fun startDataFetching() {
        viewModelScope.launch {
            while(true) {
                fetchTokens()
                delay(15000)
            }
        }
    }

    private suspend fun fetchTokens() {
        if (ignoreNextFetch) {
            ignoreNextFetch = false
            return
        }
        if (!::wallet.isInitialized) return

        TokenPriceRepository.update()
        JupiterLendRepository.update()
        val fetchedTokens = SolanaAPI.getTokenAccountsByOwner(wallet)

        with(sharedPreferences.edit()) {
            putString("cached_tokens", json.encodeToString(fetchedTokens))
            apply()
        }

        _tokens.value = fetchedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.NORMAL }
        _stockTokens.value = fetchedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.XSTOCK }
        _lendTokens.value = fetchedTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.JUPITER_LEND }
        //_predictionMarkets.value = PredictionMarket.getPredictionMarkets()
    }

    fun signTransaction(transaction: String): String {
        val t = VersionedTransaction.from(transaction)
        t.sign(wallet)
        return t.serialize().encodeBase64()
    }

    fun sendToken(token: TokenInfo, recipient: PublicKey, amount: Double) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                SolanaAPI.transfer(wallet, token, recipient, amount)
            }
        }
    }

    fun swap(order: JupiterAPI.PendingOrder) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                val signedTransaction = signTransaction(order.transaction)
                val response = JupiterAPI.completeOrder(signedTransaction, order.requestId)
                if (response?.status == "Success") {
                    ignoreNextFetch = true
                    response.swapEvents?.firstOrNull()?.let { swapEvent ->
                        val allTokens = (_tokens.value + _stockTokens.value + _lendTokens.value).toMutableList()
                        val inputToken = allTokens.find { it.tokenInfo.mintAddress == swapEvent.inputMint }
                        val outputToken = allTokens.find { it.tokenInfo.mintAddress == swapEvent.outputMint }

                        if (inputToken != null && outputToken != null) {
                            val inputAmount = swapEvent.inputAmount.toDouble() / 10.0.pow(inputToken.tokenInfo.decimals)
                            val outputAmount = swapEvent.outputAmount.toDouble() / 10.0.pow(outputToken.tokenInfo.decimals)

                            val updatedInputToken = inputToken.copy(amount = inputToken.amount - inputAmount)
                            val updatedOutputToken = outputToken.copy(amount = outputToken.amount + outputAmount)

                            val inputIndex = allTokens.indexOf(inputToken)
                            if (inputIndex != -1) {
                                allTokens[inputIndex] = updatedInputToken
                            }

                            val outputIndex = allTokens.indexOf(outputToken)
                            if (outputIndex != -1) {
                                allTokens[outputIndex] = updatedOutputToken
                            }

                            _tokens.value = allTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.NORMAL }
                            _stockTokens.value = allTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.XSTOCK }
                            _lendTokens.value = allTokens.filter { it.tokenInfo.category == TokenInfo.Companion.Category.JUPITER_LEND }
                        }
                    }
                } else {
                    showToast("Transaction failed")
                }
            }
        }
    }

    fun createTokenAccount(tokenInfo: TokenInfo) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                SolanaAPI.createTokenAccount(wallet, tokenInfo)
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun getPrivateKey(): String? {
        return sharedPreferences.getString("private_key", null)
    }
}

class PortfolioViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
