package app.tandem.duo.travel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val recipient: String,
    val departureAt: Long,
    val reminderAt: Long,
    val checklist: List<TravelChecklistItem>,
    val createdAt: Long = System.currentTimeMillis()
)

data class TravelChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
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
                    departureAt = obj.getLong("departureAt"),
                    reminderAt = obj.getLong("reminderAt"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    checklist = (0 until tasks.length()).map { j ->
                        val task = tasks.getJSONObject(j)
                        TravelChecklistItem(task.getString("id"), task.getString("title"), task.optBoolean("isDone"))
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
                    .put("departureAt", plan.departureAt)
                    .put("reminderAt", plan.reminderAt)
                    .put("createdAt", plan.createdAt)
                    .put("checklist", tasks)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PLANS, array.toString()).apply()
    }
}
