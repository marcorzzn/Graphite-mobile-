package com.example.engine

import com.example.model.CreativeDomainMode
import com.example.model.GraphiteDocumentModel
import com.example.model.GraphiteLayer
import com.example.model.LayerGeometryKind
import com.example.model.SuiteSubPanel
import java.util.Locale

enum class GuideActionVerb(val labelIt: String, val synonyms: List<String>) {
    CROP_CUT("Ritaglia / Taglia", listOf("taglia", "ritaglia", "crop", "accorcia", "trim", "split", "dividi")),
    ROTATE_FLIP("Ruota / Specchia", listOf("ruota", "gira", "specchia", "flip", "raddrizza", "orizzonte", "inclinazione")),
    REMOVE_ERASE("Rimuovi / Scorpora", listOf("rimuovi", "cancella", "togli", "isola", "scontorna", "elimina", "gomma")),
    BLUR_SMOOTH("Sfoca / Leviga", listOf("sfoca", "blur", "bokeh", "leviga", "ammorbidisci", "pelle", "profondita")),
    ADJUST_COLOR("Regola / Correggi", listOf("regola", "correggi", "schiarisci", "scurisci", "bilancia", "sbianca", "contrasto", "esposizione", "luminosita", "curva", "curve")),
    APPLY_FILTER("Applica Filtro / Stile", listOf("applica", "filtro", "lut", "effetto", "vintage", "seppia", "carboncino", "matita", "glitch", "luce", "texture")),
    CREATE_COMPOSE("Crea / Inserisci", listOf("crea", "aggiungi", "inserisci", "nuovo", "genera", "componi", "disegna", "unisci", "sottrai", "interseca", "fondi", "booleana")),
    ALIGN_LAYOUT("Allinea / Auto Layout", listOf("allinea", "centra", "distribuisci", "autolayout", "layout", "griglia", "vincoli", "constraints")),
    RESIZE_ADAPT("Ridimensiona / Adatta", listOf("ridimensiona", "scala", "adatta", "magic", "resize", "formato", "proporzioni")),
    PROTOTYPE_LINK("Collega / Prototipa", listOf("collega", "prototipo", "hotspot", "transizione", "anima", "smart", "presenta", "slide")),
    COLLAB_VERSION("Commenta / Versiona", listOf("commenta", "commento", "versione", "cronologia", "diff", "confronta", "figjam", "postit", "brainstorming")),
    EXPORT_INSPECT("Esporta / Ispeziona", listOf("esporta", "salva", "codice", "css", "compose", "svg", "pdf", "mp4", "handoff", "ispeziona", "pianifica"))
}

enum class GuideTargetObject(val labelIt: String, val synonyms: List<String>) {
    BACKGROUND("Sfondo", listOf("sfondo", "background", "fondale", "chroma", "wand", "bacchetta")),
    PHOTO_IMAGE("Foto / Immagine", listOf("foto", "immagine", "ritratto", "fotografia", "lente", "prospettiva")),
    FACE_EYES_TEETH("Viso / Occhi / Denti", listOf("viso", "pelle", "occhi", "rossi", "denti", "imperfezioni", "clone", "patch", "toppa")),
    TONE_CURVE("Curve Tonali & Luce", listOf("curva", "curve", "rgb", "luci", "ombre", "bianco", "temperatura", "nitidezza", "grana")),
    COLLAGE_STICKER("Collage & Sticker", listOf("collage", "griglia", "cornice", "sticker", "adesivo", "cuore", "stella")),
    VIDEO_CLIP("Video & Audio", listOf("video", "clip", "mp4", "audio", "musica", "velocita", "reel")),
    TEMPLATE_BRAND("Template & Brand Kit", listOf("template", "modello", "brand", "kit", "palette", "logo", "font", "instagram", "youtube", "linkedin", "poster", "biglietto", "cv")),
    PRESENTATION_SLIDE("Presentazione & Slide", listOf("presentazione", "slide", "storyboard", "relatore", "timer", "pdf")),
    SOCIAL_POST("Calendario Social & Didascalie", listOf("social", "post", "calendario", "pianifica", "didascalia", "caption", "watermark")),
    VECTOR_PATH("Tracciato & Booleane", listOf("tracciato", "vettore", "penna", "bezier", "nodi", "booleana", "sottrai", "unisci", "interseca", "network")),
    COMPONENT_VARIANT("Componente & Varianti", listOf("componente", "master", "istanza", "variante", "hover", "pressed", "stile", "stili")),
    FRAME_AUTOLAYOUT("Frame & Auto Layout", listOf("frame", "dispositivo", "iphone", "android", "desktop", "autolayout", "colonna", "riga", "padding")),
    PROTOTYPE_FLOW("Prototipo Interattivo", listOf("prototipo", "flusso", "trigger", "tap", "drag", "overlay", "scroll")),
    FIGJAM_BOARD("Lavagna FigJam", listOf("figjam", "lavagna", "post-it", "sticky", "connettore", "timbro")),
    CODE_HANDOFF("Ispezione Codice CSS/Compose", listOf("css", "compose", "xml", "tokens", "handoff", "sviluppatore", "misure"))
}

