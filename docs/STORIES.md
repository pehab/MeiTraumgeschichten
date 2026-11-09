# Geschichtenformat

Dateien: UTF-8 unter `app/src/main/assets/stories/`, Dateiendung `.story`. Die Dateinamen bestimmen die Sortierung und dienen als stabile Fortschrittskennung.

```text
title=Geschichtentitel
subtitle=Ein Satz zur Geschichte.
theme=Freundschaft
character=owl
question=Was würdest du tun?
age=4
---
wind|der wind strich leise|Abendwind
Es wurde Abend. Der Wind strich leise durch die Zweige.
---
chime|ein stern begann zu leuchten|Sternenlicht
Oben am Himmel sahen sie etwas. Ein Stern begann zu leuchten.
```

Metadaten sind Pflicht. `character` kann `dragon`, `owl` oder `hedgehog` für die eingebauten Illustrationen sein. Die Mindestalterangabe liegt zwischen 3 und 12. Jede Szene beginnt mit `sound|anchor|label`, danach folgt der vorlesbare Text. Der Parser prüft mindestens drei Wörter pro Ankerphrase, eindeutige Anker je Geschichte, einen vorhandenen Sound und dass die Phrase im Text vorkommt.

Soundkennungen: `wind`, `rain`, `steps`, `knock`, `owl`, `river`, `chime`, `rustle`, `snore`, `bell`, `creak`, `breathe`.

Die Erkennung beachtet Wortgrenzen, normalisiert Groß-/Kleinschreibung, Umlaute und ß und toleriert höchstens zwei zusätzlich erkannte Wörter innerhalb der Phrase. Sie prüft den aktuellen und folgenden Anker. Weiter entfernte Stellen lösen nichts aus. **Hier weiterlesen** ist die bewusste Möglichkeit, größere Sprünge zu machen.

Für einen zehnminütigen Vorleseabend etwa 1.100–1.300 Wörter planen. Wenige gezielte Effekte geben der eigenen Stimme Raum. Die letzten Szenen sollten ruhig ausklingen. Gesprächsfragen bleiben freiwillig; Figuren dürfen mehrere Gefühle zugleich haben und müssen Probleme nicht durch eine Moralrede lösen.
