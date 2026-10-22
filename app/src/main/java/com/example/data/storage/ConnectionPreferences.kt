package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences

data class SavedConnection(
    val host: String,
    val port: Int = 9876,
    val pin: String? = null,
    val lastConnectedAt: Long = System.currentTimeMillis()
)

class ConnectionPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("antiremote_connection", Context.MODE_PRIVATE)

    fun saveConnection(host: String, port: Int = 9876, pin: String? = null) {
        prefs.edit()
            .putString("last_host", host)
            .putInt("last_port", port)
            .putString("last_pin", pin)
            .putLong("last_connected_at", System.currentTimeMillis())
            .apply()
    }

    fun getLastConnection(): SavedConnection? {
        val host = prefs.getString("last_host", null) ?: return null
        val port = prefs.getInt("last_port", 9876)
        val pin = prefs.getString("last_pin", null)
        val timestamp = prefs.getLong("last_connected_at", 0L)
        return SavedConnection(host, port, pin, timestamp)
    }

    fun clearConnection() {
        prefs.edit().clear().apply()
    }
}
