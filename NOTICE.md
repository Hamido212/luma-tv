# Herkunft und Lizenzhinweise

Grundlage: https://github.com/Ansub/tv-detox

Analysierter Commit: `9e546c2480e1538346dc37f0ed15424d342ff6e3` (abgerufen am 19.10.2026).

TV Detox: Copyright (c) 2026 Ansub Khan, MIT. Der vollständige ursprüngliche Lizenztext ist unverändert in LICENSE enthalten. Er muss bei der Weitergabe von Quellcode oder Binärdateien der abgeleiteten Software mitgeliefert werden, später etwa auch im Lizenzdialog der App.

Aus `MainActivity.java` wurden diese Ansätze adaptiert: die Abfrage von `LEANBACK_LAUNCHER`/`LAUNCHER`, der App-Start, die HDMI-Filterung und -Beschriftung sowie die HDMI-Player-Fallbacks. Die neuen Klassen tragen entsprechende Herkunftskommentare. Die monolithische Activity und ihr Layout wurden nicht kopiert. Keine Installations-, Deinstallations- oder Paket-Deaktivierungsroutine aus dem Original ist Teil der neuen Anwendung.

Eigene Ergänzungen: getrennte Android-Varianten, Kategorien-Dashboard, Fokussteuerung, lokale Start-Historie, PS4-Zuordnung, Netzwerkstatus, eigenes Design und Dokumentation.

**Hintergründe:** Fuji am Abend, Torii bei Nacht und Kiefern im Nebel sind eigene, für Luma TV gezeichnete Vektorkompositionen (`tools/wallpapers/gen.py`). Sie sind von japanischen Holzschnitten und dem Stil der Omarchy-Hintergründe inspiriert, enthalten aber keine fremden Bilder, Fotos oder nachgezeichneten Werke.

