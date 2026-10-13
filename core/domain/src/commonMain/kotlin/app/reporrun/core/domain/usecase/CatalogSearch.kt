package app.reporrun.core.domain.usecase

/**
 * Cómo se busca un aparato por lo que la persona teclea. El catálogo trae el nombre en español
 * y, como alias, otras formas de decirlo y el nombre en inglés que rotula el gimnasio: se busca
 * en todos.
 *
 * Se ignoran las mayúsculas, los acentos y la diéresis («maquina» encuentra «máquina»). **La ñ
 * no es una n con acento**: es otra letra, así que «pina» no encuentra «piña».
 */
object CatalogSearch {
    /**
     * [text] en minúsculas y sin acentos, con la ñ intacta. Todo lo que no es letra ni dígito
     * queda como un espacio, y los espacios seguidos, como uno: «Chest-Supported Row» se
     * encuentra escribiendo «chest supported».
     */
    fun normalize(text: String): String = text.lowercase()
        .map(::fold)
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    /** El texto donde se busca un aparato: su nombre y sus alias, ya normalizados. */
    fun index(name: String, aliases: List<String> = emptyList()): String =
        (listOf(name) + aliases).joinToString(INDEX_SEPARATOR, transform = ::normalize)

    /**
     * `true` si cada palabra de [query] aparece en [index], que viene de [CatalogSearch.index].
     * Las palabras pueden estar en cualquier orden y repartidas entre el nombre y los alias. Una
     * consulta en blanco lo encuentra todo.
     */
    fun matches(index: String, query: String): Boolean =
        normalize(query).split(' ').all { word -> word.isEmpty() || word in index }

    private fun fold(char: Char): Char = when (char) {
        'á', 'à', 'â', 'ä', 'ã' -> 'a'
        'é', 'è', 'ê', 'ë' -> 'e'
        'í', 'ì', 'î', 'ï' -> 'i'
        'ó', 'ò', 'ô', 'ö', 'õ' -> 'o'
        'ú', 'ù', 'û', 'ü' -> 'u'
        else -> if (char.isLetterOrDigit()) char else ' '
    }

    /** Separa el nombre de cada alias: una palabra de la consulta no casa a caballo entre dos. */
    private const val INDEX_SEPARATOR = "\n"
}
