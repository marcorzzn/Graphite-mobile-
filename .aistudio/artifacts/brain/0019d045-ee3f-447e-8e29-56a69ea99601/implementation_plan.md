# Piano di Sviluppo Completo: Graphite Mobile Unified Creative Suite (Picsart + Canva + Figma + Graphite Guide)

## 1. Visione Architetturale e Principi Fondamentali

L'obiettivo del progetto è evolvere **Graphite Mobile (VectorCraft)** in una suite creativa universale **100% offline-first, deterministica e priva di dipendenze cloud o modelli di IA generativa**, unificando in un unico motore procedurale non distruttivo:
1. **Picsart Engine**: Fotoritocco avanzato, filtri LUT deterministici, segmentazione e rimozione sfondo matematica, collage a griglia/forma libera, pittura raster multi-pennello e montaggio video locale (Media3/Canvas).
2. **Canva Engine**: Libreria di template locali con segnaposto dinamici, Brand Kit locale con blocco stili, presentazioni multi-slide con note relatore ed export PDF, Magic Resize deterministico e pianificatore editoriale offline.
3. **Figma & FigJam Engine**: Vector Networks, Auto Layout reattivo, Constraints, Componenti Master con Varianti e Override, Prototipazione interattiva con Smart Animate, Handoff (CSS / Jetpack Compose), Collaborazione offline con Version History & Visual Diff, e lavagna infinita FigJam.
4. **Graphite Guide (Assistente Esperto Deterministico Non-IA)**: Sistema esperto basato su regole, dizionario lessicale controllato (~1.000 termini), parser sintattico a dipendenze (`Verbo + Oggetto + Modificatori`), consapevolezza del contesto in tempo reale, evidenziazione guidata della UI (Spotlight), pillole di azione rapida contestuali a 1-tap e selettore a 3 stati nella barra superiore (**Attivo**, **Modalità Nascosta Push-to-Talk**, **Disattivato**).

---

## 2. Architettura Dati Unificata e Persistenza Locale (Room SQLite + DAG)

### 2.1 Grafo Procedurale Ibrido (Unified Node Graph & Layer Stack)
Ogni documento (`UnifiedProjectDocument`) combina una gerarchia di **Frame / Slide / Artboard** con un **Grafo Diretto Aciclico (DAG)** di nodi non distruttivi:
- **Nodi Vettoriali & Layout**: `PathNode`, `VectorNetworkNode`, `BooleanOpNode`, `FrameNode` (con `AutoLayoutSpec`, `ConstraintsSpec`, `LayoutGridSpec`), `ComponentMasterNode`, `ComponentInstanceNode` (con mappa di `PropertyOverrides`).
- **Nodi Raster & Fotoritocco**: `BitmapSourceNode`, `CropTransformNode` (proporzioni, raddrizza orizzonte, riempimento bordi specchiato/clonato), `TonalAdjustmentNode` (14 parametri + Curve RGB), `RetouchMaskNode` (Clone Stamp, Patch, Chroma Key, Magic Wand, Graph-Cut), `FilterEffectNode` (LUT, distorsioni, luce, texture, schizzo a carboncino), `RasterBrushLayerNode` (con Blend Modes).
- **Nodi Template, Collage & Presentazione**: `CollageGridNode`, `PlaceholderSlotNode`, `SlideTransitionNode`, `SpeakerNotesNode`.
- **Nodi Video & Timeline**: `VideoClipNode`, `VideoTransitionNode`, `AudioTrackNode`, `SpeedEnvelopeNode`.
- **Nodi Collaborazione & FigJam**: `CommentAnchorNode`, `SnapshotCommitNode`, `StickyNoteNode`, `ConnectorEdgeNode`, `StampNode`.

