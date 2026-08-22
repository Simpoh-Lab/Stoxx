package com.example.investmenttracker

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.investmenttracker.R
import com.example.investmenttracker.data.BrokerCardItem

private const val PREFS_NAME = "stock_prefs"
private const val KEY_SAVED_STOCKS = "saved_stock_symbols"
private const val KEY_USER_AVATAR = "user_avatar_res_id"
private const val KEY_CUSTOM_AVATAR_URI = "custom_avatar_uri"
private const val KEY_USERNAME = "username"
private const val KEY_NOTIFICATIONS = "notifications_enabled"
private const val KEY_BROKER_PREFIX = "broker_"
private const val KEY_PRICE_ALERTS = "price_alerts"
private const val KEY_TRANSACTIONS = "transactions"
private const val KEY_LAST_NEWS_TIME = "last_news_timestamp"

val avatarOptions = listOf(
    R.drawable.ic_application_submark
)

object UserPreferences {
    // Accesses the application's private SharedPreferences instance.
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Retrieves the resource ID of the user's selected built-in avatar.
    fun getSavedAvatar(context: Context): Int {
        return getPreferences(context).getInt(KEY_USER_AVATAR, 0)
    }

    // Persists the resource ID of the user's chosen built-in avatar.
    fun saveAvatar(context: Context, avatarResId: Int) {
        getPreferences(context).edit().putInt(KEY_USER_AVATAR, avatarResId).apply()
    }

    // Gets the file URI of the user's uploaded custom profile photo.
    fun getCustomAvatarUri(context: Context): String? {
        return getPreferences(context).getString(KEY_CUSTOM_AVATAR_URI, null)
    }

    // Persists the file URI of the user's uploaded custom profile photo.
    fun saveCustomAvatarUri(context: Context, uri: Uri) {
        getPreferences(context).edit().putString(KEY_CUSTOM_AVATAR_URI, uri.toString()).apply()
    }

    // Alias methods for Profile Photo Uri compatibility
    // Alias for getting the custom avatar URI.
    fun getProfilePhotoUri(context: Context): String? {
        return getCustomAvatarUri(context)
    }

    // Alias for saving the custom avatar URI string.
    fun saveProfilePhotoUri(context: Context, uriString: String?) {
        getPreferences(context).edit().putString(KEY_CUSTOM_AVATAR_URI, uriString).apply()
    }

    // Default updated from "Justin" to "User"
    // Retrieves the saved username, defaulting to 'User' if not set.
    fun getUsername(context: Context): String {
        return getPreferences(context).getString(KEY_USERNAME, "User") ?: "User"
    }

    // Persists the user's entered username.
    fun saveUsername(context: Context, name: String) {
        getPreferences(context).edit().putString(KEY_USERNAME, name).apply()
    }

