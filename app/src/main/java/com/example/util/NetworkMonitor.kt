package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialOnlineStatus())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(checkInitialConnectionType())
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialOnlineStatus(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkInitialConnectionType(): String {
        val activeNetwork = connectivityManager.activeNetwork ?: return "Offline"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Offline"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                updateConnectionType(network)
            }

            override fun onLost(network: Network) {
                _isOnline.value = checkInitialOnlineStatus()
                if (!_isOnline.value) {
                    _connectionType.value = "Offline"
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isOnline.value = hasInternet
                if (hasInternet) {
                    updateConnectionType(network)
                } else {
                    _connectionType.value = "Offline"
                }
            }
        })
    }

    private fun updateConnectionType(network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _connectionType.value = when {
            capabilities == null -> "Connected"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }
}
