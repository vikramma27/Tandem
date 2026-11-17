package app.tandem.duo.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tandem.duo.data.local.*
import app.tandem.duo.travel.TravelPrepActivity
import app.tandem.duo.ui.TandemViewModel
import app.tandem.duo.ui.components.DuoKineticOrbitEmblem
import app.tandem.duo.ui.components.LiveAttentionNudgeBanner
import app.tandem.duo.ui.theme.*

private enum class TandemDestination(val label: String) {
    TODAY("Today"), SHARED("Shared"), RITUALS("Rituals"), TRAVEL("Travel"), YOU("You")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTandemScreen(viewModel: TandemViewModel) {
    val context = LocalContext.current
    val config by viewModel.userConfig.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val selectedSpaceId by viewModel.selectedSpaceId.collectAsState()
    val selectedMode by viewModel.selectedModeFilter.collectAsState()
    val items by viewModel.filteredItems.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val activeNudges by viewModel.activeNudges.collectAsState()
    val rituals by viewModel.rituals.collectAsState()
    val pairInviteCode by viewModel.pairInviteCode.collectAsState()
    val pairBusy by viewModel.pairBusy.collectAsState()
    val pairMessage by viewModel.pairMessage.collectAsState()
    val authUser by viewModel.authUser.collectAsState()

    var destination by remember { mutableStateOf(TandemDestination.TODAY) }
    var showProfilePairingSheet by remember { mutableStateOf(false) }
    var showComposerSheet by remember { mutableStateOf(false) }
    var showNudgeModal by remember { mutableStateOf(false) }
    var showNewSpaceModal by remember { mutableStateOf(false) }
    var showNewRitualDialog by remember { mutableStateOf(false) }

    val selfLabel = config.selfDisplayName.ifBlank { "You" }
    val partnerLabel = config.partnerDisplayName.ifBlank { "Your person" }
    val openCount = allItems.count { !it.isCompleted }
    val completedCount = allItems.count { it.isCompleted }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                TandemDestination.entries.forEach { item ->
                    val icon = when (item) {
                        TandemDestination.TODAY -> Icons.Default.Home
                        TandemDestination.SHARED -> Icons.Default.List
                        TandemDestination.RITUALS -> Icons.Default.Favorite
                        TandemDestination.TRAVEL -> Icons.Default.FlightTakeoff
                        TandemDestination.YOU -> Icons.Default.Person
                    }
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            destination = item
                            if (item == TandemDestination.TRAVEL) context.startActivity(Intent(context, TravelPrepActivity::class.java))
                        },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == TandemDestination.TODAY || destination == TandemDestination.SHARED) {
                ExtendedFloatingActionButton(
                    onClick = { showComposerSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add a little thing") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        when (destination) {
            TandemDestination.TODAY -> TodayDashboard(
                modifier = Modifier.fillMaxSize().padding(padding),
                selfLabel = selfLabel, partnerLabel = partnerLabel, isPaired = config.isPaired,
                openCount = openCount, completedCount = completedCount, spacesCount = spaces.size,
                tasks = allItems, nudges = activeNudges,
                onPair = { showProfilePairingSheet = true }, onAdd = { showComposerSheet = true },
                onNudge = { showNudgeModal = true },
                onTravel = { context.startActivity(Intent(context, TravelPrepActivity::class.java)) },
                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                onReact = { task, reaction -> viewModel.reactToTask(task, reaction) },
                onReplyNudge = { nudge, reply -> viewModel.acknowledgeNudge(nudge, reply) }
            )
            TandemDestination.SHARED -> SharedListsScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                spaces = spaces, selectedSpaceId = selectedSpaceId, selectedMode = selectedMode,
                tasks = items, selfLabel = selfLabel, partnerLabel = partnerLabel,
                onSelectSpace = { viewModel.selectSpace(it) }, onSelectMode = { viewModel.selectModeFilter(it) },
                onNewSpace = { showNewSpaceModal = true }, onAdd = { showComposerSheet = true },
                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                onReact = { task, reaction -> viewModel.reactToTask(task, reaction) }
            )
            TandemDestination.RITUALS -> RitualsScreen(
                modifier = Modifier.fillMaxSize().padding(padding), rituals = rituals,
                isPaired = config.isPaired, partnerLabel = partnerLabel,
                onAdd = { showNewRitualDialog = true }, onToggleToday = { viewModel.toggleRitualSelf(it) }
            )
            TandemDestination.TRAVEL -> TravelHubScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                onOpenTravel = { context.startActivity(Intent(context, TravelPrepActivity::class.java)) }
            )
            TandemDestination.YOU -> ProfileScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                name = selfLabel, email = authUser?.email.orEmpty(), partnerName = partnerLabel,
                isPaired = config.isPaired, onEditProfile = { showProfilePairingSheet = true },
                onSignOut = { viewModel.signOut() }
            )
        }
    }

    if (showProfilePairingSheet) {
        ProfileAndPairingDialog(
            currentConfig = config, isPaired = config.isPaired, inviteCode = pairInviteCode,
            busy = pairBusy, message = pairMessage, onDismiss = { showProfilePairingSheet = false },
            onSaveName = { viewModel.updateMyDisplayName(it) },
            onCreateInvite = { viewModel.createPairAndInvite() }, onJoinPair = { viewModel.joinPair(it) }
        )
    }
    if (showNewSpaceModal) {
        NewSpaceDialog(onDismiss = { showNewSpaceModal = false }, onCreate = { name, subtitle ->
            viewModel.createNewSpace(name, subtitle); showNewSpaceModal = false
        })
    }
    if (showComposerSheet) {
        AddItemModalSheet(
            selfLabel = selfLabel, partnerLabel = partnerLabel, isPaired = config.isPaired,
            onDismiss = { showComposerSheet = false },
            onSubmit = { title, note, mode, assignee, urgency, qty, unit, aisle, cost, store ->
                viewModel.createTaskOrShoppingItem(title, note, mode, assignee, urgency, qty, unit, aisle, cost, store)
                showComposerSheet = false
            }
        )
    }
    if (showNudgeModal) {
        SendNudgeDialog(onDismiss = { showNudgeModal = false }, onSend = { message, urgency ->
            viewModel.sendPartnerNudge(message, urgency); showNudgeModal = false
        })
    }
    if (showNewRitualDialog) {
        AddRitualDialog(onDismiss = { showNewRitualDialog = false }, onCreate = { title, timeWindow ->
            viewModel.addRitual(title, timeWindow); showNewRitualDialog = false
        })
    }
}

