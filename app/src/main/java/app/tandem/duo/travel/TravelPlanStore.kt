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
    val createdByUserId: String? = null,
    val departureAt: Long,
    val reminderAt: Long,
    val checklist: List<TravelChecklistItem>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TravelChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

object TravelPlanStore {
    private const val PREFS = "tandem_travel_prep"
    private const val KEY_PLANS = "plans"

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
                    createdByUserId = obj.optString("createdByUserId").takeIf { it.isNotBlank() },
                    departureAt = obj.getLong("departureAt"),
                    reminderAt = obj.getLong("reminderAt"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", obj.optLong("createdAt", 0L)),
                    checklist = (0 until tasks.length()).map { j ->
                        val task = tasks.getJSONObject(j)
                        TravelChecklistItem(task.getString("id"), task.getString("title"), task.optBoolean("isDone"), task.optLong("updatedAt", 0L))
                    }
                )
            }.sortedBy { it.departureAt }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, plan: TravelPlan) {
        val plans = all(context).filterNot { it.id == plan.id } + plan
        persist(context, plans)
    }

    fun delete(context: Context, id: String) {
        persist(context, all(context).filterNot { it.id == id })
    }

    fun markSharedNotificationShown(context: Context, planId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "shared_notified_$planId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    private fun persist(context: Context, plans: List<TravelPlan>) {
        val array = JSONArray()
        plans.forEach { plan ->
            val tasks = JSONArray()
            plan.checklist.forEach { item ->
                tasks.put(JSONObject().put("id", item.id).put("title", item.title).put("isDone", item.isDone).put("updatedAt", item.updatedAt))
            }
            array.put(
                JSONObject()
                    .put("id", plan.id)
                    .put("title", plan.title)
                    .put("recipient", plan.recipient)
                    .put("pairId", plan.pairId.orEmpty())
                    .put("recipientUserId", plan.recipientUserId.orEmpty())
                    .put("createdByUserId", plan.createdByUserId.orEmpty())
                    .put("departureAt", plan.departureAt)
                    .put("reminderAt", plan.reminderAt)
                    .put("createdAt", plan.createdAt)
                    .put("updatedAt", plan.updatedAt)
                    .put("checklist", tasks)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PLANS, array.toString()).apply()
    }
}
