# Änderungen

## 0.4.0 – 2026-10-29

Robuster als einziger Startbildschirm, sauberer im Detail. Grundlage waren drei unabhängige Prüfungen (Design, Funktionen, Code) des Stands 0.3.

**Neu**
- Einstellungen sichern und wiederherstellen (TV & Luma › Sichern & Wiederherstellen), als Datei im App-Ordner, mit `adb pull`/`adb push` übertragbar.
- Sicherer Modus: Startet Luma dreimal innerhalb einer Minute, ohne ein Bild zu zeigen, kommt Home ohne Foto und Glas. Er endet mit dem nächsten Neustart des Fernsehers oder mit einer neuen Hintergrund-Wahl.
- TalkBack sagt an, was den Fokus hat.
- Lange Einstellungsseiten scrollen zeilenweise.
- Begrüßung, russische Oberfläche und Build-Workflow aus dem Fork Horizon Home von danilll0001 (seit 0.3 auf main, hier erstmals im Release).

**Behoben**
- Dock mit sechs Apps: Die sechste Kachel wurde abgeschnitten und abgeblendet. Jetzt passen sechs Kacheln, im Modus „Icons“ ist das Dock so breit wie seine Icons.
- Liste mit bis zu sechs Apps: keine Abblendung der letzten Zeile mehr.
- Steht der Fokus oben auf „Alle Apps“/„Einstellungen“, wirkt die zuletzt gewählte Kachel nicht mehr heller als ein echter Fokus.
- Kaltstart: kein kurzes „Keine Apps auf Home“ und kein Fokus oben rechts mehr; App-Liste und Foto laden parallel, das Foto blendet ein, im Modus „Bei jedem Öffnen“ wird nur noch ein Bild dekodiert.
- Absturz, wenn eine App deinstalliert wird, während der Editor oder „Alle Apps“ offen ist.
- „Home fasst neun Apps“ kam zu früh, wenn Standard-Apps nicht installiert sind; die letzte sichtbare App ließ sich entfernen.
- Eine in der klassischen Ansicht geänderte PS4-Zuordnung kommt jetzt auch auf Home an.
- Der automatische Bildwechsel startet nach einem Kaltstart wieder.
- „Bei jedem Öffnen“ wechselt das Foto bei jeder Rückkehr auf Home, nicht nur beim Prozessstart.
- Uhr: Zeitzone, Sommerzeit und 12/24 h werden sofort übernommen (System-Zeitsignale statt eigenem Timer), Ziffern springen nicht mehr.
- Updates von Apps lösen nur noch einen Ladevorgang aus; Apps auf USB-Speicher und gesperrte Apps werden erkannt.
- Nach mehr als zehn Minuten in einer App öffnet Home wieder auf Home statt auf einer Unterseite.
- Namensdialog: Home schließt ihn, „Fertig“ auf der TV-Tastatur übernimmt den Namen.
- Zwischen 22 und 5 Uhr heißt die Begrüßung „Guten Abend“ statt „Gute Nacht“.
- Russisch: Knopf „Убрать с главного экрана“ und lange Auswahlwerte passen, der Hinweis in „Alle Apps“ läuft nicht mehr unter den Mehr-Pfeil.
- „Alle Apps“: lange Namen werden erst kleiner und dann gekürzt; die System-Einstellungen heißen „TV-Einstellungen“; zwei gleichnamige Apps werden unterschieden („YouTube · TizenTube“).
- Editor: Die Zeile, auf die „Von Home entfernen“ wirkt, bleibt markiert.

**Intern**
- Einstellungs-Schema mit Migration; das Standard-Home wird beim ersten Start festgeschrieben.
- Tests mit Robolectric (Logik, Regressionen, Bildschirmfotos in drei Sprachen) laufen in GitHub Actions, dazu Prüfungen auf INTERNET-Berechtigung und Laufzeitbibliotheken.
- Release-Signatur über Umgebungsvariablen, Standard ist der Schlüssel von 0.3.0.