### 2.2 Database Locale Room SQLite (`AppDatabase`) e Bundle Preinstallato
Tutta la persistenza risiede sul dispositivo tramite **Room SQLite** (con KSP e Repository reattivi `Flow<T>`) affiancato da un pacchetto di asset locali preinstallati (`assets/templates/`, `assets/luts/`, `assets/stickers/`, `assets/stock/`):
- `TemplateEntity`: Template preinstallati e personali indicizzati per categoria (Instagram Post/Story/Reel, Facebook, LinkedIn, YouTube, Poster, Biglietti da visita, Inviti, CV, Presentazioni), formato, colore dominante e stile.
- `BrandKitEntity`: Palette brand (fino a 10 colori), set tipografici locali (fino a 5 font `.ttf`), loghi (SVG/PNG) e flag `isStyleLocked`.
- `ComponentLibraryEntity`: Componenti UI/UX salvati con varianti di stato (`Default`, `Hover`, `Pressed`, `Disabled`) esportabili come pacchetto locale `.gcomp`.
- `VersionSnapshotEntity` & `CommentEntity`: Cronologia immutabile delle versioni con metadati di diff visuale, commenti ancorati a coordinate `(x, y)` e stato (`Aperto`, `Risolto`).
- `ScheduledPostEntity`: Calendario editoriale offline con promemoria locali e modelli di didascalie deterministiche per categoria.

---

## 3. Direzione UI/UX e 4 Modalità Operative Contestuali

Per mantenere l'interfaccia mobile pulita, reattiva ed ergonomica (rispettando target tattili minimi di `48.dp`, `WindowInsets` edge-to-edge e i livelli di complessità `EASY`, `PRO`, `ZEN`), l'area di lavoro adotta un **Mode Switcher Contestuale** nella barra superiore abbinato a pannelli inferiori a schede scorrevoli:

1. **Modalità Foto & Video (Picsart Focus)**:
   - Barra strumenti contestuale: *Ritaglia/Prospettiva, Regolazioni Tonali & Curve, Ritocco & Rimozione Sfondo, Filtri LUT & Effetti, Pennelli Raster, Collage, Video Timeline*.
   - Comparatore istantaneo **Prima / Dopo** attivabile tenendo premuto sulla tela e istogramma RGB live.
2. **Modalità Design & Social (Canva Focus)**:
   - Barra strumenti contestuale: *Template Locali, Brand Kit, Testo & Effetti Curvi, Sticker & Libreria Asset, Presentazioni & Slide, Magic Resize, Calendario Post*.
   - Navigatore multipagina a schede per storyboard e presentazioni con modalità relatore a schermo intero.
3. **Modalità UI/UX & FigJam (Figma Focus)**:
   - Barra strumenti contestuale: *Frame Dispositivi, Penna & Vector Network, Auto Layout & Constraints, Componenti & Varianti, Stili Globali, Prototipo & Hotspot, Ispezione Codice (CSS/Compose), Commutatore Lavagna FigJam*.
   - Pannello laterale/inferiore per Commenti Offline, Version History e confronto Diff visivo.
4. **Modalità Disegno & Procedurale (Graphite Core Focus)**:
   - Barra strumenti contestuale: *Pennelli Vettoriali/Raster, Gomma Pixel/Tracciato, Secchiello con Tolleranza, Pathfinder Booleano, Grafo Nodi DAG Non Distruttivo*.

---

## 4. Architettura di "Graphite Guide" (Assistente Interno Deterministico Non-IA)

**Graphite Guide** opera interamente in locale con latenza `< 5 ms`, zero chiamate di rete e comportamento 100% riproducibile:

- **Dizionario Lessicale Controllato (`GraphiteLexicon`)**: Mappa ~1.000 termini e sinonimi italiani/inglesi a token canonici strutturati in:
  - *Azioni (`ActionVerb`)*: `TAGLIA`, `RUOTA`, `RIMUOVI`, `SFOCA`, `APPLICA`, `ALLINEA`, `RAGGRUPPA`, `CREA`, `ESPORTA`, ` CONVERTI`, `RIDIMENSIONA`, `COLLEGA`.
  - *Oggetti (`TargetObject`)*: `SFONDO`, `FOTO`, `TESTO`, `LIVELLO`, `COMPONENTE`, `TEMPLATE`, `COLLAGE`, `CURVA`, `OMBRA`, `BORDO`, `FRAME`, `VIDEO`, ` BRAND_KIT`.
  - *Modificatori (`ModifierToken`)*: `ROSSO`, `TRASPARENTE`, `16_9`, `QUADRATO`, `VERTICALE`, `AUTOMATICO`, `PDF`, `SVG`, `MP4`, `CSS`.
