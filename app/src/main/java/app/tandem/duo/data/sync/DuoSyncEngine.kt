package app.tandem.duo.data.sync

import app.tandem.duo.data.local.NudgePingEntity
import app.tandem.duo.data.local.TaskItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class DuoChannelStatus(
    val pairingCode: String = "",
    val isPaired: Boolean = false,
    val relayEndpointUrl: String = "",
    val lastSyncedEpochMs: Long = 0L
)

class DuoSyncEngine {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _status = MutableStateFlow(DuoChannelStatus())
    val status: StateFlow<DuoChannelStatus> = _status

    fun generateInviteCode(): String {
        val token = UUID.randomUUID().toString().uppercase().take(8)
        return "TNDM-${token.substring(0, 4)}-${token.substring(4, 8)}"
    }

    fun configurePairing(pairingCode: String, isPaired: Boolean, endpointUrl: String) {
        _status.value = DuoChannelStatus(
            pairingCode = pairingCode.uppercase().trim(),
            isPaired = isPaired,
            relayEndpointUrl = endpointUrl.trim(),
            lastSyncedEpochMs = System.currentTimeMillis()
        )
    }

    suspend fun pushTaskMutation(task: TaskItemEntity): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isPaired) return@withContext true
        val endpoint = _status.value.relayEndpointUrl
        _status.value = _status.value.copy(lastSyncedEpochMs = System.currentTimeMillis())
        if (endpoint.isBlank()) return@withContext true

        runCatching {
            val url = URL("$endpoint/rest/v1/tandem_tasks")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
                connectTimeout = 4000
                doOutput = true
            }
            val payload = json.encodeToString(task)
            conn.outputStream.use { it.write(payload.toByteArray()) }
            conn.responseCode in 200..299
        }.getOrDefault(false)
    }

    suspend fun pushNudge(nudge: NudgePingEntity): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isPaired) return@withContext false
        _status.value = _status.value.copy(lastSyncedEpochMs = System.currentTimeMillis())
        true
    }
}
