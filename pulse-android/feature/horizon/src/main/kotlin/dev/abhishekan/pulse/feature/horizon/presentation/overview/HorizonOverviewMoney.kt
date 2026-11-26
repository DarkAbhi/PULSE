package dev.abhishekan.pulse.feature.horizon.presentation.overview

import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

internal fun HorizonOverview.formatMoney(amount: BigDecimal): String {
    val code = currency.takeIf(UserCurrency::valid) ?: UserCurrency.code
    val fractionDigits = runCatching { Currency.getInstance(code).defaultFractionDigits }.getOrDefault(2)
    return UserCurrency.format(amount.setScale(fractionDigits.coerceAtLeast(0), RoundingMode.HALF_UP), code)
}