- **Parser Deterministico a Regole (`RuleBasedCommandParser`)**: Analizza l'input testuale (o la trascrizione vocale offline di sistema) estraendo la tripla `(ActionVerb, TargetObject, List<ModifierToken>)` e risolvendo i riferimenti impliciti tramite lo stato corrente della selezione (`ContextState`).
- **Motore di Esecuzione, Spotlight UI e Suggerimenti Proattivi**:
  - **Spotlight Visivo**: Evidenzia con un anello pulsante il pulsante/pannello esatto nella UI o apre direttamente il pannello richiesto pre-impostando i parametri.
  - **Trigger Proattivi Non-Intrusivi**: Rileva pattern operativi (es. selezione di ≥ 3 oggetti non allineati -> propone *"Allinea e distribuisci / Crea Auto Layout"*; caricamento foto ritratto -> propone *"Rimozione sfondo Chroma/Graph-Cut o Ritocco Viso"*).
  - **Pillole Dinamiche a 1-Tap**: Barra contestuale sopra la toolbar inferiore con 3–4 azioni rapide calcolate deterministicamente in base a modalità attiva e tipo di livello selezionato.
- **Controllo Stato a 3 Modalità nella Top Bar**:
  - `ON (Attivo)`: Barra comandi rapida visibile + pillole contestuali + suggerimenti proattivi.
  - `HIDDEN (Modalità Nascosta Push-to-Talk)`: Nessun elemento a schermo; un'unica icona compatta nella top bar che, se tenuta premuta o toccata, apre l'input rapido ed esegue il comando nascondendosi subito dopo.
  - `OFF (Disattivato)`: Assistente completamente spento per massimizzare lo spazio sulla tela.

---

## 5. Roadmap Sequenziale di Sviluppo (Fasi Incrementali)

Seguendo la strategia scelta (**Roadmap sequenziale con rilascio incrementale per fase**), lo sviluppo è articolato in **6 Fasi Strutturali**:

### Fase 1: Fondamenta Architetturali, Database Room, 4 Modalità UI e "Graphite Guide"
*Obiettivo: Costruire l'infrastruttura dati unificata, il selettore delle 4 modalità contestuali e l'assistente deterministico completo.*
1. **Persistenza Room SQLite (`AppDatabase`)**: Creazione di entità, DAO e Repository per Template, Brand Kit, Libreria Componenti, Cronologia Versioni/Commenti e Calendario Post, più caricamento del pacchetto di asset locali preinstallati.
2. **Estensione Modello Dati Unificato (`VectorModels.kt`)**: Aggiunta dei metadati per le 4 modalità operative (`WorkspaceDomainMode`: `PHOTO`, `DESIGN`, `UI_UX`, `DRAWING`), nodi Raster/Video/AutoLayout/Componenti/Prototipi/FigJam.
3. **Motore "Graphite Guide" (`GraphiteGuideEngine.kt`)**: Implementazione del dizionario lessicale, parser a regole, motore di suggerimenti proattivi, pillole dinamiche a 1-tap, Spotlight UI e selettore a 3 stati (`ON`, `PUSH_TO_TALK`, `OFF`) nella Top Bar.
4. **Rifattorizzazione UI Adattiva**: Integrazione del Mode Switcher per le 4 modalità contestuali mantenendo la compatibilità con i livelli `EASY`, `PRO` e `ZEN`.

### Fase 2: Modulo Picsart — Editing Fotografico, Ritocco Deterministico, Filtri LUT e Pennelli Raster
*Obiettivo: Integrare tutti gli strumenti fotografici, di segmentazione matematica e di illustrazione raster.*
1. **Crop, Trasformazioni & Prospettiva**:
   - Ritaglio con preset (`1:1`, `4:5`, `16:9`, `9:16`, libero), griglia dei terzi, raddrizza orizzonte e riempimento intelligente dei bordi tramite specchiatura/clonazione locale.
   - Rotazione 90°/libera, Flip H/V, ridimensionamento Bicubico/Lanczos con blocco proporzioni, correzione prospettica a 4 punti e distorsione lente.