enum class GuideModifierToken(val labelIt: String, val keywords: List<String>) {
    CHROMA_KEY("Chroma Key Colore", listOf("chroma", "verde", "blu", "colore")),
    MAGIC_WAND("Magic Wand (Tolleranza)", listOf("wand", "bacchetta", "tolleranza", "contiguo")),
    GRAPH_CUT("Graph-Cut Contorni", listOf("graph", "contorni", "bordi", "preciso")),
    RATIO_1_1("1:1 Quadrato", listOf("1:1", "quadrato", "post")),
    RATIO_9_16("9:16 Verticale Story/Reel", listOf("9:16", "verticale", "story", "reel", "storia")),
    RATIO_16_9("16:9 Orizzontale", listOf("16:9", "orizzontale", "youtube", "slide")),
    LUT_VINTAGE("Filtro Vintage", listOf("vintage", "retrò", "retro", "seppia", "pellicola")),
    LUT_CHARCOAL("Schizzo Matita/Carboncino", listOf("matita", "carboncino", "disegno", "schizzo", "sketch")),
    BLUR_BOKEH("Sfocatura Lente Bokeh", listOf("bokeh", "lente", "gaussiana", "motion")),
    FORMAT_PDF("Esportazione PDF", listOf("pdf", "documento", "stampa")),
    FORMAT_MP4("Esportazione Video MP4", listOf("mp4", "video", "1080p")),
    FORMAT_CSS_COMPOSE("Codice CSS & Compose", listOf("css", "compose", "kotlin", "xml"))
}

data class GuideQuickActionPill(
    val id: String,
    val labelIt: String,
    val badgeIt: String,
    val targetDomain: CreativeDomainMode,
    val targetSubPanel: SuiteSubPanel?,
    val spotlightTag: String,
    val toastExplanationIt: String
)

data class GuideResolutionResult(
    val query: String,
    val matchedVerb: GuideActionVerb,
    val matchedObject: GuideTargetObject,
    val modifiers: List<GuideModifierToken>,
    val titleIt: String,
    val stepByStepIt: String,
    val targetDomain: CreativeDomainMode,
    val targetSubPanel: SuiteSubPanel,
    val spotlightTag: String,
    val instantActionId: String,
    val instantActionLabelIt: String
)

object GraphiteGuideEngine {

    /**
     * Parses a user query in natural Italian/English using a 100% deterministic rule-based
     * lexical + dependency matcher (Zero LLM / Zero Cloud / < 1ms execution).
     */
    fun parseCommand(
        rawQuery: String,
        activeDomain: CreativeDomainMode,
        selectedLayer: GraphiteLayer?
    ): GuideResolutionResult {
        val normalized = rawQuery.lowercase(Locale.ROOT).trim()
        val tokens = normalized.split(Regex("[^a-z0-9:àèéìòù]+")).filter { it.isNotBlank() }

        // 1. Match Action Verb (fallback to context-inferred verb)
        val matchedVerb = GuideActionVerb.entries.maxByOrNull { verb ->
            verb.synonyms.count { syn -> normalized.contains(syn) || tokens.contains(syn) }
        }?.takeIf { verb ->
            verb.synonyms.any { syn -> normalized.contains(syn) }
        } ?: inferVerbFromContext(normalized, activeDomain)

        // 2. Match Target Object (fallback to context-inferred object)
        val matchedObject = GuideTargetObject.entries.maxByOrNull { obj ->
            obj.synonyms.count { syn -> normalized.contains(syn) || tokens.contains(syn) }
        }?.takeIf { obj ->
            obj.synonyms.any { syn -> normalized.contains(syn) }
        } ?: inferObjectFromVerbAndContext(matchedVerb, activeDomain, selectedLayer)

        // 3. Extract Modifiers
        val matchedModifiers = GuideModifierToken.entries.filter { mod ->
            mod.keywords.any { kw -> normalized.contains(kw) }
        }

        return buildDeterministicRuleResolution(
            query = rawQuery.ifBlank { "${matchedVerb.labelIt} ${matchedObject.labelIt}" },
            verb = matchedVerb,
            target = matchedObject,
            modifiers = matchedModifiers
        )
    }

