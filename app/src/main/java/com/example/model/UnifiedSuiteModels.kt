package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DataColor
import com.example.ui.theme.DataGraphic
import com.example.ui.theme.DataNumber
import com.example.ui.theme.DataRaster
import com.example.ui.theme.DataVector

/**
 * The 4 Contextual Modes of the Unified Graphite Mobile Creative Suite.
 */
enum class CreativeDomainMode(
    val labelIt: String,
    val shortBadgeIt: String,
    val descriptionIt: String,
    val accentColor: Color
) {
    MODALITA_FOTO(
        labelIt = "Modalità Foto & Video",
        shortBadgeIt = "📷 Foto",
        descriptionIt = "Editing fotografico, curve tonali, rimozione sfondo deterministica, filtri LUT, collage e video locale",
        accentColor = DataRaster
    ),
    MODALITA_DESIGN(
        labelIt = "Modalità Design & Social",
        shortBadgeIt = "🎨 Design",
        descriptionIt = "Template locali, Brand Kit, presentazioni multi-slide, Magic Resize, sticker e calendario post offline",
        accentColor = DataColor
    ),
    MODALITA_UI_UX(
        labelIt = "Modalità UI/UX & FigJam",
        shortBadgeIt = "✏️ UI/UX",
        descriptionIt = "Frame dispositivi, Auto Layout, Componenti con varianti, Prototipazione Smart Animate, Handoff e FigJam",
        accentColor = DataVector
    ),
    MODALITA_DISEGNO(
        labelIt = "Modalità Disegno & DAG",
        shortBadgeIt = "✒️ Disegno",
        descriptionIt = "Illustrazione vettoriale/raster, 6 pennelli, gomma pixel/tracciato, Pathfinder booleano e Grafo Nodi",
        accentColor = DataGraphic
    )
}

/**
 * 3-State Toggle for the Non-AI Rule-Based "Graphite Guide" Assistant in the Top Bar.
 */
enum class GraphiteGuideState(
    val labelIt: String,
    val badgeIt: String,
    val hintIt: String
) {
    ACTIVE(
        labelIt = "Guida Attiva",
        badgeIt = "🧭 Guida ON",
        hintIt = "Suggerimenti contestuali, pillole rapide a 1-tap e barra comandi deterministica attivi"
    ),
    HIDDEN_PTT(
        labelIt = "Modalità Nascosta (Push-to-Talk)",
        badgeIt = "🎙️ Guida PTT",
        hintIt = "Interfaccia libera: tieni premuto o tocca il pulsante per inviare un comando rapido e nasconderlo subito"
    ),
    OFF(
        labelIt = "Guida Disattivata",
        badgeIt = "🧭 Guida OFF",
        hintIt = "Assistente completamente disattivato per massimizzare l'area di lavoro"
    )
}

/**
 * Sub-panels inside the Unified Creative Suite Drawer.
 */
enum class SuiteSubPanel(val labelIt: String, val domain: CreativeDomainMode) {
    // Modalità Foto & Video (Picsart)
    PHOTO_CROP_TRANSFORM("Crop & Prospettiva", CreativeDomainMode.MODALITA_FOTO),
    PHOTO_TONAL_CURVES("Regolazioni & Curve", CreativeDomainMode.MODALITA_FOTO),
    PHOTO_RETOUCH_BG("Ritocco & Sfondo", CreativeDomainMode.MODALITA_FOTO),
    PHOTO_FILTERS_EFFECTS("Filtri LUT & Effetti", CreativeDomainMode.MODALITA_FOTO),
    PHOTO_COLLAGE("Collage & Cornici", CreativeDomainMode.MODALITA_FOTO),
    VIDEO_TIMELINE("Video Editor MP4", CreativeDomainMode.MODALITA_FOTO),

    // Modalità Design & Social (Canva)
    DESIGN_TEMPLATES("Template Locali", CreativeDomainMode.MODALITA_DESIGN),
    DESIGN_BRAND_KIT("Brand Kit Locale", CreativeDomainMode.MODALITA_DESIGN),
    DESIGN_PRESENTATION("Presentazioni & Slide", CreativeDomainMode.MODALITA_DESIGN),
    DESIGN_SOCIAL_PLANNER("Magic Resize & Post", CreativeDomainMode.MODALITA_DESIGN),
    DESIGN_STICKERS_ASSETS("Sticker & Asset", CreativeDomainMode.MODALITA_DESIGN),

