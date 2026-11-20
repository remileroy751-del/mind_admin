package com.harrwama.admin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object Notifier {
    const val CHANNEL = "orders"
    const val CHANNEL_MSG = "messages"
    const val EXTRA_OPEN_ORDERS = "open_orders"
    const val EXTRA_OPEN_MESSAGES = "open_messages"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm != null && nm.getNotificationChannel(CHANNEL_MSG) == null) {
                val ch2 = NotificationChannel(CHANNEL_MSG, "Nouveaux messages", NotificationManager.IMPORTANCE_HIGH)
                ch2.description = "Alerte à chaque message reçu via le bouton « Nous écrire » du site"
                nm.createNotificationChannel(ch2)
            }
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "Nouvelles commandes", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "Alerte à chaque nouvelle commande à livrer reçue depuis le site"
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun notifyNewOrders(ctx: Context, orders: List<OrderItem>) {
        if (orders.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)

        val intent = Intent(ctx, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra(EXTRA_OPEN_ORDERS, true)
        val pi = PendingIntent.getActivity(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        if (orders.size == 1) {
            val o = orders[0]
            title = "Nouvelle commande à livrer"
            text = o.prenom + " — " + o.quantite + " × " + o.produit
        } else {
            title = orders.size.toString() + " nouvelles commandes à livrer"
            text = orders.joinToString(", ") { it.prenom }
        }

        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_order)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(orders[0].id.hashCode(), n)
    }
}

fun Notifier.notifyNewMessages(ctx: Context, items: List<MessageItem>) {
    if (items.isEmpty()) return
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return
    ensureChannel(ctx)
    val intent = Intent(ctx, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.putExtra(Notifier.EXTRA_OPEN_MESSAGES, true)
    val pi = PendingIntent.getActivity(
        ctx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val title: String
    val text: String
    if (items.size == 1) {
        title = "Nouveau message de " + items[0].nom
        text = items[0].message
    } else {
        title = items.size.toString() + " nouveaux messages"
        text = items.joinToString(", ") { it.nom }
    }
    val n = NotificationCompat.Builder(ctx, Notifier.CHANNEL_MSG)
        .setSmallIcon(R.drawable.ic_stat_order)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pi)
        .build()
    NotificationManagerCompat.from(ctx).notify(items[0].id.hashCode() + 7, n)
}

/** Vérifie régulièrement (toutes les ~15 min, même appli fermée) s'il y a de nouvelles commandes. */
class OrderCheckWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("harrwama_admin", Context.MODE_PRIVATE)
        val url = prefs.getString("site_url", "") ?: ""
        val tok = prefs.getString("token", "") ?: ""
        if (url.isBlank() || tok.isBlank()) return Result.success()
        try {
            val client = NetlifyClient(tok, url)
            checkMessages(client, prefs)
            val orders = client.listOrders()
            val seen = HashSet<String>(prefs.getStringSet("seen_orders", emptySet()) ?: emptySet())
            val initDone = prefs.getBoolean("seen_init", false)
            val fresh = if (initDone) orders.filter { !seen.contains(it.id) } else emptyList()
            if (fresh.isNotEmpty()) Notifier.notifyNewOrders(applicationContext, fresh)
            for (o in orders) seen.add(o.id)
            prefs.edit().putStringSet("seen_orders", seen).putBoolean("seen_init", true).apply()
        } catch (e: Exception) {
            // pas de réseau ou Netlify injoignable : on réessaiera à la prochaine échéance
        }
        return Result.success()
    }

    private fun checkMessages(client: NetlifyClient, prefs: android.content.SharedPreferences) {
        try {
            val msgs = client.listMessages()
            val seen = HashSet<String>(prefs.getStringSet("seen_messages", emptySet()) ?: emptySet())
            val initDone = prefs.getBoolean("seen_msg_init", false)
            val fresh = if (initDone) msgs.filter { !seen.contains(it.id) } else emptyList()
            if (fresh.isNotEmpty()) Notifier.notifyNewMessages(applicationContext, fresh)
            for (m in msgs) seen.add(m.id)
            prefs.edit().putStringSet("seen_messages", seen).putBoolean("seen_msg_init", true).apply()
        } catch (e: Exception) {
            // ignoré : on réessaiera à la prochaine échéance
        }
    }
}

object OrderScheduler {
    fun schedule(ctx: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val req = PeriodicWorkRequestBuilder<OrderCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork("order_check", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}
