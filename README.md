# Luma TV

**Ein ruhiger Foto-Startbildschirm für Android TV und Google TV.** Uhr, ein schönes Foto und deine Apps in einem Glas-Dock. Kein Werbe-Karussell, keine Empfehlungen, keine Hintergrunddienste.

*English: A calm photo home screen (launcher) for Android TV / Google TV with a glass dock or a big-type list, 18 wallpapers and on-screen settings with live preview. Native Java, no libraries. Summary at the end.*

![Luma TV: Startbildschirm mit Glas-Dock](docs/screenshots/home-dock.jpg)

Gestaltung und Bedienung folgen [CleanFrame](https://github.com/nthnclrk/CleanFrame) von Nathan Clark, einem Startbildschirm für Samsung-Fernseher (Tizen). Luma setzt das Design nativ für Android TV um. Die technische Basis (App-Liste, Start, HDMI-Eingänge) stammt aus [TV Detox](https://github.com/Ansub/tv-detox) von Ansub Khan.

## Funktionen

- **Dock oder Liste.** Das Dock zeigt bis zu neun Apps unter Glas, die Liste große App-Namen. Apps und Steuerung jeweils als Text, Icons oder beides.
- **Echtes Glas.** Das Dock zeigt den unscharfen Teil des Fotos, der darunter liegt. Wählbar sind Weich, Frost oder Klar. Die Glasflächen werden einmal pro Foto im Hintergrund berechnet, beim Navigieren wird nichts neu weichgezeichnet.
- **18 Hintergründe:** 15 Fotos (Unsplash) und drei eigene japanisch inspirierte Vektorbilder. Alternativ ein heller oder dunkler Hintergrund. Das Foto kann automatisch wechseln.
- **Einstellungen auf dem Fernseher, mit Live-Vorschau:** Hintergrund, Layout, Schrift (Manrope, Geist, Newsreader), Fokus (Glas oder Schrift), Bewegung, Apps auf Home, TV-Einstellungen, HDMI-Eingänge, Uhrformat. Nur OK übernimmt eine Einstellung, der Fokus allein ändert nichts.
- **Alle Apps** als Raster. OK öffnet eine App, OK gedrückt halten nimmt sie auf Home oder entfernt sie.
- **Apps auf Home:** Editor mit zwei Spalten zum Sortieren, Hinzufügen und Entfernen.
- **Spielkonsole per HDMI:** Ein Eingang lässt sich als PlayStation zuordnen und erscheint dann im Dock.
- **Klassische Ansicht:** der frühere Kategorien-Startbildschirm im Holzschnitt-Stil ([Details](docs/design-klassisch.md)).
- **Datensparsam:** Kein Internetzugriff, keine Konten, keine Analyse. Einzige Berechtigung ist `ACCESS_NETWORK_STATE`. Alles bleibt lokal auf dem Fernseher.

## Screenshots

Alle Screenshots stammen von einem echten TCL-Fernseher (Android TV 14).

| | |
|---|---|
| ![Liste](docs/screenshots/home-liste.jpg) | ![Einstellungen](docs/screenshots/einstellungen.jpg) |
| Liste mit großen App-Namen | Einstellungen |
| ![Hintergrund](docs/screenshots/einstellungen-hintergrund.jpg) | ![Hintergrundbilder](docs/screenshots/hintergrundbilder.jpg) |
| Hintergrund mit Live-Vorschau | Hintergrundbilder |
| ![Alle Apps](docs/screenshots/alle-apps.jpg) | ![Apps auf Home](docs/screenshots/apps-auf-home.jpg) |
| Alle Apps | Apps auf Home |

## Installation

Du brauchst ADB (Android Platform Tools) und am Fernseher aktiviertes USB- oder Netzwerk-Debugging.

1. Unter [Releases](../../releases) die APK laden:
   - `luma-tv-…-home.apk` ist der Startbildschirm (mit Home-Filter).
   - `luma-tv-…-preview.apk` ist eine normale App zum Ausprobieren. Sie ändert nichts am Startbildschirm.
2. Installieren:
   ```sh
   adb connect <IP-des-Fernsehers>:5555
   adb install luma-tv-0.3.0-home.apk
   adb shell cmd package set-home-activity --user 0 de.hamid.lumatv/.stage.StageActivity
   ```
3. Home-Taste drücken.

> **Hinweis zu Google TV:** Auf Google-TV-Geräten übernimmt der Google-TV-Startbildschirm die Home-Taste meist trotzdem. Auf dem Testgerät wurde Luma erst zuverlässig zum Startbildschirm, nachdem der Google-TV-Launcher für den Benutzer entfernt (`pm uninstall --user 0 com.google.android.apps.tv.launcherx`) und der Einrichtungsassistent deaktiviert wurde (`pm disable-user --user 0 com.google.android.tungsten.setupwraith`). Das greift ins System ein. Mach das nur, wenn du den Rückweg kennst:
>
> ```sh
> adb shell cmd package install-existing --user 0 com.google.android.apps.tv.launcherx
> adb shell pm default-state --user 0 com.google.android.tungsten.setupwraith
> adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
> ```
>
> Ohne Gewähr. Ein Werksreset stellt ebenfalls alles her, löscht aber alle Daten am Fernseher.

## Bedienung

| Taste | Startbildschirm |
|---|---|
| Links/Rechts (Dock) oder Hoch/Runter (Liste) | App wählen |
| Hoch (Dock) oder Rechts (Liste) | zu „Alle Apps“ und „Einstellungen“ |
| OK | App öffnen |
| OK gedrückt halten oder Menü | Home-Editor bei dieser App öffnen |
| Zurück | schließt eine Ebene nach der anderen |

## Bauen

JDK 17, Android SDK Platform 34. Gradle 8.9 und AGP 8.7.3 sind fest eingestellt. Keine Laufzeitbibliotheken.

```sh
./gradlew :app:assembleHomeDebug :app:assemblePreviewDebug
```

| Variante | Paket | Zweck |
|---|---|---|
| `home` | `de.hamid.lumatv` | Startbildschirm mit Home-Filter |
| `preview` | `de.hamid.lumatv.preview` | normale App zum Testen |

## Projektaufbau

```text
app/src/main/java/de/hamid/lumatv/
  stage/     Startbildschirm ab 0.3: StageActivity, StageView (Fokus, Tasten, Bildschirme),
             HomePainter (Dock, Liste), SheetPainter (Einstellungen, Alle Apps, Editor),
             Scene (Foto und Glas), Walls (Galerie), Marks (Icons), Type (Schriften), Look
  ui/        klassische Ansicht (0.2)
  platform/  App-Katalog, App-Start, HDMI-Eingänge, Einstellungen
app/src/main/assets/   Fotos (1920×1080) und variable Schriften
tools/icons/           SVG → Android-Vektor, Messung der Icon-Maße
tools/wallpapers/      Generator der Vektor-Hintergründe
```

Die ganze Oberfläche zeichnet ein einziger Canvas-View auf einer 1920×1080-Bühne, mit denselben Maßen wie CleanFrame. Die Animationen (180 ms, cubic-bezier(.2, .7, .2, 1)) laufen über eine eigene Uhr. Sie funktionieren deshalb auch, wenn die Systemanimationen ausgeschaltet sind, und lassen sich in den Einstellungen abschalten.

## Getestet

TCL Smart TV Pro, Android TV 14 (Google TV), Oberfläche 1920×1080:

- Kaltstart 1,1 bis 2,1 s.
- Navigation im Dock: Median 14 ms pro Bild, 2,2 % verspätete Bilder.
- Arbeitsspeicher direkt nach dem Start rund 59 MB PSS.

Andere Geräte sind nicht getestet.

## Credits und Lizenzen

- Code: MIT, siehe [LICENSE](LICENSE). Enthält Teile von TV Detox (MIT, Ansub Khan). Design und Verhalten nach CleanFrame (MIT, Nathan Clark).
- Fotos: Unsplash-Lizenz. Fotografen und Links stehen in [NOTICE.md](NOTICE.md).
- Schriften: Manrope, Geist, Newsreader, Shippori Mincho (SIL OFL 1.1).
- Marken-Icons: Simple Icons (CC0), Disney+ aus Custom Brand Icons (MIT). Die Marken gehören ihren Inhabern und kennzeichnen nur die jeweils installierte App. Dieses Projekt steht in keiner Verbindung zu den Anbietern, zu TCL, Google oder Samsung.

Alle Lizenztexte liegen in [`third-party/`](third-party/).

## English summary

Luma TV is a home screen (launcher) for Android TV / Google TV, built natively in Java with no runtime libraries. It ports the look and behaviour of [CleanFrame](https://github.com/nthnclrk/CleanFrame), a Samsung Tizen home screen by Nathan Clark, and features:

- a glass dock or a big-type list;
- 15 Unsplash photos plus three original vector wallpapers;
- live-preview settings, an All apps grid and a two-column Home editor;
- HDMI console shortcuts.

It needs no internet permission and stores everything locally. Install the APK from Releases with `adb`. On Google TV devices the stock launcher may keep the Home key; see the note above, including how to revert. Tested on a TCL Smart TV Pro (Android TV 14). The UI is German and English.