@Composable
private fun TodayDashboard(
    modifier: Modifier, selfLabel: String, partnerLabel: String, isPaired: Boolean,
    openCount: Int, completedCount: Int, spacesCount: Int,
    tasks: List<TaskItemEntity>, nudges: List<NudgePingEntity>,
    onPair: () -> Unit, onAdd: () -> Unit, onNudge: () -> Unit, onTravel: () -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit, onReact: (TaskItemEntity, String) -> Unit,
    onReplyNudge: (NudgePingEntity, String) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("A little more together.", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (isPaired) "Good things are easier when you share them." else "Your space is ready. Invite your person when you are.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { ConnectionCard(selfLabel, partnerLabel, isPaired, onPair, onNudge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("To do", openCount.toString(), Icons.Default.List, Modifier.weight(1f))
                MetricCard("Done", completedCount.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                MetricCard("Spaces", spacesCount.toString(), Icons.Default.Favorite, Modifier.weight(1f))
            }
        }
        item {
            Text("Make a moment", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard("Add something", "A task or list item", Icons.Default.Add, Modifier.weight(1f), onAdd)
                QuickActionCard("Plan a trip", "Get ready together", Icons.Default.FlightTakeoff, Modifier.weight(1f), onTravel)
            }
        }
        if (nudges.isNotEmpty()) {
            item { Text("A little note for you", style = MaterialTheme.typography.titleLarge) }
            items(nudges, key = { it.id }) { nudge ->
                LiveAttentionNudgeBanner(
                    nudge = nudge,
                    senderLabel = if (nudge.sender == MemberSlot.SELF) selfLabel else partnerLabel,
                    onQuickReply = { onReplyNudge(nudge, it) }
                )
            }
        }
        val nextTasks = tasks.filterNot { it.isCompleted }.take(4)
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("In motion", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(nextTasks.size.toString() + " shown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (nextTasks.isEmpty()) {
            item { EmptyStateCard("Nothing pressing right now", "Enjoy the breathing room, or add a small thing you would like to remember.", "Add a task", onAdd) }
        } else {
            items(nextTasks, key = { it.id }) { task ->
                TandemTaskCard(task, selfLabel, partnerLabel, { onToggleTask(task) }, { onReact(task, it) })
            }
        }
    }
}

