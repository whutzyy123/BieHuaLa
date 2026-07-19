package com.biehuale.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.backup.BackupExporter
import com.biehuale.app.data.backup.BackupImporter
import com.biehuale.app.data.backup.ImportPreview
import com.biehuale.app.data.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 备份 ViewModel（导出/导入）
 *
 * 详见 docs/PRD.md §10
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    fun export(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, lastError = null)
            val result = withContext(Dispatchers.IO) { backupExporter.export(uri) }
            _state.value = _state.value.copy(isExporting = false)
            result.onSuccess {
                _events.emit(BackupEvent.ExportSuccess)
            }.onFailure { e ->
                _events.emit(BackupEvent.Error("导出失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPreviewing = true, lastError = null, pendingPreview = null)
            val result = withContext(Dispatchers.IO) { backupImporter.preview(uri) }
            result.onSuccess { preview ->
                _state.value = _state.value.copy(
                    isPreviewing = false,
                    pendingPreview = preview
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isPreviewing = false)
                _events.emit(BackupEvent.Error("导入预览失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    fun confirmImport(preview: ImportPreview) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true)
            val result = withContext(Dispatchers.IO) { backupImporter.applyImport(preview) }
            _state.value = _state.value.copy(isImporting = false, pendingPreview = null)
            result.onSuccess { r ->
                _events.emit(BackupEvent.ImportSuccess(r))
            }.onFailure { e ->
                _events.emit(BackupEvent.Error("导入失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    fun cancelImport() {
        _state.value = _state.value.copy(pendingPreview = null)
    }

    fun generateFileName(): String = backupExporter.generateDefaultFileName()
}

data class BackupUiState(
    val isExporting: Boolean = false,
    val isPreviewing: Boolean = false,
    val isImporting: Boolean = false,
    val pendingPreview: ImportPreview? = null,
    val lastError: String? = null
) {
    val isBusy: Boolean get() = isExporting || isPreviewing || isImporting
}

sealed interface BackupEvent {
    data object ExportSuccess : BackupEvent
    data class ImportSuccess(val result: ImportResult) : BackupEvent
    data class Error(val message: String) : BackupEvent
}
