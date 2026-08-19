package com.example.investmenttracker.data

// Model for Brokerage items
data class BrokerCardItem(
    val id: String,
    val name: String,
    val isImported: Boolean = false,
    val accountValue: String = "",
    val accountNumber: String = "",
    val hasBalance: Boolean = false,
    val hasTransactions: Boolean = false
)

// Data class for Stock item
data class StockItem(
    val symbol: String,
    val name: String,
    val longName: String = "",
    val currentPrice: String,
    val priceChangePercentage: String,
    val isGain: Boolean,
    val chartData: List<Float>,
    val high52Weeks: String = "0.00",
    val low52Weeks: String = "0.00",
    val currency: String = "SGD",
    val type: String = "Equity",
    val previousClose: String = "0.00",
    val marketTiming: String = "9:00 - 17:00"
)

// Data class for Price Alert
data class PriceAlert(
    val id: String,
    val symbol: String,
    val price: String,
    val isAbove: Boolean,
    val isActive: Boolean = true
)

// Data class for Live News item
data class LiveNewsItem(
    val title: String,
    val source: String,
    val link: String,
    val stockSymbol: String,
    val timestamp: Long
)

// Data class for Transaction item
data class TransactionItem(
    val id: String,
    val date: String,
    val description: String,
    val amount: String,
    val isDebit: Boolean,
    val brokerage: String,
    val isAuto: Boolean,
    val units: String = "",
    val action: String = "Bought", // Bought, Sold, Dividend
    val pricePerUnit: String = "",
    val fees: String = "",
    val pnl: String = "",
    val pnlPercent: String = ""
)