    private fun inferVerbFromContext(query: String, domain: CreativeDomainMode): GuideActionVerb {
        if (query.contains("sfondo")) return GuideActionVerb.REMOVE_ERASE
        if (query.contains("template") || query.contains("collage") || query.contains("slide")) return GuideActionVerb.CREATE_COMPOSE
        if (query.contains("css") || query.contains("compose") || query.contains("mp4") || query.contains("pdf")) return GuideActionVerb.EXPORT_INSPECT
        return when (domain) {
            CreativeDomainMode.MODALITA_FOTO -> GuideActionVerb.ADJUST_COLOR
            CreativeDomainMode.MODALITA_DESIGN -> GuideActionVerb.CREATE_COMPOSE
            CreativeDomainMode.MODALITA_UI_UX -> GuideActionVerb.ALIGN_LAYOUT
            CreativeDomainMode.MODALITA_DISEGNO -> GuideActionVerb.CREATE_COMPOSE
        }
    }

    private fun inferObjectFromVerbAndContext(
        verb: GuideActionVerb,
        domain: CreativeDomainMode,
        selectedLayer: GraphiteLayer?
    ): GuideTargetObject {
        return when (verb) {
            GuideActionVerb.REMOVE_ERASE -> GuideTargetObject.BACKGROUND
            GuideActionVerb.BLUR_SMOOTH -> GuideTargetObject.FACE_EYES_TEETH
            GuideActionVerb.CROP_CUT -> GuideTargetObject.PHOTO_IMAGE
            GuideActionVerb.ROTATE_FLIP -> GuideTargetObject.PHOTO_IMAGE
            GuideActionVerb.ADJUST_COLOR -> GuideTargetObject.TONE_CURVE
            GuideActionVerb.APPLY_FILTER -> GuideTargetObject.PHOTO_IMAGE
            GuideActionVerb.ALIGN_LAYOUT -> GuideTargetObject.FRAME_AUTOLAYOUT
            GuideActionVerb.RESIZE_ADAPT -> GuideTargetObject.TEMPLATE_BRAND
            GuideActionVerb.PROTOTYPE_LINK -> GuideTargetObject.PROTOTYPE_FLOW
            GuideActionVerb.COLLAB_VERSION -> GuideTargetObject.FIGJAM_BOARD
            GuideActionVerb.EXPORT_INSPECT -> GuideTargetObject.CODE_HANDOFF
            GuideActionVerb.CREATE_COMPOSE -> when (domain) {
                CreativeDomainMode.MODALITA_FOTO -> GuideTargetObject.COLLAGE_STICKER
                CreativeDomainMode.MODALITA_DESIGN -> GuideTargetObject.TEMPLATE_BRAND
                CreativeDomainMode.MODALITA_UI_UX -> GuideTargetObject.COMPONENT_VARIANT
                CreativeDomainMode.MODALITA_DISEGNO -> if (selectedLayer?.kind == LayerGeometryKind.BRUSH_STROKE) {
                    GuideTargetObject.PHOTO_IMAGE
                } else {
                    GuideTargetObject.VECTOR_PATH
                }
            }
        }
    }

