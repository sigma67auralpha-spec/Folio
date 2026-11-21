package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.BookEntity
import com.example.data.model.BookShelfCrossRef
import com.example.data.model.BookTagCrossRef
import com.example.data.model.BookWithRelations
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadingSessionEntity
import com.example.data.model.ShelfEntity
import com.example.data.model.TagEntity
import com.example.data.model.WatchedFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolioDao {
    @Transaction
    @Query("SELECT * FROM books ORDER BY COALESCE(lastOpenedAt, addedAt) DESC")
    fun observeBooksWithRelations(): Flow<List<BookWithRelations>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun observeBookWithRelations(bookId: Long): Flow<BookWithRelations?>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE fileHash = :hash LIMIT 1")
    suspend fun findBookByHash(hash: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooksSnapshot(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET currentUnit = :unit, scrollFraction = :scrollFraction, progressPercent = :progressPercent, readStatus = :readStatus, lastOpenedAt = :lastOpenedAt WHERE id = :bookId")
    suspend fun updateProgress(
        bookId: Long,
        unit: Int,
        scrollFraction: Float,
        progressPercent: Float,
        readStatus: com.example.data.model.ReadStatus,
        lastOpenedAt: Long
    )

    @Query("UPDATE books SET totalReadingSeconds = totalReadingSeconds + :seconds WHERE id = :bookId")
    suspend fun addReadingSeconds(bookId: Long, seconds: Long)

    // Shelves
    @Query("SELECT * FROM shelves ORDER BY name ASC")
    fun observeShelves(): Flow<List<ShelfEntity>>

    @Query("SELECT * FROM shelves")
    suspend fun getAllShelvesSnapshot(): List<ShelfEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Update
    suspend fun updateShelf(shelf: ShelfEntity)

    @Delete
    suspend fun deleteShelf(shelf: ShelfEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookShelfRef(ref: BookShelfCrossRef)

    @Delete
    suspend fun deleteBookShelfRef(ref: BookShelfCrossRef)

    @Query("DELETE FROM book_shelf_cross_ref WHERE bookId = :bookId")
    suspend fun clearShelvesForBook(bookId: Long)

    @Query("SELECT * FROM book_shelf_cross_ref")
    suspend fun getAllShelfRefsSnapshot(): List<BookShelfCrossRef>

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookTagRef(ref: BookTagCrossRef)

    @Query("DELETE FROM book_tag_cross_ref WHERE bookId = :bookId")
    suspend fun clearTagsForBook(bookId: Long)

    // Highlights
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY chapterIndex ASC, paragraphIndex ASC, startOffset ASC")
    fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    suspend fun getAllHighlightsSnapshot(): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Update
    suspend fun updateHighlight(highlight: HighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY unitIndex ASC")
    fun observeBookmarks(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    // Reading sessions
    @Query("SELECT * FROM reading_sessions ORDER BY epochDay DESC")
    fun observeReadingSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllSessionsSnapshot(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId AND epochDay = :epochDay LIMIT 1")
    suspend fun findSession(bookId: Long, epochDay: Long): ReadingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("UPDATE reading_sessions SET durationSeconds = durationSeconds + :deltaSeconds WHERE id = :sessionId")
    suspend fun incrementSession(sessionId: Long, deltaSeconds: Long)

    // Watched folders
    @Query("SELECT * FROM watched_folders ORDER BY addedAt DESC")
    fun observeWatchedFolders(): Flow<List<WatchedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchedFolder(folder: WatchedFolderEntity): Long

    @Query("UPDATE watched_folders SET lastScannedAt = :timestamp WHERE id = :id")
    suspend fun updateFolderScanTime(id: Long, timestamp: Long)

    @Delete
    suspend fun deleteWatchedFolder(folder: WatchedFolderEntity)
}
