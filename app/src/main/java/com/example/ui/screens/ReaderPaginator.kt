package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.model.HighlightEntity
import com.example.data.model.ReaderPreferences
import com.example.data.model.ReaderTextAlignment
import com.example.engine.BlockStyle
import com.example.engine.ParagraphBlock
import com.example.engine.TextRun
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File
import kotlin.math.roundToInt

sealed interface PageSlice {
    val paragraphIndex: Int

    data class TextSlice(
        override val paragraphIndex: Int,
        val block: ParagraphBlock,
        val startChar: Int,
        val endChar: Int,
        val isContinuation: Boolean
    ) : PageSlice

    data class ImageSlice(
        override val paragraphIndex: Int,
        val block: ParagraphBlock
    ) : PageSlice

    data class DividerSlice(
        override val paragraphIndex: Int
    ) : PageSlice
}

data class ChapterPage(
    val pageIndex: Int,
    val slices: List<PageSlice>,
    val firstParagraphIndex: Int,
    val lastParagraphIndex: Int
)

internal fun findPageForParagraph(pages: List<ChapterPage>, paragraphIndex: Int): Int {
    if (pages.isEmpty()) return 0
    val exactIndex = pages.indexOfFirst { page ->
        page.slices.any { it.paragraphIndex == paragraphIndex }
    }
    if (exactIndex >= 0) return exactIndex
    val rangeIndex = pages.indexOfFirst { page ->
        paragraphIndex in page.firstParagraphIndex..page.lastParagraphIndex
    }
    if (rangeIndex >= 0) return rangeIndex
    val nearestBefore = pages.indexOfLast { it.firstParagraphIndex <= paragraphIndex }
    return if (nearestBefore >= 0) nearestBefore else 0
}

internal fun bionicWordRanges(text: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var i = 0
    val n = text.length
    while (i < n) {
        if (Character.isLetterOrDigit(text[i])) {
            val start = i
            while (i < n && Character.isLetterOrDigit(text[i])) i++
            val len = i - start
            val boldLen = when {
                len <= 1 -> 1
                len <= 3 -> 1
                len <= 6 -> 2
                else -> (len * 0.45f).roundToInt().coerceIn(2, len - 1)
            }
            ranges.add(start until (start + boldLen))
        } else {
            i++
        }
    }
    return ranges
}

internal fun ReaderTextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    ReaderTextAlignment.START -> TextAlign.Start
    ReaderTextAlignment.JUSTIFY -> TextAlign.Justify
    ReaderTextAlignment.CENTER -> TextAlign.Center
    ReaderTextAlignment.END -> TextAlign.End
}

internal fun resolveBlockTextStyle(
    style: BlockStyle,
    prefs: ReaderPreferences,
    fontFamily: FontFamily,
    inkColor: Color,
    omitFirstLineIndent: Boolean = false
): TextStyle {
    val baseWeightValue = prefs.readerFontWeight.weightValue
    val baseWeight = FontWeight(baseWeightValue)
    val headingWeight = FontWeight((baseWeightValue + 250).coerceIn(600, 800))
    val baseFontStyle = if (prefs.italicText) FontStyle.Italic else FontStyle.Normal
    val baseSize = prefs.fontSizeSp
    val headingAlign = prefs.chapterTitleAlignment.toComposeTextAlign()
    val bodyAlign = prefs.textAlignment.toComposeTextAlign()

    return when (style) {
        BlockStyle.HEADING_1 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.45f).sp,
            lineHeight = (baseSize * 1.45f * 1.25f).sp,
            fontWeight = headingWeight,
            fontStyle = baseFontStyle,
            textAlign = headingAlign,
            color = inkColor
        )
        BlockStyle.HEADING_2 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.25f).sp,
            lineHeight = (baseSize * 1.25f * 1.3f).sp,
            fontWeight = headingWeight,
            fontStyle = baseFontStyle,
            textAlign = headingAlign,
            color = inkColor
        )
        BlockStyle.HEADING_3 -> TextStyle(
            fontFamily = fontFamily,
            fontSize = (baseSize * 1.1f).sp,
            lineHeight = (baseSize * 1.1f * 1.35f).sp,
            fontWeight = headingWeight,
            fontStyle = baseFontStyle,
            textAlign = headingAlign,
            color = inkColor
        )
        BlockStyle.CODE_BLOCK -> TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = (baseSize * 0.85f).sp,
            lineHeight = (baseSize * 0.85f * 1.45f).sp,
            color = inkColor
        )
        BlockStyle.BLOCKQUOTE -> TextStyle(
            fontFamily = fontFamily,
            fontSize = baseSize.sp,
            lineHeight = (baseSize * prefs.lineHeightMultiplier).sp,
            letterSpacing = prefs.letterSpacingEm.em,
            fontWeight = baseWeight,
            fontStyle = FontStyle.Italic,
            textAlign = bodyAlign,
            color = inkColor.copy(alpha = 0.85f)
        )
        else -> TextStyle(
            fontFamily = fontFamily,
            fontSize = baseSize.sp,
            lineHeight = (baseSize * prefs.lineHeightMultiplier).sp,
            letterSpacing = prefs.letterSpacingEm.em,
            fontWeight = baseWeight,
            fontStyle = baseFontStyle,
            textIndent = if (!omitFirstLineIndent && prefs.firstLineIndentDp > 0.5f) {
                TextIndent(firstLine = prefs.firstLineIndentDp.sp)
            } else {
                TextIndent.None
            },
            hyphens = if (prefs.autoHyphenation) Hyphens.Auto else Hyphens.None,
            color = inkColor,
            textAlign = bodyAlign
        )
    }
}

