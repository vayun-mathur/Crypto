package com.vayunmathur.crypto.token

import kotlinx.serialization.Serializable
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID

@Serializable
data class TokenInfo(
    val symbol: String,
    val name: String,
    val category: Category,
    val mintAddress: String,
    val decimals: Int,
    val programAddress: String,
) {

    companion object {
        private val SPL_TOKEN = TOKEN_PROGRAM_ID.toBase58()
        private val TOKEN_2022 = TOKEN_2022_PROGRAM_ID.toBase58()

        enum class Category {
            NORMAL, JUPITER_LEND, XSTOCK
        }

        val SOL = TokenInfo(
            "SOL",
            "Solana",
            Category.NORMAL,
            "So11111111111111111111111111111111111111111",
            9,
            ""
        )
        val USDC = TokenInfo(
            "USDC",
            "USD Coin",
            Category.NORMAL,
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            6,
            SPL_TOKEN
        )

        val TOKEN_LIST = listOf(
            SOL,
            // STABLECOINS
            TokenInfo("EURC", "EURC", Category.NORMAL, "HzwqbKZw8HxMN6bF2yFZNrht3c2iXXzpKcFu7uBEDKtr", 6, SPL_TOKEN),
            USDC,
            TokenInfo("USDT", "USDT", Category.NORMAL, "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", 6, SPL_TOKEN),
            TokenInfo("USDS", "USDS", Category.NORMAL, "USDSwr9ApdHk5bvJKMjzff41FfuX8bSxdKcR81vTwcA", 6,SPL_TOKEN),
            TokenInfo("USDG", "USDG", Category.NORMAL, "2u1tszSeqZ3qBWF3uNGPFc8TzMk2tdiwknnRMWGWjGWH", 6, SPL_TOKEN),

            // Major Coins
            TokenInfo("WSOL", "Solana (Wrapped)", Category.NORMAL, "So11111111111111111111111111111111111111112", 9, SPL_TOKEN),
            TokenInfo("ETH", "Etherium", Category.NORMAL, "7vfCXTUXx5WJV5JADk17DUJ4ksgau7utNKj4b963voxs", 8, SPL_TOKEN),
            TokenInfo("BTC", "Bitcoin", Category.NORMAL, "3NZ9JMVBmGAqocybic2c7LQCJScmgsAZ6vQqTDzcqmJh", 8, SPL_TOKEN),

            // STOCKS
            TokenInfo("SPYx", "S&P 500", Category.XSTOCK, "XsoCS1TfEyfFhfvj8EtZ528L3CaKBDBRqRapnBbDF2W", 8, TOKEN_2022),
            TokenInfo("QQQx", "QQQ", Category.XSTOCK, "Xs8S1uUs1zvS2p7iwtsG3b6fkhpvmwz4GYU3gWAmWHZ", 8, TOKEN_2022),
            TokenInfo("GOOGLx", "Google", Category.XSTOCK, "XsCPL9dNWBMvFtTmwcCA5v3xWPSMEBCszbQdiLLq6aN", 8, TOKEN_2022),
            TokenInfo("AAPLx", "Apple", Category.XSTOCK, "XsbEhLAtcf6HdfpFZ5xEMdqW8nfAvcsP5bdudRLJzJp", 8, TOKEN_2022),
            TokenInfo("AMZNx", "Amazon", Category.XSTOCK, "Xs3eBt7uRfJX8QUs4suhyU8p2M6DoUDrJyWBa8LLZsg", 8, TOKEN_2022),
            TokenInfo("METAx", "Meta", Category.XSTOCK, "Xsa62P5mvPszXL1krVUnU5ar38bBSVcWAB6fmPCo5Zu", 8, TOKEN_2022),
            TokenInfo("NFLXx", "Netflix", Category.XSTOCK, "XsEH7wWfJJu2ZT3UCFeVfALnVA6CP5ur7Ee11KmzVpL", 8, TOKEN_2022),
            TokenInfo("TSLAx", "Tesla", Category.XSTOCK, "XsDoVfqeBukxuZHWhdvWHBhgEHjGNst4MLodqsJHzoB", 8, TOKEN_2022),
            TokenInfo("NVDAx", "Nvidia", Category.XSTOCK, "Xsc9qvGR1efVDFGLrVsmkzv3qi45LTBjeUKSPmx9qEh", 8, TOKEN_2022),
            TokenInfo("MSFTx", "Microsoft", Category.XSTOCK, "XspzcW1PRtgf6Wj92HCiZdjzKCyFekVD8P5Ueh3dRMX", 8, TOKEN_2022),
            TokenInfo("ORCLx", "Oracle", Category.XSTOCK, "XsjFwUPiLofddX5cWFHW35GCbXcSu1BCUGfxoQAQjeL", 8, TOKEN_2022),
            TokenInfo("PLTRx", "Palantir", Category.XSTOCK, "XsoBhf2ufR8fTyNSjqfU71DYGaE6Z3SUGAidpzriAA4", 8, TOKEN_2022),
            TokenInfo("GLDx", "Gold", Category.XSTOCK, "Xsv9hRk1z5ystj9MhnA7Lq4vjSsLwzL2nxrwmwtD3re", 8, TOKEN_2022),

            // Jupiter Lend
            TokenInfo("jlUSDC", "Lended USDC", Category.JUPITER_LEND, "9BEcn9aPEmhSPbPQeFGjidRiEKki46fVQDyPpSQXPA2D", 6, SPL_TOKEN),
            TokenInfo("jlUSDT", "Lended USDT", Category.JUPITER_LEND, "Cmn4v2wipYV41dkakDvCgFJpxhtaaKt11NyWV8pjSE8A", 6, SPL_TOKEN),
            TokenInfo("jlWSOL", "Lended WSOL", Category.JUPITER_LEND, "2uQsyo1fXXQkDtcpXnLofWy88PxcvnfH2L8FPSE62FVU", 6, SPL_TOKEN),
            TokenInfo("jlEURC", "Lended EURC", Category.JUPITER_LEND, "GcV9tEj62VncGithz4o4N9x6HWXARxuRgEAYk9zahNA8", 6, SPL_TOKEN),
            TokenInfo("jlUSDS", "Lended USDS", Category.JUPITER_LEND, "j14XLJZSVMcUYpAfajdZRpnfHUpJieZHS4aPektLWvh", 6, SPL_TOKEN),
            TokenInfo("jlUSDG", "Lended USDG", Category.JUPITER_LEND, "9fvHrYNw1A8Evpcj7X2yy4k4fT7nNHcA9L6UsamNHAif", 6, SPL_TOKEN)
        )

        val TOKEN_MAP = TOKEN_LIST.associateBy { it.mintAddress }
    }
}