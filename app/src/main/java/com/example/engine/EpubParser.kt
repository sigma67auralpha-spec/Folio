package com.example.engine

import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object EpubParser {

    fun parse(
        file: File,
        fallbackTitle: String,
        imageCacheDir: File? = null
    ): ParsedBookContent {
        return runCatching {
            val archive = loadArchiveEntries(file)
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
            val chapters = mutableListOf<BookChapter>()
            for (itemId in spineItemIds) {
                val item = opf.manifest[itemId] ?: continue
                val fullPath = resolvePath(opfDir, item.href)
                val htmlBytes = archive.getEntryBytes(fullPath) ?: continue
                val chapterDir = fullPath.substringBeforeLast('/', "")
                val html = TextHtmlMdParser.decodeBytes(htmlBytes)

                val imageResolver: ((String) -> String?)? = if (bookImageDir != null) {
                    val bookImgCanonical = bookImageDir.canonicalFile.path
                    { rawSrc ->
                        val resolvedImgPath = resolvePath(chapterDir, rawSrc)
                        val imgBytes = archive.getEntryBytes(resolvedImgPath)
                        if (imgBytes != null && imgBytes.isNotEmpty()) {
                            val baseImgName = resolvedImgPath.substringAfterLast('/').replace(Regex("[^a-zA-Z0-9._-]"), "_")
                            val safeImgName = "${resolvedImgPath.hashCode().toUInt()}_$baseImgName"
                            val outFile = File(bookImageDir, safeImgName)
                            if (outFile.canonicalFile.path.startsWith(bookImgCanonical + File.separator)) {
                                if (!outFile.exists()) outFile.writeBytes(imgBytes)
                                outFile.absolutePath
                            } else null
                        } else null
                    }
                } else null

                val blocks = TextHtmlMdParser.extractHtmlBlocks(html, imageResolver)
                if (blocks.isEmpty()) continue

                val normalizedFull = normalizePath(fullPath)
                val tocTitle = tocLabels[normalizedFull]
                    ?: tocLabels.entries.firstOrNull { it.key.equals(normalizedFull, ignoreCase = true) }?.value
                    ?: tocLabels.entries.firstOrNull { it.key.endsWith("/${normalizedFull.substringAfterLast('/')}", ignoreCase = true) }?.value
                val headingTitle = blocks.firstOrNull {
                    it.style == BlockStyle.HEADING_1 || it.style == BlockStyle.HEADING_2 || it.style == BlockStyle.HEADING_3
                }?.text
                val chapterTitle = tocTitle ?: headingTitle ?: "Chapter ${chapters.size + 1}"
                chapters.add(BookChapter(chapters.size, chapterTitle.take(70), blocks))
            }

            val finalChapters = chapters.ifEmpty {
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
        }.getOrElse { err ->
            ParsedBookContent.Error("Failed to read EPUB: ${err.localizedMessage ?: "corrupt archive"}")
        }
    }

    private class ArchiveIndex(
        private val entriesByLowerPath: Map<String, ByteArray>
    ) {
        fun isEmpty(): Boolean = entriesByLowerPath.isEmpty()

        fun getEntryBytes(rawPath: String): ByteArray? {
            val norm = normalizePath(rawPath).lowercase()
            entriesByLowerPath[norm]?.let { return it }
            val decoded = safeUrlDecode(norm)
            entriesByLowerPath[decoded]?.let { return it }
            return null
        }

        fun findFirstPathEndingWith(suffix: String): String? =
            entriesByLowerPath.keys.firstOrNull { it.endsWith(suffix.lowercase()) }
    }

    private const val MAX_ENTRY_BYTES = 24 * 1024 * 1024
    private const val MAX_TOTAL_ARCHIVE_BYTES = 160L * 1024 * 1024
    private const val MAX_ARCHIVE_ENTRIES = 10_000

    private fun readBoundedStream(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
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

    private fun loadArchiveEntries(file: File): ArchiveIndex {
        val map = linkedMapOf<String, ByteArray>()
        val zipFileSuccess = runCatching {
            ZipFile(file).use { zip ->
                var totalBytes = 0L
                var count = 0
                val enumEntries = zip.entries()
                while (enumEntries.hasMoreElements()) {
                    val entry = enumEntries.nextElement()
                    count++
                    if (count > MAX_ARCHIVE_ENTRIES) error("EPUB contains too many archive entries.")
                    if (!entry.isDirectory) {
                        val norm = normalizePath(entry.name).lowercase()
                        val bytes = zip.getInputStream(entry).use { readBoundedStream(it, MAX_ENTRY_BYTES) }
                        totalBytes += bytes.size
                        if (totalBytes > MAX_TOTAL_ARCHIVE_BYTES) error("EPUB exceeds total uncompressed limit.")
                        map[norm] = bytes
                    }
                }
            }
        }.isSuccess

        if (!zipFileSuccess || map.isEmpty()) {
            map.clear()
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
        }
        return ArchiveIndex(map)
    }

    private fun hasContentDrm(archive: ArchiveIndex): Boolean {
        if (archive.getEntryBytes("META-INF/rights.xml") != null) return true
        val encBytes = archive.getEntryBytes("META-INF/encryption.xml") ?: return false
        val xml = TextHtmlMdParser.decodeBytes(encBytes)
        val cipherRefs = Regex("""URI\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
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
        return Regex("""full-path\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(xml)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
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
        val xml = TextHtmlMdParser.decodeBytes(bytes)
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        fun firstTagInnerText(localName: String): String? {
            val regex = Regex(
                """<(?:[a-zA-Z0-9_-]+:)?$localName\b[^>]*>(.*?)</(?:[a-zA-Z0-9_-]+:)?$localName>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val raw = regex.find(xml)?.groupValues?.getOrNull(1) ?: return null
            return TextHtmlMdParser.decodeHtmlEntities(raw.replace(Regex("<[^>]+>"), "")).trim()
                .takeIf { it.isNotEmpty() }
        }

        data.title = firstTagInnerText("title")
        data.author = firstTagInnerText("creator")
        data.description = firstTagInnerText("description")

        // Parse <meta .../> and <meta ...>...</meta>
        val metaRegex = Regex("""<(?:[a-zA-Z0-9_-]+:)?meta\b([^>]*)(?:>(.*?)</(?:[a-zA-Z0-9_-]+:)?meta>|/?>)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        for (m in metaRegex.findAll(xml)) {
            val attrs = m.groupValues[1]
            val inner = TextHtmlMdParser.decodeHtmlEntities(m.groupValues[2].replace(Regex("<[^>]+>"), "")).trim()
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

        // Parse <item .../> in manifest
        val itemRegex = Regex("""<(?:[a-zA-Z0-9_-]+:)?item\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
        for (m in itemRegex.findAll(xml)) {
            val attrs = m.groupValues[1]
            val id = extractXmlAttr(attrs, "id").orEmpty()
            val href = extractXmlAttr(attrs, "href").orEmpty()
            val mediaType = extractXmlAttr(attrs, "media-type").orEmpty()
            val props = extractXmlAttr(attrs, "properties").orEmpty()
            if (id.isNotBlank() && href.isNotBlank()) {
                data.manifest[id] = ManifestItem(id, href, mediaType)
                if (props.split(Regex("\\s+")).any { it == "cover-image" }) data.coverItemId = id
                if (props.split(Regex("\\s+")).any { it == "nav" }) data.navItemId = id
                if (mediaType.equals("application/x-dtbncx+xml", ignoreCase = true)) data.ncxItemId = id
            }
        }

        // Parse <spine toc="...">
        val spineRegex = Regex("""<(?:[a-zA-Z0-9_-]+:)?spine\b([^>]*)>""", RegexOption.IGNORE_CASE)
        spineRegex.find(xml)?.groupValues?.getOrNull(1)?.let { attrs ->
            val tocId = extractXmlAttr(attrs, "toc")
            if (!tocId.isNullOrBlank() && data.ncxItemId == null) data.ncxItemId = tocId
        }

        // Parse <itemref idref="..."/>
        val itemrefRegex = Regex("""<(?:[a-zA-Z0-9_-]+:)?itemref\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
        for (m in itemrefRegex.findAll(xml)) {
            val idref = extractXmlAttr(m.groupValues[1], "idref")
            if (!idref.isNullOrBlank()) data.spine.add(idref)
        }

        return data
    }

    private fun parseNcx(bytes: ByteArray, ncxDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val xml = TextHtmlMdParser.decodeBytes(bytes)
        val navPointRegex = Regex(
            """<navLabel\b[^>]*>.*?<text\b[^>]*>(.*?)</text>.*?</navLabel>.*?<content\b([^>]*)/?>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (m in navPointRegex.findAll(xml)) {
            val label = TextHtmlMdParser.decodeHtmlEntities(m.groupValues[1].replace(Regex("<[^>]+>"), ""))
                .replace(Regex("\\s+"), " ").trim()
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
        val linkRegex = Regex(
            """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (m in linkRegex.findAll(html)) {
            val href = m.groupValues[1]
            val text = TextHtmlMdParser.decodeHtmlEntities(m.groupValues[2].replace(Regex("<[^>]+>"), ""))
                .replace(Regex("\\s+"), " ").trim()
            if (href.isNotBlank() && text.isNotBlank()) {
                map.putIfAbsent(resolvePath(navDir, href), text)
            }
        }
        return map
    }

    private fun extractXmlAttr(attrs: String, attrName: String): String? {
        val pattern = Regex("""(?:^|\s)${Regex.escape(attrName)}\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
        val match = pattern.find(attrs) ?: return null
        val raw = match.groupValues[1].ifEmpty { match.groupValues[2] }
        return TextHtmlMdParser.decodeHtmlEntities(raw).trim().takeIf { it.isNotBlank() }
    }
}
