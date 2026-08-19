package com.example.investmenttracker.data

import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * Mock data fetchers for stocks and news.
 */

suspend fun fetchPortfolioNews(symbols: List<String>): List<LiveNewsItem> = withContext(Dispatchers.IO) {
    if (symbols.isEmpty()) return@withContext emptyList()
    
    val allNews = mutableListOf<LiveNewsItem>()
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    val now = System.currentTimeMillis()
    val fiveDaysInMillis = 5 * 24 * 60 * 60 * 1000L
    
    symbols.forEach { symbol ->
        val cleanSymbol = symbol.uppercase().trim()
        // Google News RSS query for SGX stocks
        val rssUrl = "https://news.google.com/rss/search?q=SGX:${cleanSymbol}+stock&hl=en-SG&gl=SG&ceid=SG:en"
        
        try {
            val doc = Jsoup.connect(rssUrl)
                .timeout(10000)
                .parser(Parser.xmlParser()) // Use XML parser for RSS
                .get()
            
            val items = doc.select("item")
            items.forEach { item ->
                val title = item.select("title").text()
                val link = item.select("link").text()
                val pubDate = item.select("pubDate").text()
                val source = item.select("source").text()
                
                val timestamp = try {
                    dateFormat.parse(pubDate)?.time ?: now
                } catch (e: Exception) {
                    now
                }
                
                // Only add if within the last 5 days
                if (title.isNotBlank() && link.isNotBlank() && (now - timestamp) <= fiveDaysInMillis) {
                    // Clean up title (Google News often adds the source name at the end with a hyphen)
                    val cleanTitle = if (title.contains(" - ")) title.substringBeforeLast(" - ") else title
                    
                    allNews.add(
                        LiveNewsItem(
                            title = cleanTitle,
                            source = source,
                            link = link,
                            stockSymbol = cleanSymbol,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Google News RSS fetch failed for $cleanSymbol: ${e.message}")
        }
    }
    
    // If empty, return a slightly more realistic fallback
    if (allNews.isEmpty()) {
        val now = System.currentTimeMillis()
        return@withContext symbols.flatMap { symbol ->
            listOf(
                LiveNewsItem(
                    title = "Latest market trends for $symbol",
                    source = "Market Updates",
                    link = "https://www.google.com/finance/quote/$symbol:SGX",
                    stockSymbol = symbol,
                    timestamp = now - 3600000L
                )
            )
        }.sortedByDescending { it.timestamp }
    }
    
    return@withContext allNews.sortedByDescending { it.timestamp }
}

suspend fun fetchSGXStockDynamic(symbol: String): StockItem? = withContext(Dispatchers.IO) {
    val cleanSymbol = symbol.uppercase().trim()
    val ticker = if (cleanSymbol.endsWith(".SI")) cleanSymbol else "$cleanSymbol.SI"

    // Use a single query1 v8/finance/chart call for everything: Name, Price, Points.
    val apiUrl = "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1h&range=5d"
    
    // Testing logic for Fake Stock "TEST"
    if (cleanSymbol == "TEST") {
        return@withContext StockItem(
            symbol = "TEST",
            name = "Test Notification Corp",
            currentPrice = "S$100.00",
            priceChangePercentage = "(+5.00%)",
            isGain = true,
            chartData = listOf(90f, 95f, 100f, 98f, 102f),
            high52Weeks = "110.00",
            low52Weeks = "85.00",
            currency = "SGD",
            type = "Equity",
            previousClose = "95.00"
        )
    }

    try {
        val json = Jsoup.connect(apiUrl)
            .ignoreContentType(true)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .timeout(15000)
            .execute()
            .body()

        // 1. Extract Current Price (from meta)
        val priceMatch = "\"regularMarketPrice\":\\s*([0-9.]+)".toRegex().find(json)
        val rawPrice = priceMatch?.groupValues?.get(1)?.trim() ?: return@withContext null
        
        // Truncate to 2dp without rounding
        val price = if (rawPrice.contains(".")) {
            val parts = rawPrice.split(".")
            parts[0] + "." + parts[1].take(2).padEnd(2, '0')
        } else {
            "$rawPrice.00"
        }

        // 2. Extract Previous Close (from meta)
        val prevCloseMatch = "\"previousClose\":\\s*([0-9.]+)".toRegex().find(json)
        val prevClose = prevCloseMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val prevCloseStr = String.format(Locale.US, "%.2f", prevClose)

        // 3. Extract extra info (52-week high/low, currency, type)
        val fiftyTwoHighMatch = "\"fiftyTwoWeekHigh\":\\s*([0-9.]+)".toRegex().find(json)
        val fiftyTwoLowMatch = "\"fiftyTwoWeekLow\":\\s*([0-9.]+)".toRegex().find(json)
        val currencyMatch = "\"currency\":\"([^\"]*)\"".toRegex().find(json)
        val instrumentTypeMatch = "\"instrumentType\":\"([^\"]*)\"".toRegex().find(json)

        val high52Weeks = fiftyTwoHighMatch?.groupValues?.get(1)?.let { 
            String.format(Locale.US, "%.2f", it.toDoubleOrNull() ?: 0.0) 
        } ?: "0.00"
        val low52Weeks = fiftyTwoLowMatch?.groupValues?.get(1)?.let { 
            String.format(Locale.US, "%.2f", it.toDoubleOrNull() ?: 0.0) 
        } ?: "0.00"
        val currency = currencyMatch?.groupValues?.get(1) ?: "SGD"
        val type = instrumentTypeMatch?.groupValues?.get(1)?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Equity"

        // 4. Calculate Change Percentage manually using user's formula:
        // [(regularMarketPrice - previousClose) / regularMarketPrice] * 100
        val priceNum = rawPrice.toDoubleOrNull() ?: 0.0
        val manualPercentNum = if (priceNum > 0.0) {
            ((priceNum - prevClose) / priceNum) * 100.0
        } else {
            0.0
        }
        val sign = if (manualPercentNum >= 0) "+" else ""
        val changePercentage = String.format(Locale.US, "(%s%.2f%%)", sign, manualPercentNum)

        // 4. Extract Company Name (from meta - longName is preferred now)
        val nameMatch = "\"longName\":\"([^\"]*)\"".toRegex().find(json)
            ?: "\"shortName\":\"([^\"]*)\"".toRegex().find(json)
        
        val name = nameMatch?.groupValues?.get(1)
            ?.replace("\\u0026", "&")
            ?.replace("\\u0027", "'")
            ?.replace("\\u002F", "/")
            ?.trim() ?: "$cleanSymbol Corp"

        // 5. Extract 5D Chart Points
        val closePointsRegex = "\"close\":\\[([^]]*)\\]".toRegex()
        val closePointsMatch = closePointsRegex.find(json)
        val rawPoints = closePointsMatch?.groupValues?.get(1) ?: ""
        
        val chartData = rawPoints.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .filter { it > 0 }

        val isGain = priceNum > prevClose

        if (priceNum > 0.0) {
            return@withContext StockItem(
                symbol = cleanSymbol,
                name = name,
                currentPrice = "S$$price",
                priceChangePercentage = changePercentage,
                isGain = isGain,
                chartData = if (chartData.size >= 2) chartData else generateFallbackChart(isGain, cleanSymbol),
                high52Weeks = high52Weeks,
                low52Weeks = low52Weeks,
                currency = currency,
                type = type,
                previousClose = prevCloseStr
            )
        }
    } catch (e: Exception) {
        println("Yahoo query1 API Failed for $ticker: ${e.message}")
    }

    return@withContext null
}

// Helper for consistent simulated data if API chart points fail
private fun generateFallbackChart(isGain: Boolean, symbol: String): List<Float> {
    val seededRandom = Random(symbol.hashCode().toLong())
    val points = mutableListOf<Float>()
    var current = if (isGain) 30f else 70f
    points.add(current)
    val bias = if (isGain) 0.4f else 0.6f
    repeat(14) {
        val step = (seededRandom.nextFloat() - bias) * 20f
        current = (current + step).coerceIn(10f, 90f)
        points.add(current)
    }
    return points
}