package com.waju.factory.digitalnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.waju.factory.digitalnote.data.local.dao.NoteDao
import com.waju.factory.digitalnote.data.local.dao.StrokeDao
import com.waju.factory.digitalnote.data.local.entity.NoteEntity
import com.waju.factory.digitalnote.data.local.entity.StrokeEntity
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.DefaultCanvasPalette
import com.waju.factory.digitalnote.ui.canvas.LegacyDefaultCanvasPalette

@Database(
    entities = [NoteEntity::class, StrokeEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun strokeDao(): StrokeDao

    companion object {
        private val defaultPaletteCsv = DefaultCanvasPalette.joinToString(",") { it.value.toLong().toString() }
        private val legacyDefaultPaletteCsv = LegacyDefaultCanvasPalette.joinToString(",") { it.value.toLong().toString() }
        private val defaultTonesCsv = listOf(DefaultCanvasPalette[0], DefaultCanvasPalette[1])
            .joinToString(",") { it.value.toLong().toString() }

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

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_note.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

