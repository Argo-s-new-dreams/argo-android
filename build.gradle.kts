// AGP 9 compila Kotlin da sé («built-in Kotlin»): i plugin Kotlin qui sotto fissano solo la versione
// del compilatore (2.4.x, richiesta dalle librerie ML Kit GenAI) e aggiungono Compose e serializzazione.
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
