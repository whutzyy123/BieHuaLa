package com.biehuale.app.ui.record

import app.cash.turbine.test
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
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
    private lateinit var categoriesFlow: MutableStateFlow<List<CategoryEntity>>
    private lateinit var accountsFlow: MutableStateFlow<List<AccountEntity>>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        transactionRepository = mockk(relaxed = true)
        categoryRepository = mockk()
        accountRepository = mockk()
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
        every { categoryRepository.observeAllActive() } returns categoriesFlow
        every { accountRepository.observeActive() } returns accountsFlow
        coEvery { transactionRepository.save(any()) } returns 100L
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = RecordViewModel(
        transactionRepository,
        categoryRepository,
        accountRepository
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
}
