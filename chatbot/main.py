import os
import re
import glob
import io
import json
import unicodedata
import logging
from typing import List, Dict, Optional, Any
from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Configuración de logs
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("pitagoras-chatbot")

app = FastAPI(
    title="RetroPlani Mate V3 - Asistente y Validador IA de Planificaciones (UBB)",
    description="Microservicio de IA pedagógica y retroalimentación formativa para Pedagogía en Matemática (UBB / MINEDUC)",
    version="3.0.0"
)

# Configurar CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Almacenamiento en memoria para sesiones conversacionales, flujo dialógico V3 y base documental PDF
SESSION_INTERACTION_IDS: Dict[str, str] = {}
SESSION_STATE: Dict[str, Dict[str, Any]] = {}
RUNTIME_API_KEY: Optional[str] = None
RUTA_RECURSOS = os.environ.get("CHATBOT_RECURSOS_DIR", "./recursos")
PDF_CHUNKS_INDEX: List[Dict[str, str]] = []

# ==============================================================================
# Modelos de Datos (Pydantic)
# ==============================================================================

class ChatRequest(BaseModel):
    session_id: Optional[str] = "default-session"
    message: str
    context: Optional[str] = None

class ChatResponse(BaseModel):
    answer: str
    is_mock: bool = False
    sources: List[str] = Field(default_factory=list, description="Fuentes documentales consultadas")

class ApiKeyConfigRequest(BaseModel):
    api_key: str

class MomentAnalysis(BaseModel):
    inicio: str = Field(description="Evaluación de saludo, objetivo, agenda del día, conexión con clase anterior y activación participativa de conocimientos previos.")
    desarrollo: str = Field(description="Evaluación de actividad central constructivista, protagonismo estudiantil, rol de dupla docente, recursos y monitoreo.")
    cierre: str = Field(description="Evaluación de síntesis participativa, aclaración de dudas, ticket de salida/evaluación formativa y enlace con próxima clase.")

class LessonPlanEvaluationRequest(BaseModel):
    schoolGrade: str = Field(description="Nivel escolar (Ej: 1° Medio, 8° Básico)")
    mathAxis: str = Field(description="Eje temático: NUMEROS, ALGEBRA, GEOMETRIA, PROBABILIDAD_ESTADISTICA")
    baseLearningObjective: str = Field(description="Objetivo de Aprendizaje (OA) curricular del MINEDUC")
    specificObjectives: Optional[str] = Field(default="", description="Objetivo de la clase (Habilidad + Contenido + Actitud)")
    objetivoActitudinal: Optional[str] = Field(default="", description="Objetivo de Aprendizaje Transversal / Actitudinal (OAA)")
    duracion: Optional[str] = Field(default="90 minutos", description="Duración estimada de la clase")
    recursos: Optional[str] = Field(default="", description="Recursos didácticos y materiales anexos")
    evaluacion: Optional[str] = Field(default="", description="Instrumentos de evaluación formativa y pauta de corrección")
    inicio: str = Field(description="Descripción del momento de Inicio")
    desarrollo: str = Field(description="Descripción del momento de Desarrollo")
    cierre: str = Field(description="Descripción del momento de Cierre")

class LessonPlanEvaluationResponse(BaseModel):
    aiApproved: bool = Field(description="Indica si la planificación cuenta con coherencia pedagógica suficiente.")
    score: float = Field(description="Nota pedagógica estimada de referencia en escala 1.0 a 7.0 (60% exigencia según pauta UBB)")
    feedback: str = Field(description="Resumen cualitativo y retroalimentación pedagógica general según RetroPlani Mate V3")
    strengths: List[str] = Field(description="Fortalezas observadas según la rúbrica de 14 indicadores")
    improvements: List[str] = Field(description="Aspectos de mejora por componente (Objetivo, Momentos, Evaluación, DUA, Recursos, Errores)")
    momentAnalysis: MomentAnalysis = Field(description="Análisis pormenorizado de los tres momentos de la clase")
    diagnosticQuestions: List[str] = Field(default_factory=list, description="Preguntas diagnósticas y de reflexión para la acción (Flujo Dialógico V3)")
    is_mock: bool = False

# ==============================================================================
# Utilidades de Procesamiento de Texto e Indexación de PDFs (Recursos Adjuntos)
# ==============================================================================

def normalizar_texto(texto: str) -> str:
    """Convierte a minúsculas y elimina tildes/diacríticos para búsqueda semántica robusta."""
    if not texto:
        return ""
    texto_lower = texto.lower()
    return "".join(
        c for c in unicodedata.normalize("NFD", texto_lower)
        if unicodedata.category(c) != "Mn"
    )

def cargar_indice_pdfs():
    """Carga y fragmenta los PDFs oficiales de la carpeta ./recursos al iniciar el servicio."""
    global PDF_CHUNKS_INDEX
    PDF_CHUNKS_INDEX = []

    if not os.path.exists(RUTA_RECURSOS):
        logger.warning(f"No existe la carpeta de recursos: {RUTA_RECURSOS}")
        return

    pdf_files = sorted(glob.glob(os.path.join(RUTA_RECURSOS, "*.pdf")))
    if not pdf_files:
        logger.info(f"No se encontraron archivos PDF en {RUTA_RECURSOS}")
        return

    try:
        from pypdf import PdfReader
        for pdf_path in pdf_files:
            filename = os.path.basename(pdf_path)
            try:
                reader = PdfReader(pdf_path)
                for page_idx, page in enumerate(reader.pages):
                    raw_text = page.extract_text() or ""
                    clean_text = re.sub(r"\s+", " ", raw_text).strip()
                    if len(clean_text) > 60:
                        PDF_CHUNKS_INDEX.append({
                            "source": filename,
                            "page": str(page_idx + 1),
                            "text": clean_text,
                            "norm": normalizar_texto(clean_text)
                        })
            except Exception as e:
                logger.warning(f"No se pudo leer {filename}: {e}")
        logger.info(f"Base documental RetroPlani Mate V3 cargada: {len(PDF_CHUNKS_INDEX)} páginas indexadas de {len(pdf_files)} PDFs.")
    except ImportError:
        logger.warning("Librería pypdf no instalada; se usará base de conocimiento interna.")

# Cargar PDFs al importar el módulo
cargar_indice_pdfs()

STOPWORDS_ES = {
    "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en",
    "con", "por", "para", "sin", "sobre", "entre", "hacia", "hasta", "desde", "que", "como",
    "cual", "cuales", "quien", "cuando", "donde", "es", "son", "fue", "ser", "estar", "esta",
    "este", "estos", "estas", "ese", "esa", "esos", "esas", "mi", "tu", "su", "mis", "tus",
    "sus", "me", "te", "se", "nos", "les", "lo", "le", "y", "o", "u", "pero", "si", "no",
    "mas", "muy", "ya", "todo", "toda", "todos", "todas", "hacer", "puedo", "puede", "debe",
    "deben", "tener", "tiene", "tienen", "quiero", "necesito", "sugieres", "enseñar", "clase"
}

def buscar_en_recursos_pdf(query: str, top_k: int = 3) -> List[Dict[str, str]]:
    """Busca los fragmentos más relevantes en los 11 PDFs oficiales cargados en ./recursos."""
    if not PDF_CHUNKS_INDEX:
        return []
    q_norm = normalizar_texto(query)
    tokens = [t for t in re.findall(r"[a-z0-9]{3,}", q_norm) if t not in STOPWORDS_ES]
    if not tokens:
        return []

    scored = []
    for chunk in PDF_CHUNKS_INDEX:
        score = 0
        c_norm = chunk["norm"]
        for tok in tokens:
            if tok in c_norm:
                score += 2 + min(c_norm.count(tok), 3)
        if score > 0:
            scored.append((score, chunk))

    scored.sort(key=lambda x: x[0], reverse=True)
    return [item[1] for item in scored[:top_k]]

def extraer_texto_archivo_subido(filename: str, content_bytes: bytes) -> str:
    """Extrae texto desde un archivo .pdf o .txt/.md subido por el estudiante."""
    lower_name = filename.lower()
    if lower_name.endswith(".pdf"):
        try:
            from pypdf import PdfReader
            reader = PdfReader(io.BytesIO(content_bytes))
            pages_text = [page.extract_text() or "" for page in reader.pages]
            return "\n".join(pages_text).strip()
        except Exception as e:
            logger.warning(f"Error extrayendo PDF subido: {e}")
            return ""
    else:
        try:
            return content_bytes.decode("utf-8", errors="ignore").strip()
        except Exception:
            return ""

def detectar_nivel_escolar(msg_norm: str) -> Optional[str]:
    patrones = [
        (r"1\s*(?:°|ro|er|ero)?\s*basico|primero\s*basico", "1° Básico"),
        (r"2\s*(?:°|do)?\s*basico|segundo\s*basico", "2° Básico"),
        (r"3\s*(?:°|ro|er|ero)?\s*basico|tercero\s*basico", "3° Básico"),
        (r"4\s*(?:°|to)?\s*basico|cuarto\s*basico", "4° Básico"),
        (r"5\s*(?:°|to)?\s*basico|quinto\s*basico", "5° Básico"),
        (r"6\s*(?:°|to)?\s*basico|sexto\s*basico", "6° Básico"),
        (r"7\s*(?:°|mo)?\s*basico|septimo\s*basico", "7° Básico"),
        (r"8\s*(?:°|vo)?\s*basico|octavo\s*basico", "8° Básico"),
        (r"1\s*(?:°|ro|er|ero)?\s*medio|primero\s*medio", "1° Medio"),
        (r"2\s*(?:°|do)?\s*medio|segundo\s*medio", "2° Medio"),
        (r"3\s*(?:°|ro|er|ero)?\s*medio|tercero\s*medio", "3° Medio"),
        (r"4\s*(?:°|to)?\s*medio|cuarto\s*medio", "4° Medio"),
        (r"educacion\s*basica|basica", "Educación Básica"),
        (r"educacion\s*media|ensenanza\s*media|media", "Educación Media"),
    ]
    for patron, nombre in patrones:
        if re.search(patron, msg_norm):
            return nombre
    return None

# ==============================================================================
# Verificación de Cliente Gemini
# ==============================================================================

def get_gemini_client():
    api_key = (RUNTIME_API_KEY or os.environ.get("GOOGLE_API_KEY", "")).strip()
    if not api_key or api_key == "mock-dev-key" or not api_key.startswith("AIzaSy"):
        return None
    try:
        from google import genai
        return genai.Client(api_key=api_key)
    except Exception as e:
        logger.warning(f"No se pudo inicializar el cliente oficial google-genai: {e}")
        return None

