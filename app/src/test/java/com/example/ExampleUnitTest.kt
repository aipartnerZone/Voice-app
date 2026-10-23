package com.example

import com.example.domain.CommandParser
import com.example.domain.ParsedCommand
import com.example.domain.TargetApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `parse YouTube Hindi and Hinglish commands`() {
        val cmd1 = CommandParser.parse("Munna, YouTube kholo.")
        assertTrue(cmd1 is ParsedCommand.OpenApp)
        assertEquals(TargetApp.YOUTUBE, (cmd1 as ParsedCommand.OpenApp).targetApp)

        val cmd2 = CommandParser.parse("यूट्यूब खोलो")
        assertTrue(cmd2 is ParsedCommand.OpenApp)
        assertEquals(TargetApp.YOUTUBE, (cmd2 as ParsedCommand.OpenApp).targetApp)
    }

    @Test
    fun `parse WhatsApp, Chrome, and Files commands`() {
        val wa = CommandParser.parse("WhatsApp kholo.")
        assertTrue(wa is ParsedCommand.OpenApp && wa.targetApp == TargetApp.WHATSAPP)

        val chrome = CommandParser.parse("Chrome open karo.")
        assertTrue(chrome is ParsedCommand.OpenApp && chrome.targetApp == TargetApp.CHROME)

        val files = CommandParser.parse("Meri files kholo.")
        assertTrue(files is ParsedCommand.OpenApp && files.targetApp == TargetApp.FILES)
    }

    @Test
    fun `parse time and greeting commands locally without AI call`() {
        val timeCmd = CommandParser.parse("Munna, abhi kitne baje hain?")
        assertTrue(timeCmd is ParsedCommand.TellTime)

        val listenCmd = CommandParser.parse("Munna, meri baat suno.")
        assertTrue(listenCmd is ParsedCommand.GreetingOrListenCheck)
    }

    @Test
    fun `route general Hindi or Hinglish questions to AI assistant`() {
        val aiCmd = CommandParser.parse("Chai banane ki aasan recipe kya hai?")
        assertTrue(aiCmd is ParsedCommand.AskAiAssistant)
    }
}
