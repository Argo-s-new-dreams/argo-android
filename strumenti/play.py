"""Google Play Developer API (androidpublisher v3).

  play.py carica internal              carica l'AAB di VERSIONE nel test interno, note dal CHANGELOG
  play.py promuovi internal alpha      porta la stessa versione dal test interno al test chiuso (revisione Google)
  play.py stato                        versioni presenti in ogni traccia

Account di servizio: ~/Documents/ArgoChiavi/play-service-account.json
"""
import re, sys
from datetime import datetime
from pathlib import Path
from google.oauth2 import service_account
from googleapiclient.discovery import build as servizio
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

PACCHETTO = "blog.argosnewdreams.argo"
CREDENZIALI = Path.home() / "Documents/ArgoChiavi/play-service-account.json"
AAB = Path("app/build/outputs/bundle/release/app-release.aab")
MAPPING = Path("app/build/outputs/mapping/release/mapping.txt")


def api():
    cred = service_account.Credentials.from_service_account_file(
        CREDENZIALI, scopes=["https://www.googleapis.com/auth/androidpublisher"])
    return servizio("androidpublisher", "v3", credentials=cred, cache_discovery=False).edits()


def versione():
    build = Path("VERSIONE").read_text().strip()
    nome = re.search(r'versioneMarketing\s*=\s*"([^"]+)"', Path("app/build.gradle.kts").read_text())[1]
    codice = int((datetime.strptime(build, "%Y%m%d.%H%M") - datetime(2026, 1, 1)).total_seconds() // 60)
    return nome, build, codice


def note_dal_changelog(limite=500):
    testo = Path("CHANGELOG.md").read_text()
    m = re.search(r"^## \[.*?$(.*?)(?=^## \[|\Z)", testo, re.S | re.M)
    corpo = re.sub(r"\*\*|`|^### .*$", "", m[1] if m else "", flags=re.M)
    righe = [r.strip() for r in corpo.splitlines() if r.strip()]
    testo = "\n".join(righe)
    return testo if len(testo) <= limite else testo[: limite - 1].rsplit(" ", 1)[0] + "…"


def rilascio(nome, build, codice, stato):
    return {"name": f"{nome} · {build}", "versionCodes": [str(codice)], "status": stato,
            "releaseNotes": [{"language": "it-IT", "text": note_dal_changelog()}]}


def scrivi(traccia, nome, build, codice, carica_file):
    e = api()
    for stato in ("completed", "draft"):
        edit = e.insert(packageName=PACCHETTO, body={}).execute()["id"]
        if carica_file:
            caricato = e.bundles().upload(packageName=PACCHETTO, editId=edit, media_body=MediaFileUpload(
                str(AAB), mimetype="application/octet-stream", resumable=True)).execute()
            if caricato["versionCode"] != codice:
                sys.exit(f"L'AAB ha versionCode {caricato['versionCode']}, VERSIONE dice {codice}: ricompila.")
            if MAPPING.exists():
                e.deobfuscationfiles().upload(packageName=PACCHETTO, editId=edit, apkVersionCode=codice,
                    deobfuscationFileType="proguard", media_body=MediaFileUpload(
                        str(MAPPING), mimetype="application/octet-stream")).execute()
        e.tracks().update(packageName=PACCHETTO, editId=edit, track=traccia, body={
            "track": traccia, "releases": [rilascio(nome, build, codice, stato)]}).execute()
        try:
            e.commit(packageName=PACCHETTO, editId=edit).execute()
            return stato
        except HttpError as err:
            if stato == "completed" and "draft" in str(err).lower():
                continue
            raise


def carica(traccia="internal"):
    nome, build, codice = versione()
    if not AAB.exists():
        sys.exit(f"Manca {AAB}: lancia prima ./gradlew bundleRelease")
    stato = scrivi(traccia, nome, build, codice, carica_file=True)
    if stato == "draft":
        print(f"⚠️ {nome} · {build} caricata come BOZZA in «{traccia}»: in Play Console premi «Avvia l'implementazione».")
    else:
        print(f"✅ {nome} · {build} (codice {codice}) disponibile ai tester di «{traccia}».")


def promuovi(da="internal", a="alpha"):
    nome, build, codice = versione()
    e = api()
    edit = e.insert(packageName=PACCHETTO, body={}).execute()["id"]
    sorgente = e.tracks().get(packageName=PACCHETTO, editId=edit, track=da).execute()
    if not any(str(codice) in r.get("versionCodes", []) for r in sorgente.get("releases", [])):
        sys.exit(f"La versione {codice} non è nella traccia «{da}»: caricala prima lì.")
    e.delete(packageName=PACCHETTO, editId=edit).execute()
    stato = scrivi(a, nome, build, codice, carica_file=False)
    print(f"✅ {nome} · {build} promossa in «{a}» ({stato}). Il test chiuso passa dalla revisione di Google.")


def stato():
    e = api()
    edit = e.insert(packageName=PACCHETTO, body={}).execute()["id"]
    for t in e.tracks().list(packageName=PACCHETTO, editId=edit).execute().get("tracks", []):
        for r in t.get("releases", []):
            print(f"{t['track']:<12} {r.get('name', '?'):<28} {r.get('status')} {r.get('versionCodes')}")
    e.delete(packageName=PACCHETTO, editId=edit).execute()


if __name__ == "__main__":
    comando, *argomenti = sys.argv[1:] or ["?"]
    {"carica": carica, "promuovi": promuovi, "stato": stato}.get(comando, lambda *a: sys.exit(__doc__))(*argomenti)
