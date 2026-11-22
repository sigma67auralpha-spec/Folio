package com.example.engine

import java.nio.charset.Charset

object TextHtmlMdParser {

    private val PARA_SPLIT_REGEX = Regex("\n\\s*\n+")
    private val LINE_JOIN_REGEX = Regex("[ \t]*\n[ \t]*")
    private val ORDERED_LIST_REGEX = Regex("^\\d+\\.\\s+.*")
    private val HTML_TITLE_REGEX = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val HTML_AUTHOR_REGEX = Regex("<meta[^>]*name=[\"']author[\"'][^>]*content=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE)
    private val HTML_COMMENT_REGEX = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val HTML_SCRIPT_REGEX = Regex("<script\\b[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val HTML_STYLE_REGEX = Regex("<style\\b[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val HTML_HEAD_REGEX = Regex("<head\\b[^>]*>.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BLOCK_BOUNDARY_TAGS = setOf(
        "p", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "pre", "li", "dt", "dd", "tr", "figcaption",
        "div", "section", "article", "aside", "header", "footer"
    )
    private val NAMED_ENTITIES = mapOf(
        "nbsp" to " ",
        "#160" to " ",
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "#39" to "'",
        "apos" to "'",
        "mdash" to "—",
        "ndash" to "–",
        "hellip" to "…",
        "lsquo" to "‘",
        "rsquo" to "’",
        "ldquo" to "“",
        "rdquo" to "”",
        "laquo" to "«",
        "raquo" to "»",
        "copy" to "©"
    )

    fun decodeBytes(bytes: ByteArray): String {
        return when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            else -> {
                val utf8 = String(bytes, Charsets.UTF_8)
                if (utf8.contains('\uFFFD')) String(bytes, Charset.forName("ISO-8859-1")) else utf8
            }
        }
    }

    fun parsePlainText(rawText: String, fallbackTitle: String): ParsedBookContent.Reflowable {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val rawParagraphs = normalized.split(PARA_SPLIT_REGEX)
            .map { it.trim().replace(LINE_JOIN_REGEX, " ") }
            .filter { it.isNotEmpty() }

        val blocks = rawParagraphs.map { para ->
            val isChapterHeading = para.length < 70 &&
                (para.startsWith("Chapter ", ignoreCase = true) ||
                    para.startsWith("CHAPTER ") ||
                    para.startsWith("Part ", ignoreCase = true))
            ParagraphBlock(
                text = para,
                style = if (isChapterHeading) BlockStyle.HEADING_2 else BlockStyle.BODY
            )
        }

        val chapters = splitBlocksIntoChapters(blocks, fallbackTitle)
        return ParsedBookContent.Reflowable(
            metadata = ParsedMetadata(title = fallbackTitle, totalUnits = chapters.size),
            chapters = chapters
        )
    }

    fun parseMarkdown(rawMd: String, fallbackTitle: String): ParsedBookContent.Reflowable {
        val lines = rawMd.replace("\r\n", "\n").replace("\r", "\n").lines()
        val blocks = mutableListOf<ParagraphBlock>()
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        val paraBuffer = StringBuilder()
        var extractedTitle: String? = null

        fun flushParagraph() {
            val content = paraBuffer.toString().trim()
            paraBuffer.clear()
            if (content.isNotEmpty()) {
                val (clean, runs) = parseInlineMarkdown(content)
                blocks.add(ParagraphBlock(clean, BlockStyle.BODY, runs))
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                flushParagraph()
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBuffer.clear()
                } else {
                    inCodeBlock = false
                    blocks.add(ParagraphBlock(codeBuffer.toString().trimEnd(), BlockStyle.CODE_BLOCK))
                }
                continue
            }
            if (inCodeBlock) {
                codeBuffer.appendLine(line)
                continue
            }
            if (trimmed.isEmpty()) {
                flushParagraph()
                continue
            }
            when {
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    flushParagraph()
                    blocks.add(ParagraphBlock("—", BlockStyle.DIVIDER))
                }
                trimmed.startsWith("### ") -> {
                    flushParagraph()
                    val (clean, runs) = parseInlineMarkdown(trimmed.removePrefix("### ").trim())
                    blocks.add(ParagraphBlock(clean, BlockStyle.HEADING_3, runs))
                }
                trimmed.startsWith("## ") -> {
                    flushParagraph()
                    val (clean, runs) = parseInlineMarkdown(trimmed.removePrefix("## ").trim())
                    blocks.add(ParagraphBlock(clean, BlockStyle.HEADING_2, runs))
                }
                trimmed.startsWith("# ") -> {
                    flushParagraph()
                    val (clean, runs) = parseInlineMarkdown(trimmed.removePrefix("# ").trim())
                    if (extractedTitle == null) extractedTitle = clean
                    blocks.add(ParagraphBlock(clean, BlockStyle.HEADING_1, runs))
                }
                trimmed.startsWith("> ") -> {
                    flushParagraph()
                    val (clean, runs) = parseInlineMarkdown(trimmed.removePrefix("> ").trim())
                    blocks.add(ParagraphBlock(clean, BlockStyle.BLOCKQUOTE, runs))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches(ORDERED_LIST_REGEX) -> {
                    flushParagraph()
                    val itemText = when {
                        trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ").trim()}"
                        trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ").trim()}"
                        else -> trimmed
                    }
                    val (clean, runs) = parseInlineMarkdown(itemText)
                    blocks.add(ParagraphBlock(clean, BlockStyle.LIST_ITEM, runs))
                }
                else -> {
                    if (paraBuffer.isNotEmpty()) paraBuffer.append(' ')
                    paraBuffer.append(trimmed)
                }
            }
        }
        flushParagraph()
        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            blocks.add(ParagraphBlock(codeBuffer.toString().trimEnd(), BlockStyle.CODE_BLOCK))
        }

        val title = extractedTitle ?: fallbackTitle
        val chapters = splitBlocksIntoChapters(blocks, title)
        return ParsedBookContent.Reflowable(
            metadata = ParsedMetadata(title = title, totalUnits = chapters.size),
            chapters = chapters
        )
    }

