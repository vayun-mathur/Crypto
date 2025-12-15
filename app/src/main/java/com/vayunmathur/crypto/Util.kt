package com.vayunmathur.crypto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

@Composable
fun MaximizedRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        content()
    }
}

@Composable
fun BackButton(backStack: NavBackStack<NavKey>) {
    IconButton(onClick = { backStack.removeLastOrNull() }) {
        Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = "Back")
    }
}



private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private val BASE58_INDEXES = IntArray(128) { -1 }.apply {
    for (i in BASE58_ALPHABET.indices) {
        this[BASE58_ALPHABET[i].code] = i
    }
}

fun String.decodeBase58Bytes(): ByteArray {
    if (this.isEmpty()) return ByteArray(0)
    var zeros = 0
    while (zeros < this.length && this[zeros] == '1') {
        zeros++
    }
    val decoded = ByteArray(this.length)
    var length = 0
    for (i in zeros until this.length) {
        val c = this[i]
        val digit = if (c.code < 128) BASE58_INDEXES[c.code] else -1
        if (digit == -1) {
            throw IllegalArgumentException("Invalid Base58 character: $c")
        }
        var carry = digit
        for (j in 0 until length) {
            val temp = (decoded[j].toInt() and 0xFF) * 58 + carry
            decoded[j] = temp.toByte()
            carry = temp ushr 8
        }

        while (carry > 0) {
            decoded[length++] = carry.toByte()
            carry = carry ushr 8
        }
    }
    val output = ByteArray(zeros + length)
    for (i in 0 until length) {
        output[zeros + length - 1 - i] = decoded[i]
    }
    return output
}

fun Double.displayAmount(): String {
    return if (abs(this) < 1) {
        // Up to 3 significant figures (no scientific notation)
        val ax = abs(this)
        if (ax == 0.0) return "0"

        // number of decimals needed so total sig figs = 3
        val digitsBeforeDecimal = floor(log10(ax)).toInt()  // negative for < 1
        val decimals = 3 - (digitsBeforeDecimal + 1)

        val df = DecimalFormat("#.${"#".repeat(decimals)}")
        df.format(this)
    } else {
        // Up to 2 decimals
        val df = DecimalFormat("#.##")
        df.format(this)
    }
}