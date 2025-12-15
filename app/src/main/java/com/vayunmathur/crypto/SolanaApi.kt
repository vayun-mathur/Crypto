package com.vayunmathur.crypto

import com.vayunmathur.crypto.token.Token
import com.vayunmathur.crypto.token.TokenInfo
import io.ktor.client.* 
import io.ktor.client.call.body
import io.ktor.client.engine.cio.* 
import io.ktor.client.plugins.contentnegotiation.* 
import io.ktor.client.request.* 
import io.ktor.client.statement.* 
import io.ktor.http.* 
import io.ktor.serialization.kotlinx.json.* 
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.sol4k.AccountMeta
import org.sol4k.Base58.decode
import org.sol4k.Connection
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.RpcUrl
import org.sol4k.TransactionMessage
import org.sol4k.VersionedTransaction
import org.sol4k.instruction.BaseInstruction
import org.sol4k.instruction.CreateAssociatedToken2022AccountInstruction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.SplTransferInstruction
import kotlin.math.pow


@Serializable
data class RPCRequest(
    val jsonrpc: String = "2.0",
    val id: Int = System.currentTimeMillis().toInt(),
    val method: String,
    val params: JsonArray
)

@Serializable
data class RPCResult<T>(
    val jsonrpc: String,
    val result: Result<T>,
    val id: Int
) {
    @Serializable
    data class Result<T>(
        val context: JsonElement,
        val value: T
    )
}

typealias TokenAccountByOwnerData = List<TokenAccountByOwnerDataItem>

fun TokenAccountByOwnerData.toTokens(): List<Token> {
    return this.mapNotNull { item ->
        item.account.toToken()
    }
}

fun TokenAccount.toToken(): Token? {
    return TokenInfo.TOKEN_MAP[this.data.parsed.info.mint]?.let {
        Token(
            tokenInfo = it,
            amount = this.data.parsed.info.tokenAmount.uiAmount
        )
    }
}

@Serializable
data class TokenAccountByOwnerDataItem(
    val pubkey: String,
    val account: TokenAccount
)

@Serializable
data class TokenAccount(
    val executable: Boolean,
    val lamports: ULong,
    val owner: String,
    val rentEpoch: ULong,
    val space: ULong,
    val data: Data
) {
    @Serializable
    data class Data(
        val program: String,
        val parsed: Parsed,
        val space: ULong
    ) {
        @Serializable
        data class Parsed(
            val info: Info,
            val type: String
        ) {
            @Serializable
            data class Info(
                val mint: String,
                val owner: String,
                val tokenAmount: TokenAmount
            ) {
                @Serializable
                data class TokenAmount(
                    val amount: String,
                    val decimals: Int,
                    val uiAmount: Double,
                    val uiAmountString: String
                )
            }
        }
    }
}

@Serializable
data class PriceData(
    val usdPrice: Double,
    val blockId: Long = 0,
    val decimals: Int,
    val priceChange24h: Double = 0.0
)

typealias PriceResponse = Map<String, PriceData>

val JSON = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(JSON)
    }
}

object SolanaAPI {

    val connection = Connection(RpcUrl.MAINNNET)

    suspend fun getTokenAccountsByOwner(wallet: Keypair): List<Token> {
        val tokens1 = rpcCall<TokenAccountByOwnerData>("getTokenAccountsByOwner", buildJsonArray {
            add(wallet.publicKey.toBase58())
            add(buildJsonObject {
                put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
            })
            add(buildJsonObject {
                put("commitment", "finalized")
                put("encoding", "jsonParsed")
            })
        }).toTokens()
        val tokens2 = rpcCall<TokenAccountByOwnerData>("getTokenAccountsByOwner", buildJsonArray {
            add(wallet.publicKey.toBase58())
            add(buildJsonObject {
                put("programId", "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb")
            })
            add(buildJsonObject {
                put("commitment", "finalized")
                put("encoding", "jsonParsed")
            })
        }).toTokens()
        val solanaLamports = rpcCall<ULong>("getBalance", buildJsonArray{
            add(wallet.publicKey.toBase58())
            add(buildJsonObject {
                put("commitment", "finalized")
                put("encoding", "jsonParsed")
            })
        })
        val solanaToken = Token(TokenInfo.SOL, solanaLamports.toDouble() / 1000000000)
        return (tokens1 + tokens2 + solanaToken)
    }

    private suspend inline fun <reified T> rpcCall(method: String, params: JsonArray): T {
        val response: HttpResponse = client.post(heliusUrl) {
            contentType(ContentType.Application.Json)
            setBody(RPCRequest(
                method = method,
                params = params
            ))
        }
        return response.body<RPCResult<T>>().result.value
    }

    fun transfer(from: Keypair, token: TokenInfo, recipient: PublicKey, amount: Double) {
        val blockhash = connection.getLatestBlockhash()

        val receiverAssociatedAccount = PublicKey.findProgramDerivedAddress(recipient, PublicKey(token.mintAddress))
        val holderAssociatedAccount = PublicKey.findProgramDerivedAddress(from.publicKey, PublicKey(token.mintAddress))
        val splTransferInstruction = SplTransferInstruction(
            holderAssociatedAccount.publicKey,
            receiverAssociatedAccount.publicKey,
            PublicKey(token.mintAddress),
            from.publicKey,
            (amount*10.0.pow(token.decimals)).toLong(),
            token.decimals
        )
        val message = TransactionMessage.newMessage(
            from.publicKey,
            blockhash,
            splTransferInstruction
        )
        val transaction = VersionedTransaction(message)
        transaction.sign(from)

        val signature = connection.sendTransaction(transaction)

        println("Transaction Signature: $signature")
    }

    fun createTokenAccount(wallet: Keypair, token: TokenInfo) {
        val blockhash = connection.getLatestBlockhash()
        val programID = when(token.category) {
            TokenInfo.Companion.Category.NORMAL, TokenInfo.Companion.Category.JUPITER_LEND -> TOKEN_PROGRAM_ID
            TokenInfo.Companion.Category.XSTOCK -> TOKEN_2022_PROGRAM_ID
        }
        val (associatedAccount) = PublicKey.findProgramDerivedAddress(wallet.publicKey, PublicKey(token.mintAddress), programID)
        val instruction = when(token.category) {
            TokenInfo.Companion.Category.NORMAL, TokenInfo.Companion.Category.JUPITER_LEND ->
                CreateAssociatedTokenAccountInstruction(
                    payer = wallet.publicKey,
                    associatedToken = associatedAccount,
                    owner = wallet.publicKey,
                    mint = PublicKey(token.mintAddress),
                )
            TokenInfo.Companion.Category.XSTOCK -> CreateAssociatedToken2022AccountInstruction(
                payer = wallet.publicKey,
                associatedToken = associatedAccount,
                owner = wallet.publicKey,
                mint = PublicKey(token.mintAddress),
            )
        }
        val message = TransactionMessage.newMessage(wallet.publicKey, blockhash, instruction)
        val transaction = VersionedTransaction(message)
        transaction.sign(wallet)
        val signature = connection.sendTransaction(transaction)
    }

    private val heliusUrl =  "https://mainnet.helius-rpc.com/?api-key=1fd6f762-ef2f-444a-8eae-eabd44711f31"
}