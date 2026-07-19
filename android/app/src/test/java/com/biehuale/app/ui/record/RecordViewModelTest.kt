package com.biehuale.app.ui.record

import app.cash.turbine.test
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.QuickRecordRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var quickRecordRepository: QuickRecordRepository
    private lateinit var categoriesFlow: MutableStateFlow<List<CategoryEntity>>
    private lateinit var accountsFlow: MutableStateFlow<List<AccountEntity>>
    private lateinit var quickRecordsFlow: MutableStateFlow<List<QuickRecordEntity>>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk()
        accountRepository = mockk()
        quickRecordRepository = mockk()
        categoriesFlow = MutableStateFlow(
            listOf(
                CategoryEntity(
                    id = 1L, name = "\u9910\u996e", icon = null, colorHex = null,
                    type = CategoryType.EXPENSE, isBuiltin = true, sortOrder = 1,
                    isArchived = false, createdAt = 1L, updatedAt = 1L
                ),
                CategoryEntity(
                    id = 2L, name = "\u5de5\u8d44", icon = null, colorHex = null,
                    type = CategoryType.INCOME, isBuiltin = true, sortOrder = 1,
                    isArchived = false, createdAt = 1L, updatedAt = 1L
                )
            )
        )
        accountsFlow = MutableStateFlow(
            listOf(
                AccountEntity(
                    id = 10L, name = "\u73b0\u91d1", icon = "cash", colorHex = "#FF9800",
                    initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
                )
            )
        )
        quickRecordsFlow = MutableStateFlow(emptyList())
        every { categoryRepository.observeAllActive() } returns categoriesFlow
        every { accountRepository.observeActive() } returns accountsFlow
        every { quickRecordRepository.observeAll() } returns quickRecordsFlow
        coEvery { transactionRepository.save(any()) } returns 100L
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = RecordViewModel(
        transactionRepository,
        categoryRepository,
        accountRepository,
        quickRecordRepository
    )

    @Test
    fun save_success_emitsEventAndPersists() = runTest(dispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        vm.onDigit('5')
        vm.onDigit('.')
        vm.onDigit('5')
        vm.onDigit('0')
        advanceUntilIdle()

        assertThat(vm.uiState.value.canSave).isTrue()

        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(RecordEvent.SaveSuccess(wasEditing = false))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            transactionRepository.save(
                match<TransactionEntity> {
                    it.amount == 550L && it.type == TransactionType.EXPENSE && it.accountId == 10L
                }
            )
        }
    }

    @Test
    fun save_withoutAccount_emitsError() = runTest(dispatcher) {
        accountsFlow.value = emptyList()
        val vm = createVm()
        advanceUntilIdle()

        vm.onDigit('1')
        vm.events.test {
            vm.onSave()
            advanceUntilIdle()
            val event = awaitItem()
            assertThat(event).isInstanceOf(RecordEvent.Error::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun modeChange_switchesDefaultCategory() = runTest(dispatcher) {
        val vm = createVm()
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo(1L)

        vm.onModeChange(RecordMode.INCOME)
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedCategoryId).isEqualTo(2L)
    }

    @Test
    fun validate_rejectsZeroAmount() {
        val result = RecordViewModel.validate(
            RecordUiState(
                amountDisplay = "0",
                selectedAccountId = 10L,
                selectedCategoryId = 1L,
                accounts = accountsFlow.value,
                expenseCategories = categoriesFlow.value.filter { it.type == CategoryType.EXPENSE }
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Error::class.java)
        assertThat((result as RecordValidation.Error).message).contains("\u91d1\u989d")
    }

    @Test
    fun validate_rejectsStaleCategoryId() {
        val result = RecordViewModel.validate(
            RecordUiState(
                amountDisplay = "10.00",
                selectedAccountId = 10L,
                selectedCategoryId = 99L,
                accounts = accountsFlow.value,
                expenseCategories = categoriesFlow.value.filter { it.type == CategoryType.EXPENSE }
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Error::class.java)
        assertThat((result as RecordValidation.Error).message).contains("\u7c7b\u522b")
    }

    @Test
    fun validate_transfer_rejectsSameAccount() {
        val accounts = listOf(
            AccountEntity(
                id = 10L, name = "A", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            ),
            AccountEntity(
                id = 11L, name = "B", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            )
        )
        val result = RecordViewModel.validate(
            RecordUiState(
                mode = RecordMode.TRANSFER,
                amountDisplay = "10.00",
                selectedAccountId = 10L,
                toAccountId = 10L,
                accounts = accounts
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Error::class.java)
        assertThat((result as RecordValidation.Error).message).contains("\u4e0d\u540c")
    }

    @Test
    fun validate_transfer_requiresTwoAccounts() {
        val result = RecordViewModel.validate(
            RecordUiState(
                mode = RecordMode.TRANSFER,
                amountDisplay = "10.00",
                selectedAccountId = 10L,
                toAccountId = 11L,
                accounts = accountsFlow.value // only 1 account
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Error::class.java)
        assertThat((result as RecordValidation.Error).message).contains("2")
    }

    @Test
    fun validate_transfer_rejectsFeeNotLessThanAmount() {
        val accounts = listOf(
            AccountEntity(
                id = 10L, name = "A", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            ),
            AccountEntity(
                id = 11L, name = "B", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            )
        )
        val result = RecordViewModel.validate(
            RecordUiState(
                mode = RecordMode.TRANSFER,
                amountDisplay = "500.00",
                feeDisplay = "500.00",
                selectedAccountId = 10L,
                toAccountId = 11L,
                accounts = accounts
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Error::class.java)
        assertThat((result as RecordValidation.Error).message).contains("\u624b\u7eed\u8d39")
    }

    @Test
    fun validate_transfer_acceptsFee() {
        val accounts = listOf(
            AccountEntity(
                id = 10L, name = "A", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            ),
            AccountEntity(
                id = 11L, name = "B", icon = null, colorHex = null,
                initialBalance = 0L, isArchived = false, createdAt = 1L, updatedAt = 1L
            )
        )
        val result = RecordViewModel.validate(
            RecordUiState(
                mode = RecordMode.TRANSFER,
                amountDisplay = "500.00",
                feeDisplay = "1.00",
                selectedAccountId = 10L,
                toAccountId = 11L,
                accounts = accounts
            )
        )
        assertThat(result).isInstanceOf(RecordValidation.Ok::class.java)
        val ok = result as RecordValidation.Ok
        assertThat(ok.cents).isEqualTo(500_00L)
        assertThat(ok.feeCents).isEqualTo(1_00L)
    }

    @Test
    fun quickRecord_savesExpenseImmediately() = runTest(dispatcher) {
        val template = QuickRecordEntity(
            id = 7L,
            categoryId = 1L,
            accountId = 10L,
            amount = 400L,
            description = "\u5750\u5730\u94c1",
            sortOrder = 1,
            createdAt = 1L,
            updatedAt = 1L
        )
        coEvery { quickRecordRepository.getById(7L) } returns template
        coEvery { categoryRepository.getById(1L) } returns categoriesFlow.value.first()
        coEvery { accountRepository.getById(10L) } returns accountsFlow.value.first()

        val vm = createVm()
        advanceUntilIdle()

        vm.events.test {
            vm.onQuickRecord(7L)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(RecordEvent.SaveSuccess(wasEditing = false))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            transactionRepository.save(
                match<TransactionEntity> {
                    it.amount == 400L &&
                        it.type == TransactionType.EXPENSE &&
                        it.categoryId == 1L &&
                        it.accountId == 10L &&
                        it.description == "\u5750\u5730\u94c1"
                }
            )
        }
    }
}