    // Modalità UI/UX & FigJam (Figma)
    UIUX_AUTO_LAYOUT("Frame & Auto Layout", CreativeDomainMode.MODALITA_UI_UX),
    UIUX_COMPONENTS("Componenti & Stili", CreativeDomainMode.MODALITA_UI_UX),
    UIUX_PROTOTYPE("Prototipo & Flussi", CreativeDomainMode.MODALITA_UI_UX),
    UIUX_COLLAB_HISTORY("Commenti & Versioni", CreativeDomainMode.MODALITA_UI_UX),
    UIUX_FIGJAM_BOARD("Lavagna FigJam", CreativeDomainMode.MODALITA_UI_UX),
    UIUX_HANDOFF_INSPECT("Ispezione CSS/Compose", CreativeDomainMode.MODALITA_UI_UX),

    // Modalità Disegno & DAG (Graphite + Brushes)
    DRAW_BRUSH_STUDIO("Pennelli & Riempimento", CreativeDomainMode.MODALITA_DISEGNO),
    DRAW_VECTOR_BOOLEAN("Vector Network & Booleane", CreativeDomainMode.MODALITA_DISEGNO)
}

// ============================================================================
// 1. PICSART ENGINE MODELS (Photo, Retouch, LUTs, Collage, Brushes, Video)
// ============================================================================

enum class PhotoCropAspectRatio(val labelIt: String, val ratioW: Float, val ratioH: Float) {
    FREE("Libero", 0f, 0f),
    RATIO_1_1("1:1 Quadrato", 1f, 1f),
    RATIO_4_5("4:5 Ritratto IG", 4f, 5f),
    RATIO_16_9("16:9 Orizzontale", 16f, 9f),
    RATIO_9_16("9:16 Story/Reel", 9f, 16f)
}

enum class ResizeInterpolationMode(val labelIt: String) {
    BICUBIC("Bicubica (Morbida)"),
    LANCZOS("Lanczos 3-Tap (Nitida)"),
    BILINEAR("Bilineare (Veloce)")
}

enum class ToneCurveChannel(val labelIt: String, val color: Color) {
    RGB("Master RGB", Color.White),
    RED("Canale Rosso", Color(0xFFFF5252)),
    GREEN("Canale Verde", Color(0xFF69F0AE)),
    BLUE("Canale Blu", Color(0xFF448AFF))
}

data class CurveControlPoint(
    val inputX: Float, // 0f..1f
    val outputY: Float // 0f..1f
)

data class PhotoAdjustmentsState(
    val cropRatio: PhotoCropAspectRatio = PhotoCropAspectRatio.FREE,
    val showRuleOfThirds: Boolean = true,
    val horizonStraightenDeg: Float = 0f,
    val smartBorderMirrorFill: Boolean = true,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val interpolation: ResizeInterpolationMode = ResizeInterpolationMode.LANCZOS,
    val lockAspectRatio: Boolean = true,
    // 14 Tonal Controls (-100f..100f, default 0f)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val sharpness: Float = 0f,
    val noiseReduction: Float = 0f,
    val vignette: Float = 0f,
    val filmGrain: Float = 0f,
    // Perspective & Lens
    val perspectiveVertical: Float = 0f,
    val perspectiveHorizontal: Float = 0f,
    val lensDistortion: Float = 0f,
    // Tone Curves
    val activeCurveChannel: ToneCurveChannel = ToneCurveChannel.RGB,
    val rgbCurvePoints: List<CurveControlPoint> = listOf(
        CurveControlPoint(0f, 0f),
        CurveControlPoint(0.25f, 0.25f),
        CurveControlPoint(0.5f, 0.5f),
        CurveControlPoint(0.75f, 0.75f),
        CurveControlPoint(1f, 1f)
    ),
    val whiteBalanceNeutralHex: Long = 0xFF808080L
)

