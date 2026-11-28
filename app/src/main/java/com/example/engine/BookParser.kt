package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.BookFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

object BookParser {

    fun parseMetadataAndCover(
        context: Context,
        file: File,
        format: BookFormat,
        fallbackTitle: String,
        diskCacheFile: File? = null
    ): ParsedMetadata {
        return when (format) {
            BookFormat.EPUB -> EpubParser.parseMetadataOnly(file, fallbackTitle)
            BookFormat.PDF -> extractPdfMetadata(file, fallbackTitle)
            BookFormat.CBZ -> extractCbzMetadata(file, fallbackTitle)
            BookFormat.CBR -> extractCbrMetadata(file, fallbackTitle)
            BookFormat.MOBI,
            BookFormat.AZW3,
            BookFormat.DOCX,
            BookFormat.FB2,
            BookFormat.RTF,
            BookFormat.TXT,
            BookFormat.HTML,
            BookFormat.MARKDOWN -> {
                val res = when (format) {
                    BookFormat.MOBI, BookFormat.AZW3 -> MobiParser.parse(file, fallbackTitle)
                    BookFormat.DOCX -> DocxRtfFb2Parser.parseDocx(file, fallbackTitle)
                    BookFormat.FB2 -> DocxRtfFb2Parser.parseFb2(file, fallbackTitle)
                    BookFormat.RTF -> DocxRtfFb2Parser.parseRtf(file.readBytes(), fallbackTitle)
                    BookFormat.TXT -> TextHtmlMdParser.parsePlainText(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
                    BookFormat.HTML -> TextHtmlMdParser.parseHtml(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
                    BookFormat.MARKDOWN -> TextHtmlMdParser.parseMarkdown(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
                    else -> ParsedBookContent.Error("")
                }
                if (res is ParsedBookContent.Reflowable) {
                    if (diskCacheFile != null && res.chapters.isNotEmpty()) {
                        BookCache.writeToDisk(diskCacheFile, res)
                    }
                    res.metadata
                } else {
                    ParsedMetadata(title = fallbackTitle)
                }
            }
        }
    }

    fun parseReflowableContent(
        file: File,
        format: BookFormat,
        fallbackTitle: String,
        imageCacheDir: File? = null,
        cacheKey: String? = null,
        diskCacheFile: File? = null
    ): ParsedBookContent {
        if (!cacheKey.isNullOrBlank()) {
            BookCache.getFromMemory(cacheKey)?.let { return it }
        }
        if (diskCacheFile != null) {
            BookCache.readFromDisk(diskCacheFile)?.let { cached ->
                if (cached.chapters.isNotEmpty()) {
                    if (!cacheKey.isNullOrBlank()) BookCache.putInMemory(cacheKey, cached)
                    return cached
                }
            }
        }

        val parsed = when (format) {
            BookFormat.EPUB -> EpubParser.parse(file, fallbackTitle, imageCacheDir, skipCover = true)
            BookFormat.MOBI, BookFormat.AZW3 -> MobiParser.parse(file, fallbackTitle)
            BookFormat.DOCX -> DocxRtfFb2Parser.parseDocx(file, fallbackTitle)
            BookFormat.FB2 -> DocxRtfFb2Parser.parseFb2(file, fallbackTitle)
            BookFormat.RTF -> DocxRtfFb2Parser.parseRtf(file.readBytes(), fallbackTitle)
            BookFormat.TXT -> TextHtmlMdParser.parsePlainText(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
            BookFormat.HTML -> TextHtmlMdParser.parseHtml(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
            BookFormat.MARKDOWN -> TextHtmlMdParser.parseMarkdown(TextHtmlMdParser.decodeBytes(file.readBytes()), fallbackTitle)
            BookFormat.PDF, BookFormat.CBZ, BookFormat.CBR ->
                ParsedBookContent.Error("Format ${format.displayName} uses dedicated page renderer.")
        }

        if (parsed is ParsedBookContent.Reflowable && parsed.chapters.isNotEmpty()) {
            if (!cacheKey.isNullOrBlank()) BookCache.putInMemory(cacheKey, parsed)
            if (diskCacheFile != null) BookCache.writeToDisk(diskCacheFile, parsed)
        }
        return parsed
    }

    fun compressCoverThumbnail(raw: ByteArray, maxWidth: Int = 640, maxHeight: Int = 960): ByteArray? {
        if (raw.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return raw
        if (bounds.outWidth <= maxWidth && bounds.outHeight <= maxHeight && raw.size <= 160 * 1024) {
            return raw
        }
        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts) ?: return raw
        val scale = minOf(maxWidth.toFloat() / decoded.width, maxHeight.toFloat() / decoded.height, 1f)
        val scaled = if (scale < 0.98f) {
            val w = (decoded.width * scale).toInt().coerceAtLeast(1)
            val h = (decoded.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, w, h, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else decoded
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    private fun extractPdfMetadata(file: File, fallbackTitle: String): ParsedMetadata {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount.coerceAtLeast(1)
                    var coverBytes: ByteArray? = null
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val targetW = 420
                            val targetH = (420f * page.height / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            coverBytes = ByteArrayOutputStream().use { out ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
                                out.toByteArray()
                            }
                            bmp.recycle()
                        }
                    }
                    ParsedMetadata(title = fallbackTitle, totalUnits = count, coverBytes = coverBytes)
                }
            }
        }.getOrElse {
            ParsedMetadata(title = fallbackTitle, totalUnits = 1)
        }
    }

    private fun extractCbzMetadata(file: File, fallbackTitle: String): ParsedMetadata {
        return runCatching {
            ZipFile(file).use { zip ->
                val entries = ComicArchiveHelper.listCbzImageNames(zip)
                val coverBytes = entries.firstOrNull()?.let { name ->
                    zip.getEntry(name)?.let { entry ->
                        zip.getInputStream(entry).use { compressCoverThumbnail(it.readBytes()) }
                    }
                }
                ParsedMetadata(
                    title = fallbackTitle,
                    totalUnits = entries.size.coerceAtLeast(1),
                    coverBytes = coverBytes
                )
            }
        }.getOrElse {
            ParsedMetadata(title = fallbackTitle, totalUnits = 1)
        }
    }

    private fun extractCbrMetadata(file: File, fallbackTitle: String): ParsedMetadata {
        val headers = ComicArchiveHelper.listRarStoredHeaders(file)
        val coverBmp = if (headers.isNotEmpty()) ComicArchiveHelper.loadCbrPageBitmap(file, 0) else null
        val coverBytes = coverBmp?.let { bmp ->
            ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                bmp.recycle()
                out.toByteArray()
            }
        }
        return ParsedMetadata(
            title = fallbackTitle,
            totalUnits = headers.size.coerceAtLeast(1),
            coverBytes = coverBytes
        )
    }
}
