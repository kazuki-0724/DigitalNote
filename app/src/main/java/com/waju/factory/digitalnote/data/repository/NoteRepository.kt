package com.waju.factory.digitalnote.data.repository

import androidx.compose.ui.graphics.Color
import com.waju.factory.digitalnote.data.local.dao.CanvasTextBoxDao
import com.waju.factory.digitalnote.data.local.dao.NoteDao
import com.waju.factory.digitalnote.data.local.dao.StrokeDao
import com.waju.factory.digitalnote.data.local.entity.CanvasTextBoxEntity
import com.waju.factory.digitalnote.data.local.entity.NoteEntity
import com.waju.factory.digitalnote.data.local.entity.StrokeEntity
import com.waju.factory.digitalnote.model.NoteItem
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasSettings
import com.waju.factory.digitalnote.ui.canvas.DefaultCanvasPalette
import com.waju.factory.digitalnote.ui.canvas.DrawStroke
import com.waju.factory.digitalnote.ui.canvas.DrawingTool
import com.waju.factory.digitalnote.ui.canvas.StickyNote
import com.waju.factory.digitalnote.ui.canvas.StrokePoint
import com.waju.factory.digitalnote.ui.theme.NoteCoverColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NoteRepository(
    private val noteDao: NoteDao,
    private val strokeDao: StrokeDao,
    private val textBoxDao: CanvasTextBoxDao
) {
    suspend fun createNote(title: String, coverColor: Color = NoteCoverColors.first()): Int {
        val newId = (noteDao.maxId() ?: 0) + 1
        val defaultSettings = CanvasSettings()
        val note = NoteEntity(
            id = newId,
            title = title,
            excerpt = "ここにアイデアを書き始めましょう。",
            content = "",
            updatedLabel = "今",
            tagsCsv = "NEW",
            tonesCsv = deriveTones(defaultSettings.palette).joinToString(",") { it.value.toLong().toString() },
            coverColorArgb = coverColor.value.toLong(),
            handwritten = true,
            starred = false,
            hasAttachment = false,
            paletteCsv = encodeColors(defaultSettings.palette),
            selectedColorIndex = defaultSettings.selectedColorIndex,
            baseStrokeWidth = defaultSettings.baseStrokeWidth,
            sensitivity = defaultSettings.sensitivity,
            canvasMode = defaultSettings.mode.name,
            backgroundStyle = defaultSettings.backgroundStyle.name,
            inputMode = defaultSettings.inputMode.name,
            pageCount = defaultSettings.totalPages,
            currentPageIndex = defaultSettings.currentPageIndex,
            canvasScale = defaultSettings.scale,
            canvasOffsetX = defaultSettings.offsetX,
            canvasOffsetY = defaultSettings.offsetY
        )
        noteDao.insert(note)
        return newId
    }

    fun observeNotes(): Flow<List<NoteItem>> = combine(
        noteDao.observeAll(),
        textBoxDao.observeSearchableText()
    ) { entities, searchableRows ->
        val searchableTextByNoteId = searchableRows.associate { row ->
            row.noteId to (row.searchableText.orEmpty())
        }
        entities.map { entity ->
            entity.toModel(searchableText = searchableTextByNoteId[entity.id].orEmpty())
        }
    }

    fun observeNote(noteId: Int): Flow<NoteItem?> = noteDao.observeById(noteId).map { it?.toModel() }

    fun observeCanvasSettings(noteId: Int): Flow<CanvasSettings?> =
        noteDao.observeById(noteId).map { it?.toCanvasSettings() }

    suspend fun getCanvasSettings(noteId: Int): CanvasSettings? {
        return noteDao.getById(noteId)?.toCanvasSettings()
    }

    fun observeStrokes(noteId: Int): Flow<List<DrawStroke>> = strokeDao.observeByNoteId(noteId).map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun getStrokes(noteId: Int): List<DrawStroke> {
        return strokeDao.getByNoteId(noteId).map { it.toModel() }
    }

    suspend fun saveEditorContent(noteId: Int, title: String, content: String) {
        val existing = noteDao.getById(noteId) ?: return
        val updated = existing.copy(
            title = title,
            excerpt = content.take(80),
            content = content
        )
        noteDao.update(updated)
    }

    suspend fun updateNoteAppearance(noteId: Int, title: String, coverColor: Color) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(
            existing.copy(
                title = title,
                coverColorArgb = coverColor.value.toLong()
            )
        )
    }

    suspend fun deleteNote(noteId: Int) {
        noteDao.deleteById(noteId)
    }

    suspend fun saveCanvasSettings(noteId: Int, settings: CanvasSettings) {
        val existing = noteDao.getById(noteId) ?: return
        val palette = settings.palette.ifEmpty { DefaultCanvasPalette }
        val safePageCount = settings.totalPages.coerceAtLeast(1)
        val updated = existing.copy(
            tonesCsv = deriveTones(palette).joinToString(",") { it.value.toLong().toString() },
            paletteCsv = encodeColors(palette),
            selectedColorIndex = settings.selectedColorIndex.coerceIn(0, palette.lastIndex),
            baseStrokeWidth = settings.baseStrokeWidth,
            sensitivity = settings.sensitivity,
            canvasMode = settings.mode.name,
            backgroundStyle = settings.backgroundStyle.name,
            inputMode = settings.inputMode.name,
            pageCount = safePageCount,
            currentPageIndex = settings.currentPageIndex.coerceIn(0, safePageCount - 1),
            canvasScale = settings.scale,
            canvasOffsetX = settings.offsetX,
            canvasOffsetY = settings.offsetY
        )
        noteDao.update(updated)
    }

    suspend fun replaceStrokes(noteId: Int, strokes: List<DrawStroke>) {
        val noteExists = noteDao.getById(noteId) != null
        if (!noteExists) return

        val entities = strokes.mapIndexed { index, stroke ->
            StrokeEntity(
                noteId = noteId,
                pageIndex = stroke.pageIndex,
                toolType = stroke.tool.name,
                colorArgb = stroke.color.value.toLong(),
                width = stroke.width,
                pointsEncoded = encodePoints(stroke.points),
                createdAt = index.toLong()
            )
        }
        strokeDao.replaceByNoteId(noteId, entities)
    }

    suspend fun getStickyNotes(noteId: Int): List<StickyNote> {
        return textBoxDao.getByNoteId(noteId).map { it.toModel() }
    }

    suspend fun saveStickyNotes(noteId: Int, stickyNotes: List<StickyNote>) {
        val noteExists = noteDao.getById(noteId) != null
        if (!noteExists) return
        val entities = stickyNotes.map { it.toEntity(noteId) }
        textBoxDao.replaceByNoteId(noteId, entities)
    }

}

