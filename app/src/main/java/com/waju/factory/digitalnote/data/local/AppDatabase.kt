package com.waju.factory.digitalnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.waju.factory.digitalnote.data.local.dao.CanvasTextBoxDao
import com.waju.factory.digitalnote.data.local.dao.CanvasImageDao
import com.waju.factory.digitalnote.data.local.dao.NoteDao
import com.waju.factory.digitalnote.data.local.dao.StrokeDao
import com.waju.factory.digitalnote.data.local.entity.CanvasImageEntity
import com.waju.factory.digitalnote.data.local.entity.CanvasTextBoxEntity
import com.waju.factory.digitalnote.data.local.entity.NoteEntity
import com.waju.factory.digitalnote.data.local.entity.StrokeEntity
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.DefaultCanvasPalette
import com.waju.factory.digitalnote.ui.canvas.LegacyDefaultCanvasPalette
import com.waju.factory.digitalnote.ui.theme.NoteCoverColors

@Database(
    entities = [NoteEntity::class, StrokeEntity::class, CanvasTextBoxEntity::class, CanvasImageEntity::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun strokeDao(): StrokeDao
    abstract fun textBoxDao(): CanvasTextBoxDao
    abstract fun imageDao(): CanvasImageDao

    companion object {
        private val defaultPaletteCsv = DefaultCanvasPalette.joinToString(",") { it.value.toLong().toString() }
        private val legacyDefaultPaletteCsv = LegacyDefaultCanvasPalette.joinToString(",") { it.value.toLong().toString() }
        private val defaultTonesCsv = listOf(DefaultCanvasPalette[0], DefaultCanvasPalette[1])
            .joinToString(",") { it.value.toLong().toString() }
        private val defaultCoverColorArgb = NoteCoverColors.first().value.toLong()

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN paletteCsv TEXT NOT NULL DEFAULT '$defaultPaletteCsv'")
                db.execSQL("ALTER TABLE notes ADD COLUMN selectedColorIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN baseStrokeWidth REAL NOT NULL DEFAULT 8.0")
                db.execSQL("ALTER TABLE notes ADD COLUMN sensitivity REAL NOT NULL DEFAULT 0.85")
                db.execSQL("ALTER TABLE notes ADD COLUMN canvasMode TEXT NOT NULL DEFAULT '${CanvasMode.PAGE.name}'")
                db.execSQL("ALTER TABLE notes ADD COLUMN backgroundStyle TEXT NOT NULL DEFAULT '${CanvasBackgroundStyle.GRID.name}'")
                db.execSQL("ALTER TABLE notes ADD COLUMN inputMode TEXT NOT NULL DEFAULT '${CanvasInputMode.PEN_ONLY.name}'")
                db.execSQL("ALTER TABLE notes ADD COLUMN pageCount INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE notes
                    SET paletteCsv = '$defaultPaletteCsv',
                        tonesCsv = '$defaultTonesCsv',
                        selectedColorIndex = 0
                    WHERE paletteCsv = '$legacyDefaultPaletteCsv'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN currentPageIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN canvasScale REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE notes ADD COLUMN canvasOffsetX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE notes ADD COLUMN canvasOffsetY REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS text_boxes (
                        id INTEGER PRIMARY KEY NOT NULL,
                        noteId INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        text TEXT NOT NULL,
                        colorArgb INTEGER NOT NULL,
                        fontSize REAL NOT NULL,
                        FOREIGN KEY(noteId) REFERENCES notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_text_boxes_noteId ON text_boxes(noteId)")
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE text_boxes ADD COLUMN width REAL NOT NULL DEFAULT 180.0")
                db.execSQL("ALTER TABLE text_boxes ADD COLUMN height REAL NOT NULL DEFAULT 140.0")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN coverColorArgb INTEGER NOT NULL DEFAULT $defaultCoverColorArgb")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS canvas_images (
                        id INTEGER PRIMARY KEY NOT NULL,
                        noteId INTEGER NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        width REAL NOT NULL,
                        height REAL NOT NULL,
                        rotationDeg REAL NOT NULL,
                        cropLeft REAL NOT NULL,
                        cropTop REAL NOT NULL,
                        cropRight REAL NOT NULL,
                        cropBottom REAL NOT NULL,
                        FOREIGN KEY(noteId) REFERENCES notes(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_canvas_images_noteId ON canvas_images(noteId)")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_note.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

