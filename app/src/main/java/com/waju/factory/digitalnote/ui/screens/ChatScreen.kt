package com.waju.factory.digitalnote.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.waju.factory.digitalnote.ui.viewmodel.ChatInputType
import com.waju.factory.digitalnote.ui.viewmodel.ChatMessage
import com.waju.factory.digitalnote.ui.viewmodel.ChatUiState
import dev.jeziellago.compose.markdowntext.MarkdownText

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
    onSendThreadReply: (parentId: Long, text: String) -> Unit
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    // スレッドパネル用状態
    var threadParentId by remember { mutableStateOf<Long?>(null) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 180.dp)
        ) {
            items(topLevelMessages, key = { it.id }) { parent ->
                val replies = repliesByParent[parent.id].orEmpty()

                ThreadMessageBlock(
                    parent = parent,
                    replyCount = replies.size,
                    onOpenThread = { threadParentId = parent.id },
                    onEdit = onEditMessage,
                    onCopy = { message ->
                        val text = message.content.ifBlank { message.localImagePath }
                        if (text.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    },
                    onReply = onReplyMessage,
                    onDelete = { message -> onDeleteMessage(message.id) }
                )
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.editingMessageId != null || uiState.replyingPreview != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val label = if (uiState.editingMessageId != null) {
                            "編集中"
                        } else {
                            "返信: ${uiState.replyingPreview.orEmpty()}"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onCancelComposeMode) {
                            Text("キャンセル")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.inputType == ChatInputType.TEXT,
                        onClick = { onInputTypeChanged(ChatInputType.TEXT) },
                        label = { Text("テキスト", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.inputType == ChatInputType.MARKDOWN,
                        onClick = { onInputTypeChanged(ChatInputType.MARKDOWN) },
                        label = { Text("MD", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.inputType == ChatInputType.HTML,
                        onClick = { onInputTypeChanged(ChatInputType.HTML) },
                        label = { Text("HTML", fontSize = 12.sp) }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Outlined.Image, contentDescription = "画像を添付")
                    }
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = onInputTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 160.dp),
                        placeholder = {
                            Text(
                                when (uiState.inputType) {
                                    ChatInputType.MARKDOWN -> "Markdown を入力..."
                                    ChatInputType.HTML -> "HTML を入力..."
                                    else -> "メッセージを入力..."
                                }
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { sendAndCloseKeyboard() }
                        )
                    )
                    IconButton(
                        onClick = sendAndCloseKeyboard,
                        enabled = uiState.inputText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "送信",
                            tint = if (uiState.inputText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }
        }
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
            onSendReply = { text -> onSendThreadReply(parentForThread.id, text) },
            onDismiss = { threadParentId = null }
        )
    }
}

@Composable
private fun ThreadMessageBlock(
    parent: ChatMessage,
    replyCount: Int,
    onOpenThread: () -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ChatBubble(
            message = parent,
            isReply = false,
            onEdit = { onEdit(parent) },
            onCopy = { onCopy(parent) },
            onReply = { onReply(parent) },
            onDelete = { onDelete(parent) }
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
    onSendReply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || true }
    )
    var replyText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(replies.size) {
        if (replies.isNotEmpty()) listState.animateScrollToItem(replies.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f)
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

            // ── 親メッセージ ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = parent.content.ifBlank { "📷 画像" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── 返信数区切り ──
            if (replies.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
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

            // ── 返信一覧 ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(replies, key = { it.id }) { reply ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = reply.content.ifBlank { "📷 画像" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // ── 返信入力欄 ──
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp),
                        placeholder = { Text("返信を追加する") },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (replyText.isNotBlank()) {
                                    onSendReply(replyText)
                                    replyText = ""
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(replyText)
                                replyText = ""
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "返信を送信",
                            tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
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
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    var showActionSheet by remember { mutableStateOf(false) }
    var isRendering by remember(message.id) {
        mutableStateOf(message.type != ChatInputType.TEXT && message.type != ChatInputType.IMAGE)
    }

    if (showActionSheet) {
        ModalBottomSheet(onDismissRequest = { showActionSheet = false }) {
            ActionGrid(
                onEdit = {
                    onEdit()
                    showActionSheet = false
                },
                onCopy = {
                    onCopy()
                    showActionSheet = false
                },
                onReply = {
                    onReply()
                    showActionSheet = false
                },
                onDelete = {
                    onDelete()
                    showActionSheet = false
                }
            )
            TextButton(
                onClick = { showActionSheet = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("閉じる")
            }
            Box(modifier = Modifier.height(12.dp))
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isReply) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = if (isReply) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showActionSheet = true }
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
