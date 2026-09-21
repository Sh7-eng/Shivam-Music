package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Song

object ShareHelper {

    /**
     * Builds the sharing text for social media or messaging platforms.
     */
    fun buildShareText(song: Song): String {
        return buildString {
            append("🎵 Now listening to \"${song.name}\" by ${song.artist}")
            if (song.album.isNotBlank() && song.album != "Unknown Album") {
                append(" (Album: ${song.album})")
            }
            append(" on Shivam Music!\n\n")
            if (song.downloadLink.isNotBlank()) {
                append("Listen along here: ${song.downloadLink}\n\n")
            }
            append("Shared via Shivam Music 🎧")
        }
    }

    /**
     * Creates an Android Intent configured for sharing text with Intent.ACTION_SEND
     * wrapped inside Intent.createChooser for social media and messaging apps.
     */
    fun createShareChooserIntent(song: Song): Intent {
        val shareText = buildShareText(song)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out \"${song.name}\" by ${song.artist}")
            putExtra(Intent.EXTRA_TITLE, "${song.name} - ${song.artist}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share \"${song.name}\" via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return chooser
    }

    /**
     * Dispatches the Android share sheet to let user choose any social media or messaging app.
     */
    fun shareTrack(context: Context, song: Song) {
        val chooserIntent = createShareChooserIntent(song)
        context.startActivity(chooserIntent)
    }
}