@Composable
private fun SharedListsScreen(
    modifier: Modifier, spaces: List<SpaceEntity>, selectedSpaceId: String?, selectedMode: ListMode?,
    tasks: List<TaskItemEntity>, selfLabel: String, partnerLabel: String,
    onSelectSpace: (String?) -> Unit, onSelectMode: (ListMode?) -> Unit,
    onNewSpace: () -> Unit, onAdd: () -> Unit,
    onToggleTask: (TaskItemEntity) -> Unit, onReact: (TaskItemEntity, String) -> Unit
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Shared lists", style = MaterialTheme.typography.headlineLarge)
                Text("Keep the little things from slipping through.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your spaces", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onNewSpace) { Text("New space") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedSpaceId == null, onClick = { onSelectSpace(null) }, label = { Text("Everything") })
                spaces.forEach { space ->
                    FilterChip(selected = selectedSpaceId == space.id, onClick = { onSelectSpace(space.id) }, label = { Text(space.name) })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedMode == null, onClick = { onSelectMode(null) }, label = { Text("All types") })
                ListMode.entries.forEach { mode ->
                    FilterChip(selected = selectedMode == mode, onClick = { onSelectMode(mode) }, label = { Text(mode.label) })
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your shared items", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("Add") }
            }
        }
        if (tasks.isEmpty()) {
            item { EmptyStateCard("A fresh page", "Add a task, a shopping item, or something to ask your partner.", "Add an item", onAdd) }
        } else {
            items(tasks, key = { it.id }) { task ->
                TandemTaskCard(task, selfLabel, partnerLabel, { onToggleTask(task) }, { onReact(task, it) })
            }
        }
    }
}

