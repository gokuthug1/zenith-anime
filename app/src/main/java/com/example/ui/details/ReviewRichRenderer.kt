package com.example.ui.details

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

sealed class ReviewBlock {
    data class Heading(val level: Int, val text: String, val centered: Boolean = false) : ReviewBlock()
    data class Paragraph(val text: String, val centered: Boolean = false) : ReviewBlock()
    data class Quote(val text: String) : ReviewBlock()
    data class Image(val url: String, val widthHintDp: Int? = null, val caption: String = "") : ReviewBlock()
    data class Spoiler(val text: String, val label: String = "Spoiler") : ReviewBlock()
    data class BulletList(val items: List<String>) : ReviewBlock()
    object Divider : ReviewBlock()
}

object ReviewRichParser {
    private val anilistImgRegex = Regex("""img(\d+)?(%?)?\(\s*(https?://[^\s)]+)\s*\)""", RegexOption.IGNORE_CASE)
    private val markdownImgRegex = Regex("""!\[([^\]]*)\]\(\s*(https?://[^\s)]+)\s*\)""")
    private val bbcodeImgRegex = Regex("""\[img(?:=[^\]]*)?\]\s*(https?://[^\s\[]+)\s*\[/img\]""", RegexOption.IGNORE_CASE)
    private val htmlImgRegex = Regex("""<img[^>]+src=["'](https?://[^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val bareImgLineRegex = Regex("""^\s*(https?://\S+\.(?:jpg|jpeg|png|gif|webp)(?:\?\S*)?)\s*$""", RegexOption.IGNORE_CASE)

    fun extractAllImageUrls(rawBody: String): List<String> {
        return parseBlocks(rawBody)
            .filterIsInstance<ReviewBlock.Image>()
            .map { it.url }
            .distinct()
    }

    fun extractCleanPreviewText(rawBody: String): String {
        return rawBody
            .replace(anilistImgRegex, "")
            .replace(markdownImgRegex, "")
            .replace(bbcodeImgRegex, "")
            .replace(htmlImgRegex, "")
            .replace(Regex("""~!([\s\S]*?)!~"""), "[Spoiler]")
            .replace(Regex("""\[spoiler(?:=[^\]]*)?\][\s\S]*?\[/spoiler\]""", RegexOption.IGNORE_CASE), "[Spoiler]")
            .replace(Regex("""~~~+"""), "")
            .replace(Regex("""\[/?(?:center|quote|b|i|u|s|list|url(?:=[^\]]*)?|\*)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
            .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
            .replace(Regex("""__([^_]+)__"""), "$1")
            .replace(Regex("""~~([^~]+)~~"""), "$1")
            .replace(Regex("""\[([^\]]+)\]\((https?://[^)]+)\)"""), "$1")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    fun parseBlocks(rawBody: String): List<ReviewBlock> {
        if (rawBody.isBlank()) return emptyList()

        // Normalize multi-line tags into line-oriented tokens so we can parse cleanly
        var normalized = rawBody
            .replace("\r\n", "\n")
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<hr\s*/?>""", RegexOption.IGNORE_CASE), "\n---\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""<p[^>]*>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<h([1-4])[^>]*>([\s\S]*?)</h\1>""", RegexOption.IGNORE_CASE)) { m ->
                val hashes = "#".repeat(m.groupValues[1].toIntOrNull() ?: 2)
                "\n$hashes ${m.groupValues[2].trim()}\n"
            }

        // Convert multi-line spoilers ~! ... !~ and [spoiler]...[/spoiler]
        normalized = normalized.replace(
            Regex("""\[spoiler(?:=["']?([^\]"']+)["']?)?\]([\s\S]*?)\[/spoiler\]""", RegexOption.IGNORE_CASE)
        ) { m ->
            val label = m.groupValues[1].ifBlank { "Spoiler" }
            val content = m.groupValues[2].trim().replace("\n", " [[NL]] ")
            "\n[[SPOILER:$label]]$content[[/SPOILER]]\n"
        }
        normalized = normalized.replace(Regex("""~!([\s\S]*?)!~""")) { m ->
            val content = m.groupValues[1].trim().replace("\n", " [[NL]] ")
            "\n[[SPOILER:Spoiler]]$content[[/SPOILER]]\n"
        }

        // Convert [quote]...[/quote] and <blockquote>...</blockquote>
        normalized = normalized.replace(
            Regex("""\[quote(?:=[^\]]*)?\]([\s\S]*?)\[/quote\]""", RegexOption.IGNORE_CASE)
        ) { m ->
            val content = m.groupValues[1].trim().lines().joinToString("\n") { "> $it" }
            "\n$content\n"
        }
        normalized = normalized.replace(
            Regex("""<blockquote[^>]*>([\s\S]*?)</blockquote>""", RegexOption.IGNORE_CASE)
        ) { m ->
            val content = m.groupValues[1].trim().lines().joinToString("\n") { "> $it" }
            "\n$content\n"
        }

        // Convert inline images onto their own lines so they become ReviewBlock.Image
        normalized = normalized.replace(anilistImgRegex) { m ->
            val num = m.groupValues[1].toIntOrNull()
            val isPct = m.groupValues[2] == "%"
            val widthDp = when {
                num == null -> null
                isPct -> (num * 3).coerceIn(120, 360)
                else -> num.coerceIn(100, 420)
            }
            val url = m.groupValues[3].trim()
            "\n[[IMG:${widthDp ?: 0}|]]$url[[/IMG]]\n"
        }
        normalized = normalized.replace(markdownImgRegex) { m ->
            val alt = m.groupValues[1].trim().replace("|", " ")
            val url = m.groupValues[2].trim()
            "\n[[IMG:0|$alt]]$url[[/IMG]]\n"
        }
        normalized = normalized.replace(bbcodeImgRegex) { m ->
            val url = m.groupValues[1].trim()
            "\n[[IMG:0|]]$url[[/IMG]]\n"
        }
        normalized = normalized.replace(htmlImgRegex) { m ->
            val url = m.groupValues[1].trim()
            "\n[[IMG:0|]]$url[[/IMG]]\n"
        }

        // Handle ~~~ and [center]...[/center]
        normalized = normalized
            .replace(Regex("""\[/?center\]""", RegexOption.IGNORE_CASE), "\n~~~\n")
            .replace(Regex("""</?center>""", RegexOption.IGNORE_CASE), "\n~~~\n")

        val blocks = mutableListOf<ReviewBlock>()
        val paragraphBuffer = mutableListOf<String>()
        val quoteBuffer = mutableListOf<String>()
        val listBuffer = mutableListOf<String>()
        var inCenterBlock = false

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                val joined = paragraphBuffer.joinToString("\n").trim()
                if (joined.isNotBlank()) {
                    blocks += ReviewBlock.Paragraph(joined, centered = inCenterBlock)
                }
                paragraphBuffer.clear()
            }
        }

        fun flushQuote() {
            if (quoteBuffer.isNotEmpty()) {
                val joined = quoteBuffer.joinToString("\n").trim()
                if (joined.isNotBlank()) {
                    blocks += ReviewBlock.Quote(joined)
                }
                quoteBuffer.clear()
            }
        }

        fun flushList() {
            if (listBuffer.isNotEmpty()) {
                blocks += ReviewBlock.BulletList(listBuffer.toList())
                listBuffer.clear()
            }
        }

        fun flushAll() {
            flushParagraph()
            flushQuote()
            flushList()
        }

        for (rawLine in normalized.lines()) {
            val trimmed = rawLine.trim()

            if (trimmed == "~~~" || trimmed.matches(Regex("^~{3,}$"))) {
                flushAll()
                inCenterBlock = !inCenterBlock
                continue
            }

            val cleanTrimmed = trimmed.removePrefix("~~~").removeSuffix("~~~").trim()
            if (cleanTrimmed.isEmpty()) {
                flushAll()
                continue
            }

            // Divider
            if (cleanTrimmed.matches(Regex("""^(?:-{3,}|\*{3,}|_{3,})$"""))) {
                flushAll()
                blocks += ReviewBlock.Divider
                continue
            }

            // Image token
            if (cleanTrimmed.startsWith("[[IMG:") && cleanTrimmed.endsWith("[[/IMG]]")) {
                flushAll()
                val metaEnd = cleanTrimmed.indexOf("]]")
                if (metaEnd > 6) {
                    val meta = cleanTrimmed.substring(6, metaEnd)
                    val url = cleanTrimmed.substring(metaEnd + 2).removeSuffix("[[/IMG]]").trim()
                    val widthPart = meta.substringBefore("|").toIntOrNull()?.takeIf { it > 0 }
                    val captionPart = meta.substringAfter("|", "")
                    if (url.isNotBlank()) {
                        blocks += ReviewBlock.Image(url = url, widthHintDp = widthPart, caption = captionPart)
                    }
                }
                continue
            }

            // Bare image URL on its own line
            val bareImgMatch = bareImgLineRegex.matchEntire(cleanTrimmed)
            if (bareImgMatch != null) {
                flushAll()
                blocks += ReviewBlock.Image(url = bareImgMatch.groupValues[1].trim())
                continue
            }

            // Spoiler token
            if (cleanTrimmed.startsWith("[[SPOILER:") && cleanTrimmed.endsWith("[[/SPOILER]]")) {
                flushAll()
                val labelEnd = cleanTrimmed.indexOf("]]")
                if (labelEnd > 10) {
                    val label = cleanTrimmed.substring(10, labelEnd).ifBlank { "Spoiler" }
                    val content = cleanTrimmed.substring(labelEnd + 2)
                        .removeSuffix("[[/SPOILER]]")
                        .replace(" [[NL]] ", "\n")
                        .trim()
                    if (content.isNotBlank()) {
                        blocks += ReviewBlock.Spoiler(text = content, label = label)
                    }
                }
                continue
            }

            // Heading (#, ##, ###, ####)
            val headingMatch = Regex("""^(#{1,4})\s+(.+)$""").matchEntire(cleanTrimmed)
            if (headingMatch != null) {
                flushAll()
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].removeSuffix("#").trim()
                blocks += ReviewBlock.Heading(level = level, text = title, centered = inCenterBlock)
                continue
            }

            // Quote (> ...)
            if (cleanTrimmed.startsWith(">")) {
                flushParagraph()
                flushList()
                quoteBuffer += cleanTrimmed.removePrefix(">").trimStart()
                continue
            } else if (quoteBuffer.isNotEmpty()) {
                flushQuote()
            }

            // Bullet list (- ..., * ..., [*]...)
            val bulletMatch = Regex("""^(?:[-•]|\*(?!\*)|\[\*\])\s+(.+)$""").matchEntire(cleanTrimmed)
            if (bulletMatch != null) {
                flushParagraph()
                flushQuote()
                listBuffer += bulletMatch.groupValues[1].trim()
                continue
            } else if (listBuffer.isNotEmpty()) {
                flushList()
            }

            paragraphBuffer += cleanTrimmed
        }

        flushAll()
        return blocks
    }

