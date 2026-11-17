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
import app.tandem.duo.ui.theme.TandemTheme
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }

    fun refresh() { plans = TravelPlanStore.all(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Travel Prep"); Text("Leave nothing behind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = { (context as? ComponentActivity)?.finish() }) { Icon(Icons.Default.FlightTakeoff, null) } },
                actions = { IconButton(onClick = { showReliability = true }) { Icon(Icons.Default.NotificationsActive, contentDescription = "Notification reliability") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("PLAN A DEPARTURE") })
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
                        Text("Create a departure plan, add the essentials, and schedule a reminder on this phone. Checklist progress is saved on this device.", style = MaterialTheme.typography.bodyMedium)
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
                            Text("Plan a trip and build a checklist for the person travelling.", style = MaterialTheme.typography.bodySmall)
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
                                })
                                Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                if (task.isDone) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("Reminder: ${dateFormat.format(Date(plan.reminderAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showCreate) {
        CreateTravelPlanDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, recipient, departureAt, reminderAt, checklist ->
                val plan = TravelPlan(title = title, recipient = recipient, departureAt = departureAt, reminderAt = reminderAt, checklist = checklist)
                TravelPlanStore.save(context, plan)
                TravelReminderScheduler.schedule(context, plan)
                if (Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                refresh()
                showCreate = false
            }
        )
    }
    if (showReliability) {
        NotificationReliabilityDialog(onDismiss = { showReliability = false })
    }
}

@Composable
private fun CreateTravelPlanDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, Long, List<TravelChecklistItem>) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("Partner") }
    var departure by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 3); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }) }
    var reminderLead by remember { mutableIntStateOf(60) }
    var checklistText by remember { mutableStateOf("Phone and charger\nWallet / ID\nKeys\nTickets / booking details\nWater bottle\nMedicines / essentials") }
    val formatter = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan a departure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Trip / reason") }, singleLine = true)
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Who is travelling?") }, singleLine = true)
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, m, d ->
                        departure.set(Calendar.YEAR, y); departure.set(Calendar.MONTH, m); departure.set(Calendar.DAY_OF_MONTH, d)
                    }, departure.get(Calendar.YEAR), departure.get(Calendar.MONTH), departure.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Date: ${formatter.format(departure.time)}") }
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        departure.set(Calendar.HOUR_OF_DAY, h); departure.set(Calendar.MINUTE, m); departure.set(Calendar.SECOND, 0)
                    }, departure.get(Calendar.HOUR_OF_DAY), departure.get(Calendar.MINUTE), false).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Departure time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(departure.time)}") }
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
                enabled = title.isNotBlank() && recipient.isNotBlank() && departure.timeInMillis > System.currentTimeMillis() + reminderLead * 60_000L,
                onClick = {
                    val tasks = checklistText.lines().map { it.trim() }.filter { it.isNotBlank() }.distinct().map { TravelChecklistItem(title = it) }
                    onCreate(title.trim(), recipient.trim(), departure.timeInMillis, departure.timeInMillis - reminderLead * 60_000L, tasks)
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