2. **Regolazioni Tonali (14 Controlli) & Curve RGB**:
   - Luminosità, contrasto, esposizione, alte luci, ombre, bianco/nero, saturazione, vibrance, temperatura, tinta, nitidezza (unsharp mask), riduzione rumore (bilateral/median), vignettatura, grana.
   - Editor interattivo **Curve Tonali** (RGB e canali R, G, B singoli) con punti di controllo + contagocce **Bilanciamento del Bianco**.
3. **Ritocco Locale & Rimozione Sfondo Deterministica**:
   - **Clone Stamp** e **Patch Tool** con campionamento da area sorgente adiacente.
   - Rimozione sfondo tramite **Chroma Key** (selettore colore), **Magic Wand** con tolleranza regolabile e **Segmentazione per Contorni (Graph-Cut semplificato locale)**, con sostituzione sfondo (colore, gradiente, immagine, pattern) e **Sfocatura Sfondo** (gaussiana, lente/bokeh, motion blur) con maschera di profondità manuale.
   - **Ritocco Viso Deterministico**: Levigatura pelle (filtro passa-basso con preservazione bordi), rimozione imperfezioni, schiarimento occhi/denti e correzione occhi rossi.
4. **Filtri LUT, Effetti Speciali & Pennelli Raster**:
   - LUT locali (*Vintage, B&N, Seppia, HDR, Cinematografico, Retrò, Pop Art*), effetti di distorsione (*Increspatura, Onda, Sfera, Twirl, Pixelate, Glitch*), effetti luce (*Bagliore diffuso, Raggi solari, Lens Flare, Luce volumetrica*), texture (*Pellicola, Carta, Tela, Gesso, Rumore*) ed effetto **Disegno a Matita/Carboncino** su layer raster.
   - 6 Pennelli Raster (*Matita, Penna, Pennarello, Acquerello con texture, Aerografo, Gessetto*), gomma pixel/tracciato, secchiello con tolleranza e livelli con **Blend Modes** (`Normal`, `Multiply`, `Screen`, `Overlay`, `Darken`, `Lighten`, `ColorDodge`, `Difference`).

### Fase 3: Modulo Picsart (Collage & Video) + Modulo Canva (Template, Brand Kit, Presentazioni & Social)
*Obiettivo: Completare composizione collage, montaggio video locale e l'intero flusso di graphic design, presentazioni e social media.*
1. **Collage & Sticker Locali**:
   - Griglie predefinite (2–9 immagini) con spaziatura, raggio bordi e trascinamento slot; collage a forma libera con mascheratura in forme (*Cerchio, Cuore, Stella, Poligono*); cornici decorative; libreria di sticker SVG/PNG ricercabili per tag/categoria.
2. **Video Editor Locale & Export MP4**:
   - Timeline multi-clip con **Trim**, **Split**, transizioni (*Dissolvenza, Slide, Zoom*), velocità di riproduzione (`0.25x–4x`) con controllo audio, tracce audio locali con volume/fade, overlay di testo/sticker, applicazione LUT video ed esportazione MP4 configurabile (risoluzione e bitrate).
3. **Libreria Template Locali & Brand Kit (Canva Engine)**:
   - Libreria di template preinstallati (Instagram, Facebook, LinkedIn, YouTube, Volantini, Poster, Biglietti da visita, Inviti, CV, Presentazioni) con segnaposto immagine/testo, ricerca per categoria/formato/colore/stile e salvataggio come template personale.
   - **Brand Kit Locale**: Gestione di fino a 10 colori brand, 5 font locali, loghi SVG/PNG, applicazione con 1-tap all'intero design e blocco degli stili brand (`Style Lock`).
4. **Presentazioni Multi-Slide & Social Media Suite**:
   - Gestione slide con transizioni, animazioni di ingresso per elementi, **Modalità Presentazione a schermo intero** con timer e note del relatore, ed export multi-pagina in **PDF** o immagini.
   - **Magic Resize** deterministico (adattamento automatico proporzionale da Post a Story/Reel/Banner), **Pianificatore Post Offline** con calendario e promemoria, **Generatore Didascalie** basato su modelli testuali deterministici ed export con **Watermark opzionale**.