private fun encodeColors(colors: List<Color>): String {
    return colors.joinToString(separator = ",") { it.value.toLong().toString() }
}

private fun decodeColors(encoded: String, fallback: List<Color>): List<Color> {
    return encoded.split(',').mapNotNull { color ->
        color.toLongOrNull()?.let { Color(it.toULong()) }
    }.ifEmpty { fallback }
}

private fun deriveTones(palette: List<Color>): List<Color> {
    val safePalette = palette.ifEmpty { DefaultCanvasPalette }
    return listOf(
        safePalette.first(),
        safePalette.getOrElse(1) { safePalette.first() }
    )
}

private fun encodePoints(points: List<StrokePoint>): String {
    return points.joinToString(separator = ";") { point ->
        "${point.x},${point.y},${point.pressure},${point.timestamp}"
    }
}

private fun decodePoints(encoded: String): List<StrokePoint> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(';').mapNotNull { token ->
        val parts = token.split(',')
        if (parts.size != 4) return@mapNotNull null
        val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
        val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
        val pressure = parts[2].toFloatOrNull() ?: return@mapNotNull null
        val timestamp = parts[3].toLongOrNull() ?: return@mapNotNull null
        StrokePoint(x = x, y = y, pressure = pressure, timestamp = timestamp)
    }
}

