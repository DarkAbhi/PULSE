package dev.abhishekan.pulse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun LocalNetworkAccess(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT < 37) {
        content()
        return
    }

    val context = LocalContext.current
    val permission = Manifest.permission.ACCESS_LOCAL_NETWORK

    fun hasPermission() = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasPermission()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    LifecycleResumeEffect(Unit) {
        granted = hasPermission()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        if (!hasPermission() && !requested) {
            requested = true
            request.launch(permission)
        }
    }

    if (granted) {
        content()
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                PulseText(stringResource(R.string.debug_local_network_required))
                Button(onClick = { request.launch(permission) }) {
                    PulseText(stringResource(R.string.debug_allow_local_network), style = PulseTextStyle.Label)
                }
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    )
                }) {
                    PulseText(stringResource(R.string.debug_open_app_settings), style = PulseTextStyle.Label)
                }
            }
        }
    }
}
