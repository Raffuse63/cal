package com.example

import com.example.util.CalculatorEngine
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAllConstants() {
    val engine = CalculatorEngine()
    val symbols = listOf(
      "c", "g", "G", "μ₀", "ε₀", "h", "ħ", "e", "mₑ", "mₚ", "mₙ", "mμ", "a₀", "α", "rₑ",
      "λc", "R∞", "Nₐ", "k", "R", "F", "μB", "μN", "μₑ", "μₚ", "μₙ", "u", "eV", "Eₕ", "σ",
      "b", "atm", "Vₘ", "AU", "ly", "pc", "M☉", "M⊕", "R⊕", "R☉", "L☉", "p₀", "Z₀", "KJ",
      "RK", "kₑ", "n₀"
    )
    for (symbol in symbols) {
      val actualSymbol = if (symbol == "e") "𝑒" else symbol
      try {
        val result = engine.evaluate(actualSymbol)
        println("SUCCESS: $symbol -> $result")
      } catch (e: Exception) {
        println("FAILED: $symbol -> ${e.message}")
        fail("Symbol $symbol failed to evaluate: ${e.message}")
      }
    }
  }

  @Test
  fun testAllConstantsMultiplication() {
    val engine = CalculatorEngine()
    val symbols = listOf(
      "c", "g", "G", "μ₀", "ε₀", "h", "ħ", "e", "mₑ", "mₚ", "mₙ", "mμ", "a₀", "α", "rₑ",
      "λc", "R∞", "Nₐ", "k", "R", "F", "μB", "μN", "μₑ", "μₚ", "μₙ", "u", "eV", "Eₕ", "σ",
      "b", "atm", "Vₘ", "AU", "ly", "pc", "M☉", "M⊕", "R⊕", "R☉", "L☉", "p₀", "Z₀", "KJ",
      "RK", "kₑ", "n₀"
    )
    for (symbol in symbols) {
      val actualSymbol = if (symbol == "e") "𝑒" else symbol
      
      // Test 5 * symbol
      try {
        val expr = "5×$actualSymbol"
        val result = engine.evaluate(expr)
        println("SUCCESS: 5*$symbol -> $result")
      } catch (e: Exception) {
        println("FAILED: 5*$symbol -> ${e.message}")
        fail("Expr 5*$symbol failed to evaluate: ${e.message}")
      }

      // Test 5 symbol (implicit multiplication)
      try {
        val expr = "5$actualSymbol"
        val result = engine.evaluate(expr)
        println("SUCCESS: 5$symbol -> $result")
      } catch (e: Exception) {
        println("FAILED: 5$symbol -> ${e.message}")
        fail("Expr 5$symbol failed to evaluate: ${e.message}")
      }
    }
  }

  @Test
  fun testTinyConstantsFormatting() {
    val engine = CalculatorEngine()
    val symbols = listOf(
      "c", "g", "G", "μ₀", "ε₀", "h", "ħ", "e", "mₑ", "mₚ", "mₙ", "mμ", "a₀", "α", "rₑ",
      "λc", "R∞", "Nₐ", "k", "R", "F", "μB", "μN", "μₑ", "μₚ", "μₙ", "u", "eV", "Eₕ", "σ",
      "b", "atm", "Vₘ", "AU", "ly", "pc", "M☉", "M⊕", "R⊕", "R☉", "L☉", "p₀", "Z₀", "KJ",
      "RK", "kₑ", "n₀"
    )
    for (symbol in symbols) {
      val actualSymbol = if (symbol == "e") "𝑒" else symbol
      val rawValue = engine.evaluate(actualSymbol)
      val formatted = engine.formatResult(rawValue)
      assertNotEquals("Symbol $symbol formatted to 0", "0", formatted)
      if (abs(rawValue) >= 1e9 || abs(rawValue) <= 0.0001) {
        assertFalse("Symbol $symbol with value $rawValue should NOT automatically format using ×10^", formatted.contains("×10^"))
      }
      println("Formatted $symbol ($rawValue) -> $formatted")
    }
  }

  @Test
  fun testScientificNotationParsing() {
    val engine = CalculatorEngine()
    
    // Test evaluating standard ×10^y format
    val val1 = engine.evaluate("6.62607015×10^-34")
    assertEquals(6.62607015e-34, val1, 1e-45)
    
    val val2 = engine.evaluate("2.99792458×10^8")
    assertEquals(2.99792458e8, val2, 1.0)

    // Test evaluating superscript formatting
    val val3 = engine.evaluate("6.62607015×10⁻³⁴")
    assertEquals(6.62607015e-34, val3, 1e-45)

    val val4 = engine.evaluate("2.99792458×10⁸")
    assertEquals(2.99792458e8, val4, 1.0)

    val val5 = engine.evaluate("3E6")
    assertEquals(3000000.0, val5, 1e-9)

    val val6 = engine.evaluate("3E-6")
    assertEquals(0.000003, val6, 1e-12)

    val val7 = engine.evaluate("3E+6")
    assertEquals(3000000.0, val7, 1e-9)

    val val8 = engine.evaluate("3E6+5")
    assertEquals(3000005.0, val8, 1e-9)
  }
}


