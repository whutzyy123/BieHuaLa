package com.biehuale.app.data.repository

import com.biehuale.app.data.db.dao.CategoryDao
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.seed.DefaultCategories
import com.biehuale.app.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 类别 Repository
 *
 * 详见 docs/PRD.md §6.4
 *
 * v0.2 升级：type 参数从 String 改为 CategoryType 枚举
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun observeAllActive(): Flow<List<CategoryEntity>> = categoryDao.observeAllActive()

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type.name)

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun create(
        name: String,
        type: CategoryType,
        icon: String? = null,
        colorHex: String? = null,
        sortOrder: Int = 0
    ): Long {
        require(categoryDao.findActiveByTypeAndName(type.name, name) == null) {
            "同类型下已存在同名类别"
        }
        val now = System.currentTimeMillis()
        return categoryDao.insert(
            CategoryEntity(
                id = 0L,
                name = name,
                icon = icon,
                colorHex = colorHex,
                type = type,
                isBuiltin = false,
                sortOrder = sortOrder,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(category: CategoryEntity) {
        val conflict = categoryDao.findActiveByTypeAndName(category.type.name, category.name)
        require(conflict == null || conflict.id == category.id) {
            "同类型下已存在同名类别"
        }
        categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun archive(id: Long) {
        categoryDao.archive(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        val category = categoryDao.getById(id) ?: throw IllegalStateException("类别不存在")
        val conflict = categoryDao.findActiveByTypeAndName(category.type.name, category.name)
        require(conflict == null || conflict.id == id) { "同类型下已存在同名活跃类别，无法恢复" }
        categoryDao.restore(id, System.currentTimeMillis())
    }

    /**
     * 重置默认类别（PRD §6.4）
     */
    suspend fun resetBuiltinDefaults() {
        val builtins = categoryDao.getAllBuiltin()
        val now = System.currentTimeMillis()
        DefaultCategories.ALL.forEach { seed ->
            val existing = builtins.firstOrNull {
                it.type == seed.type && it.sortOrder == seed.sortOrder
            } ?: builtins.firstOrNull {
                it.type == seed.type && it.name == seed.name
            }
            if (existing != null) {
                categoryDao.update(
                    existing.copy(
                        name = seed.name,
                        icon = seed.icon,
                        colorHex = seed.colorHex,
                        sortOrder = seed.sortOrder,
                        isArchived = false,
                        isBuiltin = true,
                        updatedAt = now
                    )
                )
            } else {
                categoryDao.insert(
                    CategoryEntity(
                        id = 0L,
                        name = seed.name,
                        icon = seed.icon,
                        colorHex = seed.colorHex,
                        type = seed.type,
                        isBuiltin = true,
                        sortOrder = seed.sortOrder,
                        isArchived = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }
}