    fun parseHtml(rawHtml: String, fallbackTitle: String): ParsedBookContent.Reflowable {
        val titleMatch = HTML_TITLE_REGEX
            .find(rawHtml)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it).trim() }
            ?.takeIf { it.isNotBlank() }
        val authorMatch = HTML_AUTHOR_REGEX
            .find(rawHtml)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it).trim() }
            ?.takeIf { it.isNotBlank() }

        val blocks = extractHtmlBlocks(rawHtml)
        val finalTitle = titleMatch ?: fallbackTitle
        val chapters = splitBlocksIntoChapters(blocks, finalTitle)
        return ParsedBookContent.Reflowable(
            metadata = ParsedMetadata(
                title = finalTitle,
                author = authorMatch ?: "Unknown Author",
                totalUnits = chapters.size
            ),
            chapters = chapters
        )
    }

    /**
     * Stream-based HTML/XHTML block extractor that handles arbitrary nesting of <div>, <section>,
     * <article>, <span>, etc., without outer containers ever swallowing inner <p>/heading elements.
     */
    fun extractHtmlBlocks(
        html: String,
        imageResolver: ((String) -> String?)? = null
    ): List<ParagraphBlock> {
        val cleaned = html
            .let { if ("<!--" in it) it.replace(HTML_COMMENT_REGEX, "") else it }
            .let { if ("<script" in it || "<SCRIPT" in it) it.replace(HTML_SCRIPT_REGEX, "") else it }
            .let { if ("<style" in it || "<STYLE" in it) it.replace(HTML_STYLE_REGEX, "") else it }
            .let { if ("<head" in it || "<HEAD" in it) it.replace(HTML_HEAD_REGEX, "") else it }

        val blocks = mutableListOf<ParagraphBlock>()
        val currentText = StringBuilder()
        val currentRuns = mutableListOf<TextRun>()
        var currentBlockStyle = BlockStyle.BODY

        var boldDepth = 0
        var italicDepth = 0
        var codeDepth = 0
        var preDepth = 0
        var spanStartBold = -1
        var spanStartItalic = -1
        var spanStartCode = -1

        fun closeActiveSpansAt(pos: Int) {
            if (spanStartBold in 0 until pos) {
                currentRuns.add(TextRun(spanStartBold, pos, bold = true))
                spanStartBold = pos
            }
            if (spanStartItalic in 0 until pos) {
                currentRuns.add(TextRun(spanStartItalic, pos, italic = true))
                spanStartItalic = pos
            }
            if (spanStartCode in 0 until pos) {
                currentRuns.add(TextRun(spanStartCode, pos, code = true))
                spanStartCode = pos
            }
        }

        fun flushBlock() {
            val pos = currentText.length
            closeActiveSpansAt(pos)
            val raw = currentText.toString()
            currentText.clear()
            val runsSnapshot = currentRuns.toList()
            currentRuns.clear()

            if (raw.isBlank()) {
                currentBlockStyle = BlockStyle.BODY
                spanStartBold = if (boldDepth > 0) 0 else -1
                spanStartItalic = if (italicDepth > 0) 0 else -1
                spanStartCode = if (codeDepth > 0) 0 else -1
                return
            }

            // Trim leading/trailing whitespace while adjusting run offsets
            var leadTrim = 0
            while (leadTrim < raw.length && raw[leadTrim].isWhitespace()) leadTrim++
            var trailTrim = raw.length
            while (trailTrim > leadTrim && raw[trailTrim - 1].isWhitespace()) trailTrim--
            val trimmed = raw.substring(leadTrim, trailTrim)
            if (trimmed.isNotEmpty()) {
                val prefix = if (currentBlockStyle == BlockStyle.LIST_ITEM && !trimmed.startsWith("•")) "• " else ""
                val shift = prefix.length - leadTrim
                val adjustedRuns = runsSnapshot.mapNotNull { r ->
                    val s = (r.start + shift).coerceIn(prefix.length, prefix.length + trimmed.length)
                    val e = (r.end + shift).coerceIn(s, prefix.length + trimmed.length)
                    if (s < e) r.copy(start = s, end = e) else null
                }
                blocks.add(ParagraphBlock(prefix + trimmed, currentBlockStyle, adjustedRuns))
            }

            currentBlockStyle = BlockStyle.BODY
            spanStartBold = if (boldDepth > 0) 0 else -1
            spanStartItalic = if (italicDepth > 0) 0 else -1
            spanStartCode = if (codeDepth > 0) 0 else -1
        }

        fun appendNormalizedText(segment: String) {
            val decoded = decodeHtmlEntities(segment)
            if (preDepth > 0) {
                currentText.append(decoded)
                return
            }
            var prevSpace = currentText.isEmpty() || currentText.last().isWhitespace()
            for (ch in decoded) {
                if (ch.isWhitespace()) {
                    if (!prevSpace) {
                        currentText.append(' ')
                        prevSpace = true
                    }
                } else {
                    currentText.append(ch)
                    prevSpace = false
                }
            }
        }

        val blockBoundaryTags = BLOCK_BOUNDARY_TAGS

        var i = 0
        while (i < cleaned.length) {
            val lt = cleaned.indexOf('<', i)
            if (lt < 0) {
                appendNormalizedText(cleaned.substring(i))
                break
            }
            if (lt > i) {
                appendNormalizedText(cleaned.substring(i, lt))
            }
            val gt = cleaned.indexOf('>', lt + 1)
            if (gt < 0) {
                appendNormalizedText(cleaned.substring(lt))
                break
            }
            val tagContent = cleaned.substring(lt + 1, gt).trim()
            i = gt + 1

            if (tagContent.isEmpty() || tagContent.startsWith("!") || tagContent.startsWith("?")) continue

            val isClosing = tagContent.startsWith("/")
            val isSelfClosing = tagContent.endsWith("/")
            val body = tagContent.removePrefix("/").removeSuffix("/").trim()
            val tagName = body.takeWhile { !it.isWhitespace() && it != ':' && it != '/' }
                .let { rawName ->
                    val afterColon = body.substringBefore(' ').substringAfter(':', rawName)
                    if (body.substringBefore(' ').contains(':')) afterColon else rawName
                }.lowercase()

            when {
                tagName == "br" -> {
                    if (currentText.isNotEmpty() && !currentText.endsWith("\n")) {
                        currentText.append('\n')
                    }
                }
                tagName == "hr" -> {
                    flushBlock()
                    blocks.add(ParagraphBlock("—", BlockStyle.DIVIDER))
                }
                (tagName == "img" || tagName == "image") && !isClosing -> {
                    val src = extractAttr(body, "src")
                        ?: extractAttr(body, "xlink:href")
                        ?: extractAttr(body, "href")
                    val resolvedPath = src?.let { imageResolver?.invoke(it) }
                    if (resolvedPath != null) {
                        flushBlock()
                        val alt = extractAttr(body, "alt")?.let { decodeHtmlEntities(it).trim() }.orEmpty()
                        blocks.add(ParagraphBlock(alt.ifBlank { "[Illustration]" }, BlockStyle.IMAGE, imagePath = resolvedPath))
                    }
                }
                tagName in blockBoundaryTags -> {
                    if (isClosing) {
                        if (tagName == "pre" && preDepth > 0) preDepth--
                        flushBlock()
                    } else {
                        flushBlock()
                        if (!isSelfClosing) {
                            currentBlockStyle = when (tagName) {
                                "h1" -> BlockStyle.HEADING_1
                                "h2" -> BlockStyle.HEADING_2
                                "h3", "h4", "h5", "h6" -> BlockStyle.HEADING_3
                                "blockquote" -> BlockStyle.BLOCKQUOTE
                                "pre" -> {
                                    preDepth++
                                    BlockStyle.CODE_BLOCK
                                }
                                "li", "dt" -> BlockStyle.LIST_ITEM
                                else -> BlockStyle.BODY
                            }
                        }
                    }
                }
                tagName == "b" || tagName == "strong" -> {
                    if (!isClosing && !isSelfClosing) {
                        if (boldDepth == 0) spanStartBold = currentText.length
                        boldDepth++
                    } else if (isClosing && boldDepth > 0) {
                        boldDepth--
                        if (boldDepth == 0 && spanStartBold in 0 until currentText.length) {
                            currentRuns.add(TextRun(spanStartBold, currentText.length, bold = true))
                            spanStartBold = -1
                        }
                    }
                }
                tagName == "i" || tagName == "em" || tagName == "cite" -> {
                    if (!isClosing && !isSelfClosing) {
                        if (italicDepth == 0) spanStartItalic = currentText.length
                        italicDepth++
                    } else if (isClosing && italicDepth > 0) {
                        italicDepth--
                        if (italicDepth == 0 && spanStartItalic in 0 until currentText.length) {
                            currentRuns.add(TextRun(spanStartItalic, currentText.length, italic = true))
                            spanStartItalic = -1
                        }
                    }
                }
                tagName == "code" || tagName == "tt" || tagName == "kbd" -> {
                    if (!isClosing && !isSelfClosing) {
                        if (codeDepth == 0) spanStartCode = currentText.length
                        codeDepth++
                    } else if (isClosing && codeDepth > 0) {
                        codeDepth--
                        if (codeDepth == 0 && spanStartCode in 0 until currentText.length) {
                            currentRuns.add(TextRun(spanStartCode, currentText.length, code = true))
                            spanStartCode = -1
                        }
                    }
                }
            }
        }
        flushBlock()
        return blocks
    }

    private fun extractAttr(tagBody: String, attrName: String): String? {
        var searchFrom = 0
        while (searchFrom < tagBody.length) {
            val idx = tagBody.indexOf(attrName, searchFrom, ignoreCase = true)
            if (idx < 0) return null
            val beforeOk = idx == 0 || tagBody[idx - 1].isWhitespace()
            var pos = idx + attrName.length
            while (pos < tagBody.length && tagBody[pos].isWhitespace()) pos++
            if (beforeOk && pos < tagBody.length && tagBody[pos] == '=') {
                pos++
                while (pos < tagBody.length && tagBody[pos].isWhitespace()) pos++
                if (pos >= tagBody.length) return null
                val quote = tagBody[pos]
                if (quote == '"' || quote == '\'') {
                    val end = tagBody.indexOf(quote, pos + 1)
                    if (end > pos + 1) return tagBody.substring(pos + 1, end).takeIf { it.isNotBlank() }
                    return null
                }
            }
            searchFrom = idx + attrName.length
        }
        return null
    }

    fun decodeHtmlEntities(input: String): String {
        if ('&' !in input) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val amp = input.indexOf('&', i)
            if (amp < 0) {
                sb.append(input, i, input.length)
                break
            }
            if (amp > i) sb.append(input, i, amp)
            val semi = input.indexOf(';', amp + 1)
            if (semi in (amp + 2)..(amp + 12)) {
                val entity = input.substring(amp + 1, semi)
                val replacement = NAMED_ENTITIES[entity] ?: NAMED_ENTITIES[entity.lowercase()] ?: when {
                    entity.startsWith("#x", ignoreCase = true) ->
                        entity.substring(2).toIntOrNull(16)?.let { code ->
                            runCatching { String(Character.toChars(code)) }.getOrNull()
                        }
                    entity.startsWith("#") ->
                        entity.substring(1).toIntOrNull()?.let { code ->
                            runCatching { String(Character.toChars(code)) }.getOrNull()
                        }
                    else -> null
                }
                if (replacement != null) {
                    sb.append(replacement)
                    i = semi + 1
                    continue
                }
            }
            sb.append('&')
            i = amp + 1
        }
        return sb.toString()
    }

    fun parseInlineMarkdown(input: String): Pair<String, List<TextRun>> {
        val out = StringBuilder()
        val runs = mutableListOf<TextRun>()
        var i = 0
        while (i < input.length) {
            when {
                input.startsWith("**", i) -> {
                    val close = input.indexOf("**", i + 2)
                    if (close > i + 2) {
                        val start = out.length
                        val inner = input.substring(i + 2, close)
                        out.append(inner)
                        runs.add(TextRun(start, out.length, bold = true))
                        i = close + 2
                    } else {
                        out.append(input[i]); i++
                    }
                }
                input[i] == '*' && (i + 1 < input.length && input[i + 1] != ' ') -> {
                    val close = input.indexOf('*', i + 1)
                    if (close > i + 1) {
                        val start = out.length
                        val inner = input.substring(i + 1, close)
                        out.append(inner)
                        runs.add(TextRun(start, out.length, italic = true))
                        i = close + 1
                    } else {
                        out.append(input[i]); i++
                    }
                }
                input[i] == '`' -> {
                    val close = input.indexOf('`', i + 1)
                    if (close > i + 1) {
                        val start = out.length
                        val inner = input.substring(i + 1, close)
                        out.append(inner)
                        runs.add(TextRun(start, out.length, code = true))
                        i = close + 1
                    } else {
                        out.append(input[i]); i++
                    }
                }
                else -> {
                    out.append(input[i])
                    i++
                }
            }
        }
        return out.toString() to runs
    }

    fun splitBlocksIntoChapters(blocks: List<ParagraphBlock>, defaultTitle: String): List<BookChapter> {
        if (blocks.isEmpty()) {
            return listOf(BookChapter(0, defaultTitle, listOf(ParagraphBlock("(Empty document)", BlockStyle.BODY))))
        }
        val headingIndices = blocks.mapIndexedNotNull { index, block ->
            if (block.style == BlockStyle.HEADING_1 || block.style == BlockStyle.HEADING_2) index else null
        }
        if (headingIndices.size >= 2) {
            val chapters = mutableListOf<BookChapter>()
            if (headingIndices.first() > 0) {
                chapters.add(BookChapter(0, "Opening", blocks.subList(0, headingIndices.first())))
            }
            for (hIdx in headingIndices.indices) {
                val start = headingIndices[hIdx]
                val end = if (hIdx + 1 < headingIndices.size) headingIndices[hIdx + 1] else blocks.size
                chapters.add(
                    BookChapter(
                        index = chapters.size,
                        title = blocks[start].text.take(60),
                        blocks = blocks.subList(start, end)
                    )
                )
            }
            return chapters
        }
        if (blocks.size > 50) {
            return blocks.chunked(35).mapIndexed { idx, chunk ->
                BookChapter(idx, "Section ${idx + 1}", chunk)
            }
        }
        return listOf(BookChapter(0, defaultTitle, blocks))
    }
}
