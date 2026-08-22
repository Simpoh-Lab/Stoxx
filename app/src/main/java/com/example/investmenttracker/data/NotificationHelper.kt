package com.example.investmenttracker.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.investmenttracker.R

object NotificationHelper {
    // Channel 1: High Importance for immediate price targets
    private const val PRICE_CHANNEL_ID = "price_alerts_channel_v4"
    private const val PRICE_CHANNEL_NAME = "Price Alerts"
    
    // Channel 2: Default Importance for general news or portfolio summaries
    private const val NEWS_CHANNEL_ID = "portfolio_news_channel_v4"
    private const val NEWS_CHANNEL_NAME = "Portfolio News"

    // Creates separate notification channels for different types of alerts on Android Oreo and above.
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Setup Price Alerts Channel (High Priority)
            val priceChannel = NotificationChannel(PRICE_CHANNEL_ID, PRICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Immediate notifications when stocks hit your target prices"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(priceChannel)

            // 2. Setup News/Updates Channel (Default Priority)
            val newsChannel = NotificationChannel(NEWS_CHANNEL_ID, NEWS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Periodic news and updates regarding your watched stocks"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(newsChannel)
            
            android.util.Log.d("NotificationHelper", "Channels v4 created successfully")
        }
    }

    // Builds and displays a push notification for stock price targets (System 1).
    fun showPriceAlert(context: Context, symbol: String, currentPrice: String, targetPrice: String, isAbove: Boolean) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val title = "Price Alert: $symbol"
        val direction = if (isAbove) "reached above" else "dropped below"
        val content = "$symbol has $direction \$$targetPrice (Current: $currentPrice)"
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.image_secondary)

        val builder = NotificationCompat.Builder(context, PRICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_application_submark)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFF4CAF50.toInt())

        NotificationManagerCompat.from(context).notify(symbol.hashCode(), builder.build())
    }

    // Builds and displays a push notification for stock news or portfolio updates (System 2).
    fun showNewsAlert(context: Context, symbol: String, newsTitle: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val title = "Breaking News: $symbol"
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.image_secondary)

        val builder = NotificationCompat.Builder(context, NEWS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_application_submark)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(newsTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF4CAF50.toInt())

        NotificationManagerCompat.from(context).notify(newsTitle.hashCode(), builder.build())
    }
}