internal fun buildSliceAnnotatedString(
    fullText: String,
    style: BlockStyle,
    runs: List<TextRun>,
    highlights: List<HighlightEntity>,
    isTtsHighlighted: Boolean,
    prefs: ReaderPreferences,
    inkColor: Color,
    startChar: Int = 0,
    endChar: Int = fullText.length
): AnnotatedString {
    val safeStart = startChar.coerceIn(0, fullText.length)
    val safeEnd = endChar.coerceIn(safeStart, fullText.length)
    val sliceText = fullText.substring(safeStart, safeEnd)
    val useBionic = prefs.bionicReading && (style == BlockStyle.BODY || style == BlockStyle.BLOCKQUOTE)
    val bionicBoldWeight = FontWeight((prefs.readerFontWeight.weightValue + 300).coerceIn(600, 900))

    return buildAnnotatedString {
        append(sliceText)
        if (useBionic && sliceText.isNotEmpty()) {
            bionicWordRanges(fullText).forEach { range ->
                val clippedStart = maxOf(range.first, safeStart)
                val clippedEnd = minOf(range.last + 1, safeEnd)
                if (clippedStart < clippedEnd) {
                    addStyle(
                        SpanStyle(fontWeight = bionicBoldWeight),
                        clippedStart - safeStart,
                        clippedEnd - safeStart
                    )
                }
            }
        }
        runs.forEach { run ->
            val s = maxOf(run.start, safeStart)
            val e = minOf(run.end, safeEnd)
            if (s < e) {
                addStyle(
                    SpanStyle(
                        fontWeight = if (run.bold) FontWeight.Bold else null,
                        fontStyle = if (run.italic) FontStyle.Italic else null,
                        fontFamily = if (run.code) JetBrainsMonoFontFamily else null,
                        background = if (run.code) inkColor.copy(alpha = 0.08f) else Color.Unspecified
                    ),
                    s - safeStart,
                    e - safeStart
                )
            }
        }
        if (sliceText.isNotEmpty()) {
            highlights.forEach { hl ->
                addStyle(SpanStyle(background = Color(hl.color.hex)), 0, sliceText.length)
            }
            if (isTtsHighlighted) {
                addStyle(SpanStyle(background = Color(0x33C86D3B)), 0, sliceText.length)
            }
        }
    }
}

