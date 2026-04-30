package com.waju.factory.digitalnote.ui.screens

import android.graphics.BitmapFactory
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.waju.factory.digitalnote.ui.components.SectionTopBar
import com.waju.factory.digitalnote.ui.viewmodel.ChatInputType
import com.waju.factory.digitalnote.ui.viewmodel.ChatMessage
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

@Composable
fun ChatAttachmentViewerScreen(
    message: ChatMessage?,
    onBack: () -> Unit
) {
    if (message == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionTopBar(title = "添付ビューア", onBackToTop = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "表示できるメッセージがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var showSource by remember(message.id) { mutableStateOf(false) }
    val title = viewerTitle(message.type)

    Column(modifier = Modifier.fillMaxSize()) {
        // ── トップバー（MD/HTML の場合は右端にソーストグル） ───────────────
        SectionTopBar(
            title = title,
            onBackToTop = onBack,
            trailingContent = if (message.type == ChatInputType.MARKDOWN || message.type == ChatInputType.HTML) {
                {
                    TextButton(onClick = { showSource = !showSource }) {
                        Text(if (showSource) "レンダリング" else "ソース")
                    }
                }
            } else null
        )

        // ── コンテンツ ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (message.type == ChatInputType.IMAGE) 14.dp else 0.dp,
                         vertical   = if (message.type == ChatInputType.IMAGE) 8.dp else 0.dp)
        ) {
            when (message.type) {
                ChatInputType.IMAGE -> ViewerImage(path = message.localImagePath)

                ChatInputType.MARKDOWN -> {
                    if (showSource) {
                        SourceText(text = message.content)
                    } else {
                        val html = remember(message.id) { markdownToFullHtml(message.content) }
                        RichWebView(html = html, baseUrl = null)
                    }
                }

                ChatInputType.HTML -> {
                    if (showSource) {
                        SourceText(text = message.content)
                    } else {
                        // ユーザー入力が断片か完全ドキュメントか判定して適宜ラップ
                        val html = remember(message.id) {
                            if (message.content.trimStart().startsWith("<!DOCTYPE", ignoreCase = true) ||
                                message.content.trimStart().startsWith("<html", ignoreCase = true)) {
                                message.content
                            } else {
                                wrapHtmlFragment(message.content)
                            }
                        }
                        RichWebView(html = html, baseUrl = "https://localhost/")
                    }
                }

                ChatInputType.TEXT -> SourceText(text = message.content)
            }
        }
    }
}


// ── ヘルパーコンポーザブル ─────────────────────────────────────────────────────

/**
 * JS / DOM storage / ネットワーク全て有効にした WebView。
 * CDN 読み込みや複雑なレンダリングに対応。
 */
@Composable
private fun RichWebView(html: String, baseUrl: String?) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    @Suppress("DEPRECATION")
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                baseUrl,
                html,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SourceText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    )
}

@Composable
private fun ViewerImage(path: String) {
    val bitmap = remember(path) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "添付画像",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("画像を表示できません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Markdown → HTML 変換 ─────────────────────────────────────────────────────

private val mdExtensions = listOf(
    TablesExtension.create(),
    StrikethroughExtension.create()
)
private val mdParser   = Parser.builder().extensions(mdExtensions).build()
private val mdRenderer = HtmlRenderer.builder().extensions(mdExtensions).build()

private fun markdownToFullHtml(markdown: String): String {
    val body = mdRenderer.render(mdParser.parse(markdown))
    return wrapHtmlFragment(body)
}

private fun wrapHtmlFragment(body: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>
        body {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          font-size: 15px; line-height: 1.7; padding: 16px; color: #333; word-wrap: break-word;
        }
        h1, h2, h3, h4, h5, h6 { line-height: 1.3; margin-top: 1.2em; margin-bottom: 0.4em; }
        h1 { font-size: 1.8em; border-bottom: 2px solid #eee; padding-bottom: 6px; }
        h2 { font-size: 1.4em; border-bottom: 1px solid #eee; padding-bottom: 4px; }
        code { background: #f4f4f4; padding: 2px 5px; border-radius: 3px;
               font-family: monospace; font-size: 0.9em; }
        pre  { background: #f4f4f4; padding: 12px; border-radius: 6px;
               overflow-x: auto; font-size: 0.88em; }
        pre code { background: none; padding: 0; }
        blockquote { border-left: 4px solid #ccc; margin: 0.8em 0; padding: 4px 16px; color: #666; }
        table { border-collapse: collapse; width: 100%; margin: 1em 0; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background: #f0f0f0; font-weight: bold; }
        tr:nth-child(even) td { background: #fafafa; }
        img { max-width: 100%; height: auto; border-radius: 4px; }
        a   { color: #0969da; }
        hr  { border: none; border-top: 1px solid #ddd; margin: 1.2em 0; }
        del { color: #888; }
        ul, ol { padding-left: 1.8em; }
      </style>
    </head>
    <body>$body</body>
    </html>
""".trimIndent()

private fun viewerTitle(type: ChatInputType): String = when (type) {
    ChatInputType.IMAGE    -> "画像"
    ChatInputType.MARKDOWN -> "MD"
    ChatInputType.HTML     -> "HTML"
    ChatInputType.TEXT     -> "テキスト"
}

