package app.tandem.duo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tandem.duo.data.local.*
import app.tandem.duo.data.sync.DuoSyncEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TandemViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TandemDatabase.getInstance(application).dao()
    val syncEngine = DuoSyncEngine()

    val userConfig: StateFlow<UserConfigEntity> = dao.observeUserConfig()
        .map { it ?: UserConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserConfigEntity())

    private val _selectedSpaceId = MutableStateFlow<String?>(null)
    val selectedSpaceId: StateFlow<String?> = _selectedSpaceId

    private val _selectedModeFilter = MutableStateFlow<ListMode?>(null)
    val selectedModeFilter: StateFlow<ListMode?> = _selectedModeFilter

    val spaces: StateFlow<List<SpaceEntity>> = dao.observeSpaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<TaskItemEntity>> = dao.observeAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNudges: StateFlow<List<NudgePingEntity>> = dao.observeActiveNudges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rituals: StateFlow<List<RitualCheckInEntity>> = dao.observeRituals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredItems: StateFlow<List<TaskItemEntity>> = combine(
        allItems,
        _selectedSpaceId,
        _selectedModeFilter
    ) { items, spaceId, modeFilter ->
        items.filter { item ->
            val matchesSpace = spaceId == null || item.spaceId == spaceId
            val matchesMode = modeFilter == null || item.mode == modeFilter
            matchesSpace && matchesMode
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveIdentityAndPairing(
        selfName: String,
        partnerName: String,
        pairingCode: String,
        isPaired: Boolean,
        endpointUrl: String
    ) {
        viewModelScope.launch {
            val code = pairingCode.ifBlank { syncEngine.generateInviteCode() }
            val cfg = UserConfigEntity(
                id = 1,
                selfDisplayName = selfName.trim(),
                isPaired = isPaired && partnerName.isNotBlank(),
                partnerDisplayName = if (isPaired) partnerName.trim() else "",
                pairingCode = code,
                relayEndpointUrl = endpointUrl.trim()
            )
            dao.saveUserConfig(cfg)
            syncEngine.configurePairing(code, cfg.isPaired, cfg.relayEndpointUrl)
        }
    }

    fun selectSpace(spaceId: String?) {
        _selectedSpaceId.value = spaceId
    }

    fun selectModeFilter(mode: ListMode?) {
        _selectedModeFilter.value = mode
    }

    fun createNewSpace(name: String, subtitle: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "space_${UUID.randomUUID().toString().take(6)}"
            val space = SpaceEntity(
                id = id,
                name = name.trim(),
                subtitle = subtitle.trim(),
                accentHex = 0xFFE06D53,
                sortOrder = spaces.value.size
            )
            dao.insertSpace(space)
            _selectedSpaceId.value = id
        }
    }

    fun createTaskOrShoppingItem(
        title: String,
        note: String,
        mode: ListMode,
        assignedTo: MemberSlot,
        urgency: UrgencyLevel,
        quantity: String,
        unit: String,
        aisle: ShoppingAisle,
        estimatedCost: Double,
        storeTag: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val targetSpaceId = _selectedSpaceId.value ?: spaces.value.firstOrNull()?.id ?: "default"
            val newItem = TaskItemEntity(
                id = "task_${UUID.randomUUID()}",
                spaceId = targetSpaceId,
                listId = "default_$targetSpaceId",
                title = title.trim(),
                note = note.trim(),
                mode = mode,
                createdBy = MemberSlot.SELF,
                assignedTo = assignedTo,
                urgency = urgency,
                category = mode.badge,
                quantity = quantity.ifBlank { "1" },
                unit = unit.ifBlank { "pc" },
                aisle = aisle,
                estimatedCost = estimatedCost,
                storeTag = storeTag.trim()
            )
            dao.upsertTask(newItem)
            syncEngine.pushTaskMutation(newItem)
        }
    }

    fun toggleTaskCompletion(item: TaskItemEntity) {
        viewModelScope.launch {
            val nowDone = !item.isCompleted
            val updated = item.copy(
                isCompleted = nowDone,
                completedBy = if (nowDone) MemberSlot.SELF else null,
                completedAt = if (nowDone) System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertTask(updated)
            syncEngine.pushTaskMutation(updated)
        }
    }

    fun reactToTask(item: TaskItemEntity, reaction: String) {
        viewModelScope.launch {
            val updated = item.copy(
                reactionChip = reaction,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertTask(updated)
            syncEngine.pushTaskMutation(updated)
        }
    }

    fun sendPartnerNudge(message: String, urgency: UrgencyLevel, linkedTaskId: String? = null) {
        if (message.isBlank()) return
        viewModelScope.launch {
            val ping = NudgePingEntity(
                id = "nudge_${UUID.randomUUID()}",
                sender = MemberSlot.SELF,
                receiver = MemberSlot.PARTNER,
                message = message.trim(),
                linkedTaskId = linkedTaskId,
                urgency = urgency
            )
            dao.insertNudge(ping)
            syncEngine.pushNudge(ping)
        }
    }

    fun acknowledgeNudge(nudge: NudgePingEntity, reply: String) {
        viewModelScope.launch {
            dao.insertNudge(nudge.copy(isAcknowledged = true, replyChip = reply))
        }
    }

    fun clearCompletedShoppingInCurrentSpace() {
        val spId = _selectedSpaceId.value ?: return
        viewModelScope.launch {
            dao.clearPurchasedInSpace(spId)
        }
    }

    fun addRitual(title: String, timeWindow: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.upsertRitual(
                RitualCheckInEntity(
                    id = "rit_${UUID.randomUUID()}",
                    title = title.trim(),
                    streakDays = 0,
                    selfDoneToday = false,
                    partnerDoneToday = false,
                    timeWindow = timeWindow.trim()
                )
            )
        }
    }

    fun toggleRitualSelf(ritual: RitualCheckInEntity) {
        viewModelScope.launch {
            dao.upsertRitual(ritual.copy(selfDoneToday = !ritual.selfDoneToday))
        }
    }
}
