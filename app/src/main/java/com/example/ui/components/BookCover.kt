package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.FormatCategory
import com.example.ui.theme.LiterataFontFamily
import java.io.File
import kotlin.math.abs

private val coverPalettes = listOf(
    Color(0xFF23372C) to Color(0xFFF3ECE0),
    Color(0xFF4A2B23) to Color(0xFFF8EDE3),
    Color(0xFF22303F) to Color(0xFFEAF0F6),
    Color(0xFF3E3526) to Color(0xFFF6EFE0),
    Color(0xFF3B2838) to Color(0xFFF5EAF3),
    Color(0xFF1F3838) to Color(0xFFE8F4F2)
)

fun formatBadgeColors(format: BookFormat): Pair<Color, Color> {
    return when {
        format == BookFormat.EPUB -> Color(0xFF1E3A2F) to Color(0xFFF4EFE6)
        format == BookFormat.PDF -> Color(0xFF8C3B2B) to Color(0xFFFFF8F5)
        format.category == FormatCategory.COMIC -> Color(0xFF43325E) to Color(0xFFF7F2FF)
        format == BookFormat.MOBI || format == BookFormat.AZW3 -> Color(0xFF6B4423) to Color(0xFFFFF7EE)
        else -> Color(0xFF374151) to Color(0xFFF9FAFB)
    }
}

@Composable
fun BookCover(
    book: BookEntity,
    modifier: Modifier = Modifier,
    showFormatBadge: Boolean = true
) {
    val coverFile = remember(book.coverPath) { book.coverPath?.let { File(it) } }
    var coverLoadFailed by remember(book.coverPath) { mutableStateOf(false) }
    val shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 10.dp, bottomEnd = 10.dp)

    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(shape)
            .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
    ) {
        if (coverFile != null && !coverLoadFailed) {
            val context = LocalContext.current
            val imageRequest = remember(coverFile.absolutePath) {
                ImageRequest.Builder(context)
                    .data(coverFile)
                    .size(Size(360, 520))
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                onError = { coverLoadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val palette = coverPalettes[abs(book.title.hashCode()) % coverPalettes.size]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palette.first, palette.first.copy(alpha = 0.86f))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = book.author.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = palette.second.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.title,
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        color = palette.second,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        if (showFormatBadge) {
            val (badgeBg, badgeFg) = formatBadgeColors(book.format)
            Surface(
                color = badgeBg.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = book.format.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
