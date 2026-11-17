package app.tandem.duo.data.remote

import app.tandem.duo.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * The publishable key is intentionally embedded in the Android client.
 * It is not a secret; database access is restricted by Auth + RLS.
 * Never place a service_role key in this app.
 */
object TandemSupabase {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                host = "auth-callback"
                scheme = "tandem"
            }
            install(Postgrest)
            install(Realtime)
        }
    }

    suspend fun requestEmailSignIn(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email.trim()
        }
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email.trim(),
            token = code.trim()
        )
    }

    suspend fun currentUser(): UserInfo? = client.auth.currentUserOrNull()

    suspend fun updateDisplayName(name: String) {
        val user = currentUser() ?: error("Sign in first")
        client.from("profiles").update(
            {
                set("display_name", name.trim())
            }
        ) {
            filter { eq("id", user.id) }
        }
    }

    suspend fun currentPair(): PairMemberRecord? {
        val user = currentUser() ?: return null
        return client.from("pair_members").select {
            filter { eq("user_id", user.id) }
            limit(1)
        }.decodeList<PairMemberRecord>().firstOrNull()
    }

    suspend fun createPair(name: String = "Our space"): String {
        return client.postgrest.rpc(
            function = "create_pair",
            parameters = mapOf("pair_name" to name)
        ).decodeAs<String>()
    }

    suspend fun createInvite(pairId: String): PairInviteResult {
        return client.postgrest.rpc(
            function = "create_pair_invite",
            parameters = mapOf("pair_id_input" to pairId)
        ).decodeList<PairInviteResult>().first()
    }

    suspend fun acceptInvite(code: String): String {
        return client.postgrest.rpc(
            function = "accept_pair_invite",
            parameters = mapOf("invite_code_input" to code.trim())
        ).decodeAs<String>()
    }

    suspend fun pairProfiles(pairId: String): List<ProfileRecord> {
        val members = client.from("pair_members").select {
            filter { eq("pair_id", pairId) }
        }.decodeList<PairMemberRecord>()
        if (members.isEmpty()) return emptyList()
        return members.mapNotNull { member ->
            client.from("profiles").select {
                filter { eq("id", member.userId) }
            }.decodeList<ProfileRecord>().firstOrNull()
        }
    }

    suspend fun saveTravelPlan(plan: TravelPlanRecord, checklist: List<TravelChecklistRecord>) {
        client.from("travel_plans").upsert(plan)
        if (checklist.isNotEmpty()) {
            client.from("travel_checklist_items").upsert(checklist)
        }
    }

    suspend fun loadTravelPlans(pairId: String): List<TravelPlanRecord> {
        return client.from("travel_plans").select {
            filter { eq("pair_id", pairId) }
            order("departure_at")
        }.decodeList()
    }

    suspend fun loadChecklist(planId: String): List<TravelChecklistRecord> {
        return client.from("travel_checklist_items").select {
            filter { eq("plan_id", planId) }
            order("sort_order")
        }.decodeList()
    }

    fun watchTravelPlans(pairId: String): Flow<List<TravelPlanRecord>> =
        client.from("travel_plans").selectAsFlow(
            primaryKey = TravelPlanRecord::id,
            filter = FilterOperation("pair_id", FilterOperator.EQ, pairId)
        )

    suspend fun deleteTravelPlan(planId: String) {
        client.from("travel_plans").delete {
            filter { eq("id", planId) }
        }
    }
}

@Serializable
data class PairMemberRecord(
    @SerialName("pair_id") val pairId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member"
)

@Serializable
data class PairInviteResult(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class ProfileRecord(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class TravelPlanRecord(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("pair_id") val pairId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    val title: String,
    val destination: String = "",
    @SerialName("departure_at") val departureAt: String,
    @SerialName("reminder_at") val reminderAt: String? = null,
    val timezone: String = "Asia/Kolkata",
    val notes: String = "",
    val status: String = "planned",
    @SerialName("updated_at") val updatedAt: String = java.time.Instant.now().toString()
)

@Serializable
data class TravelChecklistRecord(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("plan_id") val planId: String,
    val title: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_done") val isDone: Boolean = false,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String = java.time.Instant.now().toString()
)