@Composable
private fun RitualsScreen(
    modifier: Modifier, rituals: List<RitualCheckInEntity>, isPaired: Boolean, partnerLabel: String,
    onAdd: () -> Unit, onToggleToday: (RitualCheckInEntity) -> Unit
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Little rituals", style = MaterialTheme.typography.headlineLarge)
                Text("Small moments become your favourite traditions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Consistency, not perfection.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Check in when you make time for each other.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your rituals", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Add") }
            }
        }
        if (rituals.isEmpty()) {
            item { EmptyStateCard("Start with one small ritual", "A good-morning message, an evening walk, or ten minutes without phones.", "Create a ritual", onAdd) }
        } else {
            items(rituals, key = { it.id }) { ritual ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ritual.title, style = MaterialTheme.typography.titleMedium)
                                if (ritual.timeWindow.isNotBlank()) Text(ritual.timeWindow, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (ritual.streakDays > 0) {
                                AssistChip(onClick = {}, label = { Text(ritual.streakDays.toString() + " day streak") }, leadingIcon = { Icon(Icons.Default.Favorite, null) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = ritual.selfDoneToday, onCheckedChange = { onToggleToday(ritual) })
                            Text(if (ritual.selfDoneToday) "You checked in today" else "I did this today", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (isPaired) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (ritual.partnerDoneToday) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = if (ritual.partnerDoneToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(if (ritual.partnerDoneToday) partnerLabel + " checked in today" else "Waiting for " + partnerLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelHubScreen(modifier: Modifier, onOpenTravel: () -> Unit) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Travel together", style = MaterialTheme.typography.headlineLarge)
            Text("A calmer departure starts with a shared plan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.FlightTakeoff, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                    Text("Leave the last-minute rush behind.", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Create a departure plan, choose who is travelling, tick off essentials together, and let the traveller's phone handle local reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Button(onClick = onOpenTravel) { Text("Open Travel Prep"); Icon(Icons.Default.ArrowForward, null) }
                }
            }
        }
        item {
            Text("How it works", style = MaterialTheme.typography.titleLarge)
            FeatureLine(Icons.Default.People, "Shared plans", "Both people see the same departure and checklist.")
            FeatureLine(Icons.Default.NotificationsActive, "Native reminders", "The traveller receives reminders on their own Android device.")
            FeatureLine(Icons.Default.Sync, "Background catch-up", "Changes are reconciled when a device reconnects.")
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier, name: String, email: String, partnerName: String, isPaired: Boolean,
    onEditProfile: () -> Unit, onSignOut: () -> Unit
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Your space", style = MaterialTheme.typography.headlineLarge)
            Text("The people and preferences behind your Tandem.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isPaired) "Connected with " + partnerName else "Your space is currently solo", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text(if (isPaired) "Manage connection" else "Invite your person")
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign out")
            }
        }
    }
}

@Composable
private fun ConnectionCard(selfLabel: String, partnerLabel: String, isPaired: Boolean, onPair: () -> Unit, onNudge: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (isPaired) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isPaired) Icons.Default.People else Icons.Default.PersonAdd, null, tint = if (isPaired) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (isPaired) selfLabel + " + " + partnerLabel else "Make it a shared space", style = MaterialTheme.typography.titleMedium)
                Text(if (isPaired) "Your shared space is connected." else "Invite your person to share plans, lists and rituals.", style = MaterialTheme.typography.bodySmall, color = if (isPaired) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isPaired) IconButton(onClick = onNudge) { Icon(Icons.Default.NotificationsActive, contentDescription = "Send a nudge") }
            else TextButton(onClick = onPair) { Text("Invite") }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(actionLabel); Icon(Icons.Default.ArrowForward, null) }
        }
    }
}

