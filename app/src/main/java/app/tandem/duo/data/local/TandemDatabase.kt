package app.tandem.duo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserConfigEntity::class,
        SpaceEntity::class,
        SharedListEntity::class,
        TaskItemEntity::class,
        NudgePingEntity::class,
        RitualCheckInEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TandemDatabase : RoomDatabase() {
    abstract fun dao(): TandemDao

    companion object {
        @Volatile
        private var INSTANCE: TandemDatabase? = null

        fun getInstance(context: Context): TandemDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TandemDatabase::class.java,
                    "tandem_clean.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
