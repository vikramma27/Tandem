package app.tandem.duo.travel

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import app.tandem.duo.ui.theme.TandemTheme
import app.tandem.duo.data.remote.TandemSupabase
import app.tandem.duo.data.remote.TravelPlanRecord
import app.tandem.duo.data.remote.TravelChecklistRecord
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TravelPrepActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TravelReminderScheduler.ensureChannel(this)
        setContent { TandemTheme { TravelPrepScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelPrepScreen() {
    val context = LocalContext.current
    var plans by remember { mutableStateOf(TravelPlanStore.all(context)) }
    var showCreate by remember { mutableStateOf(false) }
    var showReliability by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var pairId by remember { mutableStateOf<String?>(null) }
    var partnerUserId by remember { mutableStateOf<String?>(null) }
    var partnerName by remember { mutableStateOf("Partner") }
    var syncMessage by remember { mutableStateOf("Connecting to your shared space…") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }

    fun refresh() { plans = TravelPlanStore.all(context) }

    LaunchedEffect(Unit) {
        runCatching {
            val user = TandemSupabase.currentUser() ?: error("Sign in to sync your travel plans.")
            currentUserId = user.id
            val membership = TandemSupabase.currentPair()
            if (membership == null) {
                syncMessage = "Pair with your second person in Tandem to share travel plans."
            } else {
                pairId = membership.pairId
                val profiles = TandemSupabase.pairProfiles(membership.pairId)
                val partner = profiles.firstOrNull { it.id != user.id }
                partnerUserId = partner?.id
                partnerName = partner?.displayName?.ifBlank { "Partner" } ?: "Partner"
                syncMessage = "Connected • plans sync across both phones"
                val initial = TandemSupabase.loadTravelPlans(membership.pairId)
                syncRemotePlans(context, membership.pairId, initial, user.id, partnerName)
                refresh()
                TandemSupabase.watchTravelPlans(membership.pairId).collect { updated ->
                    syncRemotePlans(context, membership.pairId, updated, user.id, partnerName)
                    refresh()
                }
            }
        }.onFailure {
            syncMessage = it.message ?: "Travel sync is temporarily unavailable. Your saved plans remain on this phone."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Travel Prep"); Text("Leave nothing behind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = { (context as? ComponentActivity)?.finish() }) { Icon(Icons.Default.FlightTakeoff, null) } },
                actions = { IconButton(onClick = { showReliability = true }) { Icon(Icons.Default.NotificationsActive, contentDescription = "Notification reliability") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (pairId != null && currentUserId != null) showCreate = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("PLAN A DEPARTURE") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("A little care before they leave.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Create a departure plan, choose who is travelling, and keep the checklist in sync on both phones. The traveller's phone schedules the local reminders.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(syncMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (pairId == null) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Connect your second person first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Open Tandem → profile / pairing, create an invite code and connect your partner. Travel Prep will then become a shared space.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (plans.isEmpty()) {
                item {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FlightTakeoff, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("No departures planned yet", fontWeight = FontWeight.SemiBold)
                            Text("Plan a trip and build a checklist together.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(plans, key = { it.id }) { plan ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("For ${plan.recipient} • Departure ${dateFormat.format(Date(plan.departureAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                TravelReminderScheduler.cancel(context, plan)
                                TravelPlanStore.delete(context, plan.id)
                                if (plan.pairId != null) {
                                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                                        runCatching { TandemSupabase.deleteTravelPlan(plan.id) }
                                    }
                                }
                                refresh()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Delete plan") }
                        }
                        val done = plan.checklist.count { it.isDone }
                        LinearProgressIndicator(progress = { if (plan.checklist.isEmpty()) 0f else done.toFloat() / plan.checklist.size }, modifier = Modifier.fillMaxWidth())
                        Text("$done of ${plan.checklist.size} ready", style = MaterialTheme.typography.labelMedium)
                        plan.checklist.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = task.isDone, onCheckedChange = { checked ->
                                    val updated = plan.copy(checklist = plan.checklist.map { if (it.id == task.id) it.copy(isDone = checked) else it })
                                    TravelPlanStore.save(context, updated)
                                    refresh()
                                    val uid = currentUserId
                                    if (uid != null) {
                                        (context as? ComponentActivity)?.lifecycleScope?.launch {
                                            runCatching { pushRemoteTravelPlan(updated, uid) }
                                        }
                                    }
                                })
                                Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (task.isDone) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("Reminder: ${dateFormat.format(Date(plan.reminderAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Shared with ${if (plan.recipientUserId == currentUserId) "you" else partnerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showReliability = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CHECK NOTIFICATION RELIABILITY")
                }
            }
        }
    }

    if (showCreate && pairId != null && currentUserId != null) {
        val recipientOptions = buildList {
            add(currentUserId!! to "Me")
            partnerUserId?.let { add(it to partnerName) }
        }
        CreateTravelPlanDialog(
            recipientOptions = recipientOptions,
            onDismiss = { showCreate = false },
            onCreate = { title, recipientId, recipientLabel, departureAt, reminderAt, checklist ->
                val pair = pairId!!
                val uid = currentUserId!!
                val plan = TravelPlan(
                    title = title,
                    recipient = recipientLabel,
                    pairId = pair,
                    recipientUserId = recipientId,
                    departureAt = departureAt,
                    reminderAt = reminderAt,
                    checklist = checklist
                )
                TravelPlanStore.save(context, plan)
                if (recipientId == uid) TravelReminderScheduler.schedule(context, plan)
                if (Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                refresh()
                showCreate = false
                (context as? ComponentActivity)?.lifecycleScope?.launch {
                    runCatching { pushRemoteTravelPlan(plan, uid) }
                        .onFailure { syncMessage = "Saved on this phone; cloud sync will retry when connected." }
                }
            }
        )
    }
    if (showReliability) {
        NotificationReliabilityDialog(onDismiss = { showReliability = false })
    }
}

private suspend fun pushRemoteTravelPlan(plan: TravelPlan, currentUserId: String) {
    val pair = plan.pairId ?: return
    val record = TravelPlanRecord(
        id = plan.id,
        pairId = pair,
        createdBy = currentUserId,
        recipientUserId = plan.recipientUserId,
        title = plan.title,
        departureAt = Instant.ofEpochMilli(plan.departureAt).toString(),
        reminderAt = Instant.ofEpochMilli(plan.reminderAt).toString(),
        timezone = java.util.TimeZone.getDefault().id,
        updatedAt = Instant.now().toString()
    )
    val checklist = plan.checklist.mapIndexed { index, item ->
        TravelChecklistRecord(
            id = item.id,
            planId = plan.id,
            title = item.title,
            sortOrder = index,
            isDone = item.isDone,
            completedBy = if (item.isDone) currentUserId else null,
            completedAt = if (item.isDone) Instant.now().toString() else null,
            updatedAt = Instant.now().toString()
        )
    }
    TandemSupabase.saveTravelPlan(record, checklist)
}

private suspend fun syncRemotePlans(
    context: android.content.Context,
    pairId: String,
    records: List<TravelPlanRecord>,
    currentUserId: String,
    partnerName: String
) {
    val remoteIds = records.map { it.id }.toSet()
    TravelPlanStore.all(context).filter { it.pairId == pairId && it.id !in remoteIds }.forEach {
        TravelReminderScheduler.cancel(context, it)
        TravelPlanStore.delete(context, it.id)
    }
    records.forEach { record ->
        val wasAlreadySaved = TravelPlanStore.all(context).any { it.id == record.id }
        val checklist = TandemSupabase.loadChecklist(record.id).map {
            TravelChecklistItem(id = it.id, title = it.title, isDone = it.isDone)
        }
        val departure = Instant.parse(record.departureAt).toEpochMilli()
        val reminder = record.reminderAt?.let { Instant.parse(it).toEpochMilli() } ?: departure
        val isRecipient = record.recipientUserId == currentUserId
        val local = TravelPlan(
            id = record.id,
            title = record.title,
            recipient = if (isRecipient) "Me" else partnerName,
            pairId = record.pairId,
            recipientUserId = record.recipientUserId,
            departureAt = departure,
            reminderAt = reminder,
            checklist = checklist
        )
        TravelPlanStore.save(context, local)
        if (isRecipient) {
            TravelReminderScheduler.schedule(context, local)
            if (!wasAlreadySaved && TravelPlanStore.markSharedNotificationShown(context, local.id)) {
                TravelReminderScheduler.notifyPlanShared(context, local)
            }
        } else TravelReminderScheduler.cancel(context, local)
    }
}

@Composable
private fun CreateTravelPlanDialog(
    recipientOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Long, Long, List<TravelChecklistItem>) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var recipientId by remember { mutableStateOf(recipientOptions.firstOrNull()?.first.orEmpty()) }
    var recipientLabel by remember { mutableStateOf(recipientOptions.firstOrNull()?.second.orEmpty()) }
    var departureMillis by remember { mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis) }
    var reminderLead by remember { mutableIntStateOf(60) }
    var checklistText by remember { mutableStateOf("Phone and charger\nWallet / ID\nKeys\nTickets / booking details\nWater bottle\nMedicines / essentials") }
    val formatter = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan a departure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Trip / reason") }, singleLine = true)
                Text("Who is travelling?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipientOptions.forEach { option ->
                        FilterChip(
                            selected = recipientId == option.first,
                            onClick = { recipientId = option.first; recipientLabel = option.second },
                            label = { Text(option.second) }
                        )
                    }
                }
                OutlinedButton(onClick = {
                    val current = Calendar.getInstance().apply { timeInMillis = departureMillis }
                    DatePickerDialog(context, { _, y, m, d ->
                        val updated = Calendar.getInstance().apply { timeInMillis = departureMillis }
                        updated.set(Calendar.YEAR, y); updated.set(Calendar.MONTH, m); updated.set(Calendar.DAY_OF_MONTH, d)
                        departureMillis = updated.timeInMillis
                    }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Date: ${formatter.format(Date(departureMillis))}") }
                OutlinedButton(onClick = {
                    val current = Calendar.getInstance().apply { timeInMillis = departureMillis }
                    TimePickerDialog(context, { _, h, m ->
                        val updated = Calendar.getInstance().apply { timeInMillis = departureMillis }
                        updated.set(Calendar.HOUR_OF_DAY, h); updated.set(Calendar.MINUTE, m); updated.set(Calendar.SECOND, 0)
                        departureMillis = updated.timeInMillis
                    }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Departure time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(departureMillis))}") }
                Text("Remind before departure", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 60, 120).forEach { mins ->
                        FilterChip(selected = reminderLead == mins, onClick = { reminderLead = mins }, label = { Text(if (mins < 60) "$mins min" else "${mins / 60} hr") })
                    }
                }
                OutlinedTextField(
                    value = checklistText,
                    onValueChange = { checklistText = it },
                    label = { Text("Checklist • one item per line") },
                    minLines = 4,
                    maxLines = 7
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && recipientId.isNotBlank() && departureMillis > System.currentTimeMillis() + reminderLead * 60_000L,
                onClick = {
                    val tasks = checklistText.lines().map { it.trim() }.filter { it.isNotBlank() }.distinct().map { TravelChecklistItem(title = it) }
                    onCreate(title.trim(), recipientId, recipientLabel, departureMillis, departureMillis - reminderLead * 60_000L, tasks)
                }
            ) { Text("SAVE & SCHEDULE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun NotificationReliabilityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val brand = Build.BRAND.orEmpty()
    val display = Build.DISPLAY.orEmpty()
    val fingerprint = Build.FINGERPRINT.orEmpty()
    val rom = when {
        manufacturer.contains("xiaomi", true) || brand.contains("poco", true) || brand.contains("redmi", true) -> "Xiaomi / Redmi / POCO • MIUI or HyperOS family"
        manufacturer.contains("oppo", true) || brand.contains("realme", true) -> "OPPO / realme • ColorOS or realme UI family"
        manufacturer.contains("vivo", true) || brand.contains("iqoo", true) -> "vivo / iQOO • Funtouch OS or OriginOS family"
        manufacturer.contains("samsung", true) -> "Samsung • One UI"
        manufacturer.contains("oneplus", true) -> "OnePlus • OxygenOS / ColorOS family"
        manufacturer.contains("huawei", true) || brand.contains("honor", true) -> "Huawei / HONOR • EMUI or MagicOS family"
        manufacturer.contains("google", true) -> "Google • Pixel Android"
        display.contains("lineage", true) || fingerprint.contains("lineage", true) -> "LineageOS-based custom ROM detected"
        display.contains("crdroid", true) || fingerprint.contains("crdroid", true) -> "crDroid-based custom ROM detected"
        display.contains("pixelos", true) || fingerprint.contains("pixelos", true) -> "PixelOS-based custom ROM detected"
        else -> "Android build • ROM family could not be identified confidently"
    }
    val notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    val batteryExempt = if (Build.VERSION.SDK_INT >= 23) powerManager.isIgnoringBatteryOptimizations(context.packageName) else true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification reliability") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(rom, fontWeight = FontWeight.SemiBold)
                Text("Build: ${Build.DISPLAY.ifBlank { "Not reported" }}", style = MaterialTheme.typography.bodySmall)
                StatusLine("App notifications", if (notificationsOn) "Enabled" else "Disabled")
                StatusLine("Battery optimisation exemption", if (batteryExempt) "Unrestricted / exempt" else "Optimised")
                Text("For best reliability:", fontWeight = FontWeight.SemiBold)
                Text("1. Allow Tandem notifications and enable lock-screen/banner alerts.\n2. Set battery usage to Unrestricted / Don't optimise.\n3. On some ROMs, enable Auto-start and allow background activity.\n4. Keep Tandem out of Sleeping apps / deep-clean lists.")
                Text("Android and some custom ROMs can still delay or suppress alarms after force-stop, aggressive memory cleaning, or vendor-specific restrictions. This screen cannot override those system controls.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                }) { Text("NOTIFICATION SETTINGS") }
                TextButton(onClick = {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                        else context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }) { Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "EXACT ALARM ACCESS" else "BATTERY OPTIMISATION SETTINGS") }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }) { Text("BATTERY OPTIMISATION SETTINGS") }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
                }) { Text("APP INFO / BACKGROUND SETTINGS") }
                TextButton(onClick = onDismiss) { Text("DONE") }
            }
        }
    )
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}


/**
 * A 15-minute network-constrained safety net. Realtime handles active sessions;
 * this worker catches up on plans created while this device was offline.
 */
class TravelSyncWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val user = TandemSupabase.currentUser() ?: return Result.success()
            val membership = TandemSupabase.currentPair() ?: return Result.success()
            val partner = TandemSupabase.pairProfiles(membership.pairId)
                .firstOrNull { it.id != user.id }?.displayName?.ifBlank { "Partner" } ?: "Partner"
            val plans = TandemSupabase.loadTravelPlans(membership.pairId)
            syncRemotePlans(applicationContext, membership.pairId, plans, user.id, partner)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "tandem-travel-background-sync"

        fun schedule(context: android.content.Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<TravelSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
