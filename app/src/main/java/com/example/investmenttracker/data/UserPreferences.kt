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

val avatarOptions = listOf(
    R.drawable.ic_launcher_foreground
)

object UserPreferences {
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedAvatar(context: Context): Int {
        return getPreferences(context).getInt(KEY_USER_AVATAR, 0)
    }

    fun saveAvatar(context: Context, avatarResId: Int) {
        getPreferences(context).edit().putInt(KEY_USER_AVATAR, avatarResId).apply()
    }

    fun getCustomAvatarUri(context: Context): String? {
        return getPreferences(context).getString(KEY_CUSTOM_AVATAR_URI, null)
    }

    fun saveCustomAvatarUri(context: Context, uri: Uri) {
        getPreferences(context).edit().putString(KEY_CUSTOM_AVATAR_URI, uri.toString()).apply()
    }

    // Alias methods for Profile Photo Uri compatibility
    fun getProfilePhotoUri(context: Context): String? {
        return getCustomAvatarUri(context)
    }

    fun saveProfilePhotoUri(context: Context, uriString: String?) {
        getPreferences(context).edit().putString(KEY_CUSTOM_AVATAR_URI, uriString).apply()
    }

    // Default updated from "Justin" to "User"
    fun getUsername(context: Context): String {
        return getPreferences(context).getString(KEY_USERNAME, "User") ?: "User"
    }

    fun saveUsername(context: Context, name: String) {
        getPreferences(context).edit().putString(KEY_USERNAME, name).apply()
    }

    fun getNotificationsEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_NOTIFICATIONS, false)
    }

    fun saveNotificationsEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun saveBrokerData(context: Context, broker: BrokerCardItem) {
        val prefs = getPreferences(context).edit()
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_imported", broker.isImported)
        prefs.putString("${KEY_BROKER_PREFIX}${broker.id}_value", broker.accountValue)
        prefs.putString("${KEY_BROKER_PREFIX}${broker.id}_accno", broker.accountNumber)
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_balance", broker.hasBalance)
        prefs.putBoolean("${KEY_BROKER_PREFIX}${broker.id}_tx", broker.hasTransactions)
        prefs.apply()
    }

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

    fun clearBrokerData(context: Context, id: String) {
        val prefs = getPreferences(context).edit()
        prefs.remove("${KEY_BROKER_PREFIX}${id}_imported")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_value")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_accno")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_balance")
        prefs.remove("${KEY_BROKER_PREFIX}${id}_tx")
        prefs.apply()
    }

    fun savePriceAlerts(context: Context, alerts: List<com.example.investmenttracker.data.PriceAlert>) {
        val data = alerts.joinToString(";") { "${it.id}|${it.symbol}|${it.price}|${it.isAbove}|${it.isActive}" }
        getPreferences(context).edit().putString(KEY_PRICE_ALERTS, data).apply()
    }

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

    fun saveTransactions(context: Context, transactions: List<com.example.investmenttracker.data.TransactionItem>) {
        val data = transactions.joinToString(";") { 
            "${it.id}|${it.date}|${it.description}|${it.amount}|${it.isDebit}|${it.brokerage}|${it.isAuto}|${it.units}|${it.action}|${it.pricePerUnit}|${it.fees}|${it.pnl}|${it.pnlPercent}" 
        }
        getPreferences(context).edit().putString(KEY_TRANSACTIONS, data).apply()
    }

    fun loadTransactions(context: Context): List<com.example.investmenttracker.data.TransactionItem> {
        val data = getPreferences(context).getString(KEY_TRANSACTIONS, "") ?: ""
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 7) {
                com.example.investmenttracker.data.TransactionItem(
                    id = parts[0],
                    date = parts[1],
                    description = parts[2],
                    amount = parts[3],
                    isDebit = parts[4].toBoolean(),
                    brokerage = parts[5],
                    isAuto = parts[6].toBoolean(),
                    units = if (parts.size >= 8) parts[7] else "",
                    action = if (parts.size >= 9) parts[8] else "Bought",
                    pricePerUnit = if (parts.size >= 10) parts[9] else "",
                    fees = if (parts.size >= 11) parts[10] else "",
                    pnl = if (parts.size >= 12) parts[11] else "",
                    pnlPercent = if (parts.size >= 13) parts[12] else ""
                )
            } else null
        }
    }
}

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

fun getSavedStockSymbols(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedString = prefs.getString(KEY_SAVED_STOCKS, "") ?: ""
    return if (savedString.isBlank()) emptyList() else savedString.split(",")
}

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
