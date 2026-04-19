package com.waju.factory.digitalnote

import androidx.compose.ui.graphics.Color
import com.waju.factory.digitalnote.domain.filterNotes
import com.waju.factory.digitalnote.model.NoteItem
import org.junit.Test
import org.junit.Assert.assertEquals

class ExampleUnitTest {

    private val notes = listOf(
        NoteItem(
            id = 1,
            title = "Design Draft",
            excerpt = "Initial handwritten sketch",
            updatedLabel = "now",
            tags = listOf("DESIGN"),
            tones = listOf(Color(0xFF000000), Color(0xFFFFFFFF)),
            handwritten = true,
            starred = true,
            hasAttachment = true
        ),
        NoteItem(
            id = 2,
            title = "Meeting Notes",
            excerpt = "Summary and action items",
            updatedLabel = "now",
            tags = listOf("MEETING"),
            tones = listOf(Color(0xFF111111), Color(0xFFEEEEEE)),
            handwritten = false,
            starred = false,
            hasAttachment = false
        )
    )

    @Test
    fun filterNotes_filtersByQueryAndFlags() {
        val result = filterNotes(
            notes = notes,
            query = "design",
            handwrittenOnly = true,
            starredOnly = true,
            attachmentsOnly = true
        )

        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
    }

    @Test
    fun filterNotes_returnsAllWhenNoConstraints() {
        val result = filterNotes(
            notes = notes,
            query = "",
            handwrittenOnly = false,
            starredOnly = false,
            attachmentsOnly = false
        )

        assertEquals(2, result.size)
    }
}