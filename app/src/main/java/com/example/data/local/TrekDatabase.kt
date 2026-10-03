package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        MemberEntity::class,
        BreadcrumbEntity::class,
        WaypointEntity::class,
        SosAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrekDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun memberDao(): MemberDao
    abstract fun breadcrumbDao(): BreadcrumbDao
    abstract fun waypointDao(): WaypointDao
    abstract fun sosAlertDao(): SosAlertDao

    companion object {
        @Volatile
        private var INSTANCE: TrekDatabase? = null

        fun getInstance(context: Context): TrekDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrekDatabase::class.java,
                    "treksync_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
