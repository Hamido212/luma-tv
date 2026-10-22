# Klassische Ansicht: Holzschnitt am Abend

Das war der Startbildschirm von Luma TV 0.2 (19.10.2026). Seit 0.3 ist er unter Einstellungen → TV & Luma → Klassische Ansicht erreichbar. Japanisch inspirierte Vektorlandschaften im Stil von Omarchy-Hintergründen sind das gestalterische Hauptelement. Die drei Motive sind in 0.3 auch als Hintergrundbilder des neuen Startbildschirms wählbar.

## Idee

Der Hintergrund ist das Kunstwerk, die Oberfläche tritt zurück. Die Motive erinnern an japanische Farbholzschnitte: flache Farbflächen, eine große Sonnen- oder Mondscheibe, Wolkenbänder und Silhouetten. Die Bedienoberfläche bleibt in Tinte und Elfenbein, damit sie zu jedem Motiv passt. Kühn ist nur ein Element: das Bild. Alles andere ist ruhig.

## Farben

| Name | Wert | Rolle |
|---|---|---|
| Elfenbein | `#EDE6D3` | Texte, Fokusrahmen |
| Nebel | `#A9A6BC` | Datum, Nebentexte |
| Tinte | `#16172A` | Symbol auf fokussiertem Knopf |
| Tintenkarte | `#1B1C30`, 88 % | Kacheln ohne TV-Banner |
| Dialog | `#1C1D33` | Einstellungsdialoge |

Die kräftigen Farben (Abendrot, Zinnober des Torii, Mondlicht) kommen ausschließlich aus dem Hintergrund.

## Schrift

- **Shippori Mincho** (SIL OFL 1.1, auf lateinische Zeichen reduziert, 34 KB) für Begrüßung und Uhr. Die japanische Mincho-Form mit feinen Strichen trägt die Holzschnitt-Anmutung.
- **Roboto** (Systemschrift) für alles Funktionale: Reihentitel 16 sp medium, App-Name 14 sp medium, Datum 14 sp.
- Satzschreibung, keine Versalien-Etiketten, keine dekorativen Trennstriche.

## Aufbau (960 × 540 dp)

```text
Guten Abend                                    (Eingang) (WLAN) (Einstellungen)
21:15                       [Motiv: Sonne/Mond, Gipfel, Torii]
Montag, 19. Oktober

Filme & Serien
[Stremio] [Nuvio] [Netflix] [Prime Video] [Disney+ …
TV & Mediatheken
[Joyn] [RTL+]
```

- Linksbündig, Seitenrand 56 dp. Die Motive liegen rechts oben über der ersten Reihe.
- Uhr 62 sp, darüber eine Begrüßung nach Tageszeit, darunter das Datum.
- Oben rechts drei schlichte Symbole. Mit Fokus werden sie zur Elfenbein-Scheibe, und links daneben erscheint ihr Name.
- Ab der zweiten Reihe rückt die fokussierte Reihe immer auf dieselbe Höhe (118 dp). Darüber blendet der Inhalt weich aus.

## Kacheln und Fokus

- 16:9, 176 × 99 dp, Eckenradius 12 dp. Echte TV-Banner der Apps, randlos.
- Apps ohne Banner bekommen eine Tintenkarte mit Symbol und Namen.
- Fokus: Die Karte wächst um 6 %, hebt sich mit Schatten ab und bekommt einen 2,5 dp Elfenbein-Rahmen mit 5 dp Abstand. Darunter erscheint der App-Name.
- Nicht fokussierte Karten sind um etwa 20 % abgedunkelt.
- Sind die Systemanimationen aus, wechselt der Zustand sofort.

## Hintergründe

| Motiv | Inhalt |
|---|---|
| Fuji am Abend (Standard) | Indigo-Himmel mit Sternen, Abendsonne mit Wolkenbändern, Fuji mit Schneekappe, Pagode, Gartenkiefer |
| Torii bei Nacht | Sternenhimmel, Mondsichel, Torii im See mit Spiegelung, Bergkette im Dunst |
| Kiefern im Nebel | Fünf Bergrücken mit Tannen, zwischen den Schichten Nebel, blasse Sonne |

Alle Motive sind eigene Kompositionen, keine Nachbildungen fremder Wallpaper oder Bilder. Sie entstehen als Vektorgrafik in `tools/wallpapers/gen.py`. Das Skript erzeugt aus einer Beschreibung sowohl eine SVG-Vorschau als auch das Android-VectorDrawable in `app/src/main/res/drawable/wall_*.xml`. Vorschaubilder liegen in `docs/wallpapers/`.

Die App rendert das gewählte Motiv einmal im Hintergrund-Thread in eine Bitmap in Fenstergröße (1920 × 1080) und übergibt sie an die GPU. Danach kostet das Zeichnen eine einzige Textur. Nichts bewegt sich, und auch im Leerlauf wird nichts neu gezeichnet. Umschalten unter Einstellungen & Anpassen → Hintergrund.

## Am Gerät

Auf einem TCL Smart TV Pro (Android TV 14) geprüft. Screenshot: `docs/screenshots/klassisch-fuji.jpg`.
