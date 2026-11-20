package dev.abhishekan.pulse.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.feature.auth.LoginRoute

@Composable
internal fun SignedOutApp(current: SessionState.SignedOut) {
    val nav = rememberNavController()
    val notice =
        when {
            current.storageProblem ->
                stringResource(
                    R.string.pulse_app_you_re_signed_out_secure_storage_needs_attention_retry,
                )
            current.expired ->
                stringResource(
                    R.string.pulse_app_your_session_has_expired_welcome_back_please_sign_in,
                )
            else -> null
        }
    val currentNotice by rememberUpdatedState(notice)
    NavHost(nav, startDestination = "login") {
        composable("login") { LoginRoute(currentNotice) }
    }
}
