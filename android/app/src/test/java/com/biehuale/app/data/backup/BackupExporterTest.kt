package com.biehuale.app.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.io.File

/** BackupExporter tests - export + round-trip */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupExporterTest {

    private lateinit var db: AppDatabase
    private lateinit var exporter: BackupExporter
    private lateinit var importer: BackupImporter
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepository(db.accountDao(), db.quickRecordDao())
        categoryRepo = CategoryRepository(db.categoryDao(), db.quickRecordDao())
        transactionRepo = TransactionRepository(db.transactionDao(), db.accountDao(), db.categoryDao())
        exporter = BackupExporter(context, db, accountRepo, categoryRepo, transactionRepo)
        importer = BackupImporter(
            context = context,
            database = db,
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao()
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun fileName_formatIsCorrect() {
        val name = exporter.generateDefaultFileName()
        assertThat(name).startsWith("\u522b\u82b1\u4e50_")
        assertThat(name).endsWith(".json")
        val middle = name.removePrefix("\u522b\u82b1\u4e50_").removeSuffix(".json")
        assertThat(middle).matches("""\d{4}-\d{2}-\d{2}_\d{4}""")
    }

    @Test
    fun export_emptyDatabase_succeeds() = runTest {
        val uri = writeToTempFile("export_test.json")
        val result = exporter.export(uri)
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun export_thenPreview_roundTripCounts() = runTest {
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 1000_00L)
        val catId = categoryRepo.create(name = "\u9910\u996e", type = CategoryType.EXPENSE)
        val now = System.currentTimeMillis()
        transactionRepo.save(
            TransactionEntity(
                id = 0L,
                amount = 30_00L,
                type = TransactionType.EXPENSE,
                categoryId = catId,
                accountId = accId,
                toAccountId = null,
                description = "\u5348\u9910",
                occurredAt = now,
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null
            )
        )

        val uri = writeToTempFile("roundtrip_test.json")
        val exportResult = exporter.export(uri)
        assertThat(exportResult.isSuccess).isTrue()

        val file = File(uri.path!!)
        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(0L)

        val preview = importer.preview(uri).getOrThrow()
        assertThat(preview.accountsCount).isEqualTo(1)
        assertThat(preview.categoriesCount).isEqualTo(1)
        assertThat(preview.transactionsCount).isEqualTo(1)
    }

    @Test
    fun export_containsSchemaVersion1() = runTest {
        val uri = writeToTempFile("schema_test.json")
        exporter.export(uri)
        val content = File(uri.path!!).readText()
        assertThat(content).contains("\"schemaVersion\": 2")
    }

    @Test
    fun export_thenApplyImport_mergesWithoutDuplicates() = runTest {
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        val catId = categoryRepo.create(name = "\u9910\u996e", type = CategoryType.EXPENSE)
        transactionRepo.save(
            TransactionEntity(
                id = 0L,
                amount = 50_00L,
                type = TransactionType.EXPENSE,
                categoryId = catId,
                accountId = accId,
                toAccountId = null,
                description = null,
                occurredAt = System.currentTimeMillis(),
                createdAt = 0L,
                updatedAt = 0L,
                deletedAt = null
            )
        )

        val uri = writeToTempFile("apply_test.json")
        exporter.export(uri)

        val preview = importer.preview(uri).getOrThrow()
        val result = importer.applyImport(preview)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.accountsInserted).isEqualTo(0)
        assertThat(result.getOrNull()?.categoriesInserted).isEqualTo(0)
        assertThat(result.getOrNull()?.transactionsInserted).isEqualTo(0)
    }

    private fun writeToTempFile(name: String): android.net.Uri {
        val file = File.createTempFile(name.removeSuffix(".json"), ".json", context.cacheDir)
        file.deleteOnExit()
        return file.toUri()
    }
}
