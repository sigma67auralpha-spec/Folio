package com.example.engine

import java.io.Closeable
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object EpubParser {

    private const val MAX_ENTRY_BYTES = 24 * 1024 * 1024
    private const val MAX_TOTAL_ARCHIVE_BYTES = 160L * 1024 * 1024
    private const val MAX_ARCHIVE_ENTRIES = 10_000

    private val CIPHER_REF_REGEX = Regex("""URI\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val FULL_PATH_REGEX = Regex("""full-path\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val XML_COMMENT_REGEX = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val HTML_TAG_REGEX = Regex("<[^>]+>")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val SAFE_IMG_NAME_REGEX = Regex("[^a-zA-Z0-9._-]")
    private val META_REGEX = Regex(
        """<(?:[a-zA-Z0-9_-]+:)?meta\b([^>]*)(?:>(.*?)</(?:[a-zA-Z0-9_-]+:)?meta>|/?>)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val ITEM_REGEX = Regex("""<(?:[a-zA-Z0-9_-]+:)?item\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
    private val SPINE_REGEX = Regex("""<(?:[a-zA-Z0-9_-]+:)?spine\b([^>]*)>""", RegexOption.IGNORE_CASE)
    private val ITEMREF_REGEX = Regex("""<(?:[a-zA-Z0-9_-]+:)?itemref\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
    private val NAV_POINT_REGEX = Regex(
        """<navLabel\b[^>]*>.*?<text\b[^>]*>(.*?)</text>.*?</navLabel>.*?<content\b([^>]*)/?>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val NAV_LINK_REGEX = Regex(
        """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val TITLE_TAG_REGEX = Regex(
        """<(?:[a-zA-Z0-9_-]+:)?title\b[^>]*>(.*?)</(?:[a-zA-Z0-9_-]+:)?title>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val CREATOR_TAG_REGEX = Regex(
        """<(?:[a-zA-Z0-9_-]+:)?creator\b[^>]*>(.*?)</(?:[a-zA-Z0-9_-]+:)?creator>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val DESC_TAG_REGEX = Regex(
        """<(?:[a-zA-Z0-9_-]+:)?description\b[^>]*>(.*?)</(?:[a-zA-Z0-9_-]+:)?description>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    fun parseMetadataOnly(file: File, fallbackTitle: String): ParsedMetadata {
        return runCatching {
            openArchive(file).use { archive ->
                if (archive.isEmpty() || hasContentDrm(archive)) {
                    return ParsedMetadata(title = fallbackTitle)
                }
                val containerBytes = archive.getEntryBytes("META-INF/container.xml")
                    ?: return ParsedMetadata(title = fallbackTitle)
                val opfPath = parseContainerOpfPath(containerBytes)
                    ?: archive.findFirstPathEndingWith(".opf")
                    ?: return ParsedMetadata(title = fallbackTitle)
                val opfBytes = archive.getEntryBytes(opfPath)
                    ?: return ParsedMetadata(title = fallbackTitle)
                val opfDir = normalizePath(opfPath).substringBeforeLast('/', "")
                val opf = parseOpf(opfBytes)

                val coverHref = opf.coverItemId?.let { opf.manifest[it]?.href }
                    ?: opf.manifest.values.firstOrNull {
                        it.mediaType.startsWith("image/", ignoreCase = true) &&
                            (it.id.contains("cover", ignoreCase = true) || it.href.contains("cover", ignoreCase = true))
                    }?.href
                val coverBytes = coverHref?.let { href ->
                    archive.getEntryBytes(resolvePath(opfDir, href))
                }
                val estimatedUnits = if (opf.spine.isNotEmpty()) {
                    opf.spine.size
                } else {
                    opf.manifest.values.count {
                        it.mediaType.contains("html", ignoreCase = true) ||
                            it.href.endsWith(".xhtml", ignoreCase = true) ||
                            it.href.endsWith(".html", ignoreCase = true) ||
                            it.href.endsWith(".htm", ignoreCase = true)
                    }.coerceAtLeast(1)
                }
                ParsedMetadata(
                    title = opf.title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                    author = opf.author?.takeIf { it.isNotBlank() } ?: "Unknown Author",
                    series = opf.series.orEmpty(),
                    seriesIndex = opf.seriesIndex,
                    description = opf.description.orEmpty(),
                    coverBytes = coverBytes,
                    totalUnits = estimatedUnits
                )
            }
        }.getOrElse {
            ParsedMetadata(title = fallbackTitle)
        }
    }

    fun parse(
        file: File,
        fallbackTitle: String,
        imageCacheDir: File? = null
    ): ParsedBookContent {
        return runCatching {
            openArchive(file).use { archive ->
                if (archive.isEmpty()) {
                    return ParsedBookContent.Error("Invalid EPUB: empty or unreadable ZIP archive.")
                }

                if (hasContentDrm(archive)) {
                    return ParsedBookContent.Error("This EPUB is DRM-protected and cannot be opened in a local DRM-free reader.")
                }

                val containerBytes = archive.getEntryBytes("META-INF/container.xml")
                    ?: return ParsedBookContent.Error("Invalid EPUB: missing META-INF/container.xml")

                val opfPath = parseContainerOpfPath(containerBytes)
                    ?: archive.findFirstPathEndingWith(".opf")
                    ?: return ParsedBookContent.Error("Invalid EPUB: missing OPF rootfile")

                val opfBytes = archive.getEntryBytes(opfPath)
                    ?: return ParsedBookContent.Error("Invalid EPUB: cannot read $opfPath")
                val opfDir = normalizePath(opfPath).substringBeforeLast('/', "")

                val opf = parseOpf(opfBytes)

                val coverHref = opf.coverItemId?.let { opf.manifest[it]?.href }
                    ?: opf.manifest.values.firstOrNull {
                        it.mediaType.startsWith("image/", ignoreCase = true) &&
                            (it.id.contains("cover", ignoreCase = true) || it.href.contains("cover", ignoreCase = true))
                    }?.href
                val coverBytes = coverHref?.let { href ->
                    archive.getEntryBytes(resolvePath(opfDir, href))
                }

                val tocLabels = mutableMapOf<String, String>()
                opf.ncxItemId?.let { id ->
                    opf.manifest[id]?.href?.let { href ->
                        archive.getEntryBytes(resolvePath(opfDir, href))?.let { bytes ->
                            val ncxDir = resolvePath(opfDir, href).substringBeforeLast('/', "")
                            tocLabels.putAll(parseNcx(bytes, ncxDir))
                        }
                    }
                }
                opf.navItemId?.let { id ->
                    opf.manifest[id]?.href?.let { href ->
                        archive.getEntryBytes(resolvePath(opfDir, href))?.let { bytes ->
                            val navDir = resolvePath(opfDir, href).substringBeforeLast('/', "")
                            tocLabels.putAll(parseNavXhtml(bytes, navDir))
                        }
                    }
                }

                val spineItemIds = if (opf.spine.isNotEmpty()) {
                    opf.spine
                } else {
                    opf.manifest.values
                        .filter {
                            it.mediaType.contains("html", ignoreCase = true) ||
                                it.href.endsWith(".xhtml", ignoreCase = true) ||
                                it.href.endsWith(".html", ignoreCase = true) ||
                                it.href.endsWith(".htm", ignoreCase = true)
                        }
                        .map { it.id }
                }

                val bookImageDir = imageCacheDir?.apply { mkdirs() }
                val bookImgCanonical = bookImageDir?.canonicalFile?.path
                val imageWriteLock = Any()

                fun parseSpineEntry(spineIdx: Int, itemId: String): Pair<Int, BookChapter>? {
                    val item = opf.manifest[itemId] ?: return null
                    val fullPath = resolvePath(opfDir, item.href)
                    val htmlBytes = archive.getEntryBytes(fullPath) ?: return null
                    val chapterDir = fullPath.substringBeforeLast('/', "")
                    val html = TextHtmlMdParser.decodeBytes(htmlBytes)

                    val imageResolver: ((String) -> String?)? = if (bookImageDir != null && bookImgCanonical != null) {
                        { rawSrc ->
                            val resolvedImgPath = resolvePath(chapterDir, rawSrc)
                            val baseImgName = resolvedImgPath.substringAfterLast('/').replace(SAFE_IMG_NAME_REGEX, "_")
                            val safeImgName = "${resolvedImgPath.hashCode().toUInt()}_$baseImgName"
                            val outFile = File(bookImageDir, safeImgName)
                            if (outFile.canonicalFile.path.startsWith(bookImgCanonical + File.separator)) {
                                if (outFile.exists()) {
                                    outFile.absolutePath
                                } else {
                                    val imgBytes = archive.getEntryBytes(resolvedImgPath)
                                    if (imgBytes != null && imgBytes.isNotEmpty()) {
                                        synchronized(imageWriteLock) {
                                            if (!outFile.exists()) outFile.writeBytes(imgBytes)
                                        }
                                        outFile.absolutePath
                                    } else null
                                }
                            } else null
                        }
                    } else null

                    val blocks = TextHtmlMdParser.extractHtmlBlocks(html, imageResolver)
                    if (blocks.isEmpty()) return null

                    val normalizedFull = normalizePath(fullPath)
                    val tocTitle = tocLabels[normalizedFull]
                        ?: tocLabels.entries.firstOrNull { it.key.equals(normalizedFull, ignoreCase = true) }?.value
                        ?: tocLabels.entries.firstOrNull { it.key.endsWith("/${normalizedFull.substringAfterLast('/')}", ignoreCase = true) }?.value
                    val headingTitle = blocks.firstOrNull {
                        it.style == BlockStyle.HEADING_1 || it.style == BlockStyle.HEADING_2 || it.style == BlockStyle.HEADING_3
                    }?.text
                    val chapterTitle = (tocTitle ?: headingTitle ?: "Chapter ${spineIdx + 1}").take(70)
                    return spineIdx to BookChapter(spineIdx, chapterTitle, blocks)
                }

                val parsedOrdered: List<BookChapter> = if (spineItemIds.size >= 4) {
                    val workers = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                    val pool = Executors.newFixedThreadPool(workers)
                    try {
                        val futures = spineItemIds.mapIndexed { idx, itemId ->
                            pool.submit(Callable { parseSpineEntry(idx, itemId) })
                        }
                        val collected = ArrayList<BookChapter>(futures.size)
                        for (future in futures) {
                            future.get()?.let { (_, ch) ->
                                collected.add(ch.copy(index = collected.size))
                            }
                        }
                        collected
                    } finally {
                        pool.shutdownNow()
                    }
                } else {
                    val collected = ArrayList<BookChapter>(spineItemIds.size)
                    spineItemIds.forEachIndexed { idx, itemId ->
                        parseSpineEntry(idx, itemId)?.let { (_, ch) ->
                            collected.add(ch.copy(index = collected.size))
                        }
                    }
                    collected
                }

                val finalChapters = parsedOrdered.ifEmpty {
                    listOf(
                        BookChapter(
                            0,
                            opf.title ?: fallbackTitle,
                            listOf(ParagraphBlock("(No readable text or images found in this EPUB.)", BlockStyle.BODY))
                        )
                    )
                }

                ParsedBookContent.Reflowable(
                    metadata = ParsedMetadata(
                        title = opf.title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                        author = opf.author?.takeIf { it.isNotBlank() } ?: "Unknown Author",
                        series = opf.series.orEmpty(),
                        seriesIndex = opf.seriesIndex,
                        description = opf.description.orEmpty(),
                        coverBytes = coverBytes,
                        totalUnits = finalChapters.size
                    ),
                    chapters = finalChapters
                )
            }
        }.getOrElse { err ->
            ParsedBookContent.Error("Failed to read EPUB: ${err.localizedMessage ?: "corrupt archive"}")
        }
    }

    private interface EpubArchive : Closeable {
        fun isEmpty(): Boolean
        fun getEntryBytes(rawPath: String): ByteArray?
        fun findFirstPathEndingWith(suffix: String): String?
    }

    private class LazyZipArchive(
        private val zipFile: ZipFile,
        private val entriesByLowerPath: Map<String, ZipEntry>
    ) : EpubArchive {
        private val readLock = Any()
        private var cumulativeBytesRead = 0L

        override fun isEmpty(): Boolean = entriesByLowerPath.isEmpty()

        override fun getEntryBytes(rawPath: String): ByteArray? {
            val norm = normalizePath(rawPath).lowercase()
            val entry = entriesByLowerPath[norm]
                ?: entriesByLowerPath[safeUrlDecode(norm)]
                ?: return null
            synchronized(readLock) {
                if (entry.size > MAX_ENTRY_BYTES) error("EPUB entry exceeds safe size limit.")
                val bytes = zipFile.getInputStream(entry).use { readBoundedStream(it, MAX_ENTRY_BYTES) }
                cumulativeBytesRead += bytes.size
                if (cumulativeBytesRead > MAX_TOTAL_ARCHIVE_BYTES) error("EPUB exceeds total uncompressed limit.")
                return bytes
            }
        }

        override fun findFirstPathEndingWith(suffix: String): String? =
            entriesByLowerPath.keys.firstOrNull { it.endsWith(suffix.lowercase()) }

        override fun close() {
            runCatching { zipFile.close() }
        }
    }

    private class InMemoryArchive(
        private val entriesByLowerPath: Map<String, ByteArray>
    ) : EpubArchive {
        override fun isEmpty(): Boolean = entriesByLowerPath.isEmpty()

        override fun getEntryBytes(rawPath: String): ByteArray? {
            val norm = normalizePath(rawPath).lowercase()
            return entriesByLowerPath[norm] ?: entriesByLowerPath[safeUrlDecode(norm)]
        }

        override fun findFirstPathEndingWith(suffix: String): String? =
            entriesByLowerPath.keys.firstOrNull { it.endsWith(suffix.lowercase()) }

        override fun close() = Unit
    }

    private fun readBoundedStream(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) error("EPUB entry exceeds safe size limit.")
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    private fun openArchive(file: File): EpubArchive {
        val zipAttempt = runCatching {
            val zip = ZipFile(file)
            val entries = linkedMapOf<String, ZipEntry>()
            var count = 0
            val enumEntries = zip.entries()
            while (enumEntries.hasMoreElements()) {
                val entry = enumEntries.nextElement()
                count++
                if (count > MAX_ARCHIVE_ENTRIES) {
                    zip.close()
                    error("EPUB contains too many archive entries.")
                }
                if (!entry.isDirectory) {
                    entries[normalizePath(entry.name).lowercase()] = entry
                }
            }
            if (entries.isEmpty()) {
                zip.close()
                null
            } else {
                LazyZipArchive(zip, entries)
            }
        }.getOrNull()

        if (zipAttempt != null) return zipAttempt

        val map = linkedMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(file.inputStream().buffered()).use { zis ->
                var totalBytes = 0L
                var count = 0
                var entry = zis.nextEntry
                while (entry != null) {
                    count++
                    if (count > MAX_ARCHIVE_ENTRIES) error("EPUB contains too many archive entries.")
                    if (!entry.isDirectory) {
                        val norm = normalizePath(entry.name).lowercase()
                        val bytes = readBoundedStream(zis, MAX_ENTRY_BYTES)
                        totalBytes += bytes.size
                        if (totalBytes > MAX_TOTAL_ARCHIVE_BYTES) error("EPUB exceeds total uncompressed limit.")
                        map[norm] = bytes
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return InMemoryArchive(map)
    }

    private fun hasContentDrm(archive: EpubArchive): Boolean {
        if (archive.getEntryBytes("META-INF/rights.xml") != null) return true
        val encBytes = archive.getEntryBytes("META-INF/encryption.xml") ?: return false
        val xml = TextHtmlMdParser.decodeBytes(encBytes)
        val cipherRefs = CIPHER_REF_REGEX
            .findAll(xml)
            .map { it.groupValues[1].lowercase() }
            .toList()
        if (cipherRefs.isEmpty()) return false
        return cipherRefs.any { uri ->
            !uri.endsWith(".ttf") && !uri.endsWith(".otf") && !uri.endsWith(".woff") && !uri.endsWith(".woff2")
        }
    }

    internal fun normalizePath(path: String): String {
        val noFragment = path.substringBefore('#').substringBefore('?')
            .replace('\\', '/')
            .trim()
        val decoded = safeUrlDecode(noFragment).trimStart('/')
        val parts = ArrayDeque<String>()
        for (seg in decoded.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(seg)
            }
        }
        return parts.joinToString("/")
    }

    internal fun resolvePath(baseDir: String, relative: String): String {
        val cleanRel = relative.substringBefore('#').substringBefore('?').replace('\\', '/').trim()
        if (cleanRel.startsWith("/")) return normalizePath(cleanRel)
        val combined = if (baseDir.isEmpty()) cleanRel else "$baseDir/$cleanRel"
        return normalizePath(combined)
    }

    private fun safeUrlDecode(input: String): String {
        return if ('%' in input) {
            runCatching { URLDecoder.decode(input.replace("+", "%2B"), "UTF-8") }.getOrDefault(input)
        } else input
    }

    private fun parseContainerOpfPath(bytes: ByteArray): String? {
        val xml = TextHtmlMdParser.decodeBytes(bytes)
        return FULL_PATH_REGEX.find(xml)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private data class ManifestItem(val id: String, val href: String, val mediaType: String)
    private data class OpfData(
        var title: String? = null,
        var author: String? = null,
        var description: String? = null,
        var series: String? = null,
        var seriesIndex: Float = 0f,
        var coverItemId: String? = null,
        var ncxItemId: String? = null,
        var navItemId: String? = null,
        val manifest: MutableMap<String, ManifestItem> = mutableMapOf(),
        val spine: MutableList<String> = mutableListOf()
    )

    private fun parseOpf(bytes: ByteArray): OpfData {
        val data = OpfData()
        val rawXml = TextHtmlMdParser.decodeBytes(bytes)
        val xml = if ("<!--" in rawXml) rawXml.replace(XML_COMMENT_REGEX, "") else rawXml

        fun cleanInner(regex: Regex): String? {
            val raw = regex.find(xml)?.groupValues?.getOrNull(1) ?: return null
            val stripped = if ('<' in raw) raw.replace(HTML_TAG_REGEX, "") else raw
            return TextHtmlMdParser.decodeHtmlEntities(stripped).trim().takeIf { it.isNotEmpty() }
        }

        data.title = cleanInner(TITLE_TAG_REGEX)
        data.author = cleanInner(CREATOR_TAG_REGEX)
        data.description = cleanInner(DESC_TAG_REGEX)

        for (m in META_REGEX.findAll(xml)) {
            val attrs = m.groupValues[1]
            val innerRaw = m.groupValues[2]
            val inner = TextHtmlMdParser.decodeHtmlEntities(
                if ('<' in innerRaw) innerRaw.replace(HTML_TAG_REGEX, "") else innerRaw
            ).trim()
            val metaName = extractXmlAttr(attrs, "name") ?: extractXmlAttr(attrs, "property") ?: continue
            val content = extractXmlAttr(attrs, "content") ?: inner
            when (metaName.lowercase()) {
                "cover" -> if (content.isNotBlank()) data.coverItemId = content
                "calibre:series" -> if (content.isNotBlank()) data.series = content
                "calibre:series_index" -> data.seriesIndex = content.toFloatOrNull() ?: 0f
                "belongs-to-collection" -> if (data.series.isNullOrBlank() && content.isNotBlank()) data.series = content
                "dcterms:title" -> if (data.title.isNullOrBlank() && content.isNotBlank()) data.title = content
            }
        }

        for (m in ITEM_REGEX.findAll(xml)) {
            val attrs = m.groupValues[1]
            val id = extractXmlAttr(attrs, "id").orEmpty()
            val href = extractXmlAttr(attrs, "href").orEmpty()
            val mediaType = extractXmlAttr(attrs, "media-type").orEmpty()
            val props = extractXmlAttr(attrs, "properties").orEmpty()
            if (id.isNotBlank() && href.isNotBlank()) {
                data.manifest[id] = ManifestItem(id, href, mediaType)
                if (props.isNotEmpty()) {
                    val tokens = props.split(WHITESPACE_REGEX)
                    if ("cover-image" in tokens) data.coverItemId = id
                    if ("nav" in tokens) data.navItemId = id
                }
                if (mediaType.equals("application/x-dtbncx+xml", ignoreCase = true)) data.ncxItemId = id
            }
        }

        SPINE_REGEX.find(xml)?.groupValues?.getOrNull(1)?.let { attrs ->
            val tocId = extractXmlAttr(attrs, "toc")
            if (!tocId.isNullOrBlank() && data.ncxItemId == null) data.ncxItemId = tocId
        }

        for (m in ITEMREF_REGEX.findAll(xml)) {
            val idref = extractXmlAttr(m.groupValues[1], "idref")
            if (!idref.isNullOrBlank()) data.spine.add(idref)
        }

        return data
    }

    private fun parseNcx(bytes: ByteArray, ncxDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val xml = TextHtmlMdParser.decodeBytes(bytes)
        for (m in NAV_POINT_REGEX.findAll(xml)) {
            val labelRaw = m.groupValues[1]
            val stripped = if ('<' in labelRaw) labelRaw.replace(HTML_TAG_REGEX, "") else labelRaw
            val label = TextHtmlMdParser.decodeHtmlEntities(stripped)
                .replace(WHITESPACE_REGEX, " ").trim()
            val src = extractXmlAttr(m.groupValues[2], "src")
            if (!src.isNullOrBlank() && label.isNotBlank()) {
                map.putIfAbsent(resolvePath(ncxDir, src), label)
            }
        }
        return map
    }

    private fun parseNavXhtml(bytes: ByteArray, navDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val html = TextHtmlMdParser.decodeBytes(bytes)
        for (m in NAV_LINK_REGEX.findAll(html)) {
            val href = m.groupValues[1]
            val textRaw = m.groupValues[2]
            val stripped = if ('<' in textRaw) textRaw.replace(HTML_TAG_REGEX, "") else textRaw
            val text = TextHtmlMdParser.decodeHtmlEntities(stripped)
                .replace(WHITESPACE_REGEX, " ").trim()
            if (href.isNotBlank() && text.isNotBlank()) {
                map.putIfAbsent(resolvePath(navDir, href), text)
            }
        }
        return map
    }

    private fun extractXmlAttr(attrs: String, attrName: String): String? {
        val nLen = attrName.length
        var idx = 0
        while (idx < attrs.length) {
            val pos = attrs.indexOf(attrName, idx, ignoreCase = true)
            if (pos < 0) return null
            val beforeOk = pos == 0 || attrs[pos - 1].isWhitespace()
            var cur = pos + nLen
            if (beforeOk && cur < attrs.length) {
                while (cur < attrs.length && attrs[cur].isWhitespace()) cur++
                if (cur < attrs.length && attrs[cur] == '=') {
                    cur++
                    while (cur < attrs.length && attrs[cur].isWhitespace()) cur++
                    if (cur < attrs.length && (attrs[cur] == '"' || attrs[cur] == '\'')) {
                        val quote = attrs[cur]
                        val start = cur + 1
                        val end = attrs.indexOf(quote, start)
                        if (end >= start) {
                            return TextHtmlMdParser.decodeHtmlEntities(attrs.substring(start, end))
                                .trim()
                                .takeIf { it.isNotBlank() }
                        }
                    }
                }
            }
            idx = pos + 1
        }
        return null
    }
}