# ==============================================================================
# Instrucciones Oficiales de RetroPlani Mate V3 (Prof. Víctor Parra González - UBB)
# ==============================================================================

SYSTEM_INSTRUCTION_RETROPLANI_V3 = """
Eres RetroPlani Mate (V3), un asistente pedagógico especializado en orientar, revisar y retroalimentar planificaciones de clases de matemática diseñadas por profesores en formación de la Universidad del Bío-Bío (UBB), Chile (enfocado en 7.º básico a 4.º medio y su articulación con educación básica según las Bases Curriculares del MINEDUC).

REGLAS PEDAGÓGICAS Y DE FLUJO (RetroPlani Mate V3 - Prof. Víctor Parra González):
1. PRINCIPIO DE AUTONOMÍA: Tu propósito es entregar orientación y retroalimentación formativa, constructiva, específica y breve para ayudar al profesor en formación a mejorar autónomamente su planificación y sus recursos didácticos. NO debes diseñar ni redactar planificaciones completas desde cero por el usuario para copiar y pegar.
2. REDACCIÓN DEL OBJETIVO DE CLASE (Pauta y Comentarios UBB):
   - El objetivo de la clase debe incorporar SOLO 1 Habilidad (el verbo de mayor nivel taxonómico según Bloom/Marzano; evitar verbos vagos como "conocer" o redactar actividades como objetivo), el Contenido matemático específico y el Aprendizaje Transversal / Actitudinal (OAA).
3. ESTRUCTURA DE LOS 3 MOMENTOS DE LA CLASE (Documento 01_Momentos_de_la_clase):
   - INICIO: 1) Saludo inicial y presentación del objetivo de aprendizaje esperado, 2) Agenda del día (actividades, evaluación y tarea, acción DUA 78 para reducir incertidumbre), 3) Resumen/conexión con la clase anterior, 4) Actividad de activación de conocimientos previos con participación activa y equitativa de TODOS los estudiantes (evitar solo preguntas abiertas verbales respondidas por unos pocos; sugerir lluvia de ideas escrita, pelota preguntona, reloj de cita, ticket de entrada).
   - DESARROLLO: Núcleo de la clase con enfoque CONSTRUCTIVISTA (que los estudiantes descubran por sí mismos a través de las actividades los nuevos conocimientos; el concepto matemático es la síntesis del trabajo de los estudiantes guiados por el docente, evitando clases expositivas de 60 minutos). Debe explicitar el rol de cada integrante de la dupla docente (Docente 1 y Docente 2) y el equilibrio de tiempos.
   - CIERRE: a) Síntesis de lo aprendido extrayendo conceptos clave con participación estudiantil, b) Aclarar dudas, c) Identificar aprendizajes débiles, d) Evaluar formativamente el objetivo (Ticket de salida, gamificación), e) Enlace e indicaciones para la próxima clase y despedida.
4. EVALUACIÓN PARA EL APRENDIZAJE (Documento 04_Comentarios):
   - "Sin instrumento no existe registro de evidencias del aprendizaje."
   - Todo Ticket de salida, guía evaluada, escala de apreciación (1 a 3 o 1 a 5) o rúbrica debe incluir indicadores para conceptos, habilidades y actitudes (OAA), además de pauta de corrección con puntajes y escala de logro (Logro Sobresaliente: 85%-100%, Logro Adecuado: 70%-84%, Logro Básico: 50%-69%, Logro Insuficiente: <50%).
5. DISEÑO UNIVERSAL PARA EL APRENDIZAJE (DUA) Y MATEMÁTICA INCLUSIVA (Documentos 03 y 08):
   - No basta con declarar el DUA en una columna; debe operacionalizarse en acciones concretas en Inicio, Desarrollo y Cierre según los 3 Principios: Representación (acciones 1-36), Acción y Expresión (acciones 38-68) e Implicación y Compromiso (acciones 69-103), atendiendo barreras de estudiantes con NEE (NEET: CIL, DEA, FIL, TDAH; NEEP: DIL, TEA) sin bajar la exigencia matemática.
6. ERRORES Y DIFICULTADES MATEMÁTICAS Y BIBLIOGRAFÍA:
   - No basta con listar dificultades; el profesor en formación debe explicitar cómo las anticipará y abordará durante la clase, apoyándose en referentes teóricos (Rico, 1997; Socas, 1997; Godino, 2004; Bases Curriculares MINEDUC).
7. FLUJO DIALÓGICO:
   - Cuando revises una planificación completa, formula hasta 5 preguntas diagnósticas sobre los puntos débiles o ambiguos y cierra la retroalimentación con preguntas de reflexión para la acción de mejora.
"""

# ==============================================================================
# Base de Conocimiento Pedagógico y Curricular (MINEDUC / UBB / RetroPlani V3)
# ==============================================================================

