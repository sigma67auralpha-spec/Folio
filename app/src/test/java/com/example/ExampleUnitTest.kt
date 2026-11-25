package com.example

import com.example.data.model.BookFormat
import com.example.engine.BackupManager
import com.example.engine.BlockStyle
import com.example.engine.ComicArchiveHelper
import com.example.engine.DocxRtfFb2Parser
import com.example.engine.EpubParser
import com.example.engine.ParsedBookContent
import com.example.engine.TextHtmlMdParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExampleUnitTest {

    @Test
    fun formatDetectionCoversAllTwelveFormats() {
        val expected = mapOf(
            "novel.epub" to BookFormat.EPUB,
            "paper.pdf" to BookFormat.PDF,
            "classic.mobi" to BookFormat.MOBI,
            "book.azw3" to BookFormat.AZW3,
            "story.fb2" to BookFormat.FB2,
            "archive.fb2.zip" to BookFormat.FB2,
            "draft.docx" to BookFormat.DOCX,
            "notes.rtf" to BookFormat.RTF,
            "readme.txt" to BookFormat.TXT,
            "article.html" to BookFormat.HTML,
            "guide.md" to BookFormat.MARKDOWN,
            "issue1.cbz" to BookFormat.CBZ,
            "issue2.cbr" to BookFormat.CBR
        )
        expected.forEach { (name, fmt) ->
            assertEquals("Expected $fmt for $name", fmt, BookFormat.fromFileName(name))
        }
    }

    @Test
    fun markdownParserExtractsTitleAndInlineRuns() {
        val md = "# My Book\n\nThis has **bold** and *italic* words."
        val parsed = TextHtmlMdParser.parseMarkdown(md, "Fallback")
        assertEquals("My Book", parsed.metadata.title)
        assertTrue(parsed.chapters.isNotEmpty())
        val bodyBlock = parsed.chapters.first().blocks.last()
        assertEquals("This has bold and italic words.", bodyBlock.text)
        assertEquals(2, bodyBlock.runs.size)
    }

    @Test
    fun rtfParserExtractsCleanParagraphs() {
        val rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}Hello \\emdash  world!\\par Second paragraph.}"
        val parsed = DocxRtfFb2Parser.parseRtf(rtf.toByteArray(), "RTF Test")
        val blocks = parsed.chapters.first().blocks
        assertEquals(2, blocks.size)
        assertEquals("Hello — world!", blocks[0].text)
        assertEquals("Second paragraph.", blocks[1].text)
    }

    @Test
    fun comicNaturalSortOrdersNumbersProperly() {
        val pages = listOf("page_10.jpg", "page_2.jpg", "page_1.jpg")
        val sorted = pages.sortedWith(ComicArchiveHelper.naturalComparator)
        assertEquals(listOf("page_1.jpg", "page_2.jpg", "page_10.jpg"), sorted)
    }

    @Test
    fun nestedDivAndSectionHtmlExtractsAllParagraphs() {
        val html = """
            <html><head><title>Ignored</title><style>p { color: red; }</style></head>
            <body>
              <div class="chapter">
                <section>
                  <h1>Chapter One</h1>
                  <div class="inner">
                    <p>First <b>bold</b> paragraph.</p>
                    <p>Second <em>italic</em> paragraph.</p>
                  </div>
                </section>
              </div>
            </body></html>
        """.trimIndent()
        val blocks = TextHtmlMdParser.extractHtmlBlocks(html)
        assertEquals(3, blocks.size)
        assertEquals(BlockStyle.HEADING_1, blocks[0].style)
        assertEquals("Chapter One", blocks[0].text)
        assertEquals("First bold paragraph.", blocks[1].text)
        assertTrue(blocks[1].runs.any { it.bold })
        assertEquals("Second italic paragraph.", blocks[2].text)
        assertTrue(blocks[2].runs.any { it.italic })
    }

    @Test
    fun epubParserHandlesUrlEncodedAndRelativePathsAndFontObfuscation() {
        val tmp = File.createTempFile("test_book", ".epub")
        tmp.deleteOnExit()
        ZipOutputStream(tmp.outputStream()).use { zos ->
            fun add(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            add("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/Sub/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            add("META-INF/encryption.xml", """<encryption><EncryptedData><CipherData><CipherReference URI="OEBPS/Fonts/Book.ttf"/></CipherData></EncryptedData></encryption>""")
            add(
                "OEBPS/Sub/content.opf",
                """<?xml version="1.0"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Encoded Odyssey</dc:title>
                    <dc:creator>Ada Lovelace</dc:creator>
                  </metadata>
                  <manifest>
                    <item id="nav" href="../nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="c1" href="../Text/Chapter%201.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                  </spine>
                </package>""".trimIndent()
            )
            add("OEBPS/nav.xhtml", """<html><body><nav><ol><li><a href="Text/Chapter%201.xhtml">The Beginning</a></li></ol></nav></body></html>""")
            add("OEBPS/Text/Chapter 1.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><div class="wrap"><p>Hello from inside a div.</p></div></body></html>""")
        }

        val parsed = EpubParser.parse(tmp, "Fallback")
        assertTrue("Expected Reflowable but got $parsed", parsed is ParsedBookContent.Reflowable)
        val reflowable = parsed as ParsedBookContent.Reflowable
        assertEquals("Encoded Odyssey", reflowable.metadata.title)
        assertEquals("Ada Lovelace", reflowable.metadata.author)
        assertEquals(1, reflowable.chapters.size)
        assertEquals("The Beginning", reflowable.chapters.first().title)
        assertEquals("Hello from inside a div.", reflowable.chapters.first().blocks.first().text)
    }

    @Test
    fun readerPreferencesDefaultsAndModularTogglesAreConsistent() {
        val prefs = com.example.data.model.ReaderPreferences()
        assertEquals(com.example.data.model.AppAccent.TERRACOTTA, prefs.appAccent)
        assertEquals(com.example.data.model.LibraryDensity.COMFORTABLE, prefs.libraryDensity)
        assertTrue(prefs.showContinueReading)
        assertTrue(prefs.showProgressBars)
        assertTrue(prefs.showFormatBadges)
        assertTrue(prefs.showReaderFooter)
        assertTrue(prefs.enableHighlights)
        assertTrue(prefs.enableTts)
        assertTrue(prefs.keepScreenAwake)
        assertEquals(116, com.example.data.model.LibraryDensity.COMPACT.minCellDp)
        assertEquals(184, com.example.data.model.LibraryDensity.LARGE.minCellDp)
    }

    @Test
    fun streakCalculationFindsCurrentAndBestStreaks() {
        val today = 20_777L
        val dailyTotals = mapOf(
            today - 10L to 600L,
            today - 9L to 900L,
            today - 8L to 1200L,
            today - 7L to 300L,
            today - 2L to 400L,
            today - 1L to 800L
        )
        val (current, best) = com.example.ui.screens.calculateStreaks(dailyTotals, today)
        assertEquals(2, current)
        assertEquals(4, best)
    }

    @Test
    fun backupCoverNameSanitizerBlocksTraversalAndHiddenNames() {
        assertEquals("passwd", BackupManager.sanitizeBackupCoverName("../../etc/passwd"))
        assertEquals("evil.png", BackupManager.sanitizeBackupCoverName("covers\\..\\..\\evil.png"))
        assertNull(BackupManager.sanitizeBackupCoverName("."))
        assertNull(BackupManager.sanitizeBackupCoverName(".."))
        assertNull(BackupManager.sanitizeBackupCoverName(".hidden_cover.png"))
        assertNull(BackupManager.sanitizeBackupCoverName("   "))
    }

    @Test
    fun backupPathConfinementRejectsDirectoryEscape() {
        val baseDir = kotlin.io.path.createTempDirectory("folio_sec_test").toFile()
        baseDir.deleteOnExit()
        val childDir = File(baseDir, "covers").apply { mkdirs() }
        val insideFile = File(childDir, "book_1.jpg")
        val siblingEscape = File(baseDir, "books/secret.epub")
        val parentEscape = File(childDir, "../outside.txt")

        assertTrue(BackupManager.isPathInsideDir(insideFile, childDir))
        assertFalse(BackupManager.isPathInsideDir(siblingEscape, childDir))
        assertFalse(BackupManager.isPathInsideDir(parentEscape, childDir))
        assertFalse(BackupManager.isPathInsideDir(childDir, childDir))
    }

    @Test
    fun epubNormalizePathPreventsRootEscape() {
        assertEquals("secret", EpubParser.normalizePath("../../secret"))
        assertEquals("OEBPS/Text/Chapter 1.xhtml", EpubParser.normalizePath("OEBPS/Sub/../Text/Chapter%201.xhtml#frag"))
    }

    @Test
    fun elapsedSecondsCalculationClampsAndIgnoresInvalidDeltas() {
        assertEquals(0L, com.example.ui.viewmodel.computeElapsedSecondsToFlush(5_000L, 4_000L))
        assertEquals(0L, com.example.ui.viewmodel.computeElapsedSecondsToFlush(5_000L, 5_999L))
        assertEquals(5L, com.example.ui.viewmodel.computeElapsedSecondsToFlush(1_000L, 6_400L))
        assertEquals(15L, com.example.ui.viewmodel.computeElapsedSecondsToFlush(0L, 600_000L))
    }

    @Test
    fun bookCacheBinaryRoundTripPreservesChaptersAndInlineRuns() {
        val cacheDir = kotlin.io.path.createTempDirectory("folio_cache_test").toFile()
        cacheDir.deleteOnExit()
        val cacheFile = File(cacheDir, "sample_v1.bin")
        val original = ParsedBookContent.Reflowable(
            metadata = com.example.engine.ParsedMetadata(
                title = "Dune",
                author = "Frank Herbert",
                series = "Dune Chronicles",
                seriesIndex = 1f,
                description = "A desert planet epic.",
                totalUnits = 2
            ),
            chapters = listOf(
                com.example.engine.BookChapter(
                    index = 0,
                    title = "Book One",
                    blocks = listOf(
                        com.example.engine.ParagraphBlock("Book One", BlockStyle.HEADING_1),
                        com.example.engine.ParagraphBlock(
                            text = "Fear is the mind-killer.",
                            style = BlockStyle.BODY,
                            runs = listOf(com.example.engine.TextRun(0, 4, bold = true, italic = false, code = false))
                        )
                    )
                )
            )
        )
        com.example.engine.BookCache.writeToDisk(cacheFile, original)
        assertTrue(cacheFile.exists())
        val restored = com.example.engine.BookCache.readFromDisk(cacheFile)
        assertEquals(original.metadata.title, restored?.metadata?.title)
        assertEquals(original.metadata.author, restored?.metadata?.author)
        assertEquals(original.chapters.size, restored?.chapters?.size)
        assertEquals(original.chapters[0].blocks[1].text, restored?.chapters?.get(0)?.blocks?.get(1)?.text)
        assertEquals(original.chapters[0].blocks[1].runs, restored?.chapters?.get(0)?.blocks?.get(1)?.runs)
    }

    @Test
    fun decodeHtmlEntitiesHandlesPlainNamedAndNumericEntities() {
        assertEquals("Plain ASCII text", TextHtmlMdParser.decodeHtmlEntities("Plain ASCII text"))
        assertEquals("Tom & Jerry — \"Hi\" © 2026 A!", TextHtmlMdParser.decodeHtmlEntities("Tom &amp; Jerry &mdash; &quot;Hi&quot; &#169; 2026 &#x41;!"))
    }

    @Test
    fun epubParseMetadataOnlyExtractsTitleAuthorAndSpineCount() {
        val tmp = File.createTempFile("meta_only", ".epub")
        tmp.deleteOnExit()
        ZipOutputStream(tmp.outputStream()).use { zos ->
            fun add(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            add("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            add(
                "content.opf",
                """<?xml version="1.0"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Speed Reader</dc:title>
                    <dc:creator>Grace Hopper</dc:creator>
                  </metadata>
                  <manifest>
                    <item id="c1" href="c1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="c2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                    <itemref idref="c2"/>
                  </spine>
                </package>""".trimIndent()
            )
            add("c1.xhtml", "<html><body><p>Ch 1</p></body></html>")
            add("c2.xhtml", "<html><body><p>Ch 2</p></body></html>")
        }
        val meta = EpubParser.parseMetadataOnly(tmp, "Fallback")
        assertEquals("Speed Reader", meta.title)
        assertEquals("Grace Hopper", meta.author)
        assertEquals(2, meta.totalUnits)
    }

    @Test
    fun widgetSummaryComputesStreaksTodayAndTotalTime() {
        val today = 20_800L
        val sessions = listOf(
            com.example.data.model.ReadingSessionEntity(id = 1, bookId = 1, epochDay = today - 2, durationSeconds = 400L),
            com.example.data.model.ReadingSessionEntity(id = 2, bookId = 1, epochDay = today - 1, durationSeconds = 600L),
            com.example.data.model.ReadingSessionEntity(id = 3, bookId = 2, epochDay = today, durationSeconds = 900L)
        )
        val books = listOf(
            com.example.data.model.BookEntity(id = 1, title = "Book A", format = BookFormat.EPUB, fileUri = "file:///a", fileHash = "h1"),
            com.example.data.model.BookEntity(id = 2, title = "Book B", format = BookFormat.PDF, fileUri = "file:///b", fileHash = "h2")
        )
        val summary = com.example.widget.FolioWidgetUpdater.computeWidgetSummary(sessions, books, today)
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.bestStreak)
        assertEquals(900L, summary.todaySeconds)
        assertEquals(1900L, summary.totalSeconds)
        assertEquals(2, summary.bookCount)
    }

    @Test
    fun bionicWordRangesEmphasizesOpeningLettersAcrossScripts() {
        val sample = "Darrow kämpft, reading a book in 123!"
        val ranges = com.example.ui.screens.bionicWordRanges(sample)
        val emphasized = ranges.map { sample.substring(it) }
        assertEquals(listOf("Da", "kä", "rea", "a", "bo", "i", "1"), emphasized)
    }

    @Test
    fun readerPreferencesIncludesDefaultColorPresetsAndAlignmentCompat() {
        val prefs = com.example.data.model.ReaderPreferences()
        assertEquals(5, prefs.colorPresets.size)
        assertEquals("paper", prefs.activeColorPresetId)
        assertFalse(prefs.textJustified)
        val justified = prefs.copy(textAlignment = com.example.data.model.ReaderTextAlignment.JUSTIFY)
        assertTrue(justified.textJustified)
        assertEquals(com.example.data.model.ReaderReadingMode.SCROLL, prefs.readingMode)
        assertTrue(prefs.groupSeriesInLibrary)
    }

    @Test
    fun mimeTypeDetectionCoversBookFormatsAndRejectsUnknown() {
        assertEquals(BookFormat.EPUB, BookFormat.fromMimeType("application/epub+zip"))
        assertEquals(BookFormat.PDF, BookFormat.fromMimeType("application/pdf; charset=utf-8"))
        assertEquals(BookFormat.CBZ, BookFormat.fromMimeType("application/vnd.comicbook+zip"))
        assertEquals(BookFormat.CBR, BookFormat.fromMimeType("application/x-cbr"))
        assertEquals(BookFormat.AZW3, BookFormat.fromMimeType("application/vnd.amazon.mobi8-ebook"))
        assertEquals(BookFormat.FB2, BookFormat.fromMimeType("application/x-fictionbook+xml"))
        assertEquals(BookFormat.DOCX, BookFormat.fromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(BookFormat.TXT, BookFormat.fromMimeType("text/plain; charset=UTF-8"))
        assertNull(BookFormat.fromMimeType("image/png"))
        assertNull(BookFormat.fromMimeType(null))
    }

    @Test
    fun librarySeriesStacksGroupMultiBookSeriesAndSortByVolumeIndex() {
        fun wrap(b: com.example.data.model.BookEntity) = com.example.data.model.BookWithRelations(
            book = b,
            shelves = emptyList(),
            tags = emptyList(),
            highlights = emptyList()
        )
        val b1 = wrap(
            com.example.data.model.BookEntity(
                id = 1, title = "Golden Son", series = "Red Rising", seriesIndex = 2f,
                format = BookFormat.EPUB, fileUri = "file:///2", fileHash = "h2"
            )
        )
        val b2 = wrap(
            com.example.data.model.BookEntity(
                id = 2, title = "Red Rising", series = "Red Rising", seriesIndex = 1f,
                format = BookFormat.EPUB, fileUri = "file:///1", fileHash = "h1"
            )
        )
        val bSoloSeries = wrap(
            com.example.data.model.BookEntity(
                id = 3, title = "Dune", series = "Dune", seriesIndex = 1f,
                format = BookFormat.EPUB, fileUri = "file:///3", fileHash = "h3"
            )
        )
        val bStandalone = wrap(
            com.example.data.model.BookEntity(
                id = 4, title = "Piranesi", series = "", seriesIndex = 0f,
                format = BookFormat.EPUB, fileUri = "file:///4", fileHash = "h4"
            )
        )

        val stacked = com.example.ui.screens.buildLibraryDisplayEntries(listOf(b1, b2, bSoloSeries, bStandalone), groupSeries = true)
        assertEquals(3, stacked.size)
        val first = stacked[0] as com.example.ui.screens.LibraryDisplayEntry.SeriesStack
        assertEquals("Red Rising", first.seriesName)
        assertEquals(listOf("Red Rising", "Golden Son"), first.books.map { it.book.title })
        assertTrue(stacked[1] is com.example.ui.screens.LibraryDisplayEntry.SingleBook)
        assertTrue(stacked[2] is com.example.ui.screens.LibraryDisplayEntry.SingleBook)

        val flattened = com.example.ui.screens.buildLibraryDisplayEntries(listOf(b1, b2, bSoloSeries, bStandalone), groupSeries = false)
        assertEquals(4, flattened.size)
        assertTrue(flattened.all { it is com.example.ui.screens.LibraryDisplayEntry.SingleBook })
    }

    @Test
    fun findPageForParagraphLocatesExactSliceOrNearestPage() {
        val dummyBlock = com.example.engine.ParagraphBlock("Text")
        val pages = listOf(
            com.example.ui.screens.ChapterPage(
                pageIndex = 0,
                slices = listOf(com.example.ui.screens.PageSlice.TextSlice(0, dummyBlock, 0, 4, false)),
                firstParagraphIndex = 0,
                lastParagraphIndex = 2
            ),
            com.example.ui.screens.ChapterPage(
                pageIndex = 1,
                slices = listOf(com.example.ui.screens.PageSlice.TextSlice(5, dummyBlock, 0, 4, false)),
                firstParagraphIndex = 5,
                lastParagraphIndex = 8
            )
        )
        assertEquals(0, com.example.ui.screens.findPageForParagraph(pages, 0))
        assertEquals(1, com.example.ui.screens.findPageForParagraph(pages, 5))
        assertEquals(1, com.example.ui.screens.findPageForParagraph(pages, 6))
        assertEquals(1, com.example.ui.screens.findPageForParagraph(pages, 99))
    }
}