enum class RetouchAlgorithmMode(val labelIt: String, val descriptionIt: String) {
    CLONE_STAMP("Clone a Pennello", "Campiona pixel da un'area sorgente locale e dipinge senza IA"),
    PATCH_TOOL("Strumento Toppa (Patch)", "Sostituisce un'area selezionata con texture adiacente compatibile"),
    CHROMA_KEY("Chroma Key (Colore)", "Rimuove lo sfondo isolando un colore chiave con tolleranza"),
    MAGIC_WAND("Bacchetta Magica", "Selezione contigua flood-fill basata su distanza cromatica ΔE"),
    GRAPH_CUT_CONTOUR("Segmentazione Contorni", "Separazione primo piano/sfondo tramite gradiente Sobel + Graph-Cut locale"),
    SKIN_SMOOTH("Levigatura Pelle", "Filtro passa-basso bilaterale che preserva i bordi di occhi e labbra"),
    TEETH_WHITEN("Sbiancamento Denti", "Seleziona la banda giallastra e riduce la saturazione aumentando L*"),
    RED_EYE_FIX("Correzione Occhi Rossi", "Sostituisce l'eccesso del canale Rosso nella pupilla con media G/B")
}

enum class BackgroundBlurMode(val labelIt: String) {
    NONE("Nessuna Sfocatura"),
    GAUSSIAN("Gaussiana Morbida"),
    LENS_BOKEH("Lente / Bokeh Esagonale"),
    MOTION_BLUR("Motion Blur Direzionale")
}

data class RetouchAndBgState(
    val activeMode: RetouchAlgorithmMode = RetouchAlgorithmMode.CHROMA_KEY,
    val tolerance: Float = 32f,
    val featherPx: Float = 4f,
    val cloneSourceOffsetX: Float = -60f,
    val cloneSourceOffsetY: Float = 0f,
    val bgRemoved: Boolean = false,
    val replacementBgHex: Long = 0xFF1E293BL,
    val replacementPattern: String = "Nessuno",
    val bgBlurMode: BackgroundBlurMode = BackgroundBlurMode.NONE,
    val bgBlurRadius: Float = 18f,
    val skinSmoothIntensity: Float = 0f,
    val eyeTeethBrighten: Float = 0f,
    val teethWhitenIntensity: Float = 0f,
    val redEyeCorrected: Boolean = false
)

enum class PhotoFilterLutPreset(
    val labelIt: String,
    val hueDelta: Float,
    val satMult: Float,
    val contrastDelta: Float,
    val tintHex: Long
) {
    NONE("Originale", 0f, 1.0f, 0f, 0xFF808080L),
    VINTAGE("Vintage Pellicola", -14f, 0.78f, 12f, 0xFFD4A373L),
    BW_NOIR("Bianco & Nero Noir", 0f, 0.0f, 32f, 0xFF9E9E9EL),
    SEPIA("Seppia Storico", -24f, 0.45f, 15f, 0xFFC89D7CL),
    HDR_VIVID("HDR Nitido", 4f, 1.38f, 28f, 0xFF65BBE5L),
    CINEMATIC_TEAL("Cinema Teal & Orange", 18f, 1.18f, 22f, 0xFF2A9D8FL),
    RETRO_80S("Retrò Synth 80s", 42f, 1.32f, 18f, 0xFFCE6EA7L),
    POP_ART("Pop Art Poster", -35f, 1.65f, 40f, 0xFFF4A261L),
    CHARCOAL_SKETCH("Matita / Carboncino", 0f, 0.05f, 48f, 0xFFE5E5E5L)
}

enum class DistortionEffectType(val labelIt: String) {
    NONE("Nessuna"),
    RIPPLE("Increspatura"),
    WAVE("Onda Sinusoidale"),
    SPHERE("Sfera 3D"),
    TWIRL("Vortice (Twirl)"),
    PIXELATE("Pixelate Mosaico"),
    GLITCH("Glitch RGB Shift")
}

enum class LightEffectType(val labelIt: String) {
    NONE("Nessuna Luce"),
    DIFFUSE_GLOW("Bagliore Diffuso"),
    SUN_RAYS("Raggi Solari"),
    LENS_FLARE("Lens Flare Ottico"),
    VOLUMETRIC("Luce Volumetrica")
}

enum class TextureOverlayType(val labelIt: String) {
    NONE("Nessuna Texture"),
    FILM_GRAIN("Grana 35mm"),
    PAPER("Carta Ruvida"),
    CANVAS("Tela Pittorica"),
    CHALK("Gesso su Lavagna"),
    NOISE("Rumore Monocromatico")
}

