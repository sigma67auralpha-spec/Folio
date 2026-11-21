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
}
