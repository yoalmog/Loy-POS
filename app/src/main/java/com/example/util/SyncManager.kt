package com.example.util

import android.content.Context
import com.example.data.repository.PosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncState(val label: String) {
    SYNCED("Synced"),
    SYNCING("Syncing..."),
    OFFLINE_LOCAL_MODE("Offline (Local Mode)"),
    ERROR("Sync Warning")
}

class SyncManager(
    context: Context,
    private val repository: PosRepository
) {
    val networkMonitor = NetworkMonitor(context)

    private val _syncState = MutableStateFlow(
        if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE_LOCAL_MODE
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    performAutoSync()
                } else {
                    _syncState.value = SyncState.OFFLINE_LOCAL_MODE
                }
            }
        }
    }

    fun triggerSync() {
        scope.launch {
            if (!networkMonitor.isOnline.value) {
                _syncState.value = SyncState.OFFLINE_LOCAL_MODE
                return@launch
            }

            _syncState.value = SyncState.SYNCING
            try {
                // Simulate cloud sync pass over Room transactions
                delay(1200)
                _pendingSyncCount.value = 0
                _lastSyncTimestamp.value = System.currentTimeMillis()
                _syncState.value = SyncState.SYNCED
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
            }
        }
    }

    private suspend fun performAutoSync() {
        _syncState.value = SyncState.SYNCING
        delay(1000)
        _pendingSyncCount.value = 0
        _lastSyncTimestamp.value = System.currentTimeMillis()
        _syncState.value = SyncState.SYNCED
    }
}
