package com.example.data.model

enum class FormatCategory {
    REFLOWABLE,
    PDF,
    COMIC
}

enum class BookFormat(
    val displayName: String,
    val extensions: List<String>,
    val category: FormatCategory
) {
    EPUB("EPUB", listOf("epub"), FormatCategory.REFLOWABLE),
    PDF("PDF", listOf("pdf"), FormatCategory.PDF),
    MOBI("MOBI", listOf("mobi", "prc"), FormatCategory.REFLOWABLE),
    AZW3("AZW3", listOf("azw3", "azw"), FormatCategory.REFLOWABLE),
    FB2("FB2", listOf("fb2", "fb2.zip"), FormatCategory.REFLOWABLE),
    DOCX("DOCX", listOf("docx"), FormatCategory.REFLOWABLE),
    RTF("RTF", listOf("rtf"), FormatCategory.REFLOWABLE),
    TXT("TXT", listOf("txt"), FormatCategory.REFLOWABLE),
    HTML("HTML", listOf("html", "htm", "xhtml"), FormatCategory.REFLOWABLE),
    MARKDOWN("MD", listOf("md", "markdown"), FormatCategory.REFLOWABLE),
    CBZ("CBZ", listOf("cbz"), FormatCategory.COMIC),
    CBR("CBR", listOf("cbr"), FormatCategory.COMIC);

    companion object {
        fun fromFileName(fileName: String): BookFormat? {
            val lower = fileName.lowercase()
            if (lower.endsWith(".fb2.zip")) return FB2
            val ext = lower.substringAfterLast('.', "")
            return entries.firstOrNull { ext in it.extensions }
        }

        fun fromMimeType(mimeType: String?): BookFormat? = when (mimeType?.lowercase()?.substringBefore(';')?.trim()) {
            "application/epub+zip" -> EPUB
            "application/pdf" -> PDF
            "application/x-mobipocket-ebook" -> MOBI
            "application/vnd.amazon.mobi8-ebook", "application/vnd.amazon.ebook" -> AZW3
            "application/x-fictionbook+xml", "application/x-fictionbook" -> FB2
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCX
            "application/rtf", "text/rtf" -> RTF
            "text/plain" -> TXT
            "text/html", "application/xhtml+xml" -> HTML
            "text/markdown", "text/x-markdown" -> MARKDOWN
            "application/vnd.comicbook+zip", "application/x-cbz" -> CBZ
            "application/vnd.comicbook-rar", "application/x-cbr" -> CBR
            else -> null
        }

        val supportedMimeTypes: Array<String> = arrayOf(
            "application/epub+zip",
            "application/pdf",
            "application/x-mobipocket-ebook",
            "application/vnd.amazon.ebook",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/rtf",
            "text/rtf",
            "text/plain",
            "text/html",
            "text/markdown",
            "application/x-cbz",
            "application/x-cbr",
            "application/zip",
            "application/octet-stream",
            "*/*"
        )
    }
}
