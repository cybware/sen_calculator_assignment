package com.example

import org.junit.Assert.*
import org.junit.Test

class CalculatorParserTest {

    @Test
    fun testBasicArithmetic() {
        val parser = CalculatorParser()
        assertEquals(5.0, parser.parse("2 + 3"), 1e-9)
        assertEquals(1.0, parser.parse("3 - 2"), 1e-9)
        assertEquals(6.0, parser.parse("2 * 3"), 1e-9)
        assertEquals(2.5, parser.parse("5 / 2"), 1e-9)
    }

    @Test
    fun testOrderOfOperations() {
        val parser = CalculatorParser()
        assertEquals(11.0, parser.parse("2 + 3 * 3"), 1e-9)
        assertEquals(15.0, parser.parse("(2 + 3) * 3"), 1e-9)
    }

    @Test
    fun testTrigonometryDegreeMode() {
        val parser = CalculatorParser(isDegreeMode = true)
        assertEquals(0.5, parser.parse("sin(30)"), 1e-5)
        assertEquals(1.0, parser.parse("sin(90)"), 1e-9)
        assertEquals(0.5, parser.parse("cos(60)"), 1e-5)
        assertEquals(1.0, parser.parse("tan(45)"), 1e-9)
    }

    @Test
    fun testTrigonometryRadianMode() {
        val parser = CalculatorParser(isDegreeMode = false)
        // sin(pi/6) = sin(0.5235987...) = 0.5
        assertEquals(0.5, parser.parse("sin(3.141592653589793 / 6)"), 1e-9)
    }

    @Test
    fun testFactorial() {
        val parser = CalculatorParser()
        assertEquals(1.0, parser.parse("0!"), 1e-9)
        assertEquals(1.0, parser.parse("1!"), 1e-9)
        assertEquals(120.0, parser.parse("5!"), 1e-9)
        assertEquals(720.0, parser.parse("3!!"), 1e-9) // chained factorials
    }

    @Test
    fun testPowerAndRoots() {
        val parser = CalculatorParser()
        assertEquals(25.0, parser.parse("5²"), 1e-9)
        assertEquals(3.0, parser.parse("√(9)"), 1e-9)
        assertEquals(8.0, parser.parse("2^3"), 1e-9)
        assertEquals(2.0, parser.parse("8^(1/3)"), 1e-9) // upgraded y-root root representation
    }

    @Test
    fun testPermutationsUpgraded() {
        val parser = CalculatorParser()
        // 5 P 3 = 60
        assertEquals(60.0, parser.parse("5 nPr 3"), 1e-9)
        
        // Large permutation that would previously overflow/NaN in the old factorial-based parser (180! / 178!):
        // 180 P 2 = 180 * 179 = 32220
        assertEquals(32220.0, parser.parse("180 nPr 2"), 1e-9)
    }

    @Test
    fun testCombinationsUpgraded() {
        val parser = CalculatorParser()
        // 5 C 2 = 10
        assertEquals(10.0, parser.parse("5 nCr 2"), 1e-9)
        
        // Large combination that would previously overflow/NaN in the old factorial-based parser (180! / (2! * 178!)):
        // 180 C 2 = 180 * 179 / 2 = 16110
        assertEquals(16110.0, parser.parse("180 nCr 2"), 1e-9)
    }

    @Test(expected = ArithmeticException::class)
    fun testDivisionByZeroThrows() {
        val parser = CalculatorParser()
        parser.parse("5 / 0")
    }

    @Test(expected = ArithmeticException::class)
    fun testInvalidPermutationThrows() {
        val parser = CalculatorParser()
        parser.parse("3 nPr 5") // n < r should throw
    }
}
