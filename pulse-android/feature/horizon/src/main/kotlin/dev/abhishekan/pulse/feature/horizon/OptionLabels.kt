package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

internal fun horizonOption(value: String): UiText = when (value) {
    "debit" -> uiText(R.string.pulse_horizon_option_debit)
    "credit" -> uiText(R.string.pulse_horizon_option_credit)
    "housing" -> uiText(R.string.pulse_horizon_option_housing)
    "investment" -> uiText(R.string.pulse_horizon_option_investment)
    "bill" -> uiText(R.string.pulse_horizon_option_bill)
    "subscription" -> uiText(R.string.pulse_horizon_option_subscription)
    "debt" -> uiText(R.string.pulse_horizon_option_debt)
    "other" -> uiText(R.string.pulse_horizon_option_other)
    "monthly" -> uiText(R.string.pulse_horizon_option_monthly)
    "yearly" -> uiText(R.string.pulse_horizon_option_yearly)
    "active" -> uiText(R.string.pulse_horizon_option_active)
    "inactive" -> uiText(R.string.pulse_horizon_option_inactive)
    "paused" -> uiText(R.string.pulse_horizon_option_paused)
    "cancelled" -> uiText(R.string.pulse_horizon_option_cancelled)
    else -> UiText.Literal(value)
}