    // Checks if the user has enabled push notifications in app settings.
    fun getNotificationsEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATIONS, false)
    }

    // Persists the user's notification preference.
    fun saveNotificationsEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    // Persists account details and import status for a specific brokerage.
    fun saveBrokerData(context: Context, broker: BrokerCardItem) {
        val prefs = getPreferences(context).edit()
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_imported", broker.isImported)
        prefs.putString("${KEY_BROKER_PREFIX}${broker.id}_value", broker.accountValue)
        prefs.putString("${KEY_BROKER_PREFIX}${broker.id}_accno", broker.accountNumber)
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_balance", broker.hasBalance)
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_tx", broker.hasTransactions)
        prefs.apply()
    }

    // Loads saved account details and import status for a specific brokerage.
    fun loadBrokerData(context: Context, id: String, name: String): BrokerCardItem {
        val prefs = getPreferences(context)
        return BrokerCardItem(
            id = id,
            name = name,
            isImported = prefs.getBoolean("${KEY_BROKER_PREFIX}${id}_imported", false),
            accountValue = prefs.getString("${KEY_BROKER_PREFIX}${id}_value", "") ?: "",
            accountNumber = prefs.getString("${KEY_BROKER_PREFIX}${id}_accno", "") ?: "",
            hasBalance = prefs.getBoolean("${KEY_BROKER_PREFIX}${id}_balance", false),
            hasTransactions = prefs.getBoolean("${KEY_BROKER_PREFIX}${id}_tx", false)
        )
    }

    // Removes all saved data for a specific brokerage.
    fun clearBrokerData(context: Context, id: String) {
        val prefs = getPreferences(context).edit()
        prefs.remove("${KEY_BROKER_PREFIX}${id}_imported")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_value")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_accno")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_balance")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_tx")
        prefs.apply()
    }

    // Serializes and persists the list of active stock price alerts.
    fun savePriceAlerts(context: Context, alerts: List<com.example.investmenttracker.data.PriceAlert>) {
        val data = alerts.joinToString(";") { "${it.id}|${it.symbol}|${it.price}|${it.isAbove}|${it.isActive}" }
        getPreferences(context).edit().putString(KEY_PRICE_ALERTS, data).apply()
    }

    // Deserializes and loads the list of active stock price alerts.
    fun loadPriceAlerts(context: Context): List<com.example.investmenttracker.data.PriceAlert> {
        val data = getPreferences(context).getString(KEY_PRICE_ALERTS, "") ?: ""
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                com.example.investmenttracker.data.PriceAlert(
                    id = parts[0],
                    symbol = parts[1],
                    price = parts[2],
                    isAbove = parts[3].toBoolean(),
                    isActive = if (parts.size >= 5) parts[4].toBoolean() else true
                )
            } else null
        }
    }

    // Serializes and persists the list of financial transactions.
    fun saveTransactions(context: Context, transactions: List<com.example.investmenttracker.data.TransactionItem>) {
        val data = transactions.joinToString(";") { 
            "${it.id}|${it.date}|${it.time}|${it.symbol}|${it.description}|${it.amount}|${it.isDebit}|${it.brokerage}|${it.isAuto}|${it.units}|${it.action}|${it.pricePerUnit}|${it.fees}|${it.pnl}|${it.pnlPercent}" 
        }
        getPreferences(context).edit().putString(KEY_TRANSACTIONS, data).apply()
    }

    // Deserializes and loads the list of financial transactions.
    fun loadTransactions(context: Context): List<com.example.investmenttracker.data.TransactionItem> {
        val data = getPreferences(context).getString(KEY_TRANSACTIONS, "") ?: ""
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 7) {
                com.example.investmenttracker.data.TransactionItem(
                    id = parts[0],
                    date = parts[1],
                    time = if (parts.size >= 15) parts[2] else "",
                    symbol = if (parts.size >= 15) parts[3] else "",
                    description = if (parts.size >= 15) parts[4] else parts[2],
                    amount = if (parts.size >= 15) parts[5] else parts[3],
                    isDebit = if (parts.size >= 15) parts[6].toBoolean() else parts[4].toBoolean(),
                    brokerage = if (parts.size >= 15) parts[7] else parts[5],
                    isAuto = if (parts.size >= 15) parts[8].toBoolean() else parts[6].toBoolean(),
                    units = if (parts.size >= 15) parts[9] else (if (parts.size >= 8) parts[7] else ""),
                    action = if (parts.size >= 15) parts[10] else (if (parts.size >= 9) parts[8] else "Bought"),
                    pricePerUnit = if (parts.size >= 15) parts[11] else (if (parts.size >= 10) parts[9] else ""),
                    fees = if (parts.size >= 15) parts[12] else (if (parts.size >= 11) parts[10] else ""),
                    pnl = if (parts.size >= 15) parts[13] else (if (parts.size >= 12) parts[11] else ""),
                    pnlPercent = if (parts.size >= 15) parts[14] else (if (parts.size >= 13) parts[12] else "")
                )
            } else null
        }
    }

    // Retrieves the timestamp of the last news article the user was notified about.
    fun getLastNewsTimestamp(context: Context): Long {
        return getPreferences(context).getLong(KEY_LAST_NEWS_TIME, 0L)
    }

    // Persists the timestamp of the latest news article to avoid duplicate notifications.
    fun saveLastNewsTimestamp(context: Context, timestamp: Long) {
        getPreferences(context).edit().putLong(KEY_LAST_NEWS_TIME, timestamp).apply()
    }
}

// Loads the user's avatar or initializes a random one if none exists.
fun loadOrInitializeUserAvatar(context: Context): Int {
    val savedAvatar = UserPreferences.getSavedAvatar(context)
    return if (savedAvatar != 0) {
        savedAvatar
    } else {
        val initialAvatar = avatarOptions.random()
        UserPreferences.saveAvatar(context, initialAvatar)
        initialAvatar
    }
}

// Retrieves the list of stock ticker symbols currently in the user's watchlist.
fun getSavedStockSymbols(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedString = prefs.getString(KEY_SAVED_STOCKS, "") ?: ""
    return if (savedString.isBlank()) emptyList() else savedString.split(",")
}

// Adds a new unique stock symbol to the user's watchlist.
fun saveStockSymbol(context: Context, rawSymbol: String) {
    val cleanTicker = rawSymbol.uppercase().trim()
    val currentList = getSavedStockSymbols(context).toMutableList()

    if (!currentList.contains(cleanTicker)) {
        currentList.add(cleanTicker)
        val updatedString = currentList.joinToString(",")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_STOCKS, updatedString).apply()
    }
}

// Removes a specific stock symbol from the user's watchlist.
fun removeStockSymbol(context: Context, rawSymbol: String) {
    val cleanTicker = rawSymbol.removePrefix("SGX:").uppercase().trim()
    val currentList = getSavedStockSymbols(context).toMutableList()

    if (currentList.contains(cleanTicker)) {
        currentList.remove(cleanTicker)
        val updatedString = currentList.joinToString(",")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_STOCKS, updatedString).apply()
    }
}
