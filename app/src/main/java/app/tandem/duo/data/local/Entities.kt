package app.tandem.duo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class MemberSlot(val defaultLabel: String, val accentHex: Long) {
    SELF("Me", 0xFFE06D53),
    PARTNER("Paired User", 0xFFD4AF37),
    BOTH("Both", 0xFF52B788)
}

enum class ListMode(val label: String, val badge: String) {
    TODO("Action Tasks", "DO"),
    ASK_PARTNER("Needs Attention", "ASK"),
    SHOPPING("To Buy / Basket", "BUY"),
    RITUALS("Shared Rituals", "SYNC")
}

enum class UrgencyLevel(val label: String, val pulseSpeedMs: Int) {
    CALM("Whenever Free", 2600),
    SOON("Today", 1600),
    URGENT_PING("Needs Attention Now", 750)
}

enum class ShoppingAisle(val title: String) {
    PRODUCE("Fresh Produce"),
    PANTRY("Pantry & Staples"),
    DAIRY_PROTEIN("Dairy & Protein"),
    HOUSEHOLD("Household & Supplies"),
    TECH_GEAR("Electronics & Tools"),
    WELLNESS("Health & Personal Care")
}

@Serializable
@Entity(tableName = "user_config")
data class UserConfigEntity(
    @PrimaryKey val id: Int = 1,
    val selfDisplayName: String = "",
    val isPaired: Boolean = false,
    val partnerDisplayName: String = "",
    val pairingCode: String = "",
    val relayEndpointUrl: String = ""
)

@Serializable
@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    val accentHex: Long,
    val sortOrder: Int
)

@Serializable
@Entity(tableName = "shared_lists")
data class SharedListEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val title: String,
    val mode: ListMode,
    val categoryTag: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "task_items")
data class TaskItemEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val listId: String,
    val title: String,
    val note: String = "",
    val mode: ListMode = ListMode.TODO,
    val createdBy: MemberSlot = MemberSlot.SELF,
    val assignedTo: MemberSlot = MemberSlot.SELF,
    val urgency: UrgencyLevel = UrgencyLevel.SOON,
    val category: String = "General",
    val quantity: String = "1",
    val unit: String = "pc",
    val aisle: ShoppingAisle = ShoppingAisle.PRODUCE,
    val estimatedCost: Double = 0.0,
    val storeTag: String = "",
    val isCompleted: Boolean = false,
    val completedBy: MemberSlot? = null,
    val completedAt: Long? = null,
    val reactionChip: String? = null,
    val dueLabel: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "nudge_pings")
data class NudgePingEntity(
    @PrimaryKey val id: String,
    val sender: MemberSlot,
    val receiver: MemberSlot,
    val message: String,
    val linkedTaskId: String? = null,
    val urgency: UrgencyLevel = UrgencyLevel.URGENT_PING,
    val isAcknowledged: Boolean = false,
    val replyChip: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "ritual_checkins")
data class RitualCheckInEntity(
    @PrimaryKey val id: String,
    val title: String,
    val streakDays: Int = 0,
    val selfDoneToday: Boolean = false,
    val partnerDoneToday: Boolean = false,
    val timeWindow: String = ""
)
