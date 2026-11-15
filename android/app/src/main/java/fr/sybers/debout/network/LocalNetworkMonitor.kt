package fr.sybers.debout.network

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import fr.sybers.debout.domain.Broadcast
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address

data class LocalNetwork(val network: Network, val address: Inet4Address, val prefixLength: Int) {
    val broadcast: Inet4Address get() = Broadcast.of(address, prefixLength)
}

/**
 * The phone's Wi-Fi or Ethernet network, or null when it has none. Reads only link
 * properties, never the SSID, so no location permission is needed.
 */
class LocalNetworkMonitor(private val connectivityManager: ConnectivityManager) {

    val localNetwork: Flow<LocalNetwork?> = callbackFlow {
        val networks = LinkedHashMap<Network, LocalNetwork>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                val local = linkProperties.toLocalNetwork(network)
                if (local == null) networks.remove(network) else networks[network] = local
                trySend(networks.values.firstOrNull())
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(networks.values.firstOrNull())
            }
        }
        trySend(null)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            // A LAN without internet access is still a LAN we can wake machines on.
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun LinkProperties.toLocalNetwork(network: Network): LocalNetwork? =
        linkAddresses
            .firstOrNull { it.address is Inet4Address }
            ?.let { LocalNetwork(network, it.address as Inet4Address, it.prefixLength) }
}
