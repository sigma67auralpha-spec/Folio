package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookShelfCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightColor
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadStatus
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.ShelfEntity
import com.example.data.model.TagEntity
import com.example.data.model.WatchedFolderEntity

class FolioTypeConverters {
    @TypeConverter
    fun fromBookFormat(value: BookFormat): String = value.name

    @TypeConverter
    fun toBookFormat(value: String): BookFormat = BookFormat.valueOf(value)

    @TypeConverter
    fun fromReadStatus(value: ReadStatus): String = value.name

    @TypeConverter
    fun toReadStatus(value: String): ReadStatus = ReadStatus.valueOf(value)

    @TypeConverter
    fun fromHighlightColor(value: HighlightColor): String = value.name

    @TypeConverter
    fun toHighlightColor(value: String): HighlightColor = HighlightColor.valueOf(value)
}

@Database(
    entities = [
        BookEntity::class,
        ShelfEntity::class,
        BookShelfCrossRef::class,
        TagEntity::class,
        BookTagCrossRef::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        ReadingSessionEntity::class,
        WatchedFolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FolioTypeConverters::class)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun folioDao(): FolioDao

    companion object {
        @Volatile
        private var INSTANCE: FolioDatabase? = null

        fun getInstance(context: Context): FolioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "folio_library.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