    private data class InlineToken(
        val range: IntRange,
        val replacement: String,
        val style: SpanStyle,
        val urlAnnotation: String? = null
    )

    fun buildRichAnnotatedString(
        rawText: String,
        accentColor: Color,
        baseTextColor: Color
    ): AnnotatedString {
        // Normalize BBCode & HTML inline tags into Markdown equivalents for single-pass styling
        var text = rawText
            .replace(Regex("""\[b\]([\s\S]*?)\[/b\]""", RegexOption.IGNORE_CASE), "**$1**")
            .replace(Regex("""<(?:b|strong)>([\s\S]*?)</(?:b|strong)>""", RegexOption.IGNORE_CASE), "**$1**")
            .replace(Regex("""\[i\]([\s\S]*?)\[/i\]""", RegexOption.IGNORE_CASE), "*$1*")
            .replace(Regex("""<(?:i|em)>([\s\S]*?)</(?:i|em)>""", RegexOption.IGNORE_CASE), "*$1*")
            .replace(Regex("""\[u\]([\s\S]*?)\[/u\]""", RegexOption.IGNORE_CASE), "++$1++")
            .replace(Regex("""<u>([\s\S]*?)</u>""", RegexOption.IGNORE_CASE), "++$1++")
            .replace(Regex("""\[s\]([\s\S]*?)\[/s\]""", RegexOption.IGNORE_CASE), "~~$1~~")
            .replace(Regex("""<(?:s|del|strike)>([\s\S]*?)</(?:s|del|strike)>""", RegexOption.IGNORE_CASE), "~~$1~~")
            .replace(Regex("""\[url=([^\]]+)\]([\s\S]*?)\[/url\]""", RegexOption.IGNORE_CASE), "[$2]($1)")
            .replace(Regex("""<a[^>]+href=["'](https?://[^"']+)["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE), "[$2]($1)")
            .replace(Regex("""\[/?(?:list|size|color|font|align|center|quote|spoiler)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), "")

        val tokenRegex = Regex(
            """(\[([^\]]+)\]\((https?://[^\s)]+)\))|(\*\*\*([^*]+)\*\*\*)|(\*\*([^*]+)\*\*)|(__([^_]+)__)|(\+\+([^+]+)\+\+)|(~~([^~]+)~~)|(`([^`]+)`)|(\*([^*\n]+)\*)|(\b_([^_\n]+)_\b)"""
        )

        return buildAnnotatedString {
            var lastIndex = 0
            for (match in tokenRegex.findAll(text)) {
                if (match.range.first > lastIndex) {
                    append(text.substring(lastIndex, match.range.first))
                }
                when {
                    match.groups[1] != null -> {
                        val label = match.groupValues[2]
                        val url = match.groupValues[3]
                        val start = length
                        append(label)
                        addStyle(
                            SpanStyle(
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            ),
                            start,
                            length
                        )
                        addStringAnnotation("URL", url, start, length)
                    }
                    match.groups[4] != null -> {
                        val content = match.groupValues[5]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(
                                color = baseTextColor,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            ),
                            start,
                            length
                        )
                    }
                    match.groups[6] != null || match.groups[8] != null -> {
                        val content = if (match.groups[6] != null) match.groupValues[7] else match.groupValues[9]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(
                                color = baseTextColor,
                                fontWeight = FontWeight.Bold
                            ),
                            start,
                            length
                        )
                    }
                    match.groups[10] != null -> {
                        val content = match.groupValues[11]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start,
                            length
                        )
                    }
                    match.groups[12] != null -> {
                        val content = match.groupValues[13]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(
                                color = baseTextColor.copy(alpha = 0.65f),
                                textDecoration = TextDecoration.LineThrough
                            ),
                            start,
                            length
                        )
                    }
                    match.groups[14] != null -> {
                        val content = match.groupValues[15]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.14f)
                            ),
                            start,
                            length
                        )
                    }
                    match.groups[16] != null || match.groups[18] != null -> {
                        val content = if (match.groups[16] != null) match.groupValues[17] else match.groupValues[19]
                        val start = length
                        append(content)
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            start,
                            length
                        )
                    }
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}

