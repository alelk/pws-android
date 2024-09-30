package com.alelk.pws.database.util

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class PwsPsalmHtmlBuilderTest : FeatureSpec({

  feature("build html from psalm with single chorus") {
    val psalm = """
      | 1.
      | Verse 1 Line 1
      | Verse 1 Line 2
      | 
      | Chorus.
      | Chorus Line 1
      | Chorus Line 2
      | 
      | 2.
      | Verse 2 Line 1
      | Verse 2 Line 2
      | 
      | [Chorus]
      | 
      | 3.
      | Verse 3 Line 1
      | Verse 3 Line 2
      |""".trimMargin()

    scenario("build simple html") {
      val expectedHtml = """
        |<font color='#7aaf83'> 1 </font><br> Verse 1 Line 1<br> Verse 1 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus </font><br> Chorus Line 1<br> Chorus Line 2<br> <br>
        |<font color='#7aaf83'> 2 </font><br> Verse 2 Line 1<br> Verse 2 Line 2<br> <br>
        |<font color='#888888'><i> [Chorus]</i></font><br> <br>
        |<font color='#7aaf83'> 3 </font><br> Verse 3 Line 1<br> Verse 3 Line 2<br>
        |""".trimMargin().split('\n').joinToString("")

      val html = PwsPsalmHtmlBuilder(Locale.forLanguageTag("en")).buildHtml(psalm, false)
      html shouldBe expectedHtml
    }

    scenario("build expanded html") {
      val expectedHtml = """
        |<font color='#7aaf83'> 1 </font><br> Verse 1 Line 1<br> Verse 1 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus </font><br> Chorus Line 1<br> Chorus Line 2<br> <br>
        |<font color='#7aaf83'> 2 </font><br> Verse 2 Line 1<br> Verse 2 Line 2<br> <br>
        |<font color='#999999'>Chorus</font><br> Chorus Line 1<br> Chorus Line 2<br> <br> <br>
        |<font color='#7aaf83'> 3 </font><br> Verse 3 Line 1<br> Verse 3 Line 2<br>
        |""".trimMargin().split('\n').joinToString("")

      val html = PwsPsalmHtmlBuilder(Locale.forLanguageTag("en")).buildHtml(psalm, true)
      html shouldBe expectedHtml
    }
  }

  feature("build html from psalm with multiple choruses") {
    val psalm = """
      | 1.
      | Verse 1 Line 1
      | Verse 1 Line 2
      | 
      | Chorus 1.
      | Chorus 1 Line 1
      | Chorus 1 Line 2
      | 
      | 2.
      | Verse 2 Line 1
      | Verse 2 Line 2
      | 
      | [Chorus 1]
      | 
      | 3.
      | Verse 3 Line 1
      | Verse 3 Line 2
      | 
      | Chorus 2.
      | Chorus 2 Line 1
      | Chorus 2 Line 2
      | 
      | 4.
      | Verse 4 Line 1
      | Verse 4 Line 2
      | 
      | [Chorus 2]
      | 
      | [Chorus 1]
      |""".trimMargin()

    scenario("build simple html") {
      val expectedHtml = """
        |<font color='#7aaf83'> 1 </font><br> Verse 1 Line 1<br> Verse 1 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus 1 </font><br> Chorus 1 Line 1<br> Chorus 1 Line 2<br> <br>
        |<font color='#7aaf83'> 2 </font><br> Verse 2 Line 1<br> Verse 2 Line 2<br> <br>
        |<font color='#888888'><i> [Chorus 1]</i></font><br> <br>
        |<font color='#7aaf83'> 3 </font><br> Verse 3 Line 1<br> Verse 3 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus 2 </font><br> Chorus 2 Line 1<br> Chorus 2 Line 2<br> <br>
        |<font color='#7aaf83'> 4 </font><br> Verse 4 Line 1<br> Verse 4 Line 2<br> <br>
        |<font color='#888888'><i> [Chorus 2]</i></font><br> <br>
        |<font color='#888888'><i> [Chorus 1]</i></font><br>
        |""".trimMargin().split('\n').joinToString("")

      val html = PwsPsalmHtmlBuilder(Locale.forLanguageTag("en")).buildHtml(psalm, false)
      html shouldBe expectedHtml
    }

    scenario("build expanded html") {
      val expectedHtml = """
        |<font color='#7aaf83'> 1 </font><br> Verse 1 Line 1<br> Verse 1 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus 1 </font><br> Chorus 1 Line 1<br> Chorus 1 Line 2<br> <br>
        |<font color='#7aaf83'> 2 </font><br> Verse 2 Line 1<br> Verse 2 Line 2<br> <br>
        |<font color='#999999'>Chorus 1</font><br> Chorus 1 Line 1<br> Chorus 1 Line 2<br> <br> <br>
        |<font color='#7aaf83'> 3 </font><br> Verse 3 Line 1<br> Verse 3 Line 2<br> <br>
        |<font color='#7aaf83'> Chorus 2 </font><br> Chorus 2 Line 1<br> Chorus 2 Line 2<br> <br>
        |<font color='#7aaf83'> 4 </font><br> Verse 4 Line 1<br> Verse 4 Line 2<br> <br>
        |<font color='#999999'>Chorus 2</font><br> Chorus 2 Line 1<br> Chorus 2 Line 2<br> <br> <br>
        |<font color='#999999'>Chorus 1</font><br> Chorus 1 Line 1<br> Chorus 1 Line 2<br> <br>
        |""".trimMargin().split('\n').joinToString("")

      val html = PwsPsalmHtmlBuilder(Locale.forLanguageTag("en")).buildHtml(psalm, true)
      html shouldBe expectedHtml
    }
  }
})