@Composable
private fun TandemTaskCard(
    item: TaskItemEntity, selfLabel: String, partnerLabel: String,
    onToggle: () -> Unit, onReact: (String) -> Unit
) {
    val assignee = when (item.assignedTo) {
        MemberSlot.SELF -> selfLabel
        MemberSlot.PARTNER -> partnerLabel
        MemberSlot.BOTH -> "Both of you"
    }
    var reactionMenu by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.isCompleted, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                    if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { reactionMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Task reactions") }
                    DropdownMenu(expanded = reactionMenu, onDismissRequest = { reactionMenu = false }) {
                        listOf("On it", "Done soon", "Need hand").forEach { reaction ->
                            DropdownMenuItem(
                                text = { Text(if (item.reactionChip == reaction) "✓ " + reaction else reaction) },
                                onClick = { onReact(reaction); reactionMenu = false }
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(item.mode.label) })
                AssistChip(onClick = {}, label = { Text(assignee) })
                if (item.urgency == UrgencyLevel.URGENT_PING) {
                    AssistChip(onClick = {}, label = { Text("Needs attention") }, leadingIcon = { Icon(Icons.Default.NotificationsActive, null) })
                }
            }
            if (item.isCompleted) Text("Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            else if (!item.reactionChip.isNullOrBlank()) Text(item.reactionChip.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AddRitualDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var timeWindow by remember { mutableStateOf("Every evening") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a little ritual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("What will you do together?") }, singleLine = true)
                OutlinedTextField(value = timeWindow, onValueChange = { timeWindow = it }, label = { Text("When? (optional)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onCreate(title.trim(), timeWindow.trim()) }, enabled = title.isNotBlank()) { Text("Create ritual") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileAndPairingDialog(
    currentConfig: UserConfigEntity,
    isPaired: Boolean,
    inviteCode: String?,
    busy: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onCreateInvite: () -> Unit,
    onJoinPair: (String) -> Unit
) {
    var selfName by remember { mutableStateOf(currentConfig.selfDisplayName) }
    var codeToJoin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = IvoryText,
        title = { Text("Your Tandem connection", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your profile", color = ChampagneGold, style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = selfName,
                    onValueChange = { selfName = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onSaveName(selfName) },
                    enabled = selfName.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SAVE NAME") }

                Divider(color = Color.White.copy(alpha = 0.12f))
                Text("Connect your second person", color = ChampagneGold, style = MaterialTheme.typography.labelLarge)
                if (isPaired) {
                    Text(
                        "You're connected with ${currentConfig.partnerDisplayName.ifBlank { "your partner" }}. Shared plans and updates will appear for both of you.",
                        color = SageMint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "Create a private invite and share it with your partner, or enter an invite they created. Codes expire after 48 hours and can only be used once.",
                        color = MutedPewter,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onCreateInvite,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaCoral, contentColor = ObsidianBg)
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(if (inviteCode == null) "CREATE INVITE CODE" else "GENERATE NEW INVITE")
                    }
                    if (!inviteCode.isNullOrBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(ElevatedSlate).padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("YOUR PRIVATE INVITE", style = MaterialTheme.typography.labelSmall, color = MutedPewter)
                            Text(inviteCode, style = MaterialTheme.typography.headlineSmall, color = IvoryText, fontWeight = FontWeight.Bold)
                            Text("Share this code with your partner", style = MaterialTheme.typography.bodySmall, color = SageMint)
                        }
                    }
                    OutlinedTextField(
                        value = codeToJoin,
                        onValueChange = { codeToJoin = it.uppercase() },
                        label = { Text("Partner's invite code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { onJoinPair(codeToJoin) },
                        enabled = codeToJoin.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("JOIN WITH INVITE CODE") }
                }
                if (!message.isNullOrBlank()) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = CrimsonPulse)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE", color = ChampagneGold) }
        }
    )
}

@Composable
private fun NewSpaceDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = IvoryText,
        title = { Text("Create Space", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Space Name") })
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Description (Optional)") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, subtitle) },
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaCoral, contentColor = ObsidianBg)
            ) {
                Text("CREATE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedPewter) }
        }
    )
}

@Composable
private fun AddItemModalSheet(
    selfLabel: String,
    partnerLabel: String,
    isPaired: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        note: String,
        mode: ListMode,
        assignedTo: MemberSlot,
        urgency: UrgencyLevel,
        qty: String,
        unit: String,
        aisle: ShoppingAisle,
        cost: Double,
        store: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ListMode.TODO) }
    var assignee by remember { mutableStateOf(MemberSlot.SELF) }
    var urgency by remember { mutableStateOf(UrgencyLevel.SOON) }
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pc") }
    var priceText by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = IvoryText,
        title = { Text("New Item", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Details") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListMode.entries.take(3).forEach { m ->
                        FilterPill(label = m.badge, selected = mode == m, onClick = { mode = m })
                    }
                }
                if (isPaired) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterPill(label = selfLabel, selected = assignee == MemberSlot.SELF, onClick = { assignee = MemberSlot.SELF })
                        FilterPill(label = partnerLabel, selected = assignee == MemberSlot.PARTNER, onClick = { assignee = MemberSlot.PARTNER })
                        FilterPill(label = "Both", selected = assignee == MemberSlot.BOTH, onClick = { assignee = MemberSlot.BOTH })
                    }
                }
                AnimatedVisibility(visible = mode == ListMode.SHOPPING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Est. Price") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        title,
                        note,
                        mode,
                        assignee,
                        urgency,
                        qty,
                        unit,
                        ShoppingAisle.PRODUCE,
                        priceText.toDoubleOrNull() ?: 0.0,
                        store
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaCoral, contentColor = ObsidianBg)
            ) {
                Text("ADD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedPewter) }
        }
    )
}

@Composable
private fun SendNudgeDialog(
    onDismiss: () -> Unit,
    onSend: (String, UrgencyLevel) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(UrgencyLevel.URGENT_PING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = IvoryText,
        title = { Text("Send Attention Ping", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UrgencyLevel.entries.forEach { u ->
                        FilterPill(label = u.label, selected = urgency == u, onClick = { urgency = u })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(message, urgency) },
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBg)
            ) {
                Text("SEND PING", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedPewter) }
        }
    )
}
