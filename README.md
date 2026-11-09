# MeiTraumgeschichten 🌙

Eine native Android-App für gemeinsame Vorleseabende. Ein Erwachsener liest die Geschichte auf dem Bildschirm. Passende Geräusche erklingen beim Vorlesen automatisch. Alle Geschichten, Klänge und die deutsche Spracherkennung sind in der APK enthalten.

## Erste Version · 0.1.0

| Geschichte | Thema | Alter | Vorlesedauer |
| --- | --- | --- | --- |
| Milo und die Laterne für alle | Hilfe annehmen | ab etwa 4 | etwa 11 Minuten |
| Lumi und das unbekannte Rascheln | Mit Angst umgehen | ab etwa 4 | etwa 11 Minuten |
| Fiete und der schiefe Turm | Wut und Versöhnung | ab etwa 5 | etwa 11 Minuten |

Die Zeiten beruhen auf ungefähr 115 Wörtern pro Minute mit ruhigen Pausen. Jede Geschichte hat zwölf Klangstellen, einen sanften Schluss und eine freiwillige Gesprächsfrage. Die Geschichten, Figuren und synthetisierten Geräusche sind eigene Inhalte. Die App verwendet keine Readmio-Inhalte.

### Vorlesen

1. Geschichte öffnen und **Vorlesen starten** wählen.
2. Mikrofonfreigabe bestätigen. Beim ersten Start wird das mitgelieferte Sprachmodell lokal entpackt; dafür ist keine Internetverbindung nötig.
3. Normal vorlesen. Die App vergleicht erkannte Wörter mit kurzen Ankerphrasen im aktuellen und folgenden Abschnitt. Ein Geräusch wird automatisch höchstens einmal je Lesedurchgang ausgelöst.
4. Jeder Klang kann auch ohne Mikrofon mit **♪** abgespielt werden. **Hier weiterlesen** setzt die Erkennung an einen anderen Abschnitt; bereits gespielte Klänge werden dabei nicht automatisch wiederholt.

Die automatische Erkennung braucht die Ankerphrase in erkennbarer Wortfolge. Sie toleriert bis zu zwei eingeschobene Wörter, aber keine beliebige Umformulierung oder jedes ausgelassene Wort. Bei verpasster Stelle kann man den Klang antippen oder die Position setzen. Kinderstimmen im Hintergrund und Lautsprecherlautstärke können die Erkennung beeinflussen; echte Vorleseabende auf verschiedenen Geräten bleiben dafür der entscheidende Praxistest.

- Schriftgröße von 17 bis 33 sp, warmer Nachtmodus mit reduzierter Helligkeit.
- Einstellbare Effektlautstärke; die letzten beiden Klänge sind zusätzlich leiser.
- Lesefortschritt bleibt pro Geschichte erhalten; Wiederholen und Zurücksetzen sind möglich.
- Mikrofon und Klänge stoppen beim Verlassen der App und bei einer Audiounterbrechung. Fortsetzen erfolgt bewusst über die Starttaste.
- Keine Anmeldung, Werbung, Sprachaufzeichnung oder Analyse. Nur Updateprüfungen verwenden das Internet.

## Bauen und testen

Voraussetzungen: JDK 17 mit Compiler, Android SDK 35, Python 3, Netzwerkzugriff beim ersten Build. Android Studio kann das Projekt direkt öffnen.

```sh
./gradlew :core:check
bash scripts/test-update-contract.sh
python3 scripts/generate_sounds.py
python3 -m unittest discover -s tests -p 'test_*.py' -v
./gradlew :app:assembleGithubDebug :app:lintGithubDebug :app:lintPlayRelease
./gradlew :app:connectedGithubDebugAndroidTest
```

Der Build lädt das deutsche Vosk-Modell `vosk-model-small-de-0.15` (etwa 45 MB als ZIP) einmalig herunter, prüft seinen fest hinterlegten SHA-256-Wert und verpackt es in die App. Auf dem Gerät wird keine Datei aus dem Internet geladen. Der erste Start braucht zusätzlichen lokalen Speicher für das entpackte Modell. Die APK enthält ARM32, ARM64 und x86-Bibliotheken; Mindestversion ist Android 8 / API 26.

