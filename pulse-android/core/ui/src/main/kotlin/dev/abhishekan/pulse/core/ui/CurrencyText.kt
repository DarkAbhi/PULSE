package dev.abhishekan.pulse.core.ui

import dev.abhishekan.pulse.core.domain.model.UserCurrency
import java.math.BigDecimal

fun currencyText(amount: BigDecimal?): UiText = UiText.Literal(UserCurrency.format(amount))
