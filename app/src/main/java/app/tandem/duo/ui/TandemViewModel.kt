package app.tandem.duo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tandem.duo.data.local.*
import app.tandem.duo.data.remote.TandemSupabase
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TandemViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TandemDatabase.getInstance(application).dao()
    private val _authUser = MutableStateFlow<UserInfo?>(null)
    val authUser: StateFlow<UserInfo?> = _authUser.asStateFlow()
    private val _authChecking = MutableStateFlow(true)
    val authChecking: StateFlow<Boolean> = _authChecking.asStateFlow()
    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()
    private val _pairId = MutableStateFlow<String?>(null)
    val pairId: StateFlow<String?> = _pairId.asStateFlow()
    private val _pairInviteCode = MutableStateFlow<String?>(null)
    val pairInviteCode: StateFlow<String?> = _pairInviteCode.asStateFlow()
    private val _pairBusy = MutableStateFlow(false)
    val pairBusy: StateFlow<Boolean> = _pairBusy.asStateFlow()
    private val _pairMessage = MutableStateFlow<String?>(null)
    val pairMessage: StateFlow<String?> = _pairMessage.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { TandemSupabase.currentUser() }
                .onSuccess { user ->
                    _authUser.value = user
                    if (user != null) refreshPairDetails()
                }
                .onFailure { _authMessage.value = it.message ?: "Could not restore your sign-in session" }
            _authChecking.value = false
        }
    }

    fun sendSignInLink(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _authMessage.value = "Enter a valid email address."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authMessage.value = null
            runCatching { TandemSupabase.requestEmailSignIn(email) }
                .onSuccess { _authMessage.value = "Sign-in link sent. Open the email on this device to continue." }
                .onFailure { _authMessage.value = it.message ?: "Could not send the sign-in link." }
            _authBusy.value = false
        }
    }

    fun refreshSignedInUser() {
        viewModelScope.launch {
            _authUser.value = runCatching { TandemSupabase.currentUser() }.getOrNull()
            if (_authUser.value != null) refreshPairDetails()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { TandemSupabase.client.auth.signOut() }
            _authUser.value = null
            _pairId.value = null
            _pairInviteCode.value = null
        }
    }

    fun updateMyDisplayName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _authBusy.value = true
            runCatching { TandemSupabase.updateDisplayName(name) }
                .onSuccess {
                    dao.saveUserConfig(userConfig.value.copy(selfDisplayName = name.trim()))
                    _authMessage.value = "Your profile is updated."
                }
                .onFailure { _authMessage.value = it.message ?: "Could not update your profile." }
            _authBusy.value = false
        }
    }

    fun createPairAndInvite() {
        viewModelScope.launch {
            _pairBusy.value = true
            _pairMessage.value = null
            runCatching {
                val membership = TandemSupabase.currentPair()
                val id = membership?.pairId ?: TandemSupabase.createPair()
                val invite = TandemSupabase.createInvite(id)
                _pairId.value = id
                _pairInviteCode.value = invite.inviteCode.chunked(4).joinToString("-")
                refreshPairDetails()
            }.onFailure { _pairMessage.value = it.message ?: "Could not create an invite." }
            _pairBusy.value = false
        }
    }

    fun joinPair(inviteCode: String) {
        if (inviteCode.isBlank()) return
        viewModelScope.launch {
            _pairBusy.value = true
            _pairMessage.value = null
            runCatching {
                _pairId.value = TandemSupabase.acceptInvite(inviteCode)
                _pairInviteCode.value = null
                refreshPairDetails()
            }.onFailure { _pairMessage.value = it.message ?: "That invite could not be accepted." }
            _pairBusy.value = false
        }
    }

    private suspend fun refreshPairDetails() {
        val localConfig = dao.observeUserConfig().firstOrNull() ?: UserConfigEntity()
        val membership = TandemSupabase.currentPair()
        _pairId.value = membership?.pairId
        if (membership == null) {
            dao.saveUserConfig(localConfig.copy(isPaired = false, partnerDisplayName = "", pairingCode = ""))
            return
        }
        val user = TandemSupabase.currentUser() ?: return
        val partner = TandemSupabase.pairProfiles(membership.pairId).firstOrNull { it.id != user.id }
        dao.saveUserConfig(localConfig.copy(
            isPaired = partner != null,
            partnerDisplayName = partner?.displayName.orEmpty()
        ))
    }

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
