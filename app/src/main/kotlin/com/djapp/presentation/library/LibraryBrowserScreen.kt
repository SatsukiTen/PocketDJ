package com.djapp.presentation.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djapp.domain.model.SortOrder
import com.djapp.domain.model.Track

/**
 * T-104 + T-106: 権限リクエスト + ライブラリブラウザUI。UC-008 対応。
 * AC-001-02（権限未許可）・UC-008 基本フロー・代替フロー A1/A2/A3 対応。
 */
@Composable
fun LibraryBrowserScreen(
    onTrackSelected: (Track) -> Unit,
    onDismiss: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // T-104: ストレージ権限リクエスト（Android 13以上: READ_MEDIA_AUDIO）
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onIntent(LibraryIntent.SetPermission(granted))
    }

    // 画面表示時に権限を確認・リクエスト
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- ヘッダー（検索バー + ソート + 閉じるボタン）---
            LibraryHeader(
                query     = uiState.searchQuery,
                sortOrder = uiState.sortOrder,
                onSearch  = { viewModel.onIntent(LibraryIntent.Search(it)) },
                onSort    = { viewModel.onIntent(LibraryIntent.SetSortOrder(it)) },
                onDismiss = onDismiss,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            // --- コンテンツ ---
            when {
                !uiState.hasPermission -> PermissionRequiredMessage(
                    onRetry = { permissionLauncher.launch(permission) }
                )
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                uiState.tracks.isEmpty() -> EmptyLibraryMessage()
                else -> TrackList(
                    tracks          = uiState.tracks,
                    onTrackSelected = onTrackSelected,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------
// サブコンポーネント
// -----------------------------------------------------------------------

@Composable
private fun LibraryHeader(
    query    : String,
    sortOrder: SortOrder,
    onSearch : (String) -> Unit,
    onSort   : (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    // TextFieldはローカル状態で即時更新し、ViewModelへの通知は並行して行う。
    // uiState.searchQueryはデバウンス後の値のためTextFieldに直接バインドすると入力できなくなる。
    var localQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != localQuery) localQuery = query
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // タイトル行
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SELECT TRACK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "閉じる")
            }
        }
        // 検索行
        Row(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextField(
                value         = localQuery,
                onValueChange = { text -> localQuery = text; onSearch(text) },
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("曲名 / アーティストで検索") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon  = if (localQuery.isNotEmpty()) {
                    { IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Default.Close, contentDescription = "検索クリア")
                    }}
                } else null,
                singleLine    = true,
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "並び替え")
                }
                DropdownMenu(
                    expanded        = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text    = { Text(order.label()) },
                            onClick = { onSort(order); showSortMenu = false },
                            trailingIcon = if (order == sortOrder) {
                                { Text("✓", color = MaterialTheme.colorScheme.primary) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks         : List<Track>,
    onTrackSelected: (Track) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = tracks, key = { it.id }) { track ->
            TrackListItem(track = track, onClick = { onTrackSelected(track) })
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun TrackListItem(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // アルバムアート円形プレースホルダー
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        ) {
            Text(
                text  = track.title.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text     = track.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = "${track.artist}  •  ${track.durationMs.toMinutesSeconds()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionRequiredMessage(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "音楽ファイルへのアクセス権限が必要です",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("権限を許可する")
            }
        }
    }
}

@Composable
private fun EmptyLibraryMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = "音楽ファイルが見つかりません",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

// -----------------------------------------------------------------------
// 拡張関数
// -----------------------------------------------------------------------

private fun SortOrder.label(): String = when (this) {
    SortOrder.TITLE      -> "曲名順"
    SortOrder.ARTIST     -> "アーティスト名順"
    SortOrder.DATE_ADDED -> "追加日順"
}

private fun Long.toMinutesSeconds(): String {
    val totalSec = this / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
