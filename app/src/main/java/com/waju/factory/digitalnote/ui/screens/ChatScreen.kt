package com.waju.factory.digitalnote.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.waju.factory.digitalnote.ui.viewmodel.ChatInputType
import com.waju.factory.digitalnote.ui.viewmodel.ChatMessage
import com.waju.factory.digitalnote.ui.viewmodel.ChatUiState
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File

data class MessageActions(
    val onEdit: ((ChatMessage) -> Unit)? = null,
    val onCopy: ((ChatMessage) -> Unit)? = null,
    val onReply: ((ChatMessage) -> Unit)? = null,
    val onDelete: ((ChatMessage) -> Unit)? = null,
    val onCopyImage: ((ChatMessage) -> Unit)? = null,
    val onOpenViewer: ((ChatMessage) -> Unit)? = null
)

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onInputTextChanged: (String) -> Unit,
    onInputTypeChanged: (ChatInputType) -> Unit,
    onSendText: () -> Unit,
    onSendImage: (Uri) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onEditMessage: (ChatMessage) -> Unit,
    onReplyMessage: (ChatMessage) -> Unit,
    onCancelComposeMode: () -> Unit,
    onSendThreadReply: (parentId: Long, type: ChatInputType, text: String) -> Unit,
    onSendThreadReplyImage: (parentId: Long, uri: Uri) -> Unit,
    onOpenViewer: (messageId: Long) -> Unit
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // スレッドパネル用状態
    var threadParentId by remember { mutableStateOf<Long?>(null) }
    // スレッド内ビュワー（full-screen オーバーレイ）用状態
    var threadViewingMessageId by remember { mutableStateOf<Long?>(null) }

    // 共通アクションをここで1つにまとめる
    val messageActions = remember {
        MessageActions(
            onEdit = onEditMessage,
            onCopy = { message ->
                val text = message.content.ifBlank { message.localImagePath }
                if (text.isNotBlank()) clipboardManager.setText(AnnotatedString(text))
            },
            onReply = { message -> threadParentId = message.id },
            onDelete = { message -> onDeleteMessage(message.id) },
            onOpenViewer = { message -> threadViewingMessageId = message.id },
            onCopyImage = { message ->
                val copied = copyImageToClipboard(context, message.localImagePath, message.id)
                if (copied) Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ビュワー表示中はシステムバックでビュワーを閉じる
    BackHandler(enabled = threadViewingMessageId != null) {
        threadViewingMessageId = null
    }

    val topLevelMessages = remember(uiState.messages) {
        uiState.messages.filter { it.replyToMessageId == null }
    }
    val repliesByParent = remember(uiState.messages) {
        uiState.messages.filter { it.replyToMessageId != null }.groupBy { it.replyToMessageId!! }
    }

    LaunchedEffect(topLevelMessages.size, uiState.messages.size) {
        if (topLevelMessages.isNotEmpty()) {
            listState.animateScrollToItem(topLevelMessages.lastIndex)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onSendImage(uri)
    }

    val sendAndCloseKeyboard: () -> Unit = send@{
        if (uiState.inputText.isBlank()) return@send
        onSendText()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // .windowInsetsPadding(WindowInsets.safeDrawing)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
        ) {
            items(topLevelMessages, key = { it.id }) { parent ->
                val replies = repliesByParent[parent.id].orEmpty()

                ThreadMessageBlock(
                    parent = parent,
                    replyCount = replies.size,
                    onOpenThread = { threadParentId = parent.id },
                    actions = messageActions
                )
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            onTextChanged = onInputTextChanged,
            inputType = uiState.inputType,
            onInputTypeChanged = onInputTypeChanged,
            onSend = sendAndCloseKeyboard,
            onAttachImage = { imagePickerLauncher.launch("image/*") },
            placeholderText = when (uiState.inputType) {
                ChatInputType.MARKDOWN -> "Markdown を入力..."
                ChatInputType.HTML -> "HTML を入力..."
                else -> "メッセージを入力..."
            },
            editingMessageId = uiState.editingMessageId,
            replyingPreview = uiState.replyingPreview,
            onCancelComposeMode = onCancelComposeMode
        )
    }

    // スレッド専用パネル
    val parentForThread = threadParentId?.let { pid ->
        uiState.messages.firstOrNull { it.id == pid }
    }
    if (threadParentId != null && parentForThread != null) {
        val threadReplies = repliesByParent[threadParentId].orEmpty()
        ThreadDetailSheet(
            parent = parentForThread,
            replies = threadReplies,
            onSendReply = { type, text -> onSendThreadReply(parentForThread.id, type, text) },
            onSendReplyImage = { uri -> onSendThreadReplyImage(parentForThread.id, uri) },
            // スレッド内タップ → 外側 Box にビュワーをオーバーレイ
            onOpenViewer = { messageId -> threadViewingMessageId = messageId },
            onDismiss = { threadParentId = null },
            actions = messageActions,
            uiState = uiState,
            onInputTextChanged = onInputTextChanged,
            onInputTypeChanged = onInputTypeChanged,
            onSendText = sendAndCloseKeyboard,
            onCancelComposeMode = onCancelComposeMode
        )
    }

    val viewingMessage = threadViewingMessageId?.let { id ->
        uiState.messages.firstOrNull { it.id == id }
    }

    if (threadViewingMessageId != null) {
        Dialog(
            // ダイアログの外側をタップしたときの処理（フルスクリーンなので基本呼ばれません）
            onDismissRequest = { threadViewingMessageId = null },
            properties = DialogProperties(
                // これを false にしないと、ダイアログ特有の左右の余白ができてしまう
                usePlatformDefaultWidth = false,
                // システムの戻るボタン（スワイプ）で閉じられるようにする
                dismissOnBackPress = true,
                // (オプション) ステータスバーやナビゲーションバーの裏まで画面を広げたい場合は false にする
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ChatAttachmentViewerScreen(
                    message = viewingMessage,
                    onBack = { threadViewingMessageId = null }
                )
            }
        }
    }
}

@Composable
private fun ThreadMessageBlock(
    parent: ChatMessage,
    replyCount: Int,
    onOpenThread: () -> Unit,
    actions: MessageActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ChatBubble(
            message = parent,
            isReply = false,
            onEdit = actions.onEdit?.let { { it(parent) } },
            onCopy = actions.onCopy?.let { { it(parent) } },
            onDelete = actions.onDelete?.let { { it(parent) } },
            onReply = actions.onReply?.let { { it(parent) } },
            onOpenViewer = actions.onOpenViewer?.let { { it(parent) } },
            onCopyImage = actions.onCopyImage?.let { { it(parent) } }
        )

        if (replyCount > 0) {
            // 返信数ボタン：メッセージと同じく右寄せ
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onOpenThread) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "  ${replyCount}つの返信",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ── スレッド専用パネル ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadDetailSheet(
    parent: ChatMessage,
    replies: List<ChatMessage>,
    onSendReply: (type: ChatInputType, text: String) -> Unit,
    onSendReplyImage: (uri: Uri) -> Unit,
    onOpenViewer: (messageId: Long) -> Unit,
    onDismiss: () -> Unit,
    actions: MessageActions,
    uiState: ChatUiState,
    onInputTextChanged: (String) -> Unit,
    onInputTypeChanged: (ChatInputType) -> Unit,
    onSendText: () -> Unit,
    onCancelComposeMode: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || true }
    )
    var replyText by remember { mutableStateOf("") }
    var replyInputType by remember { mutableStateOf(ChatInputType.TEXT) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onSendReplyImage(uri) }

    val sendAndClose: () -> Unit = {
        if (replyText.isNotBlank()) {
            onSendReply(replyInputType, replyText)
            replyText = ""
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(replies.size) {
        // 親(0) + 区切り(1) + 返信(2..) の並びなので最後尾へスクロール
        val totalItems = 1 + (if (replies.isNotEmpty()) 1 else 0) + replies.size
        if (totalItems > 1) listState.animateScrollToItem(totalItems - 1)
    }

    // 現在編集中のメッセージが、このスレッド内のものか判定する
    val isEditingInThread = uiState.editingMessageId != null &&
            (uiState.editingMessageId == parent.id || replies.any { it.id == uiState.editingMessageId })

    // 編集中の場合はViewModelの状態(uiState)を、そうでない場合はローカルの状態を使う
    val currentText = if (isEditingInThread) uiState.inputText else replyText
    val currentInputType = if (isEditingInThread) uiState.inputType else replyInputType

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier//.fillMaxHeight(0.92f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── ヘッダー ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "閉じる")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "スレッド",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Divider()

                // ── 親 + 返信数区切り + 返信一覧を同一スクロールビューに同居 ──
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    // 親メッセージ（左寄せ）
                    item(key = "parent") {
                        ChatBubble(
                            message = parent,
                            isReply = true,
                            onEdit = null,
                            onCopy = actions.onCopy?.let { { it(parent) } },
                            onOpenViewer = { onOpenViewer(parent.id) },
                            onCopyImage = actions.onCopyImage?.let { { it(parent) } }
                        )
                    }

                    // 返信数区切り
                    if (replies.isNotEmpty()) {
                        item(key = "divider") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${replies.size}件の返信",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Divider(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // 返信一覧
                    items(replies, key = { it.id }) { reply ->
                        ChatBubble(
                            message = reply,
                            isReply = false,
                            onEdit = actions.onEdit?.let { { it(reply) } },
                            onCopy = actions.onCopy?.let { { it(reply) } },
                            onDelete = actions.onDelete?.let { { it(reply) } },
                            onOpenViewer = { onOpenViewer(reply.id) },
                            onCopyImage = actions.onCopyImage?.let { { it(reply) } }
                        )
                    }
                }

            Divider()

            ChatInputBar(
                text = currentText,
                onTextChanged = {
                    if (isEditingInThread) onInputTextChanged(it) else replyText = it
                },
                inputType = currentInputType,
                onInputTypeChanged = {
                    if (isEditingInThread) onInputTypeChanged(it) else replyInputType = it
                },
                onSend = {
                    if (isEditingInThread) {
                        onSendText() // 編集完了時はViewModelの保存処理を呼ぶ
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    } else {
                        sendAndClose() // 通常の返信処理
                    }
                },
                onAttachImage = { imagePickerLauncher.launch("image/*") },
                placeholderText = "返信を追加する",

                // ▼ 編集状態のUIを表示するための制御 ▼
                editingMessageId = if (isEditingInThread) uiState.editingMessageId else null,
                onCancelComposeMode = {
                    if (isEditingInThread) onCancelComposeMode()
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
}

// アクションをまとめるデータクラス
private data class ActionItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

// 動的に2列のグリッドを生成するコンポーネント
@Composable
private fun DynamicActionGrid(actions: List<ActionItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // アクションを2個ずつのペアに分割
        actions.chunked(2).forEach { rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1つ目のボタン
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = { Icon(rowActions[0].icon, contentDescription = null) },
                    label = rowActions[0].label,
                    onClick = rowActions[0].onClick
                )
                // 2つ目のボタン（奇数で余った場合は透明なSpacerで幅を維持）
                if (rowActions.size > 1) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(rowActions[1].icon, contentDescription = null) },
                        label = rowActions[1].label,
                        onClick = rowActions[1].onClick
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    isReply: Boolean,
    onEdit: (() -> Unit)? = null,        // Null許容にしてデフォルトをnullに
    onCopy: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpenViewer: (() -> Unit)? = null,
    onCopyImage: (() -> Unit)? = null
) {
    var showActionSheet by remember { mutableStateOf(false) }
    var showImageActionSheet by remember { mutableStateOf(false) }
    var isRendering by remember(message.id) {
        mutableStateOf(message.type != ChatInputType.TEXT && message.type != ChatInputType.IMAGE)
    }
    val chatBubbleColor = Color(0xFFE3F2FD)

    // Nullでないアクションだけを抽出したリストを生成
    val textActions = listOfNotNull(
        onEdit?.let { ActionItem(Icons.Outlined.Edit, "編集") { it(); showActionSheet = false } },
        onCopy?.let { ActionItem(Icons.Outlined.ContentCopy, "コピー") { it(); showActionSheet = false } },
        onReply?.let { ActionItem(Icons.AutoMirrored.Outlined.Reply, "返信") { it(); showActionSheet = false } },
        onDelete?.let { ActionItem(Icons.Outlined.Delete, "削除") { it(); showActionSheet = false } }
    )

    val imageActions = listOfNotNull(
        onCopyImage?.let { ActionItem(Icons.Outlined.ContentCopy, "コピー") { it(); showImageActionSheet = false } },
        onReply?.let { ActionItem(Icons.AutoMirrored.Outlined.Reply, "返信") { it(); showImageActionSheet = false } },
        onDelete?.let { ActionItem(Icons.Outlined.Delete, "削除") { it(); showImageActionSheet = false } }
    )

    // テキスト系メッセージの長押しアクションシート
    if (showActionSheet && textActions.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showActionSheet = false }) {
            DynamicActionGrid(actions = textActions)
            TextButton(
                onClick = { showActionSheet = false },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("閉じる") }
            Box(modifier = Modifier.height(12.dp))
        }
    }

    // 画像専用長押しアクションシート
    if (showImageActionSheet && imageActions.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showImageActionSheet = false }) {
            DynamicActionGrid(actions = imageActions)
            TextButton(
                onClick = { showImageActionSheet = false },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("閉じる") }
            Box(modifier = Modifier.height(12.dp))
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isReply) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = chatBubbleColor,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {
                        if (onOpenViewer != null) {
                            when (message.type) {
                                ChatInputType.MARKDOWN,
                                ChatInputType.HTML,
                                ChatInputType.IMAGE -> onOpenViewer()
                                else -> Unit
                            }
                        }
                    },
                    onLongClick = {
                        // アクションが存在する場合のみシートを開く
                        if (message.type == ChatInputType.IMAGE) {
                            if (imageActions.isNotEmpty()) showImageActionSheet = true
                        } else {
                            if (textActions.isNotEmpty()) showActionSheet = true
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (message.type) {
                    ChatInputType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isReply) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    ChatInputType.MARKDOWN -> {
                        BubbleToggleBar(isRendering = isRendering, label = "MD") { isRendering = !isRendering }
                        if (isRendering) {
                            MarkdownText(markdown = message.content, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                text = message.content,
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    ChatInputType.HTML -> {
                        BubbleToggleBar(isRendering = isRendering, label = "HTML") { isRendering = !isRendering }
                        if (isRendering) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = false
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                        isVerticalScrollBarEnabled = false
                                        isHorizontalScrollBarEnabled = false
                                    }
                                },
                                update = { webView ->
                                    webView.loadDataWithBaseURL(
                                        null,
                                        """<!DOCTYPE html><html><head>
                                            <meta name="viewport" content="width=device-width,initial-scale=1">
                                            <style>body{font-family:sans-serif;font-size:14px;padding:4px;word-wrap:break-word;}</style>
                                            </head><body>${message.content}</body></html>""",
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 340.dp)
                            )
                        } else {
                            Text(
                                text = message.content,
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    ChatInputType.IMAGE -> {
                        ImageBubbleContent(path = message.localImagePath)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGrid(
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                label = "編集",
                onClick = onEdit
            )
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                label = "コピー",
                onClick = onCopy
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null) },
                label = "返信",
                onClick = onReply
            )
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                label = "削除",
                onClick = onDelete
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ActionButton(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BubbleToggleBar(isRendering: Boolean, label: String, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Box(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = if (isRendering) Icons.Outlined.Edit else Icons.Outlined.Visibility,
                contentDescription = if (isRendering) "ソース表示" else "レンダリング",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ImageBubbleContent(path: String) {
    val bitmap = remember(path) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "添付画像",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .heightIn(max = 220.dp)
        )
    }
}

private fun copyImageToClipboard(context: Context, imagePath: String, messageId: Long): Boolean {
    return try {
        if (imagePath.isBlank()) {
            Log.e("ChatImageCopy", "Image path is blank. messageId=$messageId")
            return false
        }

        val sourceFile = File(imagePath)
        if (!sourceFile.exists()) {
            Log.e("ChatImageCopy", "Image file does not exist. messageId=$messageId path=$imagePath")
            return false
        }

        val clipDir = File(context.cacheDir, "clipboard_images").apply { mkdirs() }
        clipDir.listFiles()?.forEach { cached ->
            if (cached.isFile && System.currentTimeMillis() - cached.lastModified() > 60_000L) {
                cached.delete()
            }
        }

        val extension = sourceFile.extension.takeIf { it.isNotBlank() } ?: "webp"
        val targetFile = File(clipDir, "chat_copy_${System.currentTimeMillis()}.$extension")
        sourceFile.copyTo(targetFile, overwrite = true)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )
        val clip = ClipData.newUri(context.contentResolver, "chat_image", uri)
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(clip)
        true
    } catch (e: Exception) {
        Log.e("ChatImageCopy", "Failed to copy image. messageId=$messageId", e)
        false
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    inputType: ChatInputType,
    onInputTypeChanged: (ChatInputType) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    // オプション：編集・返信モードの状態
    editingMessageId: Long? = null,
    replyingPreview: String? = null,
    onCancelComposeMode: (() -> Unit)? = null
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ① 編集・返信中の表示（必要な場合のみ）
            if ((editingMessageId != null || replyingPreview != null) && onCancelComposeMode != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val label = if (editingMessageId != null) "編集中" else "返信: $replyingPreview"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onCancelComposeMode) {
                        Text("キャンセル", fontSize = 12.sp)
                    }
                }
            }

            // ② 入力タイプ切り替えチップ
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    ChatInputType.TEXT to "Text",
                    ChatInputType.MARKDOWN to "MD",
                    ChatInputType.HTML to "HTML"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = inputType == type,
                        onClick = { onInputTypeChanged(type) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // ③ 入力メイン行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                IconButton(onClick = onAttachImage, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Image, contentDescription = "画像を添付")
                }

                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp, max = 120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholderText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "送信",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
