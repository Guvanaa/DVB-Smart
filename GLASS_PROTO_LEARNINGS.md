# Liquid Glass Prototyp – Erkenntnisse

## Funktionsfähige Parameter (GlassModifiers.kt)

```kotlin
blur(26f)
vibrancy()                                    // nur API 33+
lens(refractionHeight = 50f, refractionAmount = 100f)  // nur API 33+
```

## Was die Parameter bedeuten

### `blur(radius)`
Weichzeichner-Stärke auf dem Backdrop-Inhalt. `26f` ist ein guter Mittelwert – sichtbar ohne zu stark zu sein.

### `lens(refractionHeight, refractionAmount)`
- **`refractionHeight`** – Wie viele Pixel tief die Brechung vom Rand ins Element geht.
  - `50f` = nur die äußere ~50px-Zone wird gebrochen → wirkt wie echter Glasrand ✓
  - `300f` = fast die gesamte Kartenfläche → erzeugt unnatürliche eckige Verzerrung ✗
- **`refractionAmount`** – Stärke der Verschiebung in Pixeln an der Kante.
  - `100f` mit `refractionHeight = 50f` = gut sichtbar, aber natürlich ✓
  - `130f+` = zu stark, wirkt kaputt ✗

> **Physikalisch korrekt:** Echtes Glas bricht Licht stärker an den Rändern (Kante) als in der Mitte. `refractionHeight = 50f` simuliert genau das.

## Kern-Erkenntnis: AndroidView-Einschränkung

Der `lens`/`blur`-Effekt arbeitet auf dem **Backdrop-Layer** – also dem Compose-Layer der als Quelle markiert ist (`layerBackdrop`).

**AndroidView (Mapbox MapView) kann NICHT als Backdrop-Quelle dienen**, weil es in einem separaten Hardware-Layer rendert den Compose nicht auslesen kann. Deshalb:

- Im aktuellen DVB-Smart: Backdrop-Quelle = dunkles Compose-Overlay → Brechung unsichtbar
- Im Prototyp: Backdrop-Quelle = echte Compose-Shapes → Brechung klar sichtbar

## Was für eine echte Implementierung nötig wäre

Um den vollen Liquid-Glass-Effekt (Blur + sichtbare Brechung) auf der echten App-Karte zu erreichen:

### Option A: Compose-native Karte (empfohlen)
- Mapbox durch eine Compose-native Kartenlibrary ersetzen
- Dann kann die Karte direkt als `layerBackdrop`-Quelle dienen
- Voller Blur + Brechung + Refraktion über echtem Karteninhalt

### Option B: Map-Screenshot als Compose-Image (Workaround)
- Periodisch ein Bitmap der MapView erstellen und als `Image`-Composable rendern
- Dieses Image als Backdrop-Quelle nutzen
- **Nachteil:** Laggt sichtbar bei Kartenbewegung

### Option C: Nur dekorativer Glaseffekt (ohne Library)
- Gradient-Ränder + Specular Highlights in reinem Compose
- Kein echter Blur/Brechung nötig → Kyant0 unnötig
- Sieht gut aus, ist aber "fake"

## Prototyp-Architektur (funktioniert)

```kotlin
Box(modifier = Modifier.fillMaxSize()) {

    // 1. Backdrop-Quelle: Compose-Content mit layerBackdrop markieren
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .background(Color(0xFF0D1B2A))
    ) {
        // Beliebige Compose-Elemente hier = werden geblurrt/gebrochen
    }

    // 2. Glass-UI darüber mit CompositionLocalProvider
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        // GlassCards hier zeigen echten Effekt
    }
}
```

## Shader-Verhalten (aus Bytecode-Analyse)

Der AGSL-Shader in Kyant0 wendet Brechung NUR innerhalb von `refractionHeight` Pixeln vom Rand an:

```glsl
if (-sd >= refractionHeight) {
    return content.eval(coord); // Kein Effekt im Zentrum!
}
// Brechung nur im Randbereich:
float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
```

Das erklärt warum kleine Elemente (Zurück-Button) den Effekt besser zeigen als große Karten – bei kleinen Elementen ist der `refractionHeight`-Bereich relativ zur Gesamtfläche größer.
