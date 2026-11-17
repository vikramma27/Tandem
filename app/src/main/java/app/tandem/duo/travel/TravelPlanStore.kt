package app.tandem.duo.travel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recipient: String,
    val pairId: String? = null,
    val recipientUserId: String? = null,
    val departureAt: Long,
    val reminderAt: Long,
    val checklist: List<TravelChecklistItem>,
    val createdAt: Long = System.currentTimeMillis(),
    val needsSync: Boolean = true
)

data class TravelChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

object TravelPlanStore {
    private const val PREFS = "tandem_travel_prep"
    private const val KEY_PLANS = "plans"
    private const val KEY_PENDING_DELETES = "pending_remote_deletes"

    fun all(context: Context): List<TravelPlan> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PLANS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val tasks = obj.optJSONArray("checklist") ?: JSONArray()
                TravelPlan(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    recipient = obj.optString("recipient", "Me"),
                    pairId = obj.optString("pairId").takeIf { it.isNotBlank() },
                    recipientUserId = obj.optString("recipientUserId").takeIf { it.isNotBlank() },
                    departureAt = obj.getLong("departureAt"),
                    reminderAt = obj.getLong("reminderAt"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    needsSync = obj.optBoolean("needsSync", false),
                    checklist = (0 until tasks.length()).map { j ->
                        val task = tasks.getJSONObject(j)
                        TravelChecklistItem(task.getString("id"), task.getString("title"), task.optBoolean("isDone"))
                    }
                )
            }.sortedBy { it.departureAt }
        }.getOrDefault(emptyList())
    }

    /** Local edits are dirty by default; remote snapshots pass markDirty=false. */
    fun save(context: Context, plan: TravelPlan, markDirty: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = readPendingDeletes(prefs)
        pending.remove(plan.id)
        prefs.edit().putString(KEY_PENDING_DELETES, pending.toString()).apply()
        val persisted = plan.copy(needsSync = markDirty)
        val plans = all(context).filterNot { it.id == persisted.id } + persisted
        persist(context, plans)
    }

    fun markSynced(context: Context, planId: String) {
        val plans = all(context).map { if (it.id == planId) it.copy(needsSync = false) else it }
        persist(context, plans)
    }

    /**
     * Queue remote deletion before removing the local copy. The worker retries
     * queued deletes after connectivity returns.
     */
    fun delete(context: Context, id: String, queueRemoteDelete: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = all(context).firstOrNull { it.id == id }
        val pending = readPendingDeletes(prefs)
        if (queueRemoteDelete && existing?.pairId != null) {
            pending.put(id, existing.pairId)
        } else {
            pending.remove(id)
        }
        prefs.edit().putString(KEY_PENDING_DELETES, pending.toString()).apply()
        persist(context, all(context).filterNot { it.id == id })
    }

    fun pendingDeletes(context: Context, pairId: String): List<String> {
        val pending = readPendingDeletes(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
        return pending.keys().asSequence().asIterable()
            .filter { pending.optString(it) == pairId }
    }

    fun markDeleteSynced(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = readPendingDeletes(prefs)
        pending.remove(id)
        prefs.edit().putString(KEY_PENDING_DELETES, pending.toString()).apply()
    }

    fun markSharedNotificationShown(context: Context, planId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "shared_notified_$planId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    private fun readPendingDeletes(prefs: android.content.SharedPreferences): JSONObject {
        val raw = prefs.getString(KEY_PENDING_DELETES, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun persist(context: Context, plans: List<TravelPlan>) {
        val array = JSONArray()
        plans.forEach { plan ->
            val tasks = JSONArray()
            plan.checklist.forEach { item ->
                tasks.put(JSONObject().put("id", item.id).put("title", item.title).put("isDone", item.isDone))
            }
            array.put(
                JSONObject()
                    .put("id", plan.id)
                    .put("title", plan.title)
                    .put("recipient", plan.recipient)
                    .put("pairId", plan.pairId.orEmpty())
                    .put("recipientUserId", plan.recipientUserId.orEmpty())
                    .put("departureAt", plan.departureAt)
                    .put("reminderAt", plan.reminderAt)
                    .put("createdAt", plan.createdAt)
                    .put("needsSync", plan.needsSync)
                    .put("checklist", tasks)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PLANS, array.toString()).apply()
    }
}
