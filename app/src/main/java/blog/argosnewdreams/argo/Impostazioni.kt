package blog.argosnewdreams.argo

import android.content.Context
import androidx.core.content.edit
import blog.argosnewdreams.argo.trascrizione.Modalita
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferenze dell'utente, sul telefono (escluse dal backup come tutto il resto). */
class Impostazioni(context: Context) {

    private val preferenze = context.getSharedPreferences("argo", Context.MODE_PRIVATE)

    private val _lingua = MutableStateFlow(preferenze.getString("lingua", "it-IT")!!)
    val lingua: StateFlow<String> = _lingua.asStateFlow()

    private val _modalita = MutableStateFlow(
        runCatching { Modalita.valueOf(preferenze.getString("modalita", Modalita.AUTOMATICA.name)!!) }.getOrDefault(Modalita.AUTOMATICA)
    )
    val modalita: StateFlow<Modalita> = _modalita.asStateFlow()

    private val _eliminaAudio = MutableStateFlow(preferenze.getBoolean("eliminaAudio", true))
    /** Promessa di Argo: nessuna registrazione resta sul telefono. Attivo di default. */
    val eliminaAudio: StateFlow<Boolean> = _eliminaAudio.asStateFlow()

    fun impostaLingua(valore: String) { _lingua.value = valore; preferenze.edit { putString("lingua", valore) } }
    fun impostaModalita(valore: Modalita) { _modalita.value = valore; preferenze.edit { putString("modalita", valore.name) } }
    fun impostaEliminaAudio(valore: Boolean) { _eliminaAudio.value = valore; preferenze.edit { putBoolean("eliminaAudio", valore) } }
}