def responder_fracciones_y_decimales(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "5° a 6° Básico"
    es_previos = any(k in msg_norm for k in ["previo", "prerrequisito", "antes", "consolidado", "base", "necesitan saber"])
    es_gancho = any(k in msg_norm for k in ["gancho", "motivacion", "motivar", "inicio", "cotidiana", "problema"])
    es_error = any(k in msg_norm for k in ["error", "dificultad", "confund", "equivoc"])

    if es_previos:
        return (
            f"Para abordar con éxito la **operatoria con fracciones en {curso}** (alineado con los **OA 5, OA 6 y OA 8 del MINEDUC** y su proyección a $\\mathbb{{Q}}$ en 7° y 8° Básico), "
            "los estudiantes deben tener consolidados los siguientes **conocimientos previos clave**:\n\n"
            "1. **Concepto de fracción como parte-todo, cociente y medida (4° y 5° Básico):**\n"
            "   • Comprender el significado del *denominador* (partes iguales en que se divide el entero) y del *numerador* (partes consideradas), por ejemplo en $\\frac{3}{4}$ (**Acción DUA 18**: apoyar vocabulario con representación visual).\n"
            "   • Ubicar fracciones propias e impropias en la **recta numérica**.\n"
            "2. **Equivalencia de fracciones (Amplificación y Simplificación):**\n"
            "   • Reconocer equivalencias como $\\frac{1}{2} = \\frac{2}{4} = \\frac{3}{6}$ (indispensable antes de sumar o restar con distinto denominador).\n"
            "   • Simplificar fracciones hasta obtener una **fracción irreductible** usando divisores comunes.\n"
            "3. **Fracciones impropias y Números Mixtos (OA 5 de 6° Básico):**\n"
            "   • Transformar entre números mixtos (ej. $2\\frac{1}{3}$) y fracciones impropias ($\\frac{7}{3}$) de forma pictórica y simbólica: $2\\frac{1}{3} = \\frac{2 \\cdot 3 + 1}{3} = \\frac{7}{3}$.\n"
            "4. **Múltiplos, Factores y Mínimo Común Múltiplo (MCM - OA 1 de 6° Básico):**\n"
            "   • Calcular el $\\text{MCM}$ entre dos o más números para hallar el **denominador común** en adiciones y sustracciones.\n"
            "5. **Adición y sustracción con igual denominador:**\n"
            "   • Comprender que al mantener el denominador se conserva la unidad fraccionaria: $\\frac{2}{7} + \\frac{3}{7} = \\frac{5}{7}$.\n\n"
            "💡 **Sugerencia para el Inicio de tu planificación (Rúbrica UBB - Indicadores 4 y 10):**\n"
            "• Evita hacer solo preguntas abiertas al aire (donde responden siempre los mismos 2 o 3 alumnos). Utiliza un **Ticket de Entrada** o pizarras individuales (**Acción DUA 39**) pidiendo representar pictóricamente $\\frac{1}{2} + \\frac{1}{4}$ para diagnosticar si cometen el error frecuente de sumar numeradores y denominadores linealmente ($\\frac{1}{2} + \\frac{1}{4} \\neq \\frac{2}{6}$)."
        )

    if es_gancho:
        return (
            f"Aquí tienes **3 ganchos motivacionales constructivistas** para introducir **Fracciones en {curso}** (orientados a que los estudiantes descubran el concepto antes de la fórmula, según pauta UBB):\n\n"
            "1. ** El Conflicto Cognitivo de la Pizza (Enfoque COPISI):**\n"
            "   • Presenta visualmente en el PPT: *«En una convivencia sobró $\\frac{1}{2}$ de una pizza vegetariana y $\\frac{1}{3}$ de una napolitana del mismo tamaño. Un estudiante afirma que en total sobraron $\\frac{2}{5}$ de pizza. ¿Por qué esta respuesta es imposible si $\\frac{2}{5}$ es menos que la mitad ($\\frac{1}{2}$) que ya teníamos?»*\n"
            "   • Pide que todos escriban su hipótesis en 2 minutos antes de socializar (**participación equitativa**).\n"
            "2. ** El Desafío de la Batería del Celular:**\n"
            "   • Muestra un celular con $\\frac{7}{8}$ de carga: si grabar un video consume $\\frac{1}{4}$ de batería y un juego consume $\\frac{1}{2}$, ¿queda batería suficiente para volver a casa?\n"
            "3. **Plegado Manipulativo con Tiras de Papel (Acción DUA 41):**\n"
            "   • Entrega dos tiras de papel iguales: una doblada en medios ($\\frac{1}{2}$) y otra en cuartos ($\\frac{1}{4}$). Desafía a las parejas a descubrir cómo expresar la suma haciendo un doblez común."
        )

    if es_error:
        return (
            f"Para la sección **«Errores y Dificultades»** de tu planificación en **Fracciones ({curso})** (Indicadores 12 y 13 de la Rúbrica UBB), considera estos obstáculos epistemológicos (Rico, 1997; Socas, 1997) y **cómo abordarlos en clase**:\n\n"
            "• **Error 1 (Extensión indebida de los Números Naturales):** Operar numerador con numerador y denominador con denominador en la suma ($\\frac{a}{b} + \\frac{c}{d} = \\frac{a+c}{b+d}$).\n"
            "  *Abordaje docente en clase:* Contrastar con tiras fraccionarias superpuestas (**COPISI**) para que el estudiante visualice que no se pueden sumar partes de distinto tamaño sin subdividirlas antes en partes equivalentes.\n"
            "• **Error 2 (Orden inverso en denominadores):** Creer que $\\frac{1}{8} > \\frac{1}{4}$ porque $8 > 4$.\n"
            "  *Abordaje docente en clase:* Ubicar ambas fracciones en la recta numérica entre $0$ y $1$ y comparar áreas concretas.\n"
            "• **Error 3 (Conversión de número mixto):** Confundir $2\\frac{1}{3}$ con $\\frac{2 \\cdot 1 + 3}{3}$.\n"
            "  *Abordaje docente en clase:* Representar primero los $2$ enteros divididos en tercios ($\\frac{3}{3} + \\frac{3}{3} + \\frac{1}{3} = \\frac{7}{3}$)."
        )

    return (
        f"Orientaciones de **RetroPlani Mate V3** para tu planificación de **Fracciones en {curso}**:\n\n"
        "• **Redacción del Objetivo de Clase:** Recuerda usar **1 solo verbo de habilidad**, el contenido y la actitud. Ejemplo: *«Resolver adiciones y sustracciones de fracciones con distinto denominador mediante representaciones pictóricas y simbólicas, trabajando de forma perseverante y colaborativa»*.\n"
        "• **Desarrollo Constructivista:** Evita partir con el algoritmo del $\\text{MCM}$ en la pizarra. Propón primero un problema visual para que los estudiantes descubran la necesidad de igualar las partes (amplificación/simplificación).\n"
        "• **Acción DUA sugerida (Documento 08):** *Acción 18* (apoyar términos «numerador» y «denominador» con claves visuales) y *Acción 91* (tutoría entre pares para equivalencias fraccionarias)."
    )


def responder_numeros_enteros(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "7° Básico"
    es_gancho = any(k in msg_norm for k in ["gancho", "motivacion", "motivar", "inicio", "idea", "sugier", "introducir", "activar"])
    es_previos = any(k in msg_norm for k in ["previo", "prerrequisito", "antes", "consolidado", "base"])

    if es_gancho:
        return (
            f"Aquí tienes **4 ganchos motivacionales constructivistas** para introducir **Números Enteros ($\\mathbb{{Z}}$) en {curso}** (alineados con el **OA 1 del MINEDUC** y el enfoque de descubrimiento guiado de la UBB):\n\n"
            "1. ** Pronóstico Extremo en Chile («De Arica a la Antártica»):**\n"
            "   • Proyecta en el PPT un mapa meteorológico real: **Arica ($+22^\\circ\\text{C}$)**, **Chillán ($0^\\circ\\text{C}$)** y **Base O'Higgins ($-14^\\circ\\text{C}$)**.\n"
            "   • *Actividad participativa de Inicio:* Pide que cada estudiante escriba en su cuaderno o pizarrita individual: *«Si en Chillán hay $0^\\circ\\text{C}$ y en la madrugada baja $6$ grados más, ¿cómo representamos esa temperatura si los números naturales ya no alcanzan? ¿Cuántos grados de diferencia hay entre $+22^\\circ\\text{C}$ y $-14^\\circ\\text{C}$?»*\n\n"
            "2. ** Del Volcán Ojos del Salado a la Fosa de Atacama:**\n"
            "   • Muestra el relieve chileno comparando la cumbre del **Ojos del Salado ($+6.893\\text{ m}$)** con la **Fosa de Atacama ($-8.065\\text{ m}$ bajo el nivel del mar)**.\n"
            "   • *Pregunta guía:* «¿Qué rol cumple el nivel del mar ($0\\text{ m}$) y qué significa que un submarino pase de $-200\\text{ m}$ a $-550\\text{ m}$?»\n\n"
            "3. ** El Desafío del Saldo en Contra (Tarjeta Bip! o Cuenta Bancaria):**\n"
            "   • Simula una tarjeta con $\\$1.500$ de saldo que realiza un viaje de emergencia de $\\$2.300$, quedando con saldo negativo ($-\\$800$). ¿Cuánto debe recargar para volver a tener $\\$1.000$ disponibles?\n\n"
            "4. **El Ascensor de los Subterráneos:**\n"
            "   • Modela una botonera vertical ($3, 2, 1, 0, -1, -2, -3$) para que descubran el orden y la distancia al cero (**valor absoluto** $|a|$).\n\n"
            " **Recordatorio de la Pauta UBB (`01_Momentos_de_la_clase.pdf`):**\n"
            "En tu **Inicio (15 min)** recuerda incluir siempre: 1) Saludo y objetivo, 2) **Agenda del día** (**Acción DUA 78**), 3) Conexión con la clase anterior y 4) **Activación participativa** donde respondan todos los estudiantes (no solo preguntas abiertas verbales)."
        )

    if es_previos:
        return (
            f"Para abordar **Números Enteros ($\\mathbb{{Z}}$) en {curso}** (**OA 1 y OA 2 de las Bases Curriculares**), los estudiantes deben tener consolidados:\n\n"
            "1. **Ubicación, orden y comparación de Números Naturales ($\\mathbb{N}_0$) en la recta numérica:** Comprender el $0$ como punto de referencia.\n"
            "2. **Adición y sustracción en $\\mathbb{N}_0$ como desplazamientos** hacia la derecha (avanzar) o izquierda (retroceder).\n"
            "3. **Noción de magnitudes con sentidos opuestos:** Sobre/bajo cero, ganancia/deuda, sobre/bajo el nivel del mar.\n\n"
            "• **Acción DUA recomendada (`08_Ejemplos_Acciones_DUA_Matematica.pdf` - Acción 28):** Presentar la información de manera progresiva en $7^\\circ$ Básico: iniciar con contextos y recta numérica, luego comparación ($-5 < -2$) y finalmente operatoria con fichas bicolor y recta numérica."
        )

    return (
        f"Orientaciones didácticas para **Números Enteros ($\\mathbb{{Z}}$) en {curso}** (**OA 1 y OA 2 MINEDUC**):\n\n"
        "• **Enfoque Constructivista y COPISI:**\n"
        "  1. **Concreto:** Fichas bicolor ($+1$ azul, $-1$ roja) trabajando el principio de **pares nulos** ($+1 + (-1) = 0$).\n"
        "  2. **Pictórico:** Desplazamientos en la recta numérica horizontal y vertical (termómetro/altitud).\n"
        "  3. **Simbólico:** Comprensión del valor absoluto $|-a| = a$ como distancia al cero.\n"
        "• **Errores y dificultades a anticipar (Rico, 1997):**\n"
        "  - Creer que $-12 > -4$ porque $12 > 4$ (obstáculo del orden en $\\mathbb{N}$).\n"
        "  - Aplicar mecánicamente la frase *«menos por menos es más»* en sumas como $-5 + (-3) = -8$ (confundiendo adición de enteros de 7° Básico con multiplicación de 8° Básico)."
    )


def responder_probabilidad_y_estadistica(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "Educación Básica y Media"
    es_enfoque = any(k in msg_norm for k in ["enfoque", "mineduc", "didactic", "introducir", "como ensenar", "sugerido", "curricular"])
    es_gancho = any(k in msg_norm for k in ["gancho", "motivacion", "motivar", "inicio", "juego"])
    es_previos = any(k in msg_norm for k in ["previo", "prerrequisito", "antes", "consolidado"])

    if es_enfoque or "basica" in msg_norm:
        return (
            f"El **enfoque didáctico sugerido por el MINEDUC** para introducir y desarrollar la **Probabilidad en {curso}** "
            "se basa en transitar desde la **intuición experimental y frecuencial** hacia el modelo teórico (**Método COPISI**):\n\n"
            "1. **Exploración Intuitiva de la Incertidumbre (1° a 4° Básico):**\n"
            "   • Se parte desde **juegos de azar concretos** y situaciones cotidianas, clasificando resultados como **«seguro», «posible», «poco posible» o «imposible»**.\n\n"
            "2. **Enfoque Frecuencial / Experimental (5° a 7° Básico - OA 22, OA 25 y OA 18):**\n"
            "   • **Concreto:** Los estudiantes realizan **experimentos aleatorios repetidos** (lanzar monedas, dados, ruletas o extraer fichas de una bolsa opaca — **Acción DUA 74**).\n"
            "   • **Pictórico:** Registran resultados en tablas de frecuencia, gráficos de barras y diagramas de árbol o de puntos.\n"
            "   • **Simbólico:** Calculan la **frecuencia relativa** $f_r = \\frac{\\text{frecuencia absoluta}}{n}$ y descubren que al aumentar el número de lanzamientos $n$, la frecuencia relativa se estabiliza cerca de un valor fijo (**intuición de la Ley de los Grandes Números**).\n\n"
            "3. **Formalización con la Regla de Laplace y Reglas Aditiva/Multiplicativa (7° Básico a 2° Medio):**\n"
            "   • Una vez comprendida la frecuencia experimental, se introduce la probabilidad teórica en espacios equiprobables mediante la **Regla de Laplace**:\n"
            "     $$P(A) = \\frac{\\text{Casos favorables al evento } A}{\\text{Total de casos posibles del espacio muestral } \\Omega}$$\n"
            "   • En **1° y 2° Medio** (como en el *Ejemplo 1 de Planificación Comentada UBB*), se avanza hacia las **reglas aditiva y multiplicativa**, tablas de contingencia y árboles de probabilidad.\n\n"
            "4. **Criterio Clave de RetroPlani Mate V3 (`09_Ejemplo 1 Planificación comentada.pdf`):**\n"
            "   • Recuerda redactar el objetivo de clase con una habilidad precisa (ej. *«Aplicar la Ley de Laplace para calcular probabilidades en experimentos aleatorios, trabajando de manera activa y reflexiva»*) y evitar verbos genéricos como *«Reforzar aprendizajes»*."
        )

    if es_gancho:
        return (
            f"Aquí tienes **3 ganchos motivacionales constructivistas** para **Probabilidad y Estadística ({curso})**:\n\n"
            "1. ** El Juego de la Suma de Dos Dados (¿Es un juego justo?):**\n"
            "   • Propón un duelo corto: el Equipo A gana si al lanzar dos dados la suma es $2, 3, 11$ o $12$; el Equipo B gana si la suma es $6, 7$ u $8$. Tras 10 lanzamientos, pregunta: *«¿Por qué gana casi siempre el Equipo B si el Equipo A tenía 4 números y el Equipo B solo 3?»*\n"
            "2. ** La Bolsa Opaca Misteriosa (Inferencia Frecuencial):**\n"
            "   • Lleva una bolsa con 10 bolitas (ej. 8 rojas y 2 azules) sin revelar su contenido. Diez estudiantes sacan una bolita, anotan el color y la devuelven. ¿Pueden estimar cuántas hay de cada color usando la frecuencia relativa $\\frac{k}{10}$?\n"
            "3. **🪙 El Mito de la Racha (Falacia del Jugador):**\n"
            "   • Si lanzamos una moneda justa y sale 4 veces seguidas «Cara», ¿qué es más probable en el 5° lanzamiento: Cara, Sello o ambos tienen probabilidad $\\frac{1}{2}$?"
        )

    if es_previos:
        return (
            f"Los **conocimientos previos esenciales** para **Probabilidad y Estadística ({curso})** son:\n\n"
            "1. **Equivalencia y conversión entre fracciones, decimales y porcentajes** (ej. $\\frac{1}{4} = 0,25 = 25\\%$), indispensables para interpretar la escala de probabilidad $0 \\le P(A) \\le 1$.\n"
            "2. **Construcción e interpretación de tablas de frecuencias** (absoluta y relativa) y gráficos estadísticos.\n"
            "3. **Concepto de conjunto, espacio muestral ($\\Omega$) y cardinalidad** (conteo mediante principio multiplicativo o diagrama de árbol)."
        )

    return (
        f"Recomendaciones de **RetroPlani Mate V3** para tu planificación en **Probabilidad y Estadística ({curso})**:\n\n"
        "• **Enfoque Constructivista:** Inicia con un experimento aleatorio real o simulación digital antes de enunciar la Regla de Laplace $P(A) = \\frac{\\#A}{\\#\\Omega}$.\n"
        "• **Acciones DUA sugeridas (`08_Ejemplos_Acciones_DUA_Matematica.pdf`):**\n"
        "  - *Acción 30 y 48:* Combinar lanzamiento real de dados/monedas, simulación digital y cálculo teórico.\n"
        "  - *Acción 62:* Entregar plantillas u organizadores gráficos con columnas para variable, frecuencia absoluta, frecuencia relativa y porcentaje."
    )


def responder_algebra_y_funciones(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "7° Básico a 2° Medio"
    return (
        f"Orientaciones didácticas y curriculares para **Álgebra y Funciones ({curso})** según **MINEDUC y RetroPlani Mate V3**:\n\n"
        "• **Gancho Motivacional / Descubrimiento Guiado:**\n"
        "  - *Ecuaciones (7° y 8° Básico):* Usa la **metáfora de la balanza en equilibrio** antes del despeje simbólico. Evita la frase mecánica *«pasa al otro lado con signo contrario»* y enfatiza aplicar la **operación inversa en ambos lados de la igualdad** ($2x + 3 = 11 \\iff 2x + 3 - 3 = 11 - 3$).\n"
        "  - *Función Lineal y Afín (8° Básico y 1° Medio):* Compara dos planes de arriendo o cuentas de luz/agua ($f(x) = mx + n$) preguntando en qué punto ambas tarifas cobran lo mismo (intersección en sistemas de ecuaciones $2 \\times 2$).\n"
        "  - *Función Cuadrática (2° Medio):* Relaciona la parábola $f(x) = ax^2 + bx + c$ con la trayectoria del lanzamiento de un balón de básquetbol (**Acción DUA 36**).\n\n"
        "• **Acciones DUA Específicas (`08_Ejemplos_Acciones_DUA_Matematica.pdf`):**\n"
        "  - *Acción 2:* En sistemas de ecuaciones en $1^\\circ$ Medio, usar un color distinto para cada recta y el mismo color para su ecuación algebraica.\n"
        "  - *Acción 26 y 50:* Entregar primero una tarjeta con pasos secuenciales (*identificar términos, despejar, operar, comprobar*) y retirar el andamiaje gradualmente.\n"
        "  - *Acción 27 y 44:* Utilizar **GeoGebra** para explorar cómo varía la gráfica al mover los parámetros $m, n$ o $a, b, c$."
    )


def responder_geometria(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "8° Básico a 3° Medio"
    return (
        f"Orientaciones de **RetroPlani Mate V3** para **Geometría ({curso})** (basadas en las Bases Curriculares y el *Ejemplo 2 de Planificación Comentada UBB*):\n\n"
        "• **Enfoque Constructivista en el Desarrollo (`10_Ejemplo 2 Planificación comentada.pdf`):**\n"
        "  - Recuerda la observación clave del Prof. Víctor Parra: *«El concepto matemático es la síntesis del trabajo de los estudiantes guiados por el docente»*. En lugar de explicar primero la definición de rotación, traslación o el Teorema de Pitágoras ($a^2 + b^2 = c^2$) y luego dar ejemplos, presenta primero imágenes de la vida diaria o un desafío manipulativo para que los estudiantes descubran las propiedades invariantes.\n\n"
        "• **Errores y Dificultades Frecuentes a Declarar en tu Planificación (Rico, 1997; Godino, 2004):**\n"
        "  - *Transformaciones Isométricas (8° Básico - OA 13):* Confundir isometrías con homotecias (creer que cambia el tamaño), o confundir el sentido de giro horario ($-90^\\circ$) y antihorario ($+90^\\circ$).\n"
        "  - *Teorema de Pitágoras (8° Básico - OA 12):* Aplicar $a^2 + b^2 = c^2$ en triángulos no rectángulos o no identificar correctamente la hipotenusa opuesta al ángulo de $90^\\circ$.\n"
        "  - **Importante:** Asegúrate de que los errores declarados coincidan con los recursos que realmente usarán los alumnos en esa sesión (ej. no declares errores de uso de GeoGebra si en la clase solo el docente proyecta GeoGebra).\n\n"
        "• **Acciones DUA Sugeridas (`08_Ejemplos_Acciones_DUA_Matematica.pdf`):**\n"
        "  - *Acción 8 y 41:* Usar modelos 3D o cubos encajables para área y volumen de prismas y cilindros.\n"
        "  - *Acción 13 y 16:* Insertar apoyos visuales no lingüísticos (flechas para radio/diámetro, glosario visual para *cateto, hipotenusa, vector de traslación*)."
    )


def responder_potencias_raices_porcentajes(msg_norm: str, nivel: Optional[str]) -> str:
    curso = nivel or "7° Básico a 2° Medio"
    if "porcentaje" in msg_norm or "razon" in msg_norm or "proporcion" in msg_norm:
        return (
            f"Orientaciones para **Razones, Proporcionalidad y Porcentajes ({curso})**:\n\n"
            "• **Conocimientos Previos:** Equivalencia entre fracciones con denominador $100$, números decimales y representación pictórica en cuadrícula de $10 \\times 10$ ($\\frac{20}{100} = 0,20 = 20\\%$). \n"
            "• **Gancho Motivacional:** Comparar descuentos reales (*«$3 \\times 2$ en supermercado»* vs. *«$30\\%$ de descuento»*) o analizar el cálculo del IVA ($19\\%$) en una boleta chilena.\n"
            "• **Acciones DUA (`08_Ejemplos_Acciones_DUA_Matematica.pdf`):**\n"
            "  - *Acción 24:* Mostrar un caso de proporcionalidad directa y un contraejemplo que no lo es para contrastar sus características.\n"
            "  - *Acción 45:* Entregar comienzos de frase para apoyar la argumentación escrita: *«Sé que corresponde al $20\\%$ porque...»*."
        )
    return (
        f"Orientaciones para **Potencias, Raíces Enésimas y Logaritmos ({curso} - OA 2 de 2° Medio)**:\n\n"
        "• **Articulación Conceptual:** Presenta la equivalencia estructural entre las tres operaciones:\n"
        "  $$b^x = a \\iff \\sqrt[x]{a} = b \\iff \\log_b(a) = x$$\n"
        "• **Gancho Motivacional:** El doblado sucesivo de una hoja de papel ($2^n$) o la **Escala Sismológica de Magnitud** en terremotos de Chile para comprender por qué los logaritmos permiten representar crecimientos exponenciales.\n"
        "• **Acción DUA 11 (Pre-enseñar vocabulario y símbolos):** Antes de operar, conectar los términos *base, exponente, cantidad subradical y argumento* con ejemplos conocidos de multiplicación iterada."
    )


def verificar_solicitud_diseno_desde_cero(msg_norm: str) -> bool:
    """Detecta si el usuario está pidiendo que el bot le haga o redacte una planificación completa desde cero (Regla 0 y 12 de V3)."""
    frases_bloqueo = [
        "hazme una planificacion", "crea una planificacion", "disena una planificacion",
        "armame una planificacion", "hacer una planificacion desde cero", "haz la planificacion por mi",
        "redactame la planificacion completa", "dame una planificacion lista", "crea una clase completa",
        "hazme la guia completa", "creame el ticket de salida listo"
    ]
    return any(f in msg_norm for f in frases_bloqueo)


def generar_chat_mock(message: str) -> str:
    """
    Motor Pedagógico, Curricular y Documental RetroPlani Mate V3 (UBB / MINEDUC).
    Combina las reglas del Prof. Víctor Parra González con los 11 documentos PDF cargados.
    """
    msg_norm = normalizar_texto(message)
    nivel = detectar_nivel_escolar(msg_norm)

    # 0. Regla Prioritaria 0 y 12 de RetroPlani Mate V3: Límite ante solicitud de hacer la planificación desde cero
    if verificar_solicitud_diseno_desde_cero(msg_norm):
        return (
            "Puedo orientarte con criterios didácticos, pero **no puedo hacer ni redactar la planificación por ti** (**Regla de Autonomía de RetroPlani Mate V3**).\n\n"
            "Mi función es **retroalimentar una planificación que tú ya hayas elaborado** o resolver tus dudas puntuales de didáctica de la matemática. Para revisar tu diseño completo:\n"
            "• Redacta una primera versión propia que incluya **Objetivo de la clase** (1 habilidad + contenido + actitud), **Inicio**, **Desarrollo**, **Cierre**, **Evaluación**, **DUA** y **Errores/Dificultades**.\n"
            "• Súbela como archivo (`.pdf` o `.docx`) con el botón **📎 Adjuntar Planificación** o cárgala en la pestaña **«✨ Validador de Planificaciones»**.\n"
            "• Cuando la compartas, realizaré la verificación documental y te haré **preguntas diagnósticas** para ayudarte a mejorarla autónomamente."
        )

    # 1. Consultas por Eje / Contenido Matemático Específico
    if any(k in msg_norm for k in ["fraccion", "fracciones", "numerador", "denominador", "numero mixto", "numeros mixtos", "mcm", "minimo comun"]):
        return responder_fracciones_y_decimales(msg_norm, nivel)

    if any(k in msg_norm for k in ["entero", "enteros", "negativo", "negativos", "bajo cero", "valor absoluto"]) or (
        "7" in msg_norm and any(k in msg_norm for k in ["numero", "numeros", "gancho"])
    ):
        return responder_numeros_enteros(msg_norm, nivel)

    if any(k in msg_norm for k in ["probabilidad", "probabilidades", "azar", "estadistica", "laplace", "frecuencia", "aleatorio", "datos", "grafico", "media", "mediana", "moda"]):
        return responder_probabilidad_y_estadistica(msg_norm, nivel)

    if any(k in msg_norm for k in ["algebra", "ecuacion", "ecuaciones", "funcion", "funciones", "lineal", "afin", "cuadratica", "patron", "patrones", "incognita", "sistema de ecuaciones", "factorizacion", "producto notable"]):
        return responder_algebra_y_funciones(msg_norm, nivel)

    if any(k in msg_norm for k in ["geometria", "pitagoras", "thales", "triangulo", "angulo", "area", "perimetro", "volumen", "circunferencia", "circulo", "isometrica", "rotacion", "traslacion", "reflexion", "homotecia", "semejanza", "trigonometria", "vector"]):
        return responder_geometria(msg_norm, nivel)

    if any(k in msg_norm for k in ["potencia", "potencias", "raiz", "raices", "logaritmo", "logaritmos", "porcentaje", "porcentajes", "razon", "razones", "proporcion"]):
        return responder_potencias_raices_porcentajes(msg_norm, nivel)

    # 2. Consultas sobre DUA, Inclusión y NEE (Documentos 03 y 08)
    if any(k in msg_norm for k in ["dua", "diversificacion", "inclusiva", "inclusion", "nee", "neet", "neep", "pie", "tea", "tdah", "barrera", "barreras", "decreto 83"]):
        return (
            "Según los documentos oficiales **`03_DUA_Diversificacion_ensenanza.pdf`** y **`08_Ejemplos_Acciones_DUA_Matematica.pdf`** de **RetroPlani Mate V3**:\n\n"
            "⚠️ **Criterio fundamental:** No basta con declarar los códigos del DUA en una columna lateral; las acciones deben verse **operacionalizadas dentro de la narración del Inicio, Desarrollo y Cierre** para atender a estudiantes con **NEET** (DEA, TDAH, FIL, CIL) y **NEEP** (TEA, DIL) manteniendo altas expectativas matemáticas.\n\n"
            "**Ejemplos de Acciones DUA concretas en Matemática (Banco de 103 acciones):**\n"
            "1. **Principio 1: Múltiples medios de Representación (Acciones 1 a 36):**\n"
            "   • *Acción 2 (Color como información):* Usar un color distinto para cada recta y su ecuación en un sistema lineal.\n"
            "   • *Acción 11 y 18 (Vocabulario y símbolos):* Pre-enseñar términos (*cateto, hipotenusa, numerador, pendiente*) con apoyos visuales no lingüísticos.\n"
            "   • *Acción 26 y 28 (Comprensión):* Entregar tarjetas con pasos secuenciales y presentar la información progresivamente.\n"
            "2. **Principio 2: Múltiples medios de Acción y Expresión (Acciones 38 a 68):**\n"
            "   • *Acción 39 y 40:* Permitir responder levantando tarjetas de opción, en pizarritas individuales, esquema visual o explicación oral breve.\n"
            "   • *Acción 43 y 50:* Permitir estrategias variadas (balanza algebraica, tabla o despeje) y retirar los andamiajes gradualmente.\n"
            "3. **Principio 3: Múltiples medios de Implicación y Compromiso (Acciones 69 a 103):**\n"
            "   • *Acción 78 (Reducir incertidumbre):* Mostrar al inicio la **Agenda del día** (actividades, evaluación y tiempos).\n"
            "   • *Acción 91 y 94:* Tutoría entre pares respetuosa y retroalimentación que valore el proceso y la perseverancia.\n\n"
            " **Recursos de apoyo (`04_Comentarios`):**\n"
            "• Decreto 83 MINEDUC: https://especial.mineduc.cl/wp-content/uploads/sites/31/2016/08/Decreto-83-2015.pdf\n"
            "• Curso DUA SENADIS: https://academia.senadis.cl/p/duainstitucionespublicas"
        )

    # 3. Consultas sobre Rúbrica, Pauta o Criterios de Evaluación de Planificaciones (Documento 02)
    if any(k in msg_norm for k in ["rubrica", "pauta", "14 indicadores", "indicadores de planificacion", "como se evalua la planificacion"]):
        return (
            "La **Rúbrica de Planificación de Clases UBB (`02_Rubrica_planificacion_clases.pdf`)** evalúa **14 indicadores** agrupados en 6 criterios:\n\n"
            "1. **Organización de los Objetivos de Aprendizaje (Ind. 1 y 2):**\n"
            "   • Datos de presentación completos (curso, unidad, fecha, recursos, evaluación, dupla docente y estudiantes NEE).\n"
            "   • **Objetivo de la clase** integrando **1 Habilidad** (mayor nivel taxonómico), **Conocimiento matemático** y **Actitud (OAA)**.\n"
            "2. **Preparación de Procesos de Enseñanza (Ind. 3 al 7):**\n"
            "   • Temporización efectiva en 3 momentos.\n"
            "   • **Inicio:** Saludo, objetivo, **agenda del día**, resumen de clase anterior y **activación participativa de conocimientos previos**.\n"
            "   • **Desarrollo:** Actividad central constructivista (descubrimiento guiado), despliegue de recursos, rol de **Docente 1 y Docente 2**.\n"
            "   • **Cierre:** Síntesis de lo aprendido, evaluación formativa breve (Ticket de salida) y enlace con la próxima clase.\n"
            "   • **Anexos:** Describir y adjuntar todos los recursos (Guía, PPT, Ticket de salida).\n"
            "3. **Planificación de la Evaluación para el Aprendizaje (Ind. 8 al 10):**\n"
            "   • Instrumento con indicadores para conceptos, habilidades y actitudes, **pauta de corrección con puntajes** y **escala de logro** ($85\\%-100\\%$ Sobresaliente, $70\\%-84\\%$ Adecuado, $50\\%-69\\%$ Básico, $<50\\%$ Insuficiente).\n"
            "4. **Léxico, Ortografía y Redacción (Ind. 11):** Lenguaje técnico matemático preciso.\n"
            "5. **Dificultades de Aprendizaje 1 y 2 (Ind. 12 y 13):** Identificar errores matemáticos del contenido **y cómo los abordará el docente en la clase**.\n"
            "6. **Referencias Bibliográficas (Ind. 14):** Bases Curriculares MINEDUC, textos escolares y literatura didáctica (ej. Rico, 1997; Socas, 1997; Godino, 2004)."
        )

    # 4. Consultas sobre Enlaces y Recursos Oficiales (Documento 05)
    if any(k in msg_norm for k in ["enlace", "enlaces", "link", "links", "bibliografia", "referencia", "texto escolar", "textos escolares"]):
        return (
            "Aquí tienes los **Enlaces y Referencias Oficiales** de los documentos **`05_Enlaces_recursos_diseno_clases.pdf`** y **`04_Comentarios`** (Prof. Víctor Parra González - UBB):\n\n"
            "• **Programas de Estudio y Bases Curriculares MINEDUC:**\n"
            "  https://www.curriculumnacional.cl/portal/Documentos-Curriculares/Programas/#doc_general\n"
            "• **Acceso a Textos Escolares de Matemática:**\n"
            "  https://www.cldvarica.cl/textos-escolares/\n"
            "• **Estrategias para Activación de Conocimientos Previos (Inicio):**\n"
            "  - https://webdelmaestrocmf.com/portal/estrategias-tecnicas-y-juegos-para-rescatar-conocimientos-previos/\n"
            "  - https://www.orientacionandujar.es/wp-content/uploads/2013/06/Trabajo-cooperativo-Estrategias-de-activacion.pdf\n"
            "  - https://docsp.aulaplaneta.com/labor-docente/tecnicas-para-activar-conocimientos-previos\n"
            "• **Estrategias y Actividades para el Cierre de la Clase:**\n"
            "  - http://www.pregrado.utalca.cl/docs/pdf/material_docente/Como%20desarrollar%20un%20cierre%20de%20clase.pdf\n"
            "  - https://web.tuclase.cl/2023/01/30/estrategias-de-ensenanza-para-los-momentos-una-clase/\n"
            "• **Bibliografía para Errores y Dificultades en Matemática:**\n"
            "  - Rico, L. (1997). *Los errores en el aprendizaje de las matemáticas*: https://funes.uniandes.edu.co/wp-content/uploads/tainacan-items/32454/1212675/RicoL95-100.pdf\n"
            "  - Godino, J. (2004). *Didáctica de las Matemáticas para Maestros*: https://www.ugr.es/~jgodino/edumat-maestros/manual/9_didactica_maestros.pdf"
        )

    # 5. Momento de Inicio
    if any(k in msg_norm for k in ["inicio", "abrir", "activar", "agenda del dia"]):
        return (
            "Según el documento **`01_Momentos_de_la_clase.pdf`** y **`04_Comentarios`** (Prof. Víctor Parra González), un **Inicio** completo (10-15 min) debe incluir estos **4 elementos**:\n\n"
            "1. **Saludo inicial y presentación del Objetivo de la clase:** Leído y proyectado con claridad, verificando que los estudiantes lo comprendan (**Acción DUA 80**).\n"
            "2. **Agenda del día:** Explicar brevemente las actividades, cómo se evaluará la clase y si habrá tarea (**Acción DUA 78**: entrega anticipación y reduce la incertidumbre).\n"
            "3. **Resumen / conexión con la clase anterior:** Dar continuidad a través de un mapa conceptual o breve retroalimentación.\n"
            "4. **Actividad de Activación de Conocimientos Previos participativa:**\n"
            "   • ⚠️ *Criterio UBB:* Evita el uso exclusivo de preguntas abiertas verbales, ya que concentran la participación en unos pocos alumnos. Solicita que **todos escriban su respuesta** (en pizarritas, ticket de entrada, lluvia de ideas escrita, pelota preguntona o reloj de citas) y luego selecciona algunas para compartir."
        )

    # 6. Momento de Cierre y Metacognición
    if any(k in msg_norm for k in ["cierre", "metacognicion", "metacognitiva", "ticket"]):
        return (
            "Según **`01_Momentos_de_la_clase.pdf`** y **`04_Comentarios`**, el **Cierre de la clase (10-15 min)** debe responder a *«¿Qué fue lo más significativo que aprendieron los estudiantes y cuáles son los aprendizajes débiles que quedaron?»* e incluir:\n\n"
            "a) **Síntesis de lo aprendido:** Extraer los conceptos clave con participación activa de los estudiantes (no solo un monólogo del profesor).\n"
            "b) **Aclarar dudas e identificar aprendizajes débiles.**\n"
            "c) **Actividad breve de evaluación formativa del Objetivo de la clase:**\n"
            "   • Aplicar un **Ticket de Salida** o plataforma de gamificación que evalúe **concepto, habilidad y actitud (OAA)**.\n"
            "   • Recuerda anexar el Ticket de Salida junto con su **pauta de corrección, puntajes y respuestas esperadas**.\n"
            "d) **Enlace e indicaciones para la próxima clase y despedida.**"
        )

    # 7. Objetivos de Aprendizaje y Taxonomía de Bloom
    if any(k in msg_norm for k in ["oa", "objetivo", "objetivos", "bloom", "verbo", "marzano"]):
        return (
            "Según **`04_Comentarios_retroalimentacion_planificaciones.pdf`** y la **Rúbrica UBB (Indicador 2)**, la redacción del **Objetivo de la Clase** debe cumplir estrictamente:\n\n"
            "1. **Incluir SOLO 1 Verbo de Habilidad** (si hay más de uno, elige el de **mayor nivel taxonómico** en Bloom o Marzano).\n"
            "   • ⚠️ Evita verbos demasiado amplios como *«Conocer»* o *«Reforzar»*, y no redactes una actividad como si fuera el objetivo. Usa verbos observables como *Identificar, Representar, Aplicar, Resolver, Modelar, Analizar, Argumentar*.\n"
            "2. **Explicitar el Contenido Matemático** delimitado para la sesión.\n"
            "3. **Incorporar el Aprendizaje Transversal / Objetivo Actitudinal (OAA)** en la misma redacción.\n\n"
            " **Ejemplo bien redactado (de Planificación Comentada UBB):**\n"
            "*«Comprender las isometrías en figuras geométricas 2D en diferentes contextos, demostrando curiosidad e interés por resolver desafíos matemáticos (OA B).»*"
        )

    # 8. Evaluación Formativa e Instrumentos
    if any(k in msg_norm for k in ["evaluacion", "evaluar", "formativa", "instrumento", "escala de apreciacion", "lista de cotejo", "pauta de correccion"]):
        return (
            "Según **`04_Comentarios_retroalimentacion_planificaciones.pdf`**, para diseñar la **Evaluación de la Clase** debes cumplir estos criterios:\n\n"
            "1. **Principio central:** *«Sin instrumento no existe registro de evidencias del aprendizaje»*. Si usas un juego, puesta en común o ticket oral, debes acompañarlo de una **Escala de Apreciación (rango 1 a 3 o 1 a 5)** o **Rúbrica**.\n"
            "2. **Tríada de Indicadores:** El instrumento debe incluir indicadores observables para **Conceptos matemáticos, Habilidades y Actitudes (Objetivo Transversal)**.\n"
            "3. **Encabezado formal y Pauta de Corrección obligatoria:**\n"
            "   • Toda guía o Ticket de Salida debe llevar encabezado (Asignatura, profesor/a, logos, título, fecha, objetivo de la clase e instrucciones de tiempo/uso de calculadora) y **anexar su pauta de corrección con puntajes**.\n"
            "4. **Escala Numérica de Niveles de Logro sugerida en la pauta UBB:**\n"
            "   • **Logro Sobresaliente:** $85\\% - 100\\%$ del puntaje total.\n"
            "   • **Logro Adecuado:** $70\\% - 84\\%$.\n"
            "   • **Logro Básico:** $50\\% - 69\\%$.\n"
            "   • **Logro Insuficiente:** Menos del $50\\%$."
        )

    # 9. Momento de Desarrollo y Enfoque Constructivista
    if any(k in msg_norm for k in ["desarrollo", "constructivista", "dupla", "co-ensenanza", "actividad", "actividades", "modelamiento"]):
        return (
            "Según **`01_Momentos_de_la_clase.pdf`** y **`04_Comentarios`** (Prof. Víctor Parra González), el **Momento de Desarrollo (55-60 min)** debe considerar:\n\n"
            "1. **Mirada Constructivista (No Tradicional):**\n"
            "   • *«Uno de los desafíos como docente es que los estudiantes descubran por sí mismos a través de las actividades los nuevos conocimientos. En síntesis, el concepto matemático es la síntesis del trabajo de los estudiantes guiados por el docente.»*\n"
            "   • Evita que el docente exponga durante 60 minutos mientras el curso solo toma apuntes. Parte con ejemplos cotidianos o problemas para que los alumnos descubran las regularidades antes de formalizar.\n"
            "2. **Rol de la Dupla Docente (Co-enseñanza):**\n"
            "   • Especifica con claridad qué acciones realizará **Docente 1** y qué acciones realizará **Docente 2** en cada actividad.\n"
            "3. **Equilibrio de Tiempos y Recursos:**\n"
            "   • Detalla cuántos minutos tomará la presentación en PPT y cuántos minutos el trabajo autónomo/grupal en la guía (recuerda anexar la guía y el PPT a la planificación)."
        )

    # 10. Búsqueda en los 11 PDFs cargados en ./recursos (RAG Documental)
    fragmentos = buscar_en_recursos_pdf(message, top_k=2)
    if fragmentos:
        citas = "\n\n".join(
            f"**Fuente `{f['source']}` (pág. {f['page']}):**\n> *{f['text'][:420]}...*"
            for f in fragmentos
        )
        return (
            f"Con base en los documentos oficiales de **RetroPlani Mate V3 (UBB / MINEDUC)** para tu consulta{f' en **{nivel}**' if nivel else ''}:\n\n"
            f"{citas}\n\n"
            " **Orientación para tu planificación:**\n"
            "• Verifica que tu **Objetivo de clase** tenga **1 solo verbo de habilidad**, el contenido matemático y el objetivo actitudinal (OAA).\n"
            "• En los **3 momentos (Inicio, Desarrollo y Cierre)**, explicita qué hace la dupla docente, cómo participan activamente todos los estudiantes y cómo se operacionalizan las **acciones DUA**."
        )

    # 11. Respuesta adaptativa general
    tema_limpio = message.strip().rstrip("?")
    return (
        f"Respecto a tu consulta sobre **«{tema_limpio}»**{f' en **{nivel}**' if nivel else ''}, desde los criterios de **RetroPlani Mate V3 (UBB)**:\n\n"
        "1. **Coherencia Curricular y Objetivo:** Redacta el objetivo con **1 verbo de habilidad** (Bloom/Marzano), el **contenido matemático** y la **actitud (OAA)**.\n"
        "2. **Inicio (15 min):** Incluye saludo, objetivo, **agenda del día** (**Acción DUA 78**), resumen de la clase anterior y **activación de conocimientos previos** donde participen todos los estudiantes por escrito o con material manipulativo.\n"
        "3. **Desarrollo Constructivista (55-60 min):** Propón actividades donde los estudiantes descubran las propiedades matemáticas antes de la formalización, especificando el rol de **Docente 1 y Docente 2**.\n"
        "4. **Cierre y Evaluación (15 min):** Incluye síntesis participativa y un **Ticket de Salida** anexado con su **pauta de corrección, puntajes y escala de logro** ($85\\%, 70\\%, 50\\%$). No olvides declarar la sección de **Errores y Dificultades** y cómo los abordarás."
    )

# ==============================================================================
# Evaluación Formativa y Flujo Dialógico de Planificaciones (RetroPlani Mate V3)
# ==============================================================================

def generar_evaluacion_mock(req: LessonPlanEvaluationRequest) -> LessonPlanEvaluationResponse:
    """
    Evalúa una planificación aplicando los 14 indicadores de la Rúbrica UBB
    (02_Rubrica_planificacion_clases.pdf) y el Flujo Dialógico de RetroPlani Mate V3.
    """
    strengths = []
    improvements = []
    diagnostic_questions = []
    # Puntaje base sobre 84 puntos (Escala UBB al 60% de exigencia -> 50 pts = 4.0, 73 pts = 6.0, 84 pts = 7.0)
    puntos = 84

    # 1. Análisis del Objetivo de Aprendizaje (OA) y Objetivo de la Clase (Indicadores 1 y 2)
    if req.baseLearningObjective and len(req.baseLearningObjective.strip()) > 15:
        strengths.append(f"Coherencia curricular: Declara el OA para {req.schoolGrade} en el eje {req.mathAxis}.")
    else:
        improvements.append("Coherencia curricular (Ind. 1-2): El OA es muy breve. Especifica el OA oficial de las Bases Curriculares del MINEDUC.")
        puntos -= 6

    obj_clase = (req.specificObjectives or "").strip()
    obj_act = (req.objetivoActitudinal or "").strip()
    if len(obj_clase) > 20:
        if any(v in normalizar_texto(obj_clase) for v in ["conocer", "reforzar", "repasar", "ver "]):
            improvements.append("Objetivo de clase (Ind. 2): Evita verbos vagos o actividades como 'conocer' o 'reforzar'. Usa 1 verbo de habilidad taxonómica (ej. Aplicar, Representar, Resolver, Modelar, Argumentar).")
            diagnostic_questions.append("1) En el objetivo de la clase utilizas un verbo general. ¿Qué habilidad matemática específica (según Bloom/Marzano) esperas que desarrollen los estudiantes en esta sesión?")
            puntos -= 4
        else:
            strengths.append("Objetivo de la clase (Ind. 2): Presenta verbo de habilidad observable y contenido matemático delimitado.")
    else:
        improvements.append("Objetivo de la clase (Ind. 2): Falta explicitar el objetivo específico de la sesión integrando 1 Habilidad, Contenido matemático y Actitud (OAA).")
        diagnostic_questions.append("1) No se observa con claridad el objetivo específico de la clase. ¿Qué habilidad, contenido matemático y actitud esperas lograr en estos 90 minutos?")
        puntos -= 8

    if not obj_act and not any(k in normalizar_texto(obj_clase) for k in ["actitud", "curiosidad", "interes", "perseverancia", "colaborativa", "respeto", "rigor", "reflexiva"]):
        improvements.append("Objetivo Transversal / Actitudinal (Ind. 2): Falta incorporar la habilidad actitudinal (OAA) en la redacción del objetivo de la clase y cómo se evaluará.")
        puntos -= 4

    # 2. Análisis del Momento de Inicio (Indicador 4)
    inicio_norm = normalizar_texto(req.inicio)
    inicio_len = len(req.inicio.strip()) if req.inicio else 0
    if inicio_len < 30:
        improvements.append("Inicio (Ind. 4): Insuficiente. Debe incluir saludo, presentación del objetivo, agenda del día, resumen de la clase anterior y actividad participativa de activación de conocimientos previos.")
        inicio_feedback = "Inicio incompleto. Incorpora la agenda del día (Acción DUA 78) y una actividad de activación de conocimientos previos donde participen todos los estudiantes."
        diagnostic_questions.append("2) En el Inicio no se detalla cómo activarás los conocimientos previos. ¿Qué estrategia usarás para que participe todo el curso y no solo quienes responden verbalmente?")
        puntos -= 10
    else:
        tiene_agenda = "agenda" in inicio_norm or "rutina" in inicio_norm or "secuencia" in inicio_norm
        strengths.append("Inicio (Ind. 4): Presenta contextualización y activación inicial para abrir la clase.")
        if not tiene_agenda:
            improvements.append("Inicio (Ind. 4 / DUA 78): Recuerda explicitar la 'Agenda del día' (actividades, evaluación y tiempos) al comenzar la clase para anticipar y reducir la incertidumbre.")
            inicio_feedback = "Bien encaminado en la activación inicial. Para fortalecerlo según la rúbrica UBB, agrega explícitamente la Agenda del día y asegura que todos los estudiantes registren sus respuestas en la activación."
            puntos -= 2
        else:
            inicio_feedback = "Excelente estructura de Inicio: considera activación de conocimientos previos y anticipación de la agenda del día."

    # 3. Análisis del Momento de Desarrollo (Indicador 5)
    desarrollo_norm = normalizar_texto(req.desarrollo)
    desarrollo_len = len(req.desarrollo.strip()) if req.desarrollo else 0
    if desarrollo_len < 60:
        improvements.append("Desarrollo (Ind. 5): Muy breve. Debe describir la actividad constructivista central, el protagonismo estudiantil, el uso de recursos, la atención DUA y el rol de cada integrante de la dupla docente (Docente 1 y Docente 2).")
        desarrollo_feedback = "Requiere mayor detalle. Recuerda que el concepto matemático debe surgir como síntesis del trabajo de los estudiantes guiados por el docente, especificando tiempos de exposición vs. práctica."
        diagnostic_questions.append("3) En el Desarrollo, ¿qué actividad previa permitirá que los estudiantes descubran o construyan el concepto matemático por sí mismos antes de la formalización docente?")
        puntos -= 14
    else:
        strengths.append(f"Desarrollo (Ind. 5): Evidencia secuencia de trabajo y actividades centradas en el eje {req.mathAxis}.")
        if "docente 1" not in desarrollo_norm and "dupla" not in desarrollo_norm and "co-ensenanza" not in desarrollo_norm:
            improvements.append("Desarrollo y Co-enseñanza (Ind. 5): Especifica qué rol y actividades realizará cada integrante de la dupla docente (Docente 1 y Docente 2) y el equilibrio de tiempos entre PPT y guía.")
            desarrollo_feedback = "Buena secuencia didáctica. Considera detallar el rol específico de Docente 1 y Docente 2 durante el monitoreo y equilibrar el tiempo de exposición con el trabajo autónomo."
        else:
            desarrollo_feedback = "Secuencia constructivista bien articulada, con roles docentes y monitoreo activo del aprendizaje."

    # 4. Análisis del Momento de Cierre (Indicador 6)
    cierre_norm = normalizar_texto(req.cierre)
    cierre_len = len(req.cierre.strip()) if req.cierre else 0
    if cierre_len < 30:
        improvements.append("Cierre (Ind. 6): Incompleto. Debe incluir: a) síntesis participativa de lo aprendido, b) aclaración de dudas, c) actividad breve de evaluación formativa (Ticket de salida) y d) enlace con la próxima clase.")
        cierre_feedback = "Cierre muy escueto. Destina 10-15 minutos a la síntesis protagonizada por los estudiantes y aplica un Ticket de Salida que verifique el logro del objetivo."
        puntos -= 10
    else:
        strengths.append("Cierre (Ind. 6): Considera síntesis final y consolidación metacognitiva de la sesión.")
        cierre_feedback = "Apropiado. Asegúrate de explicitar cómo participan los estudiantes en la síntesis final y de dejar indicaciones o enlace con la próxima clase."

    # 5. Evaluación, Recursos, DUA y Errores/Dificultades (Indicadores 7 al 14)
    eval_norm = normalizar_texto(req.evaluacion or "")
    if not eval_norm or len(eval_norm) < 10:
        improvements.append("Evaluación para el aprendizaje (Ind. 8-9): 'Sin instrumento no existe registro de evidencias'. Declara y anexa el instrumento (Ticket de salida, escala de apreciación o rúbrica) con su pauta de corrección, puntajes y niveles de logro (85%, 70%, 50%).")
        diagnostic_questions.append("4) No se detalla el instrumento de evaluación ni su pauta de corrección. ¿Cómo registrarás evidencia del logro del contenido, la habilidad y la actitud (OAA)?")
        puntos -= 6
    elif "pauta" not in eval_norm and "puntaje" not in eval_norm and "escala" not in eval_norm and "indicador" not in eval_norm:
        improvements.append("Instrumento de Evaluación (Ind. 8): Recuerda anexar al Ticket de salida o guía su pauta de corrección con puntajes, respuestas esperadas y escala de logro (Sobresaliente 85%-100%, Adecuado 70%-84%, Básico 50%-69%).")
        diagnostic_questions.append("4) Mencionas evaluación formativa, pero falta la pauta de corrección. ¿Qué indicadores usarás para evaluar concepto, habilidad y actitud?")
        puntos -= 3
    else:
        strengths.append("Evaluación para el aprendizaje (Ind. 8): Declara estrategia e instrumento para recoger evidencias del logro del objetivo.")

    todo_texto_norm = f"{inicio_norm} {desarrollo_norm} {cierre_norm} {normalizar_texto(req.objetivoActitudinal or '')}"
    if not any(k in todo_texto_norm for k in ["dua", "representacion", "expresion", "compromiso", "visual", "manipul", "nee", "pie"]):
        improvements.append("DUA y Matemática Inclusiva (Documentos 03 y 08): Incorpora acciones concretas de los 3 principios del DUA (Representación, Acción/Expresión e Implicación) operacionalizadas en Inicio, Desarrollo y Cierre.")
        diagnostic_questions.append("5) Para fortalecer la matemática inclusiva (DUA), ¿qué apoyos visuales, manipulativos o de expresión ofrecerás para atender las barreras de los estudiantes con NEE?")

    if not any(k in todo_texto_norm for k in ["error", "dificultad", "obstaculo"]):
        improvements.append("Errores y Dificultades Matemáticas (Ind. 12-13): Incluye la sección de posibles errores/dificultades del contenido matemático y cómo los abordarás en clase (apoyado en bibliografía como Rico, 1997 o Socas, 1997).")

    # Preguntas de reflexión para la acción (Cierre obligatorio de RetroPlani Mate V3)
    if not diagnostic_questions:
        diagnostic_questions = [
            "1) ¿Qué cambio harás primero en tu planificación a partir de esta retroalimentación?",
            "2) ¿Cómo comprobarás que tu Ticket de Salida e instrumento recogen evidencia tanto de la habilidad como de la actitud (OAA)?",
            "3) ¿Qué acción concreta del DUA incorporarás en el Desarrollo para asegurar el protagonismo de todos los estudiantes?"
        ]
    else:
        diagnostic_questions.append("¿Qué cambio harás primero en tu planificación y qué recurso necesitas ajustar o anexar antes de enviarla al profesor colaborador?")

    # Conversión de puntaje a escala 1.0 - 7.0 (60% exigencia sobre 84 puntos según tabla del Prof. Víctor Parra)
    puntos = max(10, min(84, puntos))
    if puntos >= 50:
        score = round(4.0 + ((puntos - 50) / 34.0) * 3.0, 1)
    else:
        score = round(1.0 + (puntos / 50.0) * 2.9, 1)

    score = max(1.0, min(7.0, score))
    approved = score >= 4.0

    feedback = (
        f"Revisión formativa de RetroPlani Mate V3 para {req.schoolGrade} ({req.mathAxis}). "
        + (
            "La planificación presenta una estructura base reconocible en sus tres momentos. Revisa las observaciones específicas de la rúbrica UBB (Agenda del día, enfoque constructivista, pauta de corrección, DUA operacionalizado y abordaje de errores) y las preguntas de reflexión para fortalecer tu versión final."
            if approved else
            "Observación inicial: La planificación presenta vacíos críticos en el desarrollo de sus momentos o en la formulación del objetivo. Completa los apartados indicados y responde las preguntas diagnósticas para asegurar la coherencia entre objetivo, actividades, recursos y evaluación."
        )
    )

    return LessonPlanEvaluationResponse(
        aiApproved=approved,
        score=score,
        feedback=feedback,
        strengths=strengths,
        improvements=improvements,
        momentAnalysis=MomentAnalysis(
            inicio=inicio_feedback,
            desarrollo=desarrollo_feedback,
            cierre=cierre_feedback
        ),
        diagnosticQuestions=diagnostic_questions,
        is_mock=True
    )

# ==============================================================================
# Endpoints de la API
# ==============================================================================

@app.get("/health")
def health_check():
    client = get_gemini_client()
    return {
        "status": "healthy",
        "service": "pitagoras-chatbot",
        "gemini_api_configured": client is not None,
        "documents_loaded": len(PDF_CHUNKS_INDEX),
        "mode": "production (Gemini 3.8 Flash + RetroPlani V3)" if client else "RetroPlani Mate V3 (Base Documental UBB/MINEDUC)"
    }

@app.post("/api/config/apikey")
def configure_api_key(req: ApiKeyConfigRequest):
    """Permite configurar o actualizar la GOOGLE_API_KEY en caliente sin reiniciar el contenedor."""
    global RUNTIME_API_KEY
    key = req.api_key.strip()
    if not key:
        RUNTIME_API_KEY = None
        return {
            "status": "cleared",
            "gemini_api_configured": False,
            "mode": "RetroPlani Mate V3 (Base Documental UBB/MINEDUC)"
        }
    if not key.startswith("AIzaSy"):
        raise HTTPException(status_code=400, detail="La API Key de Google AI Studio debe comenzar con 'AIzaSy'.")
    RUNTIME_API_KEY = key
    client = get_gemini_client()
    return {
        "status": "updated",
        "gemini_api_configured": client is not None,
        "mode": "production (Gemini 3.8 Flash + RetroPlani V3)" if client else "RetroPlani Mate V3 (Base Documental UBB/MINEDUC)"
    }

@app.post("/api/chat", response_model=ChatResponse)
@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    session_id = request.session_id or "default-session"
    client = get_gemini_client()

    # Recuperar fragmentos relevantes de los 11 PDFs oficiales para enriquecer el contexto
    fragmentos_pdf = buscar_en_recursos_pdf(request.message, top_k=3)
    fuentes_usadas = list(dict.fromkeys([f["source"] for f in fragmentos_pdf]))

    if not client:
        logger.info("Generando respuesta con el Motor Pedagógico RetroPlani Mate V3 (UBB/MINEDUC).")
        answer = generar_chat_mock(request.message)
        return ChatResponse(
            answer=answer,
            is_mock=True,
            sources=fuentes_usadas
        )

    try:
        contexto_rag = ""
        if fragmentos_pdf:
            contexto_rag = "\n\nFRAGMENTOS RECUPERADOS DE LOS DOCUMENTOS OFICIALES UBB/MINEDUC:\n" + "\n".join(
                f"[{f['source']} - pág. {f['page']}]: {f['text'][:600]}" for f in fragmentos_pdf
            )

        prev_id = SESSION_INTERACTION_IDS.get(session_id)
        create_kwargs = {
            "model": "gemini-3.8-flash",
            "system_instruction": SYSTEM_INSTRUCTION_RETROPLANI_V3 + contexto_rag,
            "input": request.message,
        }
        if prev_id:
            create_kwargs["previous_interaction_id"] = prev_id

        interaction = client.interactions.create(**create_kwargs)
        if getattr(interaction, "id", None):
            SESSION_INTERACTION_IDS[session_id] = interaction.id

        return ChatResponse(
            answer=interaction.output_text,
            is_mock=False,
            sources=fuentes_usadas
        )

    except Exception as e:
        logger.error(f"Error al llamar a Gemini API en chat: {e}. Usando Motor RetroPlani Mate V3.")
        return ChatResponse(
            answer=generar_chat_mock(request.message),
            is_mock=True,
            sources=fuentes_usadas
        )

@app.post("/api/chat-archivo", response_model=ChatResponse)
async def chat_con_archivo_endpoint(
    file: UploadFile = File(...),
    message: str = Form("Por favor revisa y retroalimenta mi planificación adjunta según los criterios de RetroPlani Mate V3."),
    session_id: str = Form("default-session")
):
    """
    Implementa el Flujo Dialógico de RetroPlani Mate V3 cuando el estudiante adjunta
    un archivo (.pdf, .docx, .txt) de su planificación directamente en el chat.
    """
    content_bytes = await file.read()
    filename = file.filename or "planificacion.pdf"
    texto_extraido = extraer_texto_archivo_subido(filename, content_bytes)

    if not texto_extraido or len(texto_extraido) < 40:
        return ChatResponse(
            answer=(
                f"📄 **Verificación documental inicial (`{filename}`):**\n\n"
                "He recibido tu archivo, pero no pude extraer suficiente texto legible de la planificación. "
                "Asegúrate de subir un archivo `.pdf` o `.txt` con texto seleccionable que incluya al menos: "
                "**Objetivo de la clase, Inicio, Desarrollo y Cierre**, o bien utiliza la pestaña **«✨ Validador de Planificaciones»**."
            ),
            is_mock=True
        )

    texto_norm = normalizar_texto(texto_extraido)
    tiene_objetivo = any(k in texto_norm for k in ["objetivo", "oa ", "aprendizaje"])
    tiene_inicio = "inicio" in texto_norm
    tiene_desarrollo = "desarrollo" in texto_norm
    tiene_cierre = "cierre" in texto_norm
    menciona_guia = "guia" in texto_norm or "cuadernillo" in texto_norm
    menciona_ppt = "ppt" in texto_norm or "presentacion" in texto_norm or "diapo" in texto_norm
    menciona_ticket = "ticket" in texto_norm
    menciona_dua = "dua" in texto_norm or "diversificacion" in texto_norm or "representacion" in texto_norm
    menciona_errores = "error" in texto_norm or "dificultad" in texto_norm

    recursos_declarados = []
    if menciona_guia:
        recursos_declarados.append("Guía de trabajo")
    if menciona_ppt:
        recursos_declarados.append("Presentación PPT")
    if menciona_ticket:
        recursos_declarados.append("Ticket de Salida")
    recursos_str = ", ".join(recursos_declarados) if recursos_declarados else "pizarra/plumón"

    # Construir respuesta estructurada según el protocolo de RetroPlani Mate V3 (Páginas 3 a 12 de la Propuesta)
    preguntas_diagnosticas = []
    if not tiene_objetivo:
        preguntas_diagnosticas.append("1) **Objetivo de la clase:** No se identifica con claridad el objetivo específico. ¿Qué **habilidad única (verbo taxonómico)**, **contenido matemático** y **actitud (OAA)** esperas lograr?")
    else:
        preguntas_diagnosticas.append("1) **Coherencia del Objetivo (Ind. 2):** ¿Tu objetivo de clase incorpora **un solo verbo de habilidad** (evitando actividades) junto con el contenido matemático y el objetivo transversal (OAA)?")

    if "agenda" not in texto_norm:
        preguntas_diagnosticas.append("2) **Inicio y Activación (Ind. 4 / DUA 78):** No se visualiza explícitamente la **Agenda del día**. Además, ¿cómo asegurarás que en la activación de conocimientos previos participen **todos los estudiantes** (ej. por escrito) y no solo quienes responden en voz alta?")

    if "docente 1" not in texto_norm and "dupla" not in texto_norm:
        preguntas_diagnosticas.append("3) **Desarrollo Constructivista y Co-enseñanza (Ind. 5):** ¿Qué actividad permitirá que los estudiantes **descubran el concepto matemático por sí mismos** antes de tu explicación, y qué rol específico cumplirá cada integrante de la **dupla docente (Docente 1 y Docente 2)**?")

    if "pauta" not in texto_norm and "puntaje" not in texto_norm:
        preguntas_diagnosticas.append("4) **Evaluación para el Aprendizaje (Ind. 8):** Declaras evaluación formativa, pero no se observa la **pauta de corrección con puntajes y escala de logro** ($85\\%, 70\\%, 50\\%$). ¿Qué instrumento e indicadores usarás para registrar evidencia de concepto, habilidad y actitud?")

    if not menciona_dua or not menciona_errores:
        preguntas_diagnosticas.append("5) **DUA y Errores Matemáticos (Ind. 12-13):** ¿Qué acciones concretas de representación, expresión y compromiso aplicarás en cada momento para estudiantes con **NEE**, y cómo abordarás los **errores matemáticos frecuentes** durante la clase?")

    preguntas_txt = "\n".join(f"• {q}" for q in preguntas_diagnosticas[:5])

    respuesta_v3 = (
        f"📋 **1. Verificación Documental Inicial (`{filename}`):**\n"
        f"He recibido tu planificación de clase en formato `{filename.split('.')[-1].upper()}`. "
        f"Observo que {'contiene' if (tiene_objetivo and tiene_inicio and tiene_desarrollo and tiene_cierre) else 'menciona parcialmente'} "
        f"**Objetivo de clase, Inicio, Desarrollo y Cierre**. Además, declara como recursos didácticos: **{recursos_str}**.\n\n"
        f"⚠️ **Nota sobre recursos anexos (Regla c de RetroPlani Mate V3):**\n"
        f"Recuerda que todo recurso declarado ({recursos_str}) debe incluirse como anexo junto con su **pauta de corrección y encabezado formal** para verificar su total coherencia con el objetivo.\n\n"
        f"🔍 **2. Flujo Dialógico: Preguntas Diagnósticas Previas:**\n"
        f"Gracias por subir tu planificación. Siguiendo el protocolo de **RetroPlani Mate V3**, antes de entregarte el cierre reflexivo necesito que aclares o revises estos puntos clave detectados en tu documento:\n\n"
        f"{preguntas_txt}\n\n"
        f"💬 **Responde en el chat lo que tengas definido** (o indícame qué puntos aún estás diseñando) para integrarlo en tu retroalimentación final."
    )

    return ChatResponse(
        answer=respuesta_v3,
        is_mock=True,
        sources=["PROPUESTA_DE_GPT_CHATBOT_RETROPLANI_MATE_V3_21-06.pdf", "02_Rubrica_planificacion_clases.pdf"]
    )

@app.post("/api/evaluar-planificacion", response_model=LessonPlanEvaluationResponse)
@app.post("/evaluar-planificacion", response_model=LessonPlanEvaluationResponse)
async def evaluar_planificacion_endpoint(req: LessonPlanEvaluationRequest):
    client = get_gemini_client()

    if not client:
        logger.info("Evaluando planificación con la Rúbrica de 14 Indicadores de RetroPlani Mate V3.")
        return generar_evaluacion_mock(req)

    try:
        plan_content = f"""
        Nivel Escolar: {req.schoolGrade}
        Eje Matemático: {req.mathAxis}
        Objetivo de Aprendizaje (OA): {req.baseLearningObjective}
        Objetivo de la Clase (Específico): {req.specificObjectives}
        Objetivo Actitudinal (OAA): {req.objetivoActitudinal}
        Duración: {req.duracion}
        Recursos: {req.recursos}
        Estrategia e Instrumento de Evaluación: {req.evaluacion}
        --- Momento de Inicio ---
        {req.inicio}
        --- Momento de Desarrollo ---
        {req.desarrollo}
        --- Momento de Cierre ---
        {req.cierre}
        """

        interaction = client.interactions.create(
            model="gemini-3.8-flash",
            system_instruction=SYSTEM_INSTRUCTION_RETROPLANI_V3,
            input=f"Evalúa la siguiente planificación aplicando los 14 indicadores de la Rúbrica UBB y el flujo de RetroPlani Mate V3:\n{plan_content}",
            response_format={
                "type": "text",
                "mime_type": "application/json",
                "schema": LessonPlanEvaluationResponse.model_json_schema()
            }
        )

        res = LessonPlanEvaluationResponse.model_validate_json(interaction.output_text)
        res.is_mock = False
        return res

    except Exception as e:
        logger.error(f"Error evaluando con Gemini API: {e}. Usando evaluación RetroPlani Mate V3.")
        return generar_evaluacion_mock(req)
