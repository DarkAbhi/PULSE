package dev.abhishekan.pulse.core.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Single account currency; selecting a code never converts stored amounts. */
object UserCurrency {
    const val DEFAULT = "GBP"
    val available: List<String> =
        """
        AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BRL BSD BTN BWP BYN BZD
        CAD CDF CHF CLP CNY COP CRC CUC CUP CVE CZK DJF DKK DOP DZD EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP
        GMD GNF GTQ GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD
        KYD KZT LAK LBP LKR LRD LSL LYD MAD MDL MGA MKD MMK MNT MOP MRU MUR MVR MWK MXN MYR MZN NAD NGN NIO
        NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SLE SLL
        SOS SRD SSP STN SVC SYP SZL THB TJS TMT TND TOP TRY TTD TWD TZS UAH UGX USD UYU UZS VES VND VUV WST
        XAF XCD XCG XOF XPF YER ZAR ZMW ZWG ZWL
        """.trimIndent().split(Regex("\\s+"))
    private val selected = MutableStateFlow(DEFAULT)
    val changes = selected.asStateFlow()
    val code: String get() = selected.value

    fun valid(code: String): Boolean = code in available

    fun select(code: String) {
        require(valid(code))
        selected.value = code
    }

    fun reset() = select(DEFAULT)

    fun label(code: String): String =
        "$code — ${runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)}"

    fun format(
        amount: BigDecimal?,
        code: String = this.code,
    ): String {
        require(valid(code))
        if (amount == null) return "—"
        val unit = runCatching { Currency.getInstance(code) }.getOrNull()
        val formatter =
            if (unit == null) {
                NumberFormat.getNumberInstance(Locale.getDefault())
            } else {
                NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { currency = unit }
            }
        formatter.minimumFractionDigits = (unit?.defaultFractionDigits ?: 2).coerceAtLeast(0)
        formatter.maximumFractionDigits = maxOf(formatter.minimumFractionDigits, amount.scale())
        val formatted = formatter.format(amount)
        return if (unit == null) "$code $formatted" else formatted
    }

    fun symbol(code: String = this.code): String {
        require(valid(code))
        return runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }.getOrDefault(code)
    }
}
