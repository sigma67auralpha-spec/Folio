package com.example.engine

enum class BlockStyle {
    BODY,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BLOCKQUOTE,
    CODE_BLOCK,
    LIST_ITEM,
    DIVIDER,
    IMAGE
}

data class TextRun(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false
)

data class ParagraphBlock(
    val text: String,
    val style: BlockStyle = BlockStyle.BODY,
    val runs: List<TextRun> = emptyList(),
    val imagePath: String? = null
)

data class BookChapter(
    val index: Int,
    val title: String,
    val blocks: List<ParagraphBlock>
)

data class ParsedMetadata(
    val title: String,
    val author: String = "Unknown Author",
    val series: String = "",
    val seriesIndex: Float = 0f,
    val description: String = "",
    val coverBytes: ByteArray? = null,
    val totalUnits: Int = 1
)

sealed interface ParsedBookContent {
    data class Reflowable(
        val metadata: ParsedMetadata,
        val chapters: List<BookChapter>
    ) : ParsedBookContent

    data class Error(val message: String) : ParsedBookContent
}
