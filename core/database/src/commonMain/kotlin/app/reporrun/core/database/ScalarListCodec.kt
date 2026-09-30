package app.reporrun.core.database

/**
 * Colecciones de valores simples (textos, enteros, nombres de enum) como un array JSON en una
 * columna `TEXT`: `["Caminadora","Bici"]`, `[10,9,8]`.
 *
 * No es un blob opaco: el formato es JSON estándar, se lee tal cual en un inspector de SQLite
 * y se puede consultar con `json_each`. Solo se usa para colecciones de escalares que nunca se
 * cruzan con otra tabla; las listas de objetos (días, ejercicios, alternativas) van en tablas
 * hijas y los tipos compuestos (`ExerciseDose`, `CardioPrescription`) en columnas propias.
 *
 * Está escrito a mano para no traer kotlinx-serialization solo por esto. Si el módulo lo
 * adopta más adelante, el formato en disco es el mismo y no hace falta migrar.
 */
internal object ScalarListCodec {
    fun encodeStrings(values: List<String>): String =
        values.joinToString(separator = ",", prefix = "[", postfix = "]") { it.quoted() }

    fun decodeStrings(text: String): List<String> = Parser(text).parseArray { parseString() }

    fun encodeInts(values: List<Int>): String = values.joinToString(separator = ",", prefix = "[", postfix = "]")

    fun decodeInts(text: String): List<Int> = Parser(text).parseArray { parseInt() }

    private fun String.quoted(): String = buildString(length + 2) {
        append('"')
        for (char in this@quoted) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                in CONTROL_CHARS -> append("\\u").append(char.code.toString(HEX).padStart(UNICODE_DIGITS, '0'))
                else -> append(char)
            }
        }
        append('"')
    }

    /** Parser mínimo de un array JSON plano; falla con [IllegalArgumentException] si no lo es. */
    private class Parser(private val text: String) {
        private var index = 0

        fun <T> parseArray(element: Parser.() -> T): List<T> {
            skipWhitespace()
            expect('[')
            skipWhitespace()
            val result = mutableListOf<T>()
            if (peek() == ']') {
                index++
            } else {
                do {
                    skipWhitespace()
                    result += element()
                    skipWhitespace()
                    val separator = next()
                    if (separator != ',' && separator != ']') fail("se esperaba ',' o ']'")
                } while (separator == ',')
            }
            skipWhitespace()
            require(index == text.length) { "texto de sobra tras el array en la posición $index" }
            return result
        }

        fun parseString(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                when (val char = next()) {
                    '"' -> return out.toString()
                    '\\' -> out.append(parseEscape())
                    else -> out.append(char)
                }
            }
        }

        fun parseInt(): Int {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && text[index].isDigit()) index++
            return text.substring(start, index).toIntOrNull() ?: fail("se esperaba un entero")
        }

        private fun parseEscape(): Char = when (val char = next()) {
            '"', '\\', '/' -> char

            'n' -> '\n'

            'r' -> '\r'

            't' -> '\t'

            'b' -> '\b'

            'f' -> '\u000C'

            'u' -> {
                require(index + UNICODE_DIGITS <= text.length) { "escape \\u incompleto" }
                val code = text.substring(index, index + UNICODE_DIGITS).toIntOrNull(HEX) ?: fail("escape \\u inválido")
                index += UNICODE_DIGITS
                code.toChar()
            }

            else -> fail("escape desconocido: \\$char")
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun peek(): Char? = text.getOrNull(index)

        private fun next(): Char = text.getOrNull(index++) ?: fail("fin de texto inesperado")

        private fun expect(char: Char) {
            if (next() != char) fail("se esperaba '$char'")
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException("$message en la posición $index")
    }

    private val CONTROL_CHARS = '\u0000'..'\u001F'
    private const val HEX = 16
    private const val UNICODE_DIGITS = 4
}
