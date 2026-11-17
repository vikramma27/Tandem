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

    var showProfilePairingSheet by remember { mutableStateOf(false) }
    var showComposerSheet by remember { mutableStateOf(false) }
    var showNudgeModal by remember { mutableStateOf(false) }
    var showNewSpaceModal by remember { mutableStateOf(false) }

    val selfLabel = config.selfDisplayName.ifBlank { "Me (Unconfigured)" }
    val partnerLabel = if (config.isPaired && config.partnerDisplayName.isNotBlank()) {
        config.partnerDisplayName
    } else {
        "Not Paired"
    }

    val completedCount = allItems.count { it.isCompleted }
    val harmonyRatio = if (allItems.isEmpty()) 0.15f else completedCount.toFloat() / allItems.size.toFloat()

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { context.startActivity(Intent(context, TravelPrepActivity::class.java)) },
                    containerColor = ElevatedSlate,
                    contentColor = SageMint,
                    icon = { Icon(Icons.Default.FlightTakeoff, contentDescription = "Travel prep") },
                    text = { Text("TRAVEL", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                )
                if (config.isPaired) {
                    ExtendedFloatingActionButton(
                        onClick = { showNudgeModal = true },
                        containerColor = ElevatedSlate,
                        contentColor = ChampagneGold,
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Ping") },
                        text = { Text("PING", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { showComposerSheet = true },
                    containerColor = TerracottaCoral,
                    contentColor = ObsidianBg,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("NEW ITEM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // 1. Header with Single-User Default & Pairing Trigger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DuoKineticOrbitEmblem(
                            isPaired = config.isPaired,
                            harmonyProgress = harmonyRatio
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "TANDEM", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                text = if (config.isPaired) "$selfLabel + $partnerLabel" else "$selfLabel • Solo / Unpaired",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (config.isPaired) SageMint else ChampagneGold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElevatedSlate)
                            .border(1.dp, ChampagneGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { showProfilePairingSheet = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (config.isPaired) "PAIRED" else "SETUP & PAIR",
                                style = MaterialTheme.typography.labelSmall,
                                color = IvoryText
                            )
                        }
                    }
                }
            }

            // 2. First-Run Profile & Pairing Setup Card if name is not configured yet
            if (config.selfDisplayName.isBlank() || !config.isPaired) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CharcoalSurface)
                            .border(1.dp, ChampagneGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "IDENTITY & TWO-PERSON PAIRING",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (config.selfDisplayName.isBlank())
                                "Set your display name to start using Tandem, then generate or enter a pairing code to connect with your second person."
                            else
                                "You are currently in solo mode (${config.selfDisplayName}). Pair with a second user anytime to unlock shared lists and attention pings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedPewter
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showProfilePairingSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaCoral, contentColor = ObsidianBg)
                        ) {
                            Text("CONFIGURE NAME & PAIRING", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Active Nudges
            items(activeNudges, key = { it.id }) { nudge ->
                LiveAttentionNudgeBanner(
                    nudge = nudge,
                    senderLabel = if (nudge.sender == MemberSlot.SELF) selfLabel else partnerLabel,
                    onQuickReply = { reply -> viewModel.acknowledgeNudge(nudge, reply) }
                )
            }

            // 4. Spaces Row (Clean by default, user creates spaces)
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "SPACES", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "+ NEW SPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerracottaCoral,
                            modifier = Modifier.clickable { showNewSpaceModal = true }
                        )
                    }
                    if (spaces.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CharcoalSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                                .clickable { showNewSpaceModal = true }
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "No spaces yet. Tap '+ NEW SPACE' to create your first space.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedPewter
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            spaces.forEach { space ->
                                val isSelected = space.id == selectedSpaceId
                                Column(
                                    modifier = Modifier
                                        .width(168.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) ElevatedSlate else CharcoalSurface)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) Color(space.accentHex) else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable { viewModel.selectSpace(space.id) }
                                        .padding(14.dp)
                                ) {
                                    Text(text = space.name, style = MaterialTheme.typography.titleMedium)
                                    if (space.subtitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = space.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 12.sp,
                                            color = MutedPewter,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Mode Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "ALL (${items.size})",
                        selected = selectedMode == null,
                        onClick = { viewModel.selectModeFilter(null) }
                    )
                    ListMode.entries.forEach { mode ->
                        FilterPill(
                            label = "${mode.badge} • ${mode.label}",
                            selected = selectedMode == mode,
                            onClick = { viewModel.selectModeFilter(mode) }
                        )
                    }
                }
            }

            // 6. Task & Shopping Items (Empty clean state when no items exist)
            if (items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CharcoalSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "YOUR WORKSPACE IS CLEAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'NEW ITEM' below to add your first task, reminder, or shopping item.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedPewter
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { task ->
                    TandemTaskCard(
                        item = task,
                        selfLabel = selfLabel,
                        partnerLabel = partnerLabel,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onReact = { reaction -> viewModel.reactToTask(task, reaction) }
                    )
                }
            }
        }
    }

    if (showProfilePairingSheet) {
        ProfileAndPairingDialog(
            currentConfig = config,
            onDismiss = { showProfilePairingSheet = false },
            onSave = { myName, otherName, code, paired, endpoint ->
                viewModel.saveIdentityAndPairing(myName, otherName, code, paired, endpoint)
                showProfilePairingSheet = false
            }
        )
    }

    if (showNewSpaceModal) {
        NewSpaceDialog(
            onDismiss = { showNewSpaceModal = false },
            onCreate = { name, sub ->
                viewModel.createNewSpace(name, sub)
                showNewSpaceModal = false
            }
        )
    }

    if (showComposerSheet) {
        AddItemModalSheet(
            selfLabel = selfLabel,
            partnerLabel = partnerLabel,
            isPaired = config.isPaired,
            onDismiss = { showComposerSheet = false },
            onSubmit = { title, note, mode, assignee, urgency, qty, unit, aisle, cost, store ->
                viewModel.createTaskOrShoppingItem(
                    title, note, mode, assignee, urgency, qty, unit, aisle, cost, store
                )
                showComposerSheet = false
            }
        )
    }

    if (showNudgeModal) {
        SendNudgeDialog(
            onDismiss = { showNudgeModal = false },
            onSend = { msg, urgency ->
                viewModel.sendPartnerNudge(msg, urgency)
                showNudgeModal = false
            }
        )
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) TerracottaCoral else CharcoalSurface)
            .border(1.dp, if (selected) TerracottaCoral else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ObsidianBg else IvoryText
        )
    }
}