**Shippori Mincho:** Copyright 2021 The Shippori Mincho Project Authors (https://github.com/fontdasu/ShipporiMincho), SIL Open Font License 1.1. Mitgeliefert wird eine auf lateinische Zeichen reduzierte Teilmenge als `app/src/main/res/font/luma_mincho.ttf`. Die Lizenz deklariert keinen Reserved Font Name. Lizenztext: `third-party/SHIPPORI-MINCHO-OFL.txt`.

## Ab Version 0.3: Startbildschirm nach CleanFrame

**CleanFrame:** Copyright (c) 2026 Nathan Clark, MIT (https://github.com/nthnclrk/CleanFrame, abgerufen am 22.10.2026). Übernommen wurden Gestaltung und Verhalten aus `app/style.css`, `app/main.js` und `docs/interaction-behaviour.md`: Maße und Abstände auf der 1920×1080-Bühne, Glas-Dock und Liste, Fokusplatten, Verläufe, Einstellungsseiten mit Vorschau, Home-Editor und die Farbdaten der Fotos (`catalog.js`, `adaptive-ink.js`). Der Code ist eine eigene Java-Neuumsetzung für Android (`app/src/main/java/de/hamid/lumatv/stage/`), CleanFrame selbst ist eine Tizen-Web-App. Lizenztext: `third-party/CLEANFRAME-MIT.txt`.

**Fotos:** Die 15 Hintergrundfotos stammen von Unsplash und stehen unter der Unsplash-Lizenz (https://unsplash.com/license). Für Luma wurden sie mittig auf 1920×1080 zugeschnitten und neu komprimiert. Die Fotografen behalten ihre Rechte:

| Hintergrund | Fotograf | Quelle |
|---|---|---|
| Nebelküste (Misty coast) | Ganapathy Kumar | https://unsplash.com/photos/n4VMQZIkgDg |
| Stille Bucht (Quiet cove) | Joshua Wordel | https://unsplash.com/photos/1L_5PT7EKzU |
| Waldnebel (Forest mist) | Dejan Zakic | https://unsplash.com/photos/rr2EF3J5F2Y |
| Bergtal (Mountain valley) | Aniket Deole | https://unsplash.com/photos/M6XC789HLe8 |
| Wüstensterne (Desert stars) | Mohammad Alizade | https://unsplash.com/photos/4wzRuAb-KWs |
| Blaue Gipfel (Blue peaks) | John Rodenn Castillo | https://unsplash.com/photos/rQqWOHZ96OM |
| Brücke am Abend (Bridge at dusk) | Justin Wolff | https://unsplash.com/photos/pbp_Ww2cewA |
| Sonnige Grate (Sunlit ridges) | Cristina Gottardi | https://unsplash.com/photos/wndpWTiDuT0 |
| Goldene Stunde (Golden hour) | Karthik Sreenivas | https://unsplash.com/photos/tmtkhWbgMu8 |
| Sonnige Gipfel (Sunlit summits) | Rudra Gupta | https://unsplash.com/photos/PuTSzJJ-qyU |
| Gold Coast am Abend | City of Gold Coast | https://unsplash.com/photos/YiaGblobOGI |
| Hafen von Sydney | Caleb | https://unsplash.com/photos/JmuyB_LibRo |
| Bernsteinhügel (Amber hills) | Chris Barbalis | https://unsplash.com/photos/vazZtmYFgFY |
| Mintwelle (Mint wave) | MagicPattern | https://unsplash.com/photos/87PP9Zd7MNo |
| Alpenglühen (Alpine glow) | Cristina Gottardi | https://unsplash.com/photos/CSpjU6hYo_0 |

**Marken-Icons:** Simple Icons, CC0 1.0 (`third-party/SIMPLE-ICONS-CC0.txt`; Netflix, YouTube, Prime Video, Stremio, Apple TV, Spotify über CleanFrame, dazu Jellyfin, VLC, PlayStation, RTL, Google Play, AdGuard, Plex, Kodi, Twitch, Crunchyroll, DAZN aus simple-icons 16.35.0). Disney+ aus Custom Brand Icons, MIT (`third-party/CUSTOM-BRAND-ICONS-MIT.txt`). Die Umrisse für „Alle Apps“, „Einstellungen“ und „Home“ sind CleanFrame-Originale (MIT). Umgewandelt mit `tools/icons/convert.py` in Android-Vektoren. Die Marken gehören ihren Inhabern; sie kennzeichnen nur die jeweils installierte App, eine Verbindung zu den Anbietern besteht nicht.

**Schriften:** Manrope (The Manrope Project Authors), Geist (Vercel) und Newsreader (Production Type), jeweils SIL Open Font License 1.1, unverändert als variable Schriften in `app/src/main/assets/fonts/`. Lizenztexte: `third-party/MANROPE-OFL.txt`, `GEIST-OFL.txt`, `NEWSREADER-OFL.txt`.

App-Banner und -Symbole auf den Kacheln stammen zur Laufzeit von den jeweils installierten Apps selbst. Sie sind nicht Teil dieses Projekts, und es wird keine Verbindung zu diesen Anbietern behauptet.

## Ab Version 0.4: Beiträge aus Horizon Home

**Horizon Home:** Copyright (c) 2026 danilll0001, MIT (https://github.com/danilll0001/luma-tv, ein Fork von Luma TV, Stand Commit `b2fe605` vom 25.10.2026). Übernommen wurden:

- die Begrüßung unter der Uhr mit einstellbarem Namen (`Look`, `StageActivity`, `StageView`, `HomePainter`, Texte `st_greeting*` und `st_name*`). Abweichend vom Fork ist sie in Luma standardmäßig ausgeschaltet;
- die russische Oberfläche (`app/src/main/res/values-ru/strings.xml`), mit „Luma“ statt „Horizon Home“;
- der Build-Workflow für GitHub Actions (`.github/workflows/build.yml`), beschränkt auf `main` und Pull Requests.

Nicht übernommen wurden Name, Logo, Banner und Paketname von Horizon Home sowie der „Eingänge“-Knopf auf Home.

Gradle Wrapper 8.9: aus der lokal vorhandenen offiziellen Gradle-Distribution erzeugt, Apache License 2.0, siehe `third-party/GRADLE-LICENSE.txt`. Gradle ist ein Build-Werkzeug und keine Laufzeitabhängigkeit der TV-App.