Für einen Build ohne erneuten Modelldownload kann `VOSK_MODEL_ARCHIVE` auf ein bereits vorhandenes, unverändertes Modell-ZIP zeigen. Modell und Buildausgaben gehören nicht ins Git-Repository. Die zwölf WAV-Effekte werden aus dem eingecheckten Syntheseskript bei jedem Build reproduzierbar erzeugt. Manuell geht dies mit `python3 scripts/generate_sounds.py`.

### Projektstruktur

| Pfad | Aufgabe |
| --- | --- |
| `core/` | Android-unabhängige Geschichtenprüfung, Wortabgleich und Lesefortschritt |
| `app/src/main/assets/stories/` | Eigene Geschichten mit Metadaten und Klangankern |
| `app/src/main/assets/sounds/` | Originale kurze WAV-Effekte mit Ein- und Ausblendung |
| `app/src/main/java/de/haberland/meitraumgeschichten/` | Native Oberfläche, lokale Spracherkennung, Audiowiedergabe |
| `app/src/main/java/de/meiapps/updates/` | Gemeinsame geprüfte MeiApps-Updatekomponente |
| `docs/` | Format, Datenschutz, Updates, Abnahmetest und Fremdkomponenten |

### CI und Testabdeckung

GitHub Actions prüft die Leselogik mit JUnit und erzwingt **mindestens 85 % Zeilenabdeckung im `core`-Modul** über JaCoCo. Die Quote bezieht sich ausdrücklich auf den Android-unabhängigen Kern, nicht auf die gesamte App.

Außerdem laufen die gemeinsamen 27 Updateprüfungen, Asset- und Manifestprüfungen, Android Lint für beide Vertriebswege sowie ein Emulatorlauf unter Android 15 / API 35. Der Emulator testet das Öffnen, manuelle Klangwiedergabe, Fortsetzen, Mikrofonablehnung und das tatsächliche Initialisieren des mitgelieferten deutschen Sprachmodells mit Start/Stopp des Mikrofons. Testberichte, Abdeckung und Emulatorergebnisse werden als Actions-Artefakte gesichert.

## Updates und Veröffentlichungen

Die gleiche Quellenlogik wie bei den anderen MeiApps: Google Play installiert → Google Play aktualisiert; direkt installiert → GitHub aktualisiert. Ein unbekanntes Ergebnis der Installationsquellenabfrage schaltet GitHub nicht versehentlich frei. Einzelheiten in [docs/UPDATES.md](docs/UPDATES.md).

Für automatische, dauerhaft aktualisierbare APKs muss einmalig das Repository-Secret **`MEI_SIGNING_JSON`** mit einer privaten, gesicherten Signieridentität hinterlegt werden. Ohne dieses Secret laufen alle Qualitätsprüfungen, aber die CI veröffentlicht keine APK als Update. Kurzlebige Debug-APKs aus CI eignen sich nur zur technischen Validierung.

Nach allen grünen Prüfungen baut der Releasejob eine signierte GitHub-Release-APK und veröffentlicht sie mit unveränderlicher Versionsadresse und geprüftem Updatemanifest. Für jede neue Version müssen `versionCode`, `versionName` und `CHANGELOG.md` aktualisiert werden. Der Play-Build benötigt die eigene Play-Veröffentlichung und dortige Signierung.

## Grenzen und nächste Schritte

Die erste Version enthält drei sorgfältig geschriebene Geschichten und kurze synthetische Klänge. Es gibt keine generierten Geschichten im laufenden Betrieb, keine Sprachaufnahme, keine Text-to-Speech-Ausgabe und keine Onlinebibliothek. Die Wirkung der Geschichten wurde nicht pädagogisch evaluiert. Weitere Geschichten und natürlichere Klangaufnahmen lassen sich ergänzen, ohne die Leselogik umzubauen.

Vor einem breiten Release: [Vorlese-Abnahmetest](docs/TESTING.md) auf echten Handys/Tablets durchführen. Die automatische Klangerkennung und subjektive Klangqualität sollten besonders beim gemeinsamen Lesen geprüft werden.