@Composable
fun ReviewRichContent(
    rawBody: String,
    isExpanded: Boolean,
    themeAccent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blocks = remember(rawBody) { ReviewRichParser.parseBlocks(rawBody) }
    val imageUrls = remember(rawBody) { ReviewRichParser.extractAllImageUrls(rawBody) }
    val previewText = remember(rawBody) { ReviewRichParser.extractCleanPreviewText(rawBody) }
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    if (!isExpanded) {
        Column(modifier = modifier.fillMaxWidth()) {
            val previewAnnotated = remember(previewText, themeAccent) {
                ReviewRichParser.buildRichAnnotatedString(
                    rawText = previewText,
                    accentColor = themeAccent,
                    baseTextColor = TextPrimary
                )
            }
            Text(
                text = previewAnnotated,
                color = TextPrimary.copy(alpha = 0.9f),
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(imageUrls.take(4)) { imgUrl ->
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { zoomedImageUrl = imgUrl }
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Review image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (imageUrls.size > 4) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeAccent.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = themeAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${imageUrls.size - 4} more",
                                    color = themeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is ReviewBlock.Heading -> {
                        val fontSize = when (block.level) {
                            1 -> 17.sp
                            2 -> 15.5.sp
                            3 -> 14.sp
                            else -> 13.5.sp
                        }
                        val annotated = remember(block.text, themeAccent) {
                            ReviewRichParser.buildRichAnnotatedString(block.text, themeAccent, TextPrimary)
                        }
                        Text(
                            text = annotated,
                            color = TextPrimary,
                            fontSize = fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = if (block.centered) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }

                    is ReviewBlock.Paragraph -> {
                        val annotated = remember(block.text, themeAccent) {
                            ReviewRichParser.buildRichAnnotatedString(block.text, themeAccent, TextPrimary.copy(alpha = 0.92f))
                        }
                        ClickableText(
                            text = annotated,
                            style = TextStyle(
                                color = TextPrimary.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                textAlign = if (block.centered) TextAlign.Center else TextAlign.Start
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { ann ->
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item)))
                                        } catch (_: Exception) {}
                                    }
                            }
                        )
                    }

                    is ReviewBlock.Quote -> {
                        val annotated = remember(block.text, themeAccent) {
                            ReviewRichParser.buildRichAnnotatedString(block.text, themeAccent, TextPrimary.copy(alpha = 0.88f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .heightIn(min = 20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(themeAccent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = annotated,
                                color = TextPrimary.copy(alpha = 0.88f),
                                fontSize = 12.5.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    is ReviewBlock.BulletList -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp)
                        ) {
                            block.items.forEach { itemText ->
                                val annotated = remember(itemText, themeAccent) {
                                    ReviewRichParser.buildRichAnnotatedString(itemText, themeAccent, TextPrimary.copy(alpha = 0.92f))
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = "•",
                                        color = themeAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = annotated,
                                        color = TextPrimary.copy(alpha = 0.92f),
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }

                    is ReviewBlock.Image -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val imgMod = if (block.widthHintDp != null) {
                                Modifier
                                    .widthIn(max = block.widthHintDp.dp)
                                    .heightIn(min = 90.dp, max = 280.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 300.dp)
                            }
                            Box(
                                modifier = imgMod
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable { zoomedImageUrl = block.url }
                            ) {
                                AsyncImage(
                                    model = block.url,
                                    contentDescription = block.caption.ifBlank { "Review Illustration" },
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (block.caption.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = block.caption,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is ReviewBlock.Spoiler -> {
                        var revealed by remember { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, AccentYellow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .clickable { revealed = !revealed }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = AccentYellow,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (revealed) "Hide ${block.label}" else "Reveal ${block.label} (Tap to view)",
                                    color = AccentYellow,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AnimatedVisibility(visible = revealed) {
                                val annotated = remember(block.text, themeAccent) {
                                    ReviewRichParser.buildRichAnnotatedString(block.text, themeAccent, TextPrimary)
                                }
                                Text(
                                    text = annotated,
                                    color = TextPrimary.copy(alpha = 0.92f),
                                    fontSize = 12.5.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    ReviewBlock.Divider -> {
                        HorizontalDivider(
                            color = DarkBorder,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (zoomedImageUrl != null) {
        Dialog(
            onDismissRequest = { zoomedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .clickable { zoomedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = zoomedImageUrl,
                    contentDescription = "Zoomed review image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                IconButton(
                    onClick = { zoomedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
