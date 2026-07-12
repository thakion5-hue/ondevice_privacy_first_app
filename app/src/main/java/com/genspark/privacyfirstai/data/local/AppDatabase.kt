package com.genspark.privacyfirstai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ReceiptMemoryEntity::class,
        ThreatScanEntity::class,
        MediaInsightEntity::class,
        DevicePhotoEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptMemoryDao(): ReceiptMemoryDao
    abstract fun threatScanDao(): ThreatScanDao
    abstract fun mediaInsightDao(): MediaInsightDao
    abstract fun devicePhotoDao(): DevicePhotoDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "privacy_first_ai.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
