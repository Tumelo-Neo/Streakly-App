package com.example.streakly.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.streakly.data.HabitManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val TAG = "NetworkMonitor"

    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            // Sync offline actions when coming online
            CoroutineScope(Dispatchers.IO).launch {
                HabitManager.syncOfflineActions()
            }
            // Update online status through HabitManager
            updateOnlineStatus(true)
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            // Update online status through HabitManager
            updateOnlineStatus(false)
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        // This will be handled by the OfflineManager through HabitManager
        // The network monitor just detects changes, HabitManager handles the status
        Log.d(TAG, "Network status changed: ${if (isOnline) "Online" else "Offline"}")
    }

    fun isConnected(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}