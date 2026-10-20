package dev.abhishekan.pulse.feature.horizon

import androidx.compose.ui.res.stringResource

@androidx.compose.runtime.Composable
internal fun horizonOption(value: String): String =
    when (value) {
        "debit" -> stringResource(R.string.pulse_horizon_option_debit)
        "credit" -> stringResource(R.string.pulse_horizon_option_credit)
        "housing" -> stringResource(R.string.pulse_horizon_option_housing)
        "investment" -> stringResource(R.string.pulse_horizon_option_investment)
        "bill" -> stringResource(R.string.pulse_horizon_option_bill)
        "subscription" -> stringResource(R.string.pulse_horizon_option_subscription)
        "debt" -> stringResource(R.string.pulse_horizon_option_debt)
        "other" -> stringResource(R.string.pulse_horizon_option_other)
        "monthly" -> stringResource(R.string.pulse_horizon_option_monthly)
        "yearly" -> stringResource(R.string.pulse_horizon_option_yearly)
        "active" -> stringResource(R.string.pulse_horizon_option_active)
        "inactive" -> stringResource(R.string.pulse_horizon_option_inactive)
        "paused" -> stringResource(R.string.pulse_horizon_option_paused)
        "cancelled" -> stringResource(R.string.pulse_horizon_option_cancelled)
        else -> value
    }