    private fun buildDeterministicRuleResolution(
        query: String,
        verb: GuideActionVerb,
        target: GuideTargetObject,
        modifiers: List<GuideModifierToken>
    ): GuideResolutionResult {
        val modSummary = if (modifiers.isNotEmpty()) {
            " [${modifiers.joinToString(", ") { it.labelIt }}]"
        } else ""

        return when {
            target == GuideTargetObject.BACKGROUND || (verb == GuideActionVerb.REMOVE_ERASE && modifiers.any { it in listOf(GuideModifierToken.CHROMA_KEY, GuideModifierToken.MAGIC_WAND, GuideModifierToken.GRAPH_CUT) }) -> {
                val method = when {
                    GuideModifierToken.MAGIC_WAND in modifiers -> "Bacchetta Magica (Flood-Fill ΔE)"
                    GuideModifierToken.GRAPH_CUT in modifiers -> "Segmentazione Contorni (Graph-Cut)"
                    else -> "Chroma Key Deterministico"
                }
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.REMOVE_ERASE,
                    matchedObject = GuideTargetObject.BACKGROUND,
                    modifiers = modifiers,
                    titleIt = "Rimozione Sfondo Deterministica: $method",
                    stepByStepIt = "1. Passa a Modalità Foto → Ritocco & Sfondo.\n2. Seleziona l'algoritmo '$method' e regola la Tolleranza (consigliata: 28–38).\n3. Scegli se sostituire lo sfondo o applicare Sfocatura Bokeh.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_RETOUCH_BG,
                    spotlightTag = "suite_tab_photo_retouch_bg",
                    instantActionId = "guide_act_remove_bg",
                    instantActionLabelIt = "Esegui Rimozione Sfondo ($method)"
                )
            }

            target == GuideTargetObject.FACE_EYES_TEETH || verb == GuideActionVerb.BLUR_SMOOTH -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.FACE_EYES_TEETH,
                    modifiers = modifiers,
                    titleIt = "Ritocco Viso, Occhi Rossi, Denti & Clone/Patch$modSummary",
                    stepByStepIt = "1. Apri Modalità Foto → Ritocco & Sfondo.\n2. Usa il filtro Passa-Basso Bilaterale per levigare la pelle preservando i bordi.\n3. Attiva Sbiancamento Denti (desaturazione canale giallo) o Correzione Occhi Rossi.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_RETOUCH_BG,
                    spotlightTag = "suite_tab_photo_retouch_bg",
                    instantActionId = "guide_act_portrait_retouch",
                    instantActionLabelIt = "Applica Ritocco Viso & Sbiancamento"
                )
            }

            target == GuideTargetObject.TONE_CURVE || verb == GuideActionVerb.ADJUST_COLOR -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.ADJUST_COLOR,
                    matchedObject = GuideTargetObject.TONE_CURVE,
                    modifiers = modifiers,
                    titleIt = "Regolazioni Tonali (14 Controlli) & Curve RGB$modSummary",
                    stepByStepIt = "1. Apri Modalità Foto → Regolazioni & Curve.\n2. Regola Esposizione, Alte Luci, Ombre, Temperatura e Nitidezza.\n3. Modifica i 5 punti di controllo della Curva RGB o usa il Contagocce Bilanciamento del Bianco.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_TONAL_CURVES,
                    spotlightTag = "suite_tab_photo_tonal_curves",
                    instantActionId = "guide_act_auto_tonal_curve",
                    instantActionLabelIt = "Applica Curva S-Contrast & Bilanciamento"
                )
            }

            verb == GuideActionVerb.APPLY_FILTER || GuideModifierToken.LUT_VINTAGE in modifiers || GuideModifierToken.LUT_CHARCOAL in modifiers -> {
                val presetName = if (GuideModifierToken.LUT_CHARCOAL in modifiers) "Matita / Carboncino" else "Cinema / Vintage LUT"
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.APPLY_FILTER,
                    matchedObject = GuideTargetObject.PHOTO_IMAGE,
                    modifiers = modifiers,
                    titleIt = "Filtri LUT Artistici, Distorsioni & Luce ($presetName)",
                    stepByStepIt = "1. Apri Modalità Foto → Filtri LUT & Effetti.\n2. Seleziona la LUT locale '$presetName' e regola l'intensità.\n3. Aggiungi opzionalmente effetti Raggi Solari, Glitch o Grana Pellicola 35mm.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_FILTERS_EFFECTS,
                    spotlightTag = "suite_tab_photo_filters_effects",
                    instantActionId = if (GuideModifierToken.LUT_CHARCOAL in modifiers) "guide_act_lut_charcoal" else "guide_act_lut_vintage",
                    instantActionLabelIt = "Applica Filtro $presetName"
                )
            }

            verb == GuideActionVerb.CROP_CUT || verb == GuideActionVerb.ROTATE_FLIP -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.PHOTO_IMAGE,
                    modifiers = modifiers,
                    titleIt = "Crop Proporzioni, Raddrizza Orizzonte & Prospettiva$modSummary",
                    stepByStepIt = "1. Apri Modalità Foto → Crop & Prospettiva.\n2. Scegli il rapporto (1:1, 4:5, 16:9, 9:16) con Griglia dei Terzi.\n3. Usa Raddrizza Orizzonte con riempimento intelligente dei bordi (specchiatura locale) e interpolazione Lanczos.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_CROP_TRANSFORM,
                    spotlightTag = "suite_tab_photo_crop_transform",
                    instantActionId = "guide_act_crop_straighten",
                    instantActionLabelIt = "Apri Crop & Raddrizza Orizzonte"
                )
            }

            target == GuideTargetObject.COLLAGE_STICKER -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.CREATE_COMPOSE,
                    matchedObject = GuideTargetObject.COLLAGE_STICKER,
                    modifiers = modifiers,
                    titleIt = "Compositore Collage (2–9 Griglie & Maschere Forma)",
                    stepByStepIt = "1. Apri Modalità Foto → Collage & Cornici.\n2. Scegli il numero di celle (2–9), spaziatura e raggio bordi.\n3. Applica maschere a forma (Cerchio, Cuore, Stella) o cornici decorative.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.PHOTO_COLLAGE,
                    spotlightTag = "suite_tab_photo_collage",
                    instantActionId = "guide_act_generate_collage",
                    instantActionLabelIt = "Genera Griglia Collage sulla Tavola"
                )
            }

            target == GuideTargetObject.VIDEO_CLIP || GuideModifierToken.FORMAT_MP4 in modifiers -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.VIDEO_CLIP,
                    modifiers = modifiers,
                    titleIt = "Video Editor Locale Multi-Clip & Esportazione MP4",
                    stepByStepIt = "1. Apri Modalità Foto → Video Editor MP4.\n2. Imposta Trim/Split, transizioni (Dissolvenza, Slide, Zoom) e velocità (0.25x–4x).\n3. Configura traccia audio locale, overlay testo/sticker ed esporta in MP4 1080p.",
                    targetDomain = CreativeDomainMode.MODALITA_FOTO,
                    targetSubPanel = SuiteSubPanel.VIDEO_TIMELINE,
                    spotlightTag = "suite_tab_video_timeline",
                    instantActionId = "guide_act_open_video",
                    instantActionLabelIt = "Apri Timeline Video & Export MP4"
                )
            }

            target == GuideTargetObject.TEMPLATE_BRAND || verb == GuideActionVerb.RESIZE_ADAPT -> {
                val isResize = verb == GuideActionVerb.RESIZE_ADAPT || query.lowercase(Locale.ROOT).contains("resize")
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.TEMPLATE_BRAND,
                    modifiers = modifiers,
                    titleIt = if (isResize) "Magic Resize Multi-Formato & Calendario Social" else "Libreria Template Locali & Brand Kit (Canva Engine)",
                    stepByStepIt = if (isResize) {
                        "1. Apri Modalità Design → Magic Resize & Post.\n2. Seleziona il formato di destinazione (Post IG, Story 9:16, LinkedIn, YouTube).\n3. Tutti i livelli vengono riposizionati proporzionalmente in modo deterministico."
                    } else {
                        "1. Apri Modalità Design → Template Locali o Brand Kit.\n2. Scegli un template per categoria/colore e sostituisci i placeholder.\n3. Tocca 'Applica Brand Kit' per aggiornare colori e font con 1 tap."
                    },
                    targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                    targetSubPanel = if (isResize) SuiteSubPanel.DESIGN_SOCIAL_PLANNER else SuiteSubPanel.DESIGN_TEMPLATES,
                    spotlightTag = if (isResize) "suite_tab_design_social_planner" else "suite_tab_design_templates",
                    instantActionId = if (isResize) "guide_act_magic_resize_story" else "guide_act_apply_brand_kit",
                    instantActionLabelIt = if (isResize) "Esegui Magic Resize 9:16 Story" else "Applica Brand Kit al Progetto"
                )
            }

            target == GuideTargetObject.PRESENTATION_SLIDE || GuideModifierToken.FORMAT_PDF in modifiers -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.PRESENTATION_SLIDE,
                    modifiers = modifiers,
                    titleIt = "Presentazioni Multi-Slide, Modalità Relatore & Export PDF",
                    stepByStepIt = "1. Apri Modalità Design → Presentazioni & Slide.\n2. Gestisci le slide dello storyboard, transizioni e note del relatore.\n3. Avvia la Presentazione a schermo intero con Timer o esporta il PDF multipagina.",
                    targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                    targetSubPanel = SuiteSubPanel.DESIGN_PRESENTATION,
                    spotlightTag = "suite_tab_design_presentation",
                    instantActionId = "guide_act_start_presentation",
                    instantActionLabelIt = "Avvia Modalità Presentazione con Timer"
                )
            }

            target == GuideTargetObject.SOCIAL_POST -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.SOCIAL_POST,
                    modifiers = modifiers,
                    titleIt = "Pianificatore Post Offline, Modelli Didascalie & Watermark",
                    stepByStepIt = "1. Apri Modalità Design → Magic Resize & Post.\n2. Genera una didascalia da modelli testuali predefiniti (Lancio, Evento, Tutorial).\n3. Salva nel Calendario Editoriale Offline con promemoria e Watermark opzionale.",
                    targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                    targetSubPanel = SuiteSubPanel.DESIGN_SOCIAL_PLANNER,
                    spotlightTag = "suite_tab_design_social_planner",
                    instantActionId = "guide_act_schedule_post",
                    instantActionLabelIt = "Pianifica Post nel Calendario Offline"
                )
            }

            target == GuideTargetObject.FRAME_AUTOLAYOUT || verb == GuideActionVerb.ALIGN_LAYOUT -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.ALIGN_LAYOUT,
                    matchedObject = GuideTargetObject.FRAME_AUTOLAYOUT,
                    modifiers = modifiers,
                    titleIt = "Figma Auto Layout, Constraints & Griglie Dispositivo",
                    stepByStepIt = "1. Apri Modalità UI/UX → Frame & Auto Layout.\n2. Scegli direzione Riga o Colonna, imposta Spaziatura (gap) e Padding.\n3. Configura i Constraints (Left, Right, Center, Scale) e le Griglie a 12 colonne.",
                    targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                    targetSubPanel = SuiteSubPanel.UIUX_AUTO_LAYOUT,
                    spotlightTag = "suite_tab_uiux_auto_layout",
                    instantActionId = "guide_act_apply_autolayout",
                    instantActionLabelIt = "Applica Auto Layout Automatico"
                )
            }

            target == GuideTargetObject.COMPONENT_VARIANT -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.COMPONENT_VARIANT,
                    modifiers = modifiers,
                    titleIt = "Componenti Master, Varianti di Stato & Override",
                    stepByStepIt = "1. Apri Modalità UI/UX → Componenti & Stili.\n2. Converti il livello selezionato in Componente Master e scegli la variante (Default, Hover, Pressed, Disabled).\n3. Inserisci istanze nella tavola con override di testo e colore.",
                    targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                    targetSubPanel = SuiteSubPanel.UIUX_COMPONENTS,
                    spotlightTag = "suite_tab_uiux_components",
                    instantActionId = "guide_act_create_component",
                    instantActionLabelIt = "Crea Componente Master dal Livello"
                )
            }

            target == GuideTargetObject.PROTOTYPE_FLOW || verb == GuideActionVerb.PROTOTYPE_LINK -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.PROTOTYPE_LINK,
                    matchedObject = GuideTargetObject.PROTOTYPE_FLOW,
                    modifiers = modifiers,
                    titleIt = "Prototipazione Interattiva, Hotspot & Smart Animate",
                    stepByStepIt = "1. Apri Modalità UI/UX → Prototipo & Flussi.\n2. Collega il livello selezionato a una schermata tramite Trigger (Tap, Drag, Long Press, Timer).\n3. Scegli la transizione Smart Animate e avvia l'Anteprima Interattiva.",
                    targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                    targetSubPanel = SuiteSubPanel.UIUX_PROTOTYPE,
                    spotlightTag = "suite_tab_uiux_prototype",
                    instantActionId = "guide_act_launch_prototype",
                    instantActionLabelIt = "Avvia Player Prototipo Interattivo"
                )
            }

            target == GuideTargetObject.FIGJAM_BOARD || verb == GuideActionVerb.COLLAB_VERSION -> {
                val isFigJam = query.lowercase(Locale.ROOT).let { it.contains("figjam") || it.contains("post") || it.contains("lavagna") }
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.COLLAB_VERSION,
                    matchedObject = if (isFigJam) GuideTargetObject.FIGJAM_BOARD else GuideTargetObject.PROTOTYPE_FLOW,
                    modifiers = modifiers,
                    titleIt = if (isFigJam) "Lavagna Infinita FigJam (Post-it, Connettori & Timer)" else "Collaborazione Offline: Commenti, Version History & Diff",
                    stepByStepIt = if (isFigJam) {
                        "1. Apri Modalità UI/UX → Lavagna FigJam.\n2. Aggiungi Post-it colorati con autore, collega i nodi con frecce e vota con i timbri.\n3. Avvia il Timer per sessioni di brainstorming."
                    } else {
                        "1. Apri Modalità UI/UX → Commenti & Versioni.\n2. Ancora un commento alle coordinate della tavola o salva uno Snapshot nella Version History.\n3. Usa Visual Diff per confrontare le modifiche tra due versioni."
                    },
                    targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                    targetSubPanel = if (isFigJam) SuiteSubPanel.UIUX_FIGJAM_BOARD else SuiteSubPanel.UIUX_COLLAB_HISTORY,
                    spotlightTag = if (isFigJam) "suite_tab_uiux_figjam_board" else "suite_tab_uiux_collab_history",
                    instantActionId = if (isFigJam) "guide_act_add_sticky" else "guide_act_save_version_snapshot",
                    instantActionLabelIt = if (isFigJam) "Aggiungi Post-it FigJam" else "Salva Snapshot nella Version History"
                )
            }

            target == GuideTargetObject.CODE_HANDOFF || verb == GuideActionVerb.EXPORT_INSPECT -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = GuideActionVerb.EXPORT_INSPECT,
                    matchedObject = GuideTargetObject.CODE_HANDOFF,
                    modifiers = modifiers,
                    titleIt = "Ispezione Design & Generazione Codice CSS / Jetpack Compose",
                    stepByStepIt = "1. Apri Modalità UI/UX → Ispezione CSS/Compose.\n2. Visualizza dimensioni in dp/px, colori HEX/RGB, tipografia e distanze.\n3. Copia il codice CSS3 o Jetpack Compose o esporta asset a 1x, 2x, 3x, SVG e PDF.",
                    targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                    targetSubPanel = SuiteSubPanel.UIUX_HANDOFF_INSPECT,
                    spotlightTag = "suite_tab_uiux_handoff_inspect",
                    instantActionId = "guide_act_inspect_code",
                    instantActionLabelIt = "Apri Ispezione Codice CSS & Compose"
                )
            }

            else -> {
                GuideResolutionResult(
                    query = query,
                    matchedVerb = verb,
                    matchedObject = GuideTargetObject.VECTOR_PATH,
                    modifiers = modifiers,
                    titleIt = "Vector Network, Pennelli Raster & Operazioni Booleane",
                    stepByStepIt = "1. Apri Modalità Disegno → Pennelli o Vector Network.\n2. Scegli tra i 6 pennelli (Matita, Penna, Pennarello, Acquerello, Aerografo, Gessetto) o converti le forme in Tracciato Bézier.\n3. Usa Pathfinder (Unisci, Sottrai, Interseca, XOR) per combinare i livelli.",
                    targetDomain = CreativeDomainMode.MODALITA_DISEGNO,
                    targetSubPanel = SuiteSubPanel.DRAW_BRUSH_STUDIO,
                    spotlightTag = "suite_tab_draw_brush_studio",
                    instantActionId = "guide_act_bake_vector_network",
                    instantActionLabelIt = "Converti in Vector Network Modificabile"
                )
            }
        }
    }

    /**
     * Generates deterministic 1-tap action pills tailored to the current domain mode and selected layer.
     */
    fun computeDynamicActionPills(
        domain: CreativeDomainMode,
        doc: GraphiteDocumentModel,
        selectedLayer: GraphiteLayer?
    ): List<GuideQuickActionPill> {
        val pills = mutableListOf<GuideQuickActionPill>()
        when (domain) {
            CreativeDomainMode.MODALITA_FOTO -> {
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_remove_bg",
                        labelIt = "Rimuovi Sfondo",
                        badgeIt = "✂️ Chroma/Cut",
                        targetDomain = CreativeDomainMode.MODALITA_FOTO,
                        targetSubPanel = SuiteSubPanel.PHOTO_RETOUCH_BG,
                        spotlightTag = "suite_tab_photo_retouch_bg",
                        toastExplanationIt = "Rimozione sfondo deterministica (Chroma Key / Magic Wand / Graph-Cut)"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_tonal_curves",
                        labelIt = "Curve RGB & Luci",
                        badgeIt = "📈 14 Controlli",
                        targetDomain = CreativeDomainMode.MODALITA_FOTO,
                        targetSubPanel = SuiteSubPanel.PHOTO_TONAL_CURVES,
                        spotlightTag = "suite_tab_photo_tonal_curves",
                        toastExplanationIt = "Apre i 14 controlli tonali, Curve RGB e Bilanciamento del Bianco"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_lut_charcoal",
                        labelIt = "Filtro Carboncino",
                        badgeIt = "🎨 LUT Locale",
                        targetDomain = CreativeDomainMode.MODALITA_FOTO,
                        targetSubPanel = SuiteSubPanel.PHOTO_FILTERS_EFFECTS,
                        spotlightTag = "suite_tab_photo_filters_effects",
                        toastExplanationIt = "Applica l'effetto schizzo a matita/carboncino o LUT cinematografica"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_collage_video",
                        labelIt = "Collage & Video MP4",
                        badgeIt = "🎬 Timeline",
                        targetDomain = CreativeDomainMode.MODALITA_FOTO,
                        targetSubPanel = SuiteSubPanel.VIDEO_TIMELINE,
                        spotlightTag = "suite_tab_video_timeline",
                        toastExplanationIt = "Apre l'editor Video multi-clip con transizioni ed export MP4"
                    )
                )
            }

            CreativeDomainMode.MODALITA_DESIGN -> {
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_templates",
                        labelIt = "Template Social/Print",
                        badgeIt = "🖼️ 10 Categorie",
                        targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                        targetSubPanel = SuiteSubPanel.DESIGN_TEMPLATES,
                        spotlightTag = "suite_tab_design_templates",
                        toastExplanationIt = "Sfoglia template preinstallati per Instagram, YouTube, Poster, CV e Presentazioni"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_brand_kit",
                        labelIt = "Applica Brand Kit",
                        badgeIt = "💎 1-Tap Palette",
                        targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                        targetSubPanel = SuiteSubPanel.DESIGN_BRAND_KIT,
                        spotlightTag = "suite_tab_design_brand_kit",
                        toastExplanationIt = "Applica istantaneamente colori, font e logo del Brand Kit locale"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_magic_resize",
                        labelIt = "Magic Resize",
                        badgeIt = "📐 Adatta Formato",
                        targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                        targetSubPanel = SuiteSubPanel.DESIGN_SOCIAL_PLANNER,
                        spotlightTag = "suite_tab_design_social_planner",
                        toastExplanationIt = "Adatta automaticamente la tavola a Post 1:1, Story 9:16, LinkedIn o YouTube"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_presentation",
                        labelIt = "Presenta Slide / PDF",
                        badgeIt = "📽️ Storyboard",
                        targetDomain = CreativeDomainMode.MODALITA_DESIGN,
                        targetSubPanel = SuiteSubPanel.DESIGN_PRESENTATION,
                        spotlightTag = "suite_tab_design_presentation",
                        toastExplanationIt = "Gestisci slide, note del relatore, timer a schermo intero ed export PDF"
                    )
                )
            }

            CreativeDomainMode.MODALITA_UI_UX -> {
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_autolayout",
                        labelIt = "Auto Layout (${doc.layers.size} liv.)",
                        badgeIt = "⊞ Riga/Colonna",
                        targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                        targetSubPanel = SuiteSubPanel.UIUX_AUTO_LAYOUT,
                        spotlightTag = "suite_tab_uiux_auto_layout",
                        toastExplanationIt = "Dispone automaticamente i livelli con spaziatura, padding e Constraints"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_component_variant",
                        labelIt = "Crea Componente",
                        badgeIt = "❖ Master/Varianti",
                        targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                        targetSubPanel = SuiteSubPanel.UIUX_COMPONENTS,
                        spotlightTag = "suite_tab_uiux_components",
                        toastExplanationIt = "Crea un Componente Master con stati Default, Hover, Pressed e Disabled"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_prototype_play",
                        labelIt = "Avvia Prototipo",
                        badgeIt = "▶️ Smart Animate",
                        targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                        targetSubPanel = SuiteSubPanel.UIUX_PROTOTYPE,
                        spotlightTag = "suite_tab_uiux_prototype",
                        toastExplanationIt = "Collega hotspot interattivi e avvia il prototipo a schermo intero"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_figjam_inspect",
                        labelIt = "FigJam & CSS/Compose",
                        badgeIt = "📋 Handoff",
                        targetDomain = CreativeDomainMode.MODALITA_UI_UX,
                        targetSubPanel = SuiteSubPanel.UIUX_FIGJAM_BOARD,
                        spotlightTag = "suite_tab_uiux_figjam_board",
                        toastExplanationIt = "Apre la lavagna FigJam con post-it o l'ispezione codice CSS/Compose"
                    )
                )
            }

            CreativeDomainMode.MODALITA_DISEGNO -> {
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_6_brushes",
                        labelIt = "6 Pennelli & Secchiello",
                        badgeIt = "🖌️ Acquerello/Chalk",
                        targetDomain = CreativeDomainMode.MODALITA_DISEGNO,
                        targetSubPanel = SuiteSubPanel.DRAW_BRUSH_STUDIO,
                        spotlightTag = "suite_tab_draw_brush_studio",
                        toastExplanationIt = "Seleziona Matita, Penna, Pennarello, Acquerello, Aerografo, Gessetto o Gomma"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_bake_network",
                        labelIt = "Vector Network (${selectedLayer?.name ?: "Livello"})",
                        badgeIt = "✒️ Bézier C¹/C⁰",
                        targetDomain = CreativeDomainMode.MODALITA_DISEGNO,
                        targetSubPanel = SuiteSubPanel.DRAW_VECTOR_BOOLEAN,
                        spotlightTag = "suite_tab_draw_vector_boolean",
                        toastExplanationIt = "Converte la forma in tracciato modificabile con maniglie Bézier e giunzioni"
                    )
                )
                pills.add(
                    GuideQuickActionPill(
                        id = "pill_boolean_ops",
                        labelIt = "Operazioni Booleane",
                        badgeIt = "⊖ Sottrai/Unisci",
                        targetDomain = CreativeDomainMode.MODALITA_DISEGNO,
                        targetSubPanel = SuiteSubPanel.DRAW_VECTOR_BOOLEAN,
                        spotlightTag = "suite_tab_draw_vector_boolean",
                        toastExplanationIt = "Combina i livelli con Unione, Sottrazione, Intersezione o Esclusione (XOR)"
                    )
                )
            }
        }
        return pills
    }

    /**
     * Rule-based proactive tip triggered by the current workspace context (zero AI).
     */
    fun computeProactiveTip(
        domain: CreativeDomainMode,
        doc: GraphiteDocumentModel,
        selectedLayer: GraphiteLayer?
    ): String {
        val unalignedLayers = doc.layers.count { it.visible && !it.locked }
        return when {
            domain == CreativeDomainMode.MODALITA_UI_UX && unalignedLayers >= 3 ->
                "Suggerimento Esperto: Hai $unalignedLayers livelli attivi. Tocca 'Auto Layout' per disporli automaticamente in riga o colonna con spaziatura uniforme."
            domain == CreativeDomainMode.MODALITA_FOTO && selectedLayer != null ->
                "Suggerimento Foto: Tieni premuto o attiva 'Confronto Prima/Dopo' mentre regoli le Curve RGB o la rimozione sfondo Graph-Cut su '${selectedLayer.name}'."
            domain == CreativeDomainMode.MODALITA_DESIGN ->
                "Suggerimento Design: Usa il 'Brand Kit Locale' per applicare la tua palette fino a 10 colori su tutti gli elementi della tavola con un solo tocco."
            selectedLayer != null && selectedLayer.kind != LayerGeometryKind.BEZIER_PATH && selectedLayer.kind != LayerGeometryKind.TEXT ->
                "Suggerimento Vettoriale: Puoi convertire '${selectedLayer.name}' in un Vector Network Bézier per trascinare singoli vertici e maniglie."
            else ->
                "Graphite Guide Pronta: Scrivi un comando (es. 'rimuovi sfondo', 'applica filtro vintage', 'crea auto layout', 'esporta css') o tocca una pillola rapida."
        }
    }
}