@Composable
private fun TandemTaskCard(
    item: TaskItemEntity,
    selfLabel: String,
    partnerLabel: String,
    onToggle: () -> Unit,
    onReact: (String) -> Unit
) {
    val badgeColor = when (item.mode) {
        ListMode.TODO -> TerracottaCoral
        ListMode.ASK_PARTNER -> ChampagneGold
        ListMode.SHOPPING -> SageMint
        ListMode.RITUALS -> Color(0xFF6C8EAD)
    }
    val assigneeText = when (item.assignedTo) {
        MemberSlot.SELF -> selfLabel
        MemberSlot.PARTNER -> partnerLabel
        MemberSlot.BOTH -> "Both"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalSurface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (item.isCompleted) SageMint else ElevatedSlate)
                    .border(1.5.dp, if (item.isCompleted) SageMint else badgeColor, RoundedCornerShape(9.dp))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.mode.badge} • FOR ${assigneeText.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isCompleted) MutedPewter else IvoryText
                )
                if (item.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.note, style = MaterialTheme.typography.bodyMedium, color = MutedPewter)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("On it", "Done soon", "Need hand").forEach { chip ->
                        val active = item.reactionChip == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) ChampagneGold else ElevatedSlate)
                                .clickable { onReact(chip) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) ObsidianBg else IvoryText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAndPairingDialog(
    currentConfig: UserConfigEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, String) -> Unit
) {
    var selfName by remember { mutableStateOf(currentConfig.selfDisplayName) }
    var partnerName by remember { mutableStateOf(currentConfig.partnerDisplayName) }
    var pairingCode by remember { mutableStateOf(currentConfig.pairingCode) }
    var isPaired by remember { mutableStateOf(currentConfig.isPaired) }
    var endpoint by remember { mutableStateOf(currentConfig.relayEndpointUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        titleContentColor = IvoryText,
        title = { Text("Configure Your Identity & Pair", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selfName,
                    onValueChange = { selfName = it },
                    label = { Text("Your Display Name") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaired, onCheckedChange = { isPaired = it })
                    Text("Pair with second person", color = IvoryText)
                }
                AnimatedVisibility(visible = isPaired) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Second Person's Display Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it },
                            label = { Text("Shared 2-Peer Pairing Key") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Optional Relay / Supabase URL") },
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selfName, partnerName, pairingCode, isPaired, endpoint) },
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaCoral, contentColor = ObsidianBg)
            ) {
                Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedPewter) }
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
