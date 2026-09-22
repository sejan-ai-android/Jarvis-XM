package com.example

import com.example.data.prefs.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMarkdownCleaningForSpeech() {
    val rawText = "**Jarvis:** All systems are `nominal` and *operational*."
    val cleaned = rawText
      .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
      .replace(Regex("\\*([^*]+)\\*"), "$1")
      .replace(Regex("`([^`]+)`"), "$1")
      .replace(Regex("#+\\s"), "")
      .replace(Regex("[-*]\\s"), "")
      .trim()

    assertEquals("Jarvis: All systems are nominal and operational.", cleaned)
  }

  @Test
  fun testDefaultUserPreferences() {
    val prefs = UserPreferences()
    assertEquals("Sir", prefs.userName)
    assertEquals(1.0f, prefs.speechRate)
    assertTrue(prefs.autoSpeakResponses)
  }
}
