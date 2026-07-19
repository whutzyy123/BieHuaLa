package com.biehuale.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.biehuale.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 类别 DAO
 *
 * 详见 docs/PRD.md §6.4
 *  - 内置类别（is_builtin=1）不能硬删，只能归档或"重置默认"
 *  - 自建类别可软删
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY type, sort_order")
    fun observeAllActive(): Flow<List<CategoryEntity>>

    /** 含已归档，供账单/详情展示「（已归档）」 */
    @Query("SELECT * FROM categories ORDER BY type, sort_order, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("""
        SELECT * FROM categories
        WHERE is_archived = 0 AND type = :type AND name = :name
        LIMIT 1
    """)
    suspend fun findActiveByTypeAndName(type: String, name: String): CategoryEntity?

    /** 查所有内置类别（用于"重置默认"时遍历恢复） */
    @Query("SELECT * FROM categories WHERE is_builtin = 1")
    suspend fun getAllBuiltin(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    /**
     * 软删除
     * 注：transactions.category_id 是 ON DELETE SET NULL，所以这里安全
     */
    @Query("UPDATE categories SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long)
}
