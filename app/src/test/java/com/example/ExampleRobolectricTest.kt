package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.getThemeColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SHIVAM MUSIC", appName)
  }

  @Test
  fun `verify theme presets available`() {
    val themes = AppThemePreset.values()
    assertEquals(6, themes.size)
    themes.forEach { preset ->
      val darkScheme = getThemeColorScheme(preset, isDark = true)
      val lightScheme = getThemeColorScheme(preset, isDark = false)
      assertNotNull(darkScheme)
      assertNotNull(lightScheme)
    }
  }

  @Test
  fun `verify curated mood archetypes are configured`() {
    val moods = com.example.data.remote.AiPlaylistService.CURATED_MOODS
    assertEquals(8, moods.size)
    moods.forEach { mood ->
      assertNotNull(mood.id)
      assertNotNull(mood.name)
      assertNotNull(mood.emoji)
      assertNotNull(mood.tagline)
    }
  }

  @Test
  fun `verify track sharing intent and text formatting`() {
    val testSong = com.example.data.model.Song(
      id = "test-1",
      name = "Midnight Vibes",
      artist = "Aurora Beats",
      album = "Electric Dreams",
      duration = 210,
      downloadLink = "https://example.com/stream/test-1.mp3"
    )

    val shareText = com.example.util.ShareHelper.buildShareText(testSong)
    org.junit.Assert.assertTrue(shareText.contains("Midnight Vibes"))
    org.junit.Assert.assertTrue(shareText.contains("Aurora Beats"))
    org.junit.Assert.assertTrue(shareText.contains("Electric Dreams"))
    org.junit.Assert.assertTrue(shareText.contains("Shivam Music"))

    val chooserIntent = com.example.util.ShareHelper.createShareChooserIntent(testSong)
    assertEquals(android.content.Intent.ACTION_CHOOSER, chooserIntent.action)
    val sendIntent = chooserIntent.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)
    assertNotNull(sendIntent)
    assertEquals(android.content.Intent.ACTION_SEND, sendIntent?.action)
    assertEquals("text/plain", sendIntent?.type)
    assertEquals(shareText, sendIntent?.getStringExtra(android.content.Intent.EXTRA_TEXT))
  }
}
