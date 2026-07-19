package com.biehuale.app.data.backup

import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.biehuale.app.data.db.AppDatabase
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.CategoryType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** BackupImporter tests - parse + schemaVersion + merge mode */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupImporterTest {

    private lateinit var db: AppDatabase
    private lateinit var importer: BackupImporter
    private lateinit var accountRepo: AccountRepository
    private lateinit var categoryRepo: CategoryRepository
    private lateinit var transactionRepo: TransactionRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepository(db.accountDao())
        categoryRepo = CategoryRepository(db.categoryDao())
        transactionRepo = TransactionRepository(db.transactionDao())
        importer = BackupImporter(
            context = ctx,
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
    fun schemaVersion0_rejected() = runTest {
        val raw = """
            {
              "schemaVersion": 0,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [], "categories": [], "transactions": []
            }
        """.trimIndent()
        val result = runCatching { importer.parseBackupJson(raw) }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("schemaVersion")
    }

    @Test
    fun schemaVersionTooHigh_rejected() = runTest {
        val raw = """
            {
              "schemaVersion": 99,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [], "categories": [], "transactions": []
            }
        """.trimIndent()
        val result = runCatching { importer.parseBackupJson(raw) }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("\u9ad8\u4e8e")
    }

    @Test
    fun schemaVersion1_accepted() = runTest {
        val raw = """
            {
              "schemaVersion": 1,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [], "categories": [], "transactions": []
            }
        """.trimIndent()
        val result = runCatching { importer.parseBackupJson(raw) }
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun emptyFile_rejected() {
        val result = runCatching { importer.parseBackupJson("") }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("\u7a7a\u6587\u4ef6")
    }

    @Test
    fun invalidJson_rejected() {
        val result = runCatching { importer.parseBackupJson("not valid json") }
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("JSON \u89e3\u6790\u5931\u8d25")
    }

    @Test
    fun merge_sameNameAccount_reusesId() = runTest {
        val existingId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 100_00L)

        val raw = """
            {
              "schemaVersion": 1,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [{
                "id": 999, "name": "\u73b0\u91d1", "icon": null, "color": null,
                "initialBalance": 50000, "isArchived": false,
                "createdAt": 1000, "updatedAt": 1000
              }],
              "categories": [], "transactions": []
            }
        """.trimIndent()
        val uri = writeToTempFile(raw)
        val preview = importer.preview(uri).getOrThrow()
        val result = importer.applyImport(preview)

        val existing = accountRepo.getById(existingId)
        assertThat(existing).isNotNull()
        assertThat(existing?.name).isEqualTo("\u73b0\u91d1")
        // 合并采用备份 initialBalance（换机恢复正确余额）
        assertThat(existing?.initialBalance).isEqualTo(500_00L)
        assertThat(result.getOrNull()?.accountsInserted).isEqualTo(0)
    }

    @Test
    fun merge_differentNameAccount_inserts() = runTest {
        accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)

        val raw = """
            {
              "schemaVersion": 1,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [{
                "id": 999, "name": "\u5fae\u4fe1", "icon": null, "color": null,
                "initialBalance": 30000, "isArchived": false,
                "createdAt": 1000, "updatedAt": 1000
              }],
              "categories": [], "transactions": []
            }
        """.trimIndent()
        val uri = writeToTempFile(raw)
        val preview = importer.preview(uri).getOrThrow()
        val result = importer.applyImport(preview)

        assertThat(result.getOrNull()?.accountsInserted).isEqualTo(1)
    }

    @Test
    fun applyImport_transactionCountAndBalance() = runTest {
        val accId = accountRepo.create(name = "\u73b0\u91d1", initialBalance = 0L)
        categoryRepo.create(name = "\u9910\u996e", type = CategoryType.EXPENSE)

        val raw = """
            {
              "schemaVersion": 1,
              "appVersion": "0.1.0",
              "exportedAt": "2026-07-19T00:00:00Z",
              "accounts": [{
                "id": 1, "name": "\u73b0\u91d1", "icon": null, "color": null,
                "initialBalance": 0, "isArchived": false,
                "createdAt": 1000, "updatedAt": 1000
              }],
              "categories": [{
                "id": 1, "name": "\u9910\u996e", "icon": null, "color": null,
                "type": "EXPENSE", "isBuiltin": true, "sortOrder": 1,
                "isArchived": false, "createdAt": 1000, "updatedAt": 1000
              }],
              "transactions": [
                {
                  "id": 1, "amount": 5000, "type": "EXPENSE",
                  "categoryId": 1, "accountId": 1, "toAccountId": null,
                  "description": "\u5348\u9910", "occurredAt": 1000,
                  "createdAt": 1000, "updatedAt": 1000, "deletedAt": null
                },
                {
                  "id": 2, "amount": 3000, "type": "EXPENSE",
                  "categoryId": 1, "accountId": 1, "toAccountId": null,
                  "description": "\u65e9\u996d", "occurredAt": 1000,
                  "createdAt": 1000, "updatedAt": 1000, "deletedAt": null
                }
              ]
            }
        """.trimIndent()
        val uri = writeToTempFile(raw)
        val preview = importer.preview(uri).getOrThrow()
        assertThat(preview.transactionsCount).isEqualTo(2)

        val result = importer.applyImport(preview)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.transactionsInserted).isEqualTo(2)

        assertThat(accountRepo.getBalance(accId)).isEqualTo(-80_00L)
    }

    private fun writeToTempFile(content: String): android.net.Uri {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = java.io.File.createTempFile("backup_test", ".json", ctx.cacheDir)
        file.writeText(content)
        file.deleteOnExit()
        return file.toUri()
    }
}
