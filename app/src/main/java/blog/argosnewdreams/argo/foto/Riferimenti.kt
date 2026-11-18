package blog.argosnewdreams.argo.foto

/**
 * Email, siti e telefoni in un testo letto da una foto (biglietti da visita, brochure, cartellini).
 * Regole semplici e prevedibili; tollera i separatori tipici dei numeri (spazi, punti, trattini).
 */
object Riferimenti {

    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}""")
    private val sito = Regex("""(?i)\b(?:https?://\S+|www\.[a-z0-9\-]+(?:\.[a-z0-9\-]+)*\.[a-z]{2,}(?:/\S*)?)""")
    private val telefono = Regex("""(?:\+|\b00)?\(?\d[\d\s./\-()]{6,}\d""")

    fun trova(testo: String): List<String> {
        val trovati = linkedSetOf<String>()
        email.findAll(testo).forEach { trovati += it.value }
        sito.findAll(testo).forEach { trovati += it.value.trimEnd('.', ',', ';', ')') }
        for (riga in testo.lines()) {
            // Partita IVA, codice fiscale, REA, capitale sociale: numeri lunghi che non sono telefoni
            if (datiSocietari.containsMatchIn(riga)) continue
            telefono.findAll(riga.replace(email, " ")).forEach { corrispondenza ->
                val cifre = corrispondenza.value.count { it.isDigit() }
                if (cifre in 8..15) trovati += normalizzaTelefono(corrispondenza.value)
            }
        }
        return trovati.toList()
    }

    private val datiSocietari = Regex("""(?i)\b(p\.?\s?iva|vat|c\.?\s?f\.|cod(ice)?\.?\s?fisc|rea\b|cap(itale)?\.?\s?soc|ust-?id|steuer)""")

    private fun normalizzaTelefono(grezzo: String): String {
        val compatto = grezzo.trim().replace(Regex("""[\s./\-()]+"""), " ").trim()
        return if (compatto.startsWith("00")) "+" + compatto.drop(2).trimStart() else compatto
    }
}
