package com.example.util

import android.util.Log

object IrProtocolDecoder {
    private const val TAG = "IrProtocolDecoder"

    data class DecodedSignal(
        val frequency: Int,
        val pattern: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DecodedSignal
            if (frequency != other.frequency) return false
            if (!pattern.contentEquals(other.pattern)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = frequency
            result = 31 * result + pattern.contentHashCode()
            return result
        }
    }

    /**
     * Decode standard protocols or Pronto hex into frequency & raw timing pattern
     */
    fun decode(
        protocol: String,
        hexCode: String,
        rawPattern: String,
        frequency: Int,
        bitLength: Int
    ): DecodedSignal {
        return try {
            when (protocol.uppercase()) {
                "NEC" -> {
                    val pattern = decodeNec(hexCode, bitLength)
                    DecodedSignal(frequency = 38040, pattern = pattern)
                }
                "SONY" -> {
                    val pattern = decodeSony(hexCode, bitLength)
                    DecodedSignal(frequency = 40000, pattern = pattern)
                }
                "RC5" -> {
                    val pattern = decodeRc5(hexCode, bitLength)
                    DecodedSignal(frequency = 36000, pattern = pattern)
                }
                "PRONTO" -> {
                    decodePronto(hexCode.ifBlank { rawPattern }) ?: DecodedSignal(
                        frequency = frequency.coerceAtLeast(30000),
                        pattern = decodeRaw(rawPattern)
                    )
                }
                "RAW" -> {
                    DecodedSignal(
                        frequency = frequency.coerceAtLeast(30000),
                        pattern = decodeRaw(rawPattern)
                    )
                }
                else -> {
                    DecodedSignal(
                        frequency = frequency.coerceAtLeast(30000),
                        pattern = decodeRaw(rawPattern)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding signal: ${e.message}", e)
            // Fallback to raw pattern or a simple mock click
            val fallbackPattern = decodeRaw(rawPattern)
            DecodedSignal(
                frequency = frequency.coerceAtLeast(30000),
                pattern = if (fallbackPattern.isEmpty()) intArrayOf(500, 500) else fallbackPattern
            )
        }
    }

    private fun decodeNec(hexCode: String, bitLength: Int): IntArray {
        val cleanHex = hexCode.trim().removePrefix("0x").removePrefix("0X")
        if (cleanHex.isEmpty()) return intArrayOf()

        val value = cleanHex.toLong(16)
        val patternList = mutableListOf<Int>()

        // 1. Header (Preamble): 9000µs pulse, 4500µs space
        patternList.add(9000)
        patternList.add(4500)

        // 2. Data bits (MSB First for raw transmission convenience)
        for (i in (bitLength - 1) downTo 0) {
            val bit = (value ushr i) and 1L
            // Mark (Pulse): 560µs
            patternList.add(560)
            if (bit == 1L) {
                // Space: 1690µs
                patternList.add(1690)
            } else {
                // Space: 560µs
                patternList.add(560)
            }
        }

        // 3. Stop bit/Trailer: 560µs mark
        patternList.add(560)

        return patternList.toIntArray()
    }

    private fun decodeSony(hexCode: String, bitLength: Int): IntArray {
        val cleanHex = hexCode.trim().removePrefix("0x").removePrefix("0X")
        if (cleanHex.isEmpty()) return intArrayOf()

        val value = cleanHex.toLong(16)
        val patternList = mutableListOf<Int>()

        // 1. Header: 2400µs mark, 600µs space
        patternList.add(2400)
        patternList.add(600)

        // 2. Data bits (Sony typically sends LSB-first)
        for (i in 0 until bitLength) {
            val bit = (value ushr i) and 1L
            if (bit == 1L) {
                patternList.add(1200) // Mark for 1
            } else {
                patternList.add(600)  // Mark for 0
            }
            patternList.add(600) // Space (always 600µs)
        }

        return patternList.toIntArray()
    }

    private fun decodeRc5(hexCode: String, bitLength: Int): IntArray {
        val cleanHex = hexCode.trim().removePrefix("0x").removePrefix("0X")
        if (cleanHex.isEmpty()) return intArrayOf()

        val value = cleanHex.toLong(16)
        val halfBitDuration = 889 // µs

        // Accumulate high/low (pulse/space) states for Manchester encoding
        // true: Pulse (Mark), false: Space (Silence)
        val states = mutableListOf<Boolean>()

        // In RC5:
        // '0' bit is high-low transition (Mark for 889µs, then Space for 889µs)
        // '1' bit is low-high transition (Space for 889µs, then Mark for 889µs)
        for (i in (bitLength - 1) downTo 0) {
            val bit = (value ushr i) and 1L
            if (bit == 0L) {
                states.add(true)
                states.add(false)
            } else {
                states.add(false)
                states.add(true)
            }
        }

        return convertStatesToAlternatingPattern(states, halfBitDuration)
    }

    private fun convertStatesToAlternatingPattern(states: List<Boolean>, halfDuration: Int): IntArray {
        if (states.isEmpty()) return intArrayOf()

        val patternList = mutableListOf<Int>()
        var currentPulseState = states[0]
        var currentDuration = halfDuration

        // Android ConsumerIrManager requires pattern to start with a PULSE (mark)
        // If the first state is false (space), we prepend a tiny 1 microsecond pulse.
        if (!currentPulseState) {
            patternList.add(1) // Tiny, negligible pulse
            // The silence duration starts accumulating
        }

        for (i in 1 until states.size) {
            val state = states[i]
            if (state == currentPulseState) {
                currentDuration += halfDuration
            } else {
                patternList.add(currentDuration)
                currentPulseState = state
                currentDuration = halfDuration
            }
        }
        // Add final block
        patternList.add(currentDuration)

        // If the pattern ends in custom state logic list, ensure it has alternating sizing
        return patternList.toIntArray()
    }

    private fun decodePronto(prontoHex: String): DecodedSignal? {
        val tokens = prontoHex.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.size < 4) return null

        try {
            val rawTokens = tokens.map { it.toInt(16) }
            // Block 0: Always 0000 (indicates learned raw)
            if (rawTokens[0] != 0) return null

            // Block 1: Frequency indicator
            // Formula: frequency = 1000000 / (Block1 * 0.241246)
            val customFreqFactor = rawTokens[1]
            if (customFreqFactor == 0) return null
            val frequencyHz = (1000000.0 / (customFreqFactor * 0.241246)).toInt()

            val oncePairsCount = rawTokens[2]
            val repeatPairsCount = rawTokens[3]
            val totalPairs = oncePairsCount + repeatPairsCount

            if (tokens.size < 4 + totalPairs * 2) return null

            val patternList = mutableListOf<Int>()
            // Microseconds multiplier: (1,000,000 * customFreqFactor * 0.241246) / 1,000,000 = factor
            // Actually timing[µs] = ProntoValue * (1000000 / frequency)
            val conversionFactor = 1000000.0 / frequencyHz

            for (i in 0 until totalPairs * 2) {
                val value = rawTokens[4 + i]
                val microSec = (value * conversionFactor).toInt()
                patternList.add(microSec)
            }

            return DecodedSignal(
                frequency = frequencyHz,
                pattern = patternList.toIntArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Pronto Hex: ${e.message}")
            return null
        }
    }

    private fun decodeRaw(rawPattern: String): IntArray {
        return try {
            rawPattern.split(Regex("[,\\s\\s+]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.toInt() }
                .toIntArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing raw values: ${e.message}")
            intArrayOf()
        }
    }
}
