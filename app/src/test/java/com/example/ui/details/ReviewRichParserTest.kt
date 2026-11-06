package com.example.ui.details

import com.example.data.util.ShareUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRichParserTest {

    @Test
    fun extractsAniListMalAndMarkdownImages() {
        val sampleReview = """
            # Story Overview
            ~~~
            img220(https://i.imgur.com/sample1.jpg)
            ~~~
            This is a **bold** statement and [i]italic[/i] BBCode.
            [img]https://cdn.myanimelist.net/images/anime/1000/12345.jpg[/img]
            ![Banner](https://s4.anilist.co/file/anilistcdn/media/anime/banner/1.jpg)
            ~!Major ending spoiler here!~
        """.trimIndent()

        val images = ReviewRichParser.extractAllImageUrls(sampleReview)
        assertEquals(3, images.size)
        assertEquals("https://i.imgur.com/sample1.jpg", images[0])
        assertEquals("https://cdn.myanimelist.net/images/anime/1000/12345.jpg", images[1])
        assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/banner/1.jpg", images[2])

        val blocks = ReviewRichParser.parseBlocks(sampleReview)
        assertTrue(blocks.any { it is ReviewBlock.Heading && it.text == "Story Overview" })
        assertTrue(blocks.any { it is ReviewBlock.Image && it.url == "https://i.imgur.com/sample1.jpg" && it.widthHintDp == 220 })
        assertTrue(blocks.any { it is ReviewBlock.Image && it.url == "https://cdn.myanimelist.net/images/anime/1000/12345.jpg" })
        assertTrue(blocks.any { it is ReviewBlock.Spoiler && it.text.contains("Major ending spoiler here") })
    }

    @Test
    fun shareUrlsFormatCleanly() {
        val detailsUrl = ShareUtils.buildShareWebUrl("154587")
        assertEquals("https://gokuthug1.github.io/zenith-anime/?anime=154587#details/154587", detailsUrl)

        val watchUrl = ShareUtils.buildShareWebUrl("154587", 5)
        assertEquals("https://gokuthug1.github.io/zenith-anime/?anime=154587&ep=5#watch/154587/5", watchUrl)

        val appLink = ShareUtils.buildAppDeepLink("154587", 5)
        assertEquals("zenithanime://watch/154587/5", appLink)
    }
}
