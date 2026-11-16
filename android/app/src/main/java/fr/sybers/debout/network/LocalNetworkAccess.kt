package fr.sybers.debout.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** From this SDK, apps targeting it need ACCESS_LOCAL_NETWORK or every LAN packet fails. */
const val LOCAL_NETWORK_PROTECTION_SDK = 37

fun Context.hasLocalNetworkAccess(): Boolean =
    Build.VERSION.SDK_INT < LOCAL_NETWORK_PROTECTION_SDK ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