enum class TextSpecialEffectType(val labelIt: String) {
    NONE("Standard"),
    DROP_SHADOW("Ombra Proiettata"),
    NEON_GLOW("Bagliore Neon"),
    OUTLINE_STROKE("Contorno Doppio"),
    BEVEL_EMBOSS("Rilievo 3D"),
    REFLECTION("Riflesso Speculare"),
    CURVED_ARC("Testo su Percorso Curvo")
}

data class FilterAndEffectsState(
    val activeLut: PhotoFilterLutPreset = PhotoFilterLutPreset.NONE,
    val lutIntensity: Float = 0.85f,
    val distortion: DistortionEffectType = DistortionEffectType.NONE,
    val distortionAmount: Float = 25f,
    val lightEffect: LightEffectType = LightEffectType.NONE,
    val lightIntensity: Float = 0.5f,
    val textureOverlay: TextureOverlayType = TextureOverlayType.NONE,
    val textureOpacity: Float = 0.35f,
    val textEffect: TextSpecialEffectType = TextSpecialEffectType.NONE,
    val textCurveAngleDeg: Float = 0f
)

enum class CollageShapeMask(val labelIt: String) {
    NONE("Griglia Rettangolare"),
    CIRCLE("Cerchio"),
    HEART("Cuore"),
    STAR("Stella 5 Punte"),
    HEXAGON("Poligono Esagonale")
}

enum class DecorativeFrameStyle(val labelIt: String) {
    NONE("Nessuna Cornice"),
    SIMPLE_BORDER("Bordo Semplice"),
    DOUBLE_LINE("Doppio Filetto Editoriale"),
    DOTTED("Puntinato Decorativo"),
    CORNER_BRACKETS("Angolari Fotografici")
}

data class CollageComposerState(
    val cellCount: Int = 4, // 2..9
    val columns: Int = 2,
    val rows: Int = 2,
    val spacingPx: Float = 16f,
    val cornerRadiusPx: Float = 20f,
    val backgroundHex: Long = 0xFF141820L,
    val shapeMask: CollageShapeMask = CollageShapeMask.NONE,
    val frameStyle: DecorativeFrameStyle = DecorativeFrameStyle.SIMPLE_BORDER
)

enum class RasterBrushPreset(
    val labelIt: String,
    val defaultSizePx: Float,
    val defaultOpacity: Float,
    val defaultHardness: Float,
    val simulatedTextureIt: String
) {
    PENCIL("Matita Grafite 2B", 6f, 0.85f, 0.9f, "Tratto granuloso sensibile alla pressione"),
    INK_PEN("Penna China Tecnica", 8f, 1.0f, 1.0f, "Bordo netto calibrato con stabilizzazione C¹"),
    MARKER("Pennarello Chisel", 24f, 0.75f, 0.85f, "Inchiostro semitrasparente sovrapponibile"),
    WATERCOLOR("Acquerello Bagnato", 38f, 0.45f, 0.25f, "Diffusione morbida con texture carta cotone"),
    AIRBRUSH("Aerografo Sfumato", 48f, 0.35f, 0.1f, "Spruzzo radiale progressivo"),
    CHALK("Gessetto Pastello", 28f, 0.8f, 0.6f, "Trama ruvida ad alto attrito"),
    PIXEL_ERASER("Gomma per Pixel", 26f, 1.0f, 0.8f, "Cancella morbido sui livelli raster"),
    VECTOR_ERASER("Gomma Tratto Vettoriale", 20f, 1.0f, 1.0f, "Rimuove interi segmenti o tracciati toccati"),
    BUCKET_FILL("Secchiello con Tolleranza", 1f, 1.0f, 1.0f, "Riempimento area o forma chiusa")
}

enum class VideoTransitionStyle(val labelIt: String) {
    NONE("Taglio Netto"),
    DISSOLVE("Dissolvenza Incrociata"),
    SLIDE_LEFT("Slide Laterale"),
    ZOOM_IN("Zoom Dinamico")
}

data class VideoClipModel(
    val id: String,
    val title: String,
    val durationSec: Float = 6.0f,
    val trimStartSec: Float = 0.0f,
    val trimEndSec: Float = 6.0f,
    val speedMultiplier: Float = 1.0f, // 0.25x .. 4.0x
    val preserveAudioPitch: Boolean = true,
    val muteClipAudio: Boolean = false,
    val transitionToNext: VideoTransitionStyle = VideoTransitionStyle.DISSOLVE,
    val lutFilter: PhotoFilterLutPreset = PhotoFilterLutPreset.NONE
)

