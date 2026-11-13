package com.example.smartexpense.repository

import android.content.Context
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.R
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.utils.AppPrefs
import kotlinx.coroutines.flow.Flow

/**
 * Danh mục dùng chung cho mọi tài khoản; chỉ admin thêm/sửa (3.3).
 * Seed 8 danh mục mặc định ở lần chạy đầu (6 chi + 2 thu, lưới 4x2 mục 5.6).
 */
class CategoryRepository(private val context: Context) {

    private val dao get() = AppDatabase.get(context).categoryDao()

    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)

    suspend fun getAllOnce(): List<CategoryEntity> = dao.getAllOnce()

    suspend fun insert(name: String, type: String, colorHex: String, iconKey: String = ""): Long =
        dao.insert(CategoryEntity(name = name, iconUrl = "", iconKey = iconKey, colorHex = colorHex, type = type))

    suspend fun update(category: CategoryEntity) = dao.update(category)

    /** Chỉ seed 1 lần (cờ trong EncryptedSharedPreferences), không seed lại sau khi admin đã chỉnh sửa */
    suspend fun seedIfNeeded() {
        val prefs = AppPrefs.get(context)
        if (prefs.categoriesSeeded) return
        if (dao.count() == 0) {
            dao.insertAll(defaultCategories(context))
        }
        prefs.categoriesSeeded = true
    }

    companion object {
        /** 8 danh mục mặc định (mục 3.3) - icon là drawable cục bộ, iconUrl để nhóm điền URL thật sau */
        fun defaultCategories(context: Context) = listOf(
            CategoryEntity(name = context.getString(R.string.cat_food), iconUrl = "", iconKey = "ic_food", colorHex = "#F97316", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_transport), iconUrl = "", iconKey = "ic_car", colorHex = "#0EA5E9", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_education), iconUrl = "", iconKey = "ic_school", colorHex = "#8B5CF6", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_shopping), iconUrl = "", iconKey = "ic_shopping", colorHex = "#EC4899", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_entertainment), iconUrl = "", iconKey = "ic_movie", colorHex = "#FACC15", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_health), iconUrl = "", iconKey = "ic_health", colorHex = "#10B981", type = CategoryEntity.TYPE_EXPENSE),
            CategoryEntity(name = context.getString(R.string.cat_salary), iconUrl = "", iconKey = "ic_salary", colorHex = "#16A34A", type = CategoryEntity.TYPE_INCOME),
            CategoryEntity(name = context.getString(R.string.cat_bonus), iconUrl = "", iconKey = "ic_gift", colorHex = "#2563EB", type = CategoryEntity.TYPE_INCOME)
        )
    }
}