private fun NoteEntity.toModel(searchableText: String = ""): NoteItem {
    val palette = decodeColors(paletteCsv, DefaultCanvasPalette)
    val tones = decodeColors(tonesCsv, deriveTones(palette))

    return NoteItem(
        id = id,
        title = title,
        excerpt = excerpt,
        content = content,
        updatedLabel = updatedLabel,
        tags = tagsCsv.split(',').filter { it.isNotBlank() },
        tones = tones,
        coverColor = Color(coverColorArgb.toULong()),
        searchableText = searchableText,
        handwritten = handwritten,
        starred = starred,
        hasAttachment = hasAttachment
    )
}

private fun NoteEntity.toCanvasSettings(): CanvasSettings {
    val palette = decodeColors(paletteCsv, DefaultCanvasPalette)
    val safePageCount = pageCount.coerceAtLeast(1)
    return CanvasSettings(
        mode = runCatching { CanvasMode.valueOf(canvasMode) }.getOrDefault(CanvasMode.PAGE),
        backgroundStyle = runCatching { CanvasBackgroundStyle.valueOf(backgroundStyle) }.getOrDefault(CanvasBackgroundStyle.GRID),
        inputMode = runCatching { CanvasInputMode.valueOf(inputMode) }.getOrDefault(CanvasInputMode.PEN_ONLY),
        totalPages = safePageCount,
        currentPageIndex = currentPageIndex.coerceIn(0, safePageCount - 1),
        palette = palette,
        selectedColorIndex = selectedColorIndex.coerceIn(0, palette.lastIndex),
        baseStrokeWidth = baseStrokeWidth,
        sensitivity = sensitivity,
        scale = canvasScale,
        offsetX = canvasOffsetX,
        offsetY = canvasOffsetY
    )
}

private fun NoteItem.toEntity(): NoteEntity {
    val defaultContent = if (content.isNotBlank()) content else excerpt
    val tones = if (tones.isEmpty()) deriveTones(DefaultCanvasPalette) else tones
    val defaultSettings = CanvasSettings()
    return NoteEntity(
        id = id,
        title = title,
        excerpt = excerpt,
        content = defaultContent,
        updatedLabel = updatedLabel,
        tagsCsv = tags.joinToString(separator = ","),
        tonesCsv = tones.joinToString(separator = ",") { it.value.toLong().toString() },
        coverColorArgb = coverColor.value.toLong(),
        handwritten = handwritten,
        starred = starred,
        hasAttachment = hasAttachment,
        paletteCsv = encodeColors(defaultSettings.palette),
        selectedColorIndex = defaultSettings.selectedColorIndex,
        baseStrokeWidth = defaultSettings.baseStrokeWidth,
        sensitivity = defaultSettings.sensitivity,
        canvasMode = defaultSettings.mode.name,
        backgroundStyle = defaultSettings.backgroundStyle.name,
        inputMode = defaultSettings.inputMode.name,
        pageCount = defaultSettings.totalPages,
        currentPageIndex = defaultSettings.currentPageIndex,
        canvasScale = defaultSettings.scale,
        canvasOffsetX = defaultSettings.offsetX,
        canvasOffsetY = defaultSettings.offsetY
    )
}

private fun StrokeEntity.toModel(): DrawStroke {
    return DrawStroke(
        id = id,
        pageIndex = pageIndex,
        tool = DrawingTool.valueOf(toolType),
        color = Color(colorArgb.toULong()),
        width = width,
        points = decodePoints(pointsEncoded)
    )
}

private fun CanvasTextBoxEntity.toModel(): StickyNote {
    return StickyNote(
        id = id,
        pageIndex = pageIndex,
        x = x,
        y = y,
        width = width,
        height = height,
        text = text,
        color = Color(colorArgb.toULong()),
        fontSize = fontSize
    )
}

private fun StickyNote.toEntity(noteId: Int): CanvasTextBoxEntity {
    return CanvasTextBoxEntity(
        id = id,
        noteId = noteId,
        pageIndex = pageIndex,
        x = x,
        y = y,
        width = width,
        height = height,
        text = text,
        colorArgb = color.value.toLong(),
        fontSize = fontSize
    )
}


