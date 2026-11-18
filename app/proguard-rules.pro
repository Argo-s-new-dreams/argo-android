# kotlinx.serialization: le classi @Serializable di Argo (incontri e pacchetto) devono restare leggibili
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class blog.argosnewdreams.argo.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class blog.argosnewdreams.argo.**$$serializer { *; }

# sherpa-onnx: la parte nativa legge e scrive i campi delle classi Kotlin via JNI, per nome
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Google ML Kit (OCR e trascrizione): parti interne raggiunte per riflessione.
# Con R8 in modalità «full» la lettura del testo delle foto falliva con un NullPointerException in
# com.google.mlkit.vision.text.internal (0.2.2 sul Galaxy S21).
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_genai_speech.** { *; }
-keep class com.google.android.odml.** { *; }