data class VideoTimelineState(
    val clips: List<VideoClipModel> = listOf(
        VideoClipModel("clip-1", "Scena Intro Tavola", 5.0f, 0.0f, 5.0f, 1.0f),
        VideoClipModel("clip-2", "Animazione Nodi DAG", 6.5f, 0.5f, 6.0f, 1.25f, transitionToNext = VideoTransitionStyle.ZOOM_IN)
    ),
    val selectedClipId: String = "clip-1",
    val playheadSec: Float = 0.0f,
    val audioTrackTitle: String = "Ambient Synth Loop (Royalty-Free Locale)",
    val audioVolume: Float = 0.8f,
    val audioFadeInSec: Float = 1.0f,
    val audioFadeOutSec: Float = 1.5f,
    val overlayText: String = "Graphite Studio Reel",
    val exportResolution: String = "1080p Full HD (1920×1080)",
    val exportBitrateMbps: Int = 16
)

// ============================================================================
// 2. CANVA ENGINE MODELS (Templates, Brand Kit, Presentations, Social, Assets)
// ============================================================================

enum class CanvaTemplateCategory(val labelIt: String, val defaultW: Int, val defaultH: Int) {
    INSTAGRAM_POST("Post Instagram", 1080, 1080),
    INSTAGRAM_STORY("Story / Reel Cover", 1080, 1920),
    FACEBOOK_COVER("Copertina Facebook", 1640, 924),
    LINKEDIN_BANNER("Banner LinkedIn", 1584, 396),
    YOUTUBE_THUMB("YouTube Thumbnail", 1280, 720),
    FLYER_POSTER("Volantino / Poster", 1200, 1600),
    BUSINESS_CARD("Biglietto da Visita", 1050, 600),
    INVITATION("Invito Evento", 1080, 1350),
    RESUME_CV("Curriculum Vitae", 1240, 1754),
    PRESENTATION_16_9("Presentazione 16:9", 1920, 1080)
}

data class LocalCanvaTemplate(
    val id: String,
    val titleIt: String,
    val category: CanvaTemplateCategory,
    val styleTag: String, // Minimal, Moderno, Bold, Editoriale, Tech
    val dominantColorHex: Long,
    val accentColorHex: Long,
    val bgHex: Long,
    val headlineText: String,
    val subheadText: String,
    val isUserSaved: Boolean = false
)

data class LocalBrandKit(
    val id: String = "brand-default",
    val brandName: String = "Graphite Enterprise Brand",
    val paletteHexList: List<Long> = listOf(
        0xFF65BBE5L, 0xFF68C587L, 0xFFE4BB72L, 0xFFCE6EA7L, 0xFF1A1D24L, 0xFFFBF9EBL
    ),
    val headingFontName: String = "Space Grotesk Bold",
    val bodyFontName: String = "Inter Regular",
    val monospaceFontName: String = "JetBrains Mono",
    val logoBadgeText: String = "◈ GRAPHITE",
    val isStyleLocked: Boolean = false
)

enum class SlideEntranceAnimation(val labelIt: String) {
    NONE("Istantanea"),
    FADE_IN("Apparizione Morbida"),
    SLIDE_UP("Scorrimento dal Basso"),
    ZOOM_POP("Zoom Elastico")
}

data class PresentationSlide(
    val id: String,
    val title: String,
    val subtitle: String,
    val speakerNotes: String,
    val transition: VideoTransitionStyle = VideoTransitionStyle.DISSOLVE,
    val entranceAnim: SlideEntranceAnimation = SlideEntranceAnimation.FADE_IN,
    val durationSec: Int = 15,
    val accentHex: Long = 0xFF65BBE5L
)

data class SocialPostScheduleItem(
    val id: String,
    val title: String,
    val channel: String, // Instagram, LinkedIn, YouTube, Facebook
    val dateTimeLabel: String,
    val captionText: String,
    val formatLabel: String,
    val watermarkEnabled: Boolean = false
)

data class LocalStickerAsset(
    val id: String,
    val titleIt: String,
    val categoryIt: String, // Forme, Badge, Frecce, Social, UI Icons
    val tags: List<String>,
    val kind: LayerGeometryKind,
    val colorHex: Long,
    val polygonSides: Int = 6
)

