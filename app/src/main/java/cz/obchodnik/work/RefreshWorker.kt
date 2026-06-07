package cz.obchodnik.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.obchodnik.MainActivity
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.format.MarketFormatters
import cz.obchodnik.domain.model.StaticAssetCatalog
import cz.obchodnik.widget.ObchodnikWidget
import kotlinx.coroutines.flow.first

class RefreshWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as ObchodnikApp
        val settingsRepository = app.container.settingsRepository
        val watchlistRepository = app.container.watchlistRepository
        val marketRepository = app.container.marketRepository
        val alertRepository = app.container.alertRepository

        return try {
            val settings = settingsRepository.settings.first()
            val watchlist = watchlistRepository.watchlist()
            val activeAlerts = alertRepository.enabledAlerts()

            // Combine watchlist assets and any assets with active alerts to ensure we get fresh prices for alerts
            val alertAssets = activeAlerts.mapNotNull { alert ->
                StaticAssetCatalog.assets.find { it.id == alert.assetId }
            }
            val allAssetsToRefresh = (watchlist + alertAssets).distinctBy { it.id }

            if (allAssetsToRefresh.isNotEmpty()) {
                val currency = settings.currency
                // Perform forced network refresh to update cached prices in Room database
                val result = marketRepository.refreshQuotes(allAssetsToRefresh, currency, force = true)
                if (result is cz.obchodnik.core.Result.Error) {
                    Log.e("RefreshWorker", "Failed to refresh quotes: ${result.message}")
                }

                // Evaluate price alerts using the updated quotes
                for (alert in activeAlerts) {
                    val quote = marketRepository.cachedQuote(alert.assetId, currency) ?: continue
                    val currentPrice = quote.price
                    val triggered = if (alert.above) {
                        currentPrice >= alert.target
                    } else {
                        currentPrice <= alert.target
                    }

                    if (triggered) {
                        // Deactivate the alert and record the trigger time
                        val updatedAlert = alert.copy(
                            enabled = false,
                            triggeredAt = System.currentTimeMillis(),
                            triggeredPrice = currentPrice,
                            triggeredCurrency = currency,
                        )
                        alertRepository.save(updatedAlert)

                        // If user enabled notifications, post a local system notification
                        if (settings.notificationsEnabled) {
                            val asset = StaticAssetCatalog.assets.find { it.id == alert.assetId }
                            val assetSymbol = asset?.symbol ?: alert.assetId
                            val direction = if (alert.above) "stoupl nad" else "klesl pod"
                            val formattedTarget = MarketFormatters.price(alert.target, currency)
                            val formattedCurrent = MarketFormatters.price(currentPrice, currency)

                            sendNotification(
                                context = context,
                                title = "$assetSymbol: Cenové upozornění",
                                message = "Cena aktiva $assetSymbol $direction $formattedTarget (aktuálně $formattedCurrent)."
                            )
                        }
                    }
                }
            }

            // Update all active widgets with the newly updated Room DB data
            ObchodnikWidget().updateAll(context)
            Result.success()
        } catch (e: Exception) {
            Log.e("RefreshWorker", "Error updating widget data or evaluating alerts", e)
            Result.retry()
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cenove_alerty",
                "Cenové alerty",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upozornění na splnění cenových alertů"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "cenove_alerty")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
