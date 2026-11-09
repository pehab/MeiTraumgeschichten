# Updatekonzept

Die Java-Komponente unter `de.meiapps.updates` wurde aus MeiOCRWorkout übernommen. Quellenentscheidung und Downloadprüfung werden durch die bestehenden Hosttests abgesichert.

| Installation | Updateweg |
| --- | --- |
| Google Play (`com.android.vending`) | Play In-App Updates / Play-Store-Seite |
| APK, ADB oder anderer erfolgreich ermittelter Installer | GitHub-Feed |
| Quellenabfrage schlägt fehl | Kein GitHub-Fallback |

## Varianten

- `github`: APK-Installation mit Freigabe über die Android-Einstellungen. Nur diese Variante deklariert `REQUEST_INSTALL_PACKAGES`.
- `play`: Keine APK-Installationsberechtigung. Falls diese Variante außerhalb von Play installiert wurde, kann der gemeinsame Updater für ein GitHub-Update den Browser öffnen.

Das APK wird auf Dateigröße, SHA-256, Paketname, erwartete Version und dieselbe Signatur wie die bereits installierte App geprüft. Android entscheidet abschließend über die Installation. Lesedaten bleiben bei normalen Updates erhalten.

Feed: `https://github.com/pehab/MeiTraumgeschichten/releases/download/updates/MeiTraumgeschichten-update.json`

## Einmalige Signierkonfiguration

Unter **Settings → Secrets and variables → Actions → New repository secret** das Secret `MEI_SIGNING_JSON` hinterlegen. Es verwendet das gleiche Format wie die anderen MeiApps:

```json
{
  "keystore": "BASE64_DES_PRIVATEN_KEYSTORES",
  "password": "STORE_PASSWORT",
  "alias": "KEY_ALIAS",
  "keyPassword": "KEY_PASSWORT"
}
```

Den Keystore und die Passwörter außerhalb des Repositories sicher aufbewahren. Niemals in Dateien im Git-Repository oder in öffentlichen Issues hinterlegen. Eine einmal veröffentlichte Signieridentität muss für weitere Updates beibehalten werden. Für die neue App darf eine eigene Identität verwendet werden; bestehende Schlüssel anderer Apps dürfen nicht ersetzt werden.

Ohne Signierkonfiguration baut CI technische Debug-Artefakte, veröffentlicht aber weder einen produktiven APK-Download noch einen Updatefeed. Die Signierung wird nur im Releasejob auf `main` geladen, nie in Pull Requests.

## Veröffentlichung

Nach erfolgreichen Kern-, Lint- und Gerätetests erzeugt der Releasejob auf `main` die signierte APK. Der Release `github-v<VERSION_CODE>` und dessen APK bleiben unverändert. Der gesonderte `updates`-Release enthält den aktuellen Feed. Ein erneut gestarteter Lauf derselben Versionsnummer ersetzt bereits veröffentlichte APKs nicht. Für eine neue Version den monoton steigenden `versionCode` anheben.

Der Play-Build kann mit `./gradlew :app:bundlePlayRelease` erstellt werden. Die Veröffentlichung im Play Store erfolgt separat. Der Updater ist kein Ersatz für einen eingerichteten Play-Eintrag.
