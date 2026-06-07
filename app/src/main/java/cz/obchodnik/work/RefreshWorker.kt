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
import cz.obchodnik.domain.AlertEvaluator
import cz.obchodnik.domain.PortfolioHistory
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
        val portfolioRepository = app.container.portfolioRepository

        return try {
            val settings = settingsRepository.settings.first()
            val watchlist = watchlistRepository.watchlist()
            val activeAlerts = alertRepository.enabledAlerts()
            val holdings = portfolioRepository.observeHoldings().first()

            // Combine watchlist assets, alert assets and portfolio assets so all have fresh prices.
            val alertAssets = activeAlerts.mapNotNull { alert ->
                StaticAssetCatalog.assets.find { it.id == alert.assetId }
            }
            val holdingAssets = holdings.mapNotNull { holding ->
                watchlist.find { it.id == holding.assetId }
                    ?: StaticAssetCatalog.assets.find { it.id == holding.assetId }
            }
            val allAssetsToRefresh = (watchlist + alertAssets + holdingAssets).distinctBy { it.id }

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
                    val outcome = AlertEvaluator.evaluate(
                        alert = alert,
                        price = currentPrice,
                        currency = currency,
                        now = System.currentTimeMillis(),
                    ) ?: continue

                    alertRepository.save(outcome.updatedAlert)

                    // If user enabled notifications, post a local system notification
                    if (outcome.notify && settings.notificationsEnabled) {
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

                if (holdings.isNotEmpty()) {
                    var totalValue = 0.0
                    var totalInvested = 0.0
                    for (holding in holdings) {
                        val price = marketRepository.cachedQuote(holding.assetId, currency)?.price
                            ?: holding.avgPrice
                        totalValue += holding.qty * price
                        totalInvested += holding.qty * holding.avgPrice
                    }
                    portfolioRepository.recordSnapshot(
                        dayStartMillis = PortfolioHistory.startOfDayMillis(System.currentTimeMillis()),
                        totalValue = totalValue,
                        totalInvested = totalInvested,
                        currency = currency,
                    )
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
