package com.leadflow.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Lead::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leadDao(): LeadDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leads ADD COLUMN status TEXT NOT NULL DEFAULT 'New'")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "leadflow.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
