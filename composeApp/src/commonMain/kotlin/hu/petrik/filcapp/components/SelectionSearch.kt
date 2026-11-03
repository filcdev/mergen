package hu.petrik.filcapp.components

/**
 * Accent-independent search for Hungarian names and common Latin spellings.
 * The original labels remain unchanged and keep their correct accents.
 * Also supports decomposed Unicode accents (e.g. e + combining acute).
 */
internal fun normalizeSelectionSearch(value: String): String {
    val lowercase = value.lowercase()
    return buildString(lowercase.length) {
        for (character in lowercase) {
            when (character) {
                'á', 'à', 'â', 'ä', 'ã', 'å', 'ā', 'ă', 'ą' -> append('a')
                'ç', 'ć', 'č' -> append('c')
                'ď', 'đ' -> append('d')
                'é', 'è', 'ê', 'ë', 'ē', 'ě', 'ę' -> append('e')
                'í', 'ì', 'î', 'ï', 'ī' -> append('i')
                'ł' -> append('l')
                'ñ', 'ń', 'ň' -> append('n')
                'ó', 'ò', 'ô', 'ö', 'ő', 'õ', 'ø', 'ō' -> append('o')
                'ř' -> append('r')
                'ś', 'š', 'ş' -> append('s')
                'ť', 'ţ' -> append('t')
                'ú', 'ù', 'û', 'ü', 'ű', 'ū' -> append('u')
                'ý', 'ÿ' -> append('y')
                'ž', 'ź', 'ż' -> append('z')
                in '\u0300'..'\u036f' -> Unit
                else -> append(character)
            }
        }
    }
}
