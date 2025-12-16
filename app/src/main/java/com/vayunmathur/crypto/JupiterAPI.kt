package com.vayunmathur.crypto

import com.vayunmathur.crypto.token.TokenInfo
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.sol4k.Keypair
import kotlin.math.pow

object JupiterAPI {
    private const val API_KEY = "873a272b-baf7-4a7a-b0e6-e689a33430c9"

    suspend fun getPrices(mints: List<String>): PriceResponse {
        val ids = mints.joinToString(",")
        try {
            val response: HttpResponse = client.get("https://api.jup.ag/price/v3") {
                header("x-api-key", API_KEY)
                parameter("ids", ids)
            }
            return response.body()
        } catch(e: Exception) {
            return emptyMap()
        }
    }

    @Serializable
    data class PendingOrder(
        val transaction: String,
        val requestId: String,
        val inAmount: Long,
        val outAmount: Long
    )

    suspend fun createOrder(
        inputToken: TokenInfo,
        outputToken: TokenInfo,
        amount: Double,
        taker: Keypair
    ): PendingOrder? {
        try {
            return client.get("https://api.jup.ag/ultra/v1/order") {
                header("x-api-key", API_KEY)
                parameter("inputMint", inputToken.mintAddress)
                parameter("outputMint", outputToken.mintAddress)
                parameter("amount", (amount * 10.0.pow(inputToken.decimals)).toLong())
                parameter("taker", taker.publicKey.toBase58())
            }.body()
        } catch(e: Exception) {
            return null
        }
    }

    @Serializable
    data class SwapEvent(
        val inputMint: String,
        val inputAmount: String,
        val outputMint: String,
        val outputAmount: String
    )

    @Serializable
    data class CompleteOrderResponse(
        val status: String,
        val signature: String? = null,
        val slot: String? = null,
        val code: Int? = null,
        val totalInputAmount: String? = null,
        val totalOutputAmount: String? = null,
        val inputAmountResult: String? = null,
        val outputAmountResult: String? = null,
        val swapEvents: List<SwapEvent>? = null,
    )

    suspend fun completeOrder(signedTransaction: String, requestId: String): CompleteOrderResponse? {
        try {
            return client.post("https://api.jup.ag/ultra/v1/execute") {
                header("x-api-key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("signedTransaction", signedTransaction)
                    put("requestId", requestId)
                })
            }.body()
        } catch (e: Exception) {
            return null
        }
    }
}