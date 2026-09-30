package com.evo2.assistant

import java.text.Normalizer
import java.util.Locale

enum class VoiceCommandType { YOUTUBE_HOME, YOUTUBE_SEARCH, FLASH_ON, FLASH_OFF, SCROLL_DOWN, UNKNOWN }

data class VoiceCommand(val type: VoiceCommandType, val query: String = "")

object VoiceCommands {
    fun parse(spoken: String): VoiceCommand {
        val normalized = normalize(spoken)
        val words = normalized.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

        val hasFlash = words.contains("fener") || words.contains("isik") || words.contains("flashlight")
        val turnOn = listOf("yandir", "yandirsin", "ac", "acsin", "yak", "on").any { words.contains(it) }
        val turnOff = listOf("sondur", "sondursun", "bagla", "baglasin", "kapat", "off").any { words.contains(it) }
        if (hasFlash && turnOn) return VoiceCommand(VoiceCommandType.FLASH_ON)
        if (hasFlash && turnOff) return VoiceCommand(VoiceCommandType.FLASH_OFF)

        val scrollCommand = (words.contains("asagi") && listOf("surusdur", "surusdursun", "surustur", "surustursun", "kaydir", "scroll").any { words.contains(it) }) ||
            words.contains("scroll down") || words.contains("swipe down") ||
            (words.contains("yukari") && listOf("kaydir", "surusdur", "surustur").any { words.contains(it) })
        if (scrollCommand) return VoiceCommand(VoiceCommandType.SCROLL_DOWN)

        if (words.contains("youtube")) {
            val query = normalized
                .replace(Regex("youtube(?:-?(?:da|de|dan|den|ni|nu|yu|yi|ye|ya))?"), " ")
                .replace(Regex("\\b(ac|acsin|oxut|oxu|cal|mahnini|mahnisini|mahnisi|musiqi|video|tap|axtar|open|play|search|ni|nu|da|de)\\b"), " ")
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .replace(Regex("\\s+"), " ").trim()
            return if (query.isBlank()) VoiceCommand(VoiceCommandType.YOUTUBE_HOME)
            else VoiceCommand(VoiceCommandType.YOUTUBE_SEARCH, query)
        }
        return VoiceCommand(VoiceCommandType.UNKNOWN)
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('ı', 'i')
        .replace('ə', 'e')
}
