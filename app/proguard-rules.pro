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
