package com.example.smartexpense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smartexpense.data.local.dao.AdminLogDao
import com.example.smartexpense.data.local.dao.CategoryDao
import com.example.smartexpense.data.local.dao.ExchangeRateDao
import com.example.smartexpense.data.local.dao.TransactionDao
import com.example.smartexpense.data.local.dao.UserProfileDao
import com.example.smartexpense.model.AdminLogEntity
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.model.ExchangeRateEntity
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.model.UserProfile

/**
 * Room database dạng Singleton (mục 1).
 * Schema v1 đã đủ role/status/lastLoginAt và colorHex/iconKey để tránh migration sau này (mục 7).
 * v2 (1.4): thêm bảng exchange_rates - cache tỷ giá cho phân hệ Tỷ giá USD→VND.
 */
@Database(
    entities = [
        UserProfile::class,
        TransactionEntity::class,
        CategoryEntity::class,
        AdminLogEntity::class,
        ExchangeRateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun adminLogDao(): AdminLogDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        /** v1 -> v2: tạo bảng exchange_rates cache tỷ giá (không đụng dữ liệu cũ). */
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exchange_rates (" +
                        "code TEXT NOT NULL, " +
                        "base TEXT NOT NULL, " +
                        "rate REAL NOT NULL, " +
                        "fetchedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(code))"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartexpense.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
