package app.tandem.duo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TandemDao {
    @Query("SELECT * FROM user_config WHERE id = 1")
    fun observeUserConfig(): Flow<UserConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserConfig(config: UserConfigEntity)

    @Query("SELECT * FROM spaces ORDER BY sortOrder ASC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM shared_lists WHERE spaceId = :spaceId ORDER BY createdAt ASC")
    fun observeListsForSpace(spaceId: String): Flow<List<SharedListEntity>>

    @Query("SELECT * FROM task_items ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeAllItems(): Flow<List<TaskItemEntity>>

    @Query("SELECT * FROM nudge_pings WHERE isAcknowledged = 0 ORDER BY createdAt DESC")
    fun observeActiveNudges(): Flow<List<NudgePingEntity>>

    @Query("SELECT * FROM ritual_checkins")
    fun observeRituals(): Flow<List<RitualCheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)

    @Delete
    suspend fun deleteSpace(space: SpaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: SharedListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(item: TaskItemEntity)

    @Delete
    suspend fun deleteTask(item: TaskItemEntity)

    @Query("DELETE FROM task_items WHERE spaceId = :spaceId AND mode = 'SHOPPING' AND isCompleted = 1")
    suspend fun clearPurchasedInSpace(spaceId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNudge(nudge: NudgePingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRitual(ritual: RitualCheckInEntity)
}
