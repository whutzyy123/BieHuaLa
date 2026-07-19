package com.biehuale.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 搜索栏（全部流水页顶部展开态）
 */
@Composable
fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
            // 首次 launch 时还未附着
        }
    }

    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
            .focusRequester(focusRequester),
        placeholder = { Text("搜索说明…") },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(BhlIcons.ArrowBack, contentDescription = "关闭")
            }
        },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = { onKeywordChange("") }) {
                    Icon(BhlIcons.Close, contentDescription = "清空")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}