internal fun paginateChapterBlocks(
    blocks: List<ParagraphBlock>,
    prefs: ReaderPreferences,
    fontFamily: FontFamily,
    inkColor: Color,
    textMeasurer: TextMeasurer,
    contentWidthPx: Int,
    contentHeightPx: Int,
    density: Density
): List<ChapterPage> {
    if (blocks.isEmpty() || contentWidthPx <= 40 || contentHeightPx <= 60) {
        return listOf(ChapterPage(0, emptyList(), 0, 0))
    }

    val paragraphSpacingPx = with(density) { prefs.paragraphSpacingDp.dp.roundToPx() }
    val dividerHeightPx = with(density) { 18.dp.roundToPx() }
    val blockquoteHPadPx = with(density) { 26.dp.roundToPx() }
    val blockquoteVPadPx = with(density) { 16.dp.roundToPx() }
    val codeHPadPx = with(density) { 24.dp.roundToPx() }
    val codeVPadPx = with(density) { 24.dp.roundToPx() }
    val captionHeightPx = with(density) { 26.dp.roundToPx() }
    val imageMaxHeightPx = minOf(
        with(density) { 300.dp.roundToPx() },
        (contentHeightPx * 0.72f).roundToInt().coerceAtLeast(120)
    )

    val pages = mutableListOf<ChapterPage>()
    val currentSlices = mutableListOf<PageSlice>()
    var usedHeight = 0

    fun flushPage() {
        if (currentSlices.isNotEmpty()) {
            pages.add(
                ChapterPage(
                    pageIndex = pages.size,
                    slices = currentSlices.toList(),
                    firstParagraphIndex = currentSlices.first().paragraphIndex,
                    lastParagraphIndex = currentSlices.last().paragraphIndex
                )
            )
            currentSlices.clear()
            usedHeight = 0
        }
    }

    fun estimateImageHeightPx(imagePath: String): Int {
        val targetWidth = (contentWidthPx * prefs.imageWidthPercent.coerceIn(0.35f, 1.0f)).roundToInt()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { BitmapFactory.decodeFile(imagePath, opts) }
        return if (opts.outWidth > 0 && opts.outHeight > 0) {
            val scaled = ((opts.outHeight.toFloat() / opts.outWidth.toFloat()) * targetWidth).roundToInt()
            scaled.coerceIn(80, imageMaxHeightPx)
        } else {
            imageMaxHeightPx.coerceAtMost(contentHeightPx / 2)
        }
    }

    blocks.forEachIndexed { pIdx, block ->
        when (block.style) {
            BlockStyle.DIVIDER -> {
                val gap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0
                if (currentSlices.isNotEmpty() && usedHeight + gap + dividerHeightPx > contentHeightPx) {
                    flushPage()
                }
                val finalGap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0
                currentSlices.add(PageSlice.DividerSlice(pIdx))
                usedHeight += finalGap + dividerHeightPx
            }

            BlockStyle.IMAGE -> {
                val imgFile = block.imagePath?.let { File(it) }
                if (!prefs.showImages || imgFile == null || !imgFile.exists()) return@forEachIndexed
                val hasCaption = prefs.showImageCaptions &&
                    block.text.isNotBlank() &&
                    !block.text.equals("image", ignoreCase = true) &&
                    !block.text.equals("cover", ignoreCase = true)
                val imgHeight = estimateImageHeightPx(imgFile.absolutePath) + if (hasCaption) captionHeightPx else 0
                val gap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0
                if (currentSlices.isNotEmpty() && usedHeight + gap + imgHeight > contentHeightPx) {
                    flushPage()
                }
                val finalGap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0
                currentSlices.add(PageSlice.ImageSlice(pIdx, block))
                usedHeight += finalGap + imgHeight
            }

            else -> {
                if (block.text.isEmpty()) return@forEachIndexed
                val extraHPad = when (block.style) {
                    BlockStyle.BLOCKQUOTE -> blockquoteHPadPx
                    BlockStyle.CODE_BLOCK -> codeHPadPx
                    else -> 0
                }
                val extraVPad = when (block.style) {
                    BlockStyle.BLOCKQUOTE -> blockquoteVPadPx
                    BlockStyle.CODE_BLOCK -> codeVPadPx
                    else -> 0
                }
                val innerWidth = (contentWidthPx - extraHPad).coerceAtLeast(40)
                val initialAnnotated = buildSliceAnnotatedString(
                    fullText = block.text,
                    style = block.style,
                    runs = block.runs,
                    highlights = emptyList(),
                    isTtsHighlighted = false,
                    prefs = prefs,
                    inkColor = inkColor
                )
                val initialStyle = resolveBlockTextStyle(
                    style = block.style,
                    prefs = prefs,
                    fontFamily = fontFamily,
                    inkColor = inkColor,
                    omitFirstLineIndent = false
                )
                val fullLayout = textMeasurer.measure(
                    text = initialAnnotated,
                    style = initialStyle,
                    constraints = Constraints(maxWidth = innerWidth)
                )
                val fullHeight = fullLayout.size.height + extraVPad
                val gap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0

                if (usedHeight + gap + fullHeight <= contentHeightPx) {
                    currentSlices.add(
                        PageSlice.TextSlice(
                            paragraphIndex = pIdx,
                            block = block,
                            startChar = 0,
                            endChar = block.text.length,
                            isContinuation = false
                        )
                    )
                    usedHeight += gap + fullHeight
                } else {
                    val isHeading = block.style == BlockStyle.HEADING_1 ||
                        block.style == BlockStyle.HEADING_2 ||
                        block.style == BlockStyle.HEADING_3
                    val firstLineHeight = (if (fullLayout.lineCount > 0) {
                        fullLayout.getLineBottom(0).roundToInt()
                    } else 40) + extraVPad

                    if (currentSlices.isNotEmpty() &&
                        (isHeading || usedHeight + gap + firstLineHeight * 2 > contentHeightPx)
                    ) {
                        flushPage()
                    }

                    var charStart = 0
                    while (charStart < block.text.length) {
                        val sliceGap = if (currentSlices.isNotEmpty()) paragraphSpacingPx else 0
                        val availHeight = (contentHeightPx - usedHeight - sliceGap - extraVPad).coerceAtLeast(1)
                        val isCont = charStart > 0
                        val sliceAnnotated = if (charStart == 0) {
                            initialAnnotated
                        } else {
                            buildSliceAnnotatedString(
                                fullText = block.text,
                                style = block.style,
                                runs = block.runs,
                                highlights = emptyList(),
                                isTtsHighlighted = false,
                                prefs = prefs,
                                inkColor = inkColor,
                                startChar = charStart,
                                endChar = block.text.length
                            )
                        }
                        val sliceStyle = resolveBlockTextStyle(
                            style = block.style,
                            prefs = prefs,
                            fontFamily = fontFamily,
                            inkColor = inkColor,
                            omitFirstLineIndent = isCont
                        )
                        val layout = textMeasurer.measure(
                            text = sliceAnnotated,
                            style = sliceStyle,
                            constraints = Constraints(maxWidth = innerWidth)
                        )
                        val minFirstLine = if (layout.lineCount > 0) layout.getLineBottom(0).roundToInt() else 1
                        if (currentSlices.isNotEmpty() && minFirstLine > availHeight) {
                            flushPage()
                            continue
                        }

                        var fittingLine = 0
                        for (line in 0 until layout.lineCount) {
                            if (layout.getLineBottom(line) <= availHeight) {
                                fittingLine = line
                            } else {
                                break
                            }
                        }

                        val lineEnd = layout.getLineEnd(fittingLine, visibleEnd = true)
                            .coerceAtLeast(1)
                            .coerceAtMost(block.text.length - charStart)
                        val sliceEnd = (charStart + lineEnd).coerceAtMost(block.text.length)
                        val sliceHeight = layout.getLineBottom(fittingLine).roundToInt() + extraVPad

                        currentSlices.add(
                            PageSlice.TextSlice(
                                paragraphIndex = pIdx,
                                block = block,
                                startChar = charStart,
                                endChar = sliceEnd,
                                isContinuation = isCont
                            )
                        )
                        usedHeight += sliceGap + sliceHeight

                        var nextStart = sliceEnd
                        while (nextStart < block.text.length &&
                            (block.text[nextStart] == '\n' || block.text[nextStart] == ' ')
                        ) {
                            nextStart++
                        }
                        charStart = nextStart

                        if (charStart < block.text.length) {
                            flushPage()
                        }
                    }
                }
            }
        }
    }

    flushPage()
    if (pages.isEmpty()) {
        return listOf(ChapterPage(0, emptyList(), 0, 0))
    }
    return pages
}
