package com.example

import kotlin.math.*

/**
 * A highly precise, deterministic mathematical expression parser for a scientific calculator.
 * Supports standard arithmetic, trigonometry, hyperbolics, logarithms, roots, exponents, factorials,
 * permutations (nPr), combinations (nCr), and inverse trig functions.
 */
class CalculatorParser(private val isDegreeMode: Boolean = true) {

    private var pos = -1
    private var ch = 0
    private var expr = ""

    private fun nextChar() {
        ch = if (++pos < expr.length) expr[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    /**
     * Parses and evaluates a mathematical expression string.
     * Maps user-facing display symbols to parseable mathematical operators.
     */
    fun parse(expression: String): Double {
        // Preprocess string to map display symbols to parser-friendly tokens
        this.expr = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "3.141592653589793")
            .replace("e", "2.718281828459045")
            .replace("²", "^2")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace("sinh⁻¹", "asinh")
            .replace("cosh⁻¹", "acosh")
            .replace("tanh⁻¹", "atanh")
            .replace("nPr", "P")
            .replace("nCr", "C")
            .replace(" ", "") // Remove layout spaces

        this.pos = -1
        nextChar()
        val x = parseExpression()
        if (pos < expr.length) {
            throw IllegalArgumentException("Unexpected character at position $pos: '${ch.toChar()}'")
        }
        return x
    }

    // Grammar Hierarchy:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor | term `P` factor | term `C` factor
    // factor = power | factor `!` (factorial)
    // power = base `^` power | base (right-associative)
    // base = `+` base | `-` base | `(` expression `)` | number | function `(` expression `)`

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm() // Addition
            else if (eat('-'.code)) x -= parseTerm() // Subtraction
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor() // Multiplication
            else if (eat('/'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                x /= divisor // Division
            } else if (eat('P'.code)) {
                val r = parseFactor()
                x = permutation(x, r) // Permutations (nPr)
            } else if (eat('C'.code)) {
                val r = parseFactor()
                x = combination(x, r) // Combinations (nCr)
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        var x = parsePower()
        // Handle postfix factorial operator (supports chaining, e.g. 3!! = 6! = 720)
        while (eat('!'.code)) {
            x = factorial(x)
        }
        return x
    }

    private fun parsePower(): Double {
        var x = parseBase()
        if (eat('^'.code)) {
            x = x.pow(parsePower()) // Power (exponentiation)
        }
        return x
    }

    private fun parseBase(): Double {
        if (eat('+'.code)) return parseBase() // Unary plus
        if (eat('-'.code)) return -parseBase() // Unary minus

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) { // Parentheses
            x = parseExpression()
            eat(')'.code) // Consumer closing parenthesis
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // Numbers
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            val numStr = expr.substring(startPos, this.pos)
            x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
        } else if ((ch >= 'a'.code && ch <= 'z'.code) || ch == '√'.code || ch == '⁻'.code || ch == '¹'.code) { // Functions & constants
            val funcName = StringBuilder()
            if (ch == '√'.code) {
                funcName.append('√')
                nextChar()
            } else {
                while ((ch >= 'a'.code && ch <= 'z'.code) || ch >= '0'.code && ch <= '9'.code || ch == '⁻'.code || ch == '¹'.code) {
                    funcName.append(ch.toChar())
                    nextChar()
                }
            }
            val name = funcName.toString()
            if (eat('('.code)) {
                val arg = parseExpression()
                eat(')'.code)
                x = when (name) {
                    "sin" -> if (isDegreeMode) sin(Math.toRadians(arg)) else sin(arg)
                    "cos" -> if (isDegreeMode) cos(Math.toRadians(arg)) else cos(arg)
                    "tan" -> if (isDegreeMode) tan(Math.toRadians(arg)) else tan(arg)
                    "asin" -> {
                        if (arg < -1.0 || arg > 1.0) throw ArithmeticException("asin undefined for x outside [-1, 1]")
                        val v = asin(arg)
                        if (isDegreeMode) Math.toDegrees(v) else v
                    }
                    "acos" -> {
                        if (arg < -1.0 || arg > 1.0) throw ArithmeticException("acos undefined for x outside [-1, 1]")
                        val v = acos(arg)
                        if (isDegreeMode) Math.toDegrees(v) else v
                    }
                    "atan" -> {
                        val v = atan(arg)
                        if (isDegreeMode) Math.toDegrees(v) else v
                    }
                    "sinh" -> sinh(arg)
                    "cosh" -> cosh(arg)
                    "tanh" -> tanh(arg)
                    "asinh" -> ln(arg + sqrt(arg * arg + 1.0))
                    "acosh" -> {
                        if (arg < 1.0) throw ArithmeticException("acosh undefined for x < 1")
                        ln(arg + sqrt(arg * arg - 1.0))
                    }
                    "atanh" -> {
                        if (arg <= -1.0 || arg >= 1.0) throw ArithmeticException("atanh undefined for x outside (-1, 1)")
                        0.5 * ln((1.0 + arg) / (1.0 - arg))
                    }
                    "log" -> {
                        if (arg <= 0.0) throw ArithmeticException("logarithm undefined for x <= 0")
                        log10(arg)
                    }
                    "ln" -> {
                        if (arg <= 0.0) throw ArithmeticException("logarithm undefined for x <= 0")
                        ln(arg)
                    }
                    "√" -> {
                        if (arg < 0.0) throw ArithmeticException("Square root of negative number")
                        sqrt(arg)
                    }
                    else -> throw IllegalArgumentException("Unknown function: $name")
                }
            } else {
                // Constants
                x = when (name) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    else -> throw IllegalArgumentException("Unknown variable or constant: $name")
                }
            }
        } else {
            throw IllegalArgumentException("Unexpected character: '${ch.toChar()}'")
        }

        return x
    }

    private fun factorial(n: Double): Double {
        if (n < 0.0 || n != floor(n)) {
            throw ArithmeticException("Factorial is only defined for non-negative integers")
        }
        val num = n.toInt()
        if (num > 170) return Double.POSITIVE_INFINITY // Standard IEEE 754 limit for Double
        var result = 1.0
        for (i in 2..num) {
            result *= i
        }
        return result
    }

    private fun permutation(n: Double, r: Double): Double {
        if (n < 0.0 || r < 0.0 || n != floor(n) || r != floor(r) || n < r) {
            throw ArithmeticException("Permutation nPr is only defined for integers with n >= r >= 0")
        }
        if (r == 0.0) return 1.0
        var result = 1.0
        val steps = r.toLong()
        for (i in 0 until steps) {
            result *= (n - i)
            if (result.isInfinite()) break
        }
        return result
    }

    private fun combination(n: Double, r: Double): Double {
        if (n < 0.0 || r < 0.0 || n != floor(n) || r != floor(r) || n < r) {
            throw ArithmeticException("Combination nCr is only defined for integers with n >= r >= 0")
        }
        if (r == 0.0 || n == r) return 1.0
        var rVal = r
        if (rVal > n - rVal) {
            rVal = n - rVal
        }
        var result = 1.0
        val steps = rVal.toLong()
        for (i in 1..steps) {
            result = result * (n - rVal + i) / i
        }
        return round(result)
    }
}
