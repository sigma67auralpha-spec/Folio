package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ReadStatus(val label: String) {
    UNREAD("Unread"),
    READING("Reading"),
    FINISHED("Finished")
}

enum class HighlightColor(val label: String, val hex: Long) {
    AMBER("Amber", 0x66FBBF24),
    SAGE("Sage", 0x664ADE80),
    ROSE("Rose", 0x66FB7185),
    SKY("Sky", 0x6660A5FA)
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "Unknown Author",
    val series: String = "",
    val seriesIndex: Float = 0f,
    val description: String = "",
    val format: BookFormat,
    val fileUri: String,
    val localCopyPath: String? = null,
    val coverPath: String? = null,
    val fileHash: String,
    val fileSizeBytes: Long = 0L,
    val totalUnits: Int = 1,
    val currentUnit: Int = 0,
    val scrollFraction: Float = 0f,
    val progressPercent: Float = 0f,
    val readStatus: ReadStatus = ReadStatus.UNREAD,
    val totalReadingSeconds: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val isMissingFile: Boolean = false
)

@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "book_shelf_cross_ref",
    primaryKeys = ["bookId", "shelfId"],
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ShelfEntity::class, parentColumns = ["id"], childColumns = ["shelfId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("shelfId")]
)
data class BookShelfCrossRef(
    val bookId: Long,
    val shelfId: Long
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "book_tag_cross_ref",
    primaryKeys = ["bookId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class BookTagCrossRef(
    val bookId: Long,
    val tagId: Long
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("bookId")]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val quoteText: String,
    val noteText: String = "",
    val color: HighlightColor = HighlightColor.AMBER,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val unitIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("bookId"), Index("epochDay")]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val epochDay: Long,
    val durationSeconds: Long
)

@Entity(tableName = "watched_folders", indices = [Index(value = ["treeUri"], unique = true)])
data class WatchedFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastScannedAt: Long = System.currentTimeMillis()
)

data class BookWithRelations(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookShelfCrossRef::class,
            parentColumn = "bookId",
            entityColumn = "shelfId"
        )
    )
    val shelves: List<ShelfEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookTagCrossRef::class,
            parentColumn = "bookId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "bookId")
    val highlights: List<HighlightEntity>
)
