# Componenti di terze parti

| Componente | Uso in Argo | Licenza | Fonte |
|---|---|---|---|
| NVIDIA Parakeet TDT 0.6B v3 | Modello di trascrizione di «Argo locale», scaricato sul telefono alla prima richiesta (versione int8 per sherpa-onnx, revisione `ed5d745`) | CC-BY-4.0 | [nvidia/parakeet-tdt-0.6b-v3](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3), [csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8](https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8) |
| sherpa-onnx 1.12.21 (con ONNX Runtime) | Motore di inferenza di Argo locale e VAD (`app/libs/sherpa-onnx-1.12.21.aar`) | Apache 2.0 (ONNX Runtime: MIT) | [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx), AAR da [csukuangfj/sherpa-onnx-libs](https://huggingface.co/csukuangfj/sherpa-onnx-libs) |
| Silero VAD | Rilevamento del parlato (`app/src/main/assets/silero_vad.onnx`, dal pacchetto `silero-vad` 6.2.3) | MIT | [snakers4/silero-vad](https://github.com/snakers4/silero-vad) |
| Google ML Kit (GenAI Speech Recognition, Text Recognition) | Trascrizione Avanzata/Base e OCR delle foto | [Termini ML Kit](https://developers.google.com/ml-kit/terms) | Google Maven |
| AndroidX, Jetpack Compose, CameraX, Kotlin, kotlinx | Interfaccia, fotocamera, coroutine, serializzazione | Apache 2.0 | Google Maven, Maven Central |

Attribuzione richiesta da CC-BY-4.0: *Parakeet TDT 0.6B v3* © NVIDIA, usato in versione quantizzata int8 senza altre modifiche. Il testo è riportato anche nell'app (Impostazioni › Crediti).