### Fase 4: Modulo Figma — Vector Networks, Auto Layout, Componenti, Prototipazione & Handoff
*Obiettivo: Portare gli strumenti di UI/UX design, design system e prototipazione interattiva al livello professionale.*
1. **Vector Networks, Frame Dispositivi & Griglie**:
   - Evoluzione dello strumento Penna in **Vector Network** (giunzioni multi-segmento, fusione percorsi, maniglie di Bézier indipendenti), preset **Device Frames** (Android, iPhone, iPad, Desktop) con clipping del contenuto e **Layout Grids** (Colonne, Righe, Griglia con gutter e margini).
2. **Auto Layout, Constraints & Stili Condivisi**:
   - Motore **Auto Layout** deterministico (direzione Riga/Colonna, spaziatura, padding, allineamento, distribuzione *Hug / Fill / Fixed*) e **Constraints** di ancoraggio (`Left`, `Right`, `Center`, `Scale`, `Top`, `Bottom`).
   - **Stili Condivisi** (`Text Styles` e `Color Styles`) con propagazione globale istantanea ed effetti avanzati (*Ombra interna/esterna, Bagliore, Sfocatura livello e Background Blur*).
3. **Componenti Master, Varianti & Libreria Locale**:
   - Creazione di **Componenti Master** e **Istanze** collegate, gestione **Varianti di stato** (`Default`, `Hover`, `Pressed`, `Disabled`), **Override** granulari di testo/colore/immagine senza rompere il collegamento al master, ed esportazione/importazione di librerie locali.
4. **Prototipazione Interattiva & Ispezione Codice (Handoff)**:
   - Collegamento tra Frame con **Hotspot e Trigger** (`Tap`, `Drag`, `Long Press`, `Timer`), transizioni (`Instant`, `Dissolve`, `Slide`, `Push`, `Smart Animate` basato su interpolazione di ID livello), overflow scrolling e overlay.
   - **Player Prototipo a schermo intero** con evidenziazione hotspot e registrazione flussi; **Pannello Ispezione Handoff** con misurazione distanze in dp/px e generazione automatica di codice **CSS** e **Jetpack Compose** oltre a export multi-scala (`1x`, `2x`, `3x`, `SVG`, `PDF`).

### Fase 5: Collaborazione Offline, Version History con Visual Diff e Lavagna FigJam
*Obiettivo: Completare i flussi di revisione asincrona locale e brainstorming visuale.*
1. **Commenti Ancorati & Version History Locale**:
   - Pin di **Commenti ancorati** a coordinate o oggetti con autore locale, timestamp, risposte e filtro stato (`Aperto` / `Risolto`).
   - **Snapshot & Version History**: Salvataggio di versioni nominate, **Confronto Visivo (Visual Diff)** sovrapposto o affiancato tra due versioni per evidenziare livelli aggiunti/modificati/rimossi, e **Merge selettivo** di modifiche da file progetto `.gproj` esterni.
2. **Modalità FigJam (Brainstorming su Tela Infinita)**:
   - Tela infinita dedicata con **Post-it (Sticky Notes)** colorati con autore, **Connettori intelligenti** con frecce ancorate agli oggetti, **Timbri ed Emoji** di votazione, **Evidenziatore a mano libera** e **Timer integrato** per sessioni di progettazione.

### Fase 6: Verifica End-to-End, Ottimizzazione Performance e Test Robolectric
*Obiettivo: Garantire stabilità enterprise, fluidità a 60fps e zero regressioni.*
1. **Ottimizzazione Memoria & Caching DAG**: Valutazione pigra (lazy evaluation) e caching delle bitmap intermedie nei nodi raster/filtri per mantenere il rendering fluido anche su progetti complessi.
2. **Validazione e Compilazione**: Esecuzione di `compile_applet` e suite di test unitari/Robolectric (`gradle :app:testDebugUnitTest`) per verificare il parser di Graphite Guide, il motore Auto Layout, la pipeline dei filtri deterministici e l'esportazione vettoriale/raster/codice.