// ============================================================================
// 3. FIGMA & FIGJAM ENGINE MODELS (Auto Layout, Components, Prototype, Collab)
// ============================================================================

enum class DeviceFramePreset(val labelIt: String, val widthPx: Int, val heightPx: Int) {
    ANDROID_PHONE("Android 412×915", 824, 1830),
    IPHONE_16_PRO("iPhone 393×852", 786, 1704),
    IPAD_PRO_11("Tablet 834×1194", 1194, 834),
    DESKTOP_HD("Desktop 1440×900", 1440, 900),
    SQUARE_ARTBOARD("Quadrato 1080×1080", 1080, 1080)
}

enum class AutoLayoutDirection(val labelIt: String) {
    HORIZONTAL_ROW("Riga Orizzontale (→)"),
    VERTICAL_COLUMN("Colonna Verticale (↓)"),
    WRAP_GRID("Griglia Fluida (Wrap)")
}

enum class ConstraintAxisRule(val labelIt: String) {
    START("Sinistra / Alto"),
    END("Destra / Basso"),
    CENTER("Centro"),
    STRETCH_SCALE("Scala Proporzionale")
}

data class FigmaAutoLayoutState(
    val enabled: Boolean = false,
    val direction: AutoLayoutDirection = AutoLayoutDirection.HORIZONTAL_ROW,
    val spacingPx: Float = 24f,
    val paddingHorizontalPx: Float = 32f,
    val paddingVerticalPx: Float = 32f,
    val clipContent: Boolean = true,
    val constraintH: ConstraintAxisRule = ConstraintAxisRule.CENTER,
    val constraintV: ConstraintAxisRule = ConstraintAxisRule.CENTER,
    val layoutGridColumns: Int = 12,
    val layoutGridGutterPx: Float = 16f,
    val layoutGridMarginPx: Float = 24f,
    val showLayoutColumns: Boolean = false
)

enum class ComponentVariantState(val labelIt: String, val scaleMod: Float, val alphaMod: Float) {
    DEFAULT("Default", 1.0f, 1.0f),
    HOVER("Hover (Passaggio)", 1.04f, 1.0f),
    PRESSED("Pressed (Premuto)", 0.95f, 0.9f),
    DISABLED("Disabled (Disabilitato)", 1.0f, 0.42f)
}

data class FigmaComponentMaster(
    val id: String,
    val name: String,
    val sourceLayerId: String,
    val activeVariant: ComponentVariantState = ComponentVariantState.DEFAULT,
    val overrideLabel: String = "",
    val overrideFillHex: Long? = null,
    val publishedToLocalLib: Boolean = true
)

enum class PrototypeTriggerType(val labelIt: String) {
    ON_TAP("Al Tocco (Tap)"),
    ON_DRAG("Al Trascinamento (Drag)"),
    WHILE_PRESSED("Mentre tieni premuto"),
    AFTER_DELAY("Timer Automatico (800ms)")
}

enum class PrototypeTransitionType(val labelIt: String) {
    INSTANT("Istantanea"),
    DISSOLVE("Dissolvenza"),
    SLIDE_IN("Scorrimento Laterale"),
    PUSH("Spinta (Push)"),
    SMART_ANIMATE("Smart Animate (Interpolazione Livelli)")
}

data class PrototypeLink(
    val id: String,
    val sourceLayerId: String,
    val sourceLayerName: String,
    val targetSlideTitle: String,
    val trigger: PrototypeTriggerType = PrototypeTriggerType.ON_TAP,
    val transition: PrototypeTransitionType = PrototypeTransitionType.SMART_ANIMATE,
    val isOverlayModal: Boolean = false
)

data class OfflineCommentThread(
    val id: String,
    val author: String,
    val message: String,
    val artboardX: Float,
    val artboardY: Float,
    val createdAtLabel: String,
    val isResolved: Boolean = false,
    val replies: List<String> = emptyList()
)

data class VersionSnapshotItem(
    val id: String,
    val versionTag: String,
    val titleIt: String,
    val author: String,
    val timestampLabel: String,
    val layerCount: Int,
    val nodeCount: Int,
    val diffSummaryIt: String,
    val serializedJson: String
)

data class FigJamStickyItem(
    val id: String,
    val text: String,
    val author: String,
    val colorHex: Long,
    val x: Float,
    val y: Float,
    val votes: Int = 1,
    val connectedToId: String? = null
)
