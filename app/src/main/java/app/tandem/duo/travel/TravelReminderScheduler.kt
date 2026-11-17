package app.tandem.duo.travel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import app.tandem.duo.R

object TravelReminderScheduler {
    const val CHANNEL_ID = "tandem_travel_prep"
    const val EXTRA_PLAN_ID = "plan_id"
    const val EXTRA_STAGE = "stage"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Travel preparation", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Departure checklists and travel reminders"
                    enableVibration(true)
                }
            )
        }
    }

    fun schedule(context: Context, plan: TravelPlan) {
        scheduleOne(context, plan, plan.reminderAt, 1)
        scheduleOne(context, plan, plan.departureAt, 2)
    }

    private fun scheduleOne(context: Context, plan: TravelPlan, at: Long, stage: Int) {
        if (at <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TravelReminderReceiver::class.java)
            .putExtra(EXTRA_PLAN_ID, plan.id)
            .putExtra(EXTRA_STAGE, stage)
        val requestCode = (plan.id.hashCode() * 31 + stage) and 0x7fffffff
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, plan: TravelPlan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        (1..2).forEach { stage ->
            val intent = Intent(context, TravelReminderReceiver::class.java)
            val requestCode = (plan.id.hashCode() * 31 + stage) and 0x7fffffff
            val pending = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        TravelPlanStore.all(context).forEach { plan ->
            schedule(context, plan)
        }
    }
}

class TravelReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planId = intent.getStringExtra(TravelReminderScheduler.EXTRA_PLAN_ID) ?: return
        val stage = intent.getIntExtra(TravelReminderScheduler.EXTRA_STAGE, 1)
        val plan = TravelPlanStore.all(context).firstOrNull { it.id == planId } ?: return
        TravelReminderScheduler.ensureChannel(context)
        val title = if (stage == 2) "Time to head out" else "Travel prep reminder"
        val remaining = plan.checklist.count { !it.isDone }
        val body = if (stage == 2) {
            if (remaining == 0) "${plan.title}: checklist complete. Have a safe journey!" else "${plan.title}: $remaining checklist item(s) still to do."
        } else {
            "${plan.title} for ${plan.recipient} • $remaining item(s) left on the checklist."
        }
        val openApp = PendingIntent.getActivity(
            context, plan.id.hashCode(),
            Intent(context, TravelPrepActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, TravelReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body + "\n" + plan.checklist.joinToString("\n") { (if (it.isDone) "✓ " else "○ ") + it.title }))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(context).notify(planId.hashCode() + stage, notification) }
        }
    }
}

class TravelBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            TravelReminderScheduler.rescheduleAll(context)
        }
    }
}
