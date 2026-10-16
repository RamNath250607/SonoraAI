package com.sonora.ai.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun parse_playCommands() {
        val cmd1 = VoiceCommandParser.parse("Play Jana Nayagan Thalapathy song")
        assertTrue(cmd1 is SonoraCommand.Play)
        val play1 = cmd1 as SonoraCommand.Play
        assertTrue(play1.query.contains("jana nayagan", ignoreCase = true))

        val cmd2 = VoiceCommandParser.parse("Play latest Anirudh songs")
        assertTrue(cmd2 is SonoraCommand.Play)
        val play2 = cmd2 as SonoraCommand.Play
        assertTrue(play2.query.contains("anirudh", ignoreCase = true))

        val cmd3 = VoiceCommandParser.parse("Play romantic Tamil songs")
        assertTrue(cmd3 is SonoraCommand.Play)

        // Tamil Tanglish patterns
        val cmd4 = VoiceCommandParser.parse("Arabic Kuthu paatu podu")
        assertTrue(cmd4 is SonoraCommand.Play)

        val cmd5 = VoiceCommandParser.parse("Leo song play pannu")
        assertTrue(cmd5 is SonoraCommand.Play)
    }

    @Test
    fun parse_playbackControlCommands() {
        assertEquals(SonoraCommand.Pause, VoiceCommandParser.parse("pause"))
        assertEquals(SonoraCommand.Pause, VoiceCommandParser.parse("niruthu"))
        assertEquals(SonoraCommand.Pause, VoiceCommandParser.parse("pause song"))

        assertEquals(SonoraCommand.Resume, VoiceCommandParser.parse("resume"))
        assertEquals(SonoraCommand.Resume, VoiceCommandParser.parse("continue"))

        assertEquals(SonoraCommand.Next, VoiceCommandParser.parse("next"))
        assertEquals(SonoraCommand.Next, VoiceCommandParser.parse("next song"))
        assertEquals(SonoraCommand.Next, VoiceCommandParser.parse("adutha paatu"))

        assertEquals(SonoraCommand.Previous, VoiceCommandParser.parse("previous"))
        assertEquals(SonoraCommand.Previous, VoiceCommandParser.parse("munthaiya paatu"))

        assertEquals(SonoraCommand.FavoriteCurrent, VoiceCommandParser.parse("add to favorite"))
        assertEquals(SonoraCommand.FavoriteCurrent, VoiceCommandParser.parse("like this song"))
    }
}
