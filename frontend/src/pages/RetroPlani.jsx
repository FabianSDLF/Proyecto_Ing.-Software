import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { chatbotService, portfolioService } from '../services';
import { Button, Input, Select, Badge } from '../components/ui';
import MathMarkdown from '../components/MathMarkdown';

const MATH_AXIS_OPTIONS = [
  { value: 'NUMEROS', label: 'Números' },
  { value: 'ALGEBRA', label: 'Álgebra y Funciones' },
  { value: 'GEOMETRIA', label: 'Geometría' },
  { value: 'PROBABILIDAD_ESTADISTICA', label: 'Probabilidad y Estadística' }
];

const SUGGESTED_QUESTIONS = [
  {
    label: '🍕 Fracciones en 6° Básico (Conocimientos Previos)',
    prompt: 'Quiero enseñar operatoria con fracciones en 6° básico, ¿qué conocimientos previos deben tener consolidados los alumnos?'
  },
  {
    label: '🎲 Enfoque MINEDUC en Probabilidad',
    prompt: '¿Cuál es el enfoque didáctico sugerido por el MINEDUC para introducir la Probabilidad en educación básica?'
  },
  {
    label: '🌡️ Gancho motivacional: Enteros en 7° Básico',
    prompt: 'Necesito un "gancho" motivacional para enseñar números enteros en 7° básico. ¿Qué me sugieres?'
  },
  {
    label: '♿ Acciones DUA y NEE en Matemática (103 Acciones)',
    prompt: '¿Cómo debo operacionalizar los 3 principios del DUA y atender a estudiantes con NEE en mi planificación de matemática?'
  },
  {
    label: '📋 Rúbrica Oficial UBB (14 Indicadores)',
    prompt: '¿Cuáles son los 14 indicadores de la rúbrica de planificación de clases UBB y cómo redactar el objetivo de clase?'
  },
  {
    label: '🔗 Enlaces y Bibliografía Oficial (Inicio, Cierre, Errores)',
    prompt: '¿Qué enlaces oficiales y referencias bibliográficas me sugieres para el diseño de clases y tratamiento de errores?'
  }
];

function extractMomentsFromContentJson(contentJson) {
  if (!contentJson) {
    return {
      objetivoActitudinal: '',
      duracion: '90 minutos',
      recursos: '',
      evaluacion: '',
      inicio: '',
      desarrollo: '',
      cierre: ''
    };
  }

  let parsed = contentJson;
  if (typeof contentJson === 'string') {
    try {
      parsed = JSON.parse(contentJson);
    } catch {
      return {
        objetivoActitudinal: '',
        duracion: '90 minutos',
        recursos: '',
        evaluacion: '',
        inicio: contentJson,
        desarrollo: '',
        cierre: ''
      };
    }
  }

  const getMomentText = (moment) => {
    if (!moment) return '';
    if (typeof moment === 'string') return moment;
    if (typeof moment === 'object') {
      return moment.descripcion || moment.description || JSON.stringify(moment);
    }
    return String(moment);
  };

  const getResourcesText = (obj) => {
    if (obj.recursos && typeof obj.recursos === 'string') return obj.recursos;
    const allRes = [
      ...(Array.isArray(obj.inicio?.recursos) ? obj.inicio.recursos : []),
      ...(Array.isArray(obj.desarrollo?.recursos) ? obj.desarrollo.recursos : []),
      ...(Array.isArray(obj.cierre?.recursos) ? obj.cierre.recursos : [])
    ];
    return [...new Set(allRes)].join(', ');
  };

  return {
    objetivoActitudinal: parsed.objetivoActitudinal || parsed.adaptaciones_pie || '',
    duracion: parsed.duracion || '90 minutos',
    recursos: getResourcesText(parsed),
    evaluacion: parsed.evaluacion || '',
    inicio: getMomentText(parsed.inicio),
    desarrollo: getMomentText(parsed.desarrollo),
    cierre: getMomentText(parsed.cierre)
  };
}

export default function RetroPlani() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('CHAT'); // 'CHAT' | 'VALIDADOR'
  const [healthStatus, setHealthStatus] = useState(null);

  // Estado del Chat Directo
  const [messages, setMessages] = useState([
    {
      sender: 'bot',
      text: '¡Hola! Soy **RetroPlani Mate V3 (IA)**, tu asistente pedagógico especializado en revisar y orientar planificaciones de matemática (UBB / MINEDUC).\n\n• **Sube tu planificación en `.pdf` o `.txt`** con el botón **📎 Adjuntar** (o usa la pestaña **«✨ Validador de Planificaciones»**) para revisar **Objetivo, Inicio, Desarrollo, Cierre, Evaluación, DUA (103 acciones) y Errores matemáticos**.\n• **Antes de entregarte la retroalimentación final**, te haré algunas **preguntas diagnósticas** para comprender mejor tus decisiones pedagógicas.\n• Recuerda que mi propósito es orientarte para que mejores autónomamente tu diseño, sin hacer la clase por ti.',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [chatInput, setChatInput] = useState('');
  const [isSendingChat, setIsSendingChat] = useState(false);
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);

  // Estado de Planificaciones Guardadas del Estudiante
  const [savedPlans, setSavedPlans] = useState([]);
  const [loadingPlans, setLoadingPlans] = useState(false);
  const [selectedPlanId, setSelectedPlanId] = useState('');

  // Estado del Formulario del Validador
  const [planForm, setPlanForm] = useState({
    schoolGrade: '',
    mathAxis: 'NUMEROS',
    baseLearningObjective: '',
    objectives: '',
    objetivoActitudinal: '',
    duracion: '90 minutos',
    recursos: '',
    evaluacion: '',
    inicio: '',
    desarrollo: '',
    cierre: ''
  });
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [evaluationResult, setEvaluationResult] = useState(null);

  useEffect(() => {
    chatbotService.checkHealth().then(setHealthStatus);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    async function loadStudentPlans() {
      setLoadingPlans(true);
      try {
        const data = await portfolioService.getLessonPlans({ signal: controller.signal });
        if (Array.isArray(data)) {
          setSavedPlans(data);
        }
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.error('Error cargando planificaciones:', err);
        }
      } finally {
        setLoadingPlans(false);
      }
    }
    loadStudentPlans();
    return () => controller.abort();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSelectSavedPlan = (planId) => {
    setSelectedPlanId(planId);
    if (!planId) return;

    const found = savedPlans.find((p) => String(p.id) === String(planId));
    if (!found) return;

    const moments = extractMomentsFromContentJson(found.contentJson);
    setPlanForm({
      schoolGrade: found.schoolGrade || '',
      mathAxis: found.mathAxis || 'NUMEROS',
      baseLearningObjective: found.baseLearningObjective || found.objective || '',
      objectives: found.objectives || found.specificObjectives || '',
      objetivoActitudinal: moments.objetivoActitudinal,
      duracion: moments.duracion,
      recursos: moments.recursos,
      evaluacion: moments.evaluacion,
      inicio: moments.inicio,
      desarrollo: moments.desarrollo,
      cierre: moments.cierre
    });
    setEvaluationResult(null);
    toast.success(`Planificación "${found.schoolGrade} - ${found.mathAxis}" cargada para revisión`);
  };

  const handleSendChat = async (textOverride) => {
    const text = (textOverride ?? chatInput).trim();
    if (!text || isSendingChat) return;

    const userMsg = {
      sender: 'user',
      text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textOverride) setChatInput('');
    setIsSendingChat(true);

    try {
      const response = await chatbotService.sendMessage(text, 'retroplani-student-session');
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: response.answer,
          sources: response.sources,
          isMock: response.is_mock,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: '⚠️ No se pudo conectar con el servicio de RetroPlani Mate (IA). Verifica que el contenedor del chatbot esté activo ',
          isError: true,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } finally {
      setIsSendingChat(false);
    }
  };

  const handleFileAttachment = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';

    const userMsg = {
      sender: 'user',
      text: `📎 Archivo adjunto para revisión: **${file.name}**`,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    setIsSendingChat(true);

    try {
      const response = await chatbotService.sendFileMessage(
        file,
        'Revisa mi planificación adjunta según los criterios de RetroPlani Mate V3',
        'retroplani-student-session'
      );
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: response.answer,
          sources: response.sources,
          isMock: response.is_mock,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
      toast.success(`Archivo "${file.name}" analizado con éxito`);
    } catch {
      toast.error('No se pudo analizar el archivo adjunto.');
    } finally {
      setIsSendingChat(false);
    }
  };

  const handleClearChat = () => {
    setMessages([
      {
        sender: 'bot',
        text: 'Conversación reiniciada. ¿En qué aspecto de tu planificación matemática (Objetivo, Inicio, Desarrollo, Cierre, DUA o Evaluación) te gustaría trabajar ahora?',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }
    ]);
  };

  const handleEvaluatePlan = async (e) => {
    if (e) e.preventDefault();

    if (!planForm.schoolGrade.trim() || !planForm.baseLearningObjective.trim()) {
      toast.error('Por favor completa al menos el Curso y el Objetivo de Aprendizaje (OA).');
      return;
    }
    if (!planForm.inicio.trim() && !planForm.desarrollo.trim() && !planForm.cierre.trim()) {
      toast.error('Describe al menos uno de los momentos de la clase (Inicio, Desarrollo o Cierre).');
      return;
    }

    setIsEvaluating(true);
    try {
      const result = await chatbotService.evaluateLessonPlan(planForm);
      setEvaluationResult(result);
      toast.success('¡Análisis pedagógico RetroPlani Mate V3 completado!');
    } catch (err) {
      toast.error(err.message || 'No se pudo evaluar la planificación con IA.');
    } finally {
      setIsEvaluating(false);
    }
  };

  const handleConsultResultInChat = () => {
    if (!evaluationResult) return;
    const summaryPrompt = `Quiero mejorar mi planificación de ${planForm.schoolGrade} (${planForm.mathAxis}). ¿Qué acciones concretas del DUA (Representación, Expresión y Compromiso) y qué preguntas para el Ticket de Salida me sugieres incorporar?`;
    setActiveTab('CHAT');
    handleSendChat(summaryPrompt);
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-16">
      {/* Encabezado Principal */}
      <div className="bg-gradient-to-r from-[#003B70] to-blue-800 rounded-2xl p-6 text-white shadow-lg flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div className="flex items-start gap-4">
          <div className="w-14 h-14 rounded-2xl bg-orange-500 flex items-center justify-center text-3xl shadow-md flex-shrink-0">
            🤖
          </div>
          <div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
                RetroPlani Mate V3 (IA)
              </h1>
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/15 text-blue-100 border border-white/20">
                <span
                  className={`w-2 h-2 rounded-full ${
                    healthStatus?.status === 'healthy' ? 'bg-green-400' : 'bg-yellow-400'
                  }`}
                />
                {healthStatus?.mode || 'Conectado'}
              </span>
              {healthStatus?.documents_loaded > 0 && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-100 border border-emerald-400/30">
                  📚 {healthStatus.documents_loaded} págs. documentales UBB/MINEDUC
                </span>
              )}
            </div>
            <p className="text-blue-100 mt-1 text-sm md:text-base">
              Valida tus planificaciones con el asistente virtual antes de enviarlas al profesor colaborador.
            </p>
          </div>
        </div>

        <div className="flex gap-2 self-start md:self-center">
          <button
            type="button"
            onClick={() => navigate('/portafolio/nueva-planificacion')}
            className="px-4 py-2.5 bg-orange-500 hover:bg-orange-600 text-white text-sm font-bold rounded-xl shadow transition-colors cursor-pointer"
          >
            + Nueva Planificación
          </button>
        </div>
      </div>

      {/* Selector de Pestañas */}
      <div className="flex border-b border-gray-200 bg-white rounded-t-2xl px-6 pt-4 gap-6 shadow-sm">
        <button
          type="button"
          onClick={() => setActiveTab('CHAT')}
          className={`pb-4 text-sm md:text-base font-bold border-b-2 transition-colors flex items-center gap-2 cursor-pointer ${
            activeTab === 'CHAT'
              ? 'border-blue-600 text-blue-700'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <span>💬</span> Chat con Asistente Pedagógico
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('VALIDADOR')}
          className={`pb-4 text-sm md:text-base font-bold border-b-2 transition-colors flex items-center gap-2 cursor-pointer ${
            activeTab === 'VALIDADOR'
              ? 'border-blue-600 text-blue-700'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <span>✨</span> Validador de Planificaciones (Rúbrica 14 Indicadores)
          {savedPlans.length > 0 && (
            <Badge variant="info">{savedPlans.length}</Badge>
          )}
        </button>
      </div>

      {/* CONTENIDO PESTAÑA 1: CHAT INTERACTIVO */}
      {activeTab === 'CHAT' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Columna Principal del Chat */}
          <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-200 flex flex-col h-[640px] overflow-hidden">
            {/* Barra superior del chat */}
            <div className="px-6 py-3.5 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-lg">🎓</span>
                <span className="font-bold text-gray-800 text-sm">
                  Tutoría Didáctica en Matemática (RetroPlani Mate V3 - UBB)
                </span>
              </div>
              <button
                type="button"
                onClick={handleClearChat}
                className="text-xs text-gray-500 hover:text-red-600 font-medium px-2.5 py-1 rounded-lg hover:bg-gray-200/60 transition-colors cursor-pointer"
              >
                Limpiar conversación
              </button>
            </div>

            {/* Historial de Mensajes con Renderizado Matemático KaTeX + Markdown */}
            <div className="flex-1 p-6 overflow-y-auto space-y-4 bg-slate-50/60">
              {messages.map((msg, index) => (
                <div
                  key={index}
                  className={`flex flex-col ${
                    msg.sender === 'user' ? 'items-end' : 'items-start'
                  }`}
                >
                  <div
                    className={`max-w-[88%] rounded-2xl px-4 py-3.5 text-sm shadow-sm ${
                      msg.sender === 'user'
                        ? 'bg-[#003B70] text-white rounded-br-none'
                        : msg.isError
                        ? 'bg-red-50 text-red-800 border border-red-200 rounded-bl-none'
                        : 'bg-white text-gray-800 border border-gray-200 rounded-bl-none'
                    }`}
                  >
                    <MathMarkdown
                      content={msg.text}
                      isDarkBg={msg.sender === 'user'}
                    />
                  </div>
                  <span className="text-[11px] text-gray-400 mt-1 px-1">
                    {msg.sender === 'user' ? 'Tú' : 'RetroPlani Mate V3'} • {msg.time}
                  </span>
                </div>
              ))}

              {isSendingChat && (
                <div className="flex items-center gap-2.5 p-4 bg-white border border-gray-200 rounded-2xl max-w-xs text-gray-600 shadow-sm">
                  <span className="animate-spin text-base">⏳</span>
                  <span className="text-xs font-medium italic">
                    RetroPlani está consultando las pautas y documentos UBB...
                  </span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Caja de Envío con soporte para adjuntar archivo PDF/TXT */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendChat();
              }}
              className="p-4 bg-white border-t border-gray-200 flex items-center gap-2.5"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.txt,.md"
                onChange={handleFileAttachment}
                className="hidden"
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isSendingChat}
                title="Adjuntar planificación en PDF o TXT para Verificación Documental V3"
                className="px-3 py-3 text-xs font-bold text-blue-800 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-xl transition-colors flex items-center gap-1.5 flex-shrink-0 cursor-pointer disabled:opacity-50"
              >
                <span>📎</span>
                <span className="hidden sm:inline">Adjuntar PDF</span>
              </button>

              <input
                type="text"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                placeholder="Pregunta sobre fracciones, enteros, probabilidad, DUA, rúbrica UBB o sube tu PDF..."
                disabled={isSendingChat}
                className="flex-1 px-4 py-3 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-600 disabled:bg-gray-100"
              />
              <Button
                type="submit"
                variant="primary"
                disabled={isSendingChat || !chatInput.trim()}
                className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-3"
              >
                Enviar
              </Button>
            </form>
          </div>

          {/* Columna Lateral de Consultas Rápidas y Contexto */}
          <div className="space-y-5">
            <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-200 space-y-3.5">
              <h3 className="font-bold text-gray-800 text-base flex items-center gap-2">
                <span>💡</span> Consultas Pedagógicas y Pautas UBB
              </h3>
              <p className="text-xs text-gray-500">
                Haz clic en una consulta para ver orientaciones con soporte de fórmulas matemáticas y documentos oficiales:
              </p>
              <div className="space-y-2">
                {SUGGESTED_QUESTIONS.map((item, idx) => (
                  <button
                    key={idx}
                    type="button"
                    disabled={isSendingChat}
                    onClick={() => handleSendChat(item.prompt)}
                    className="w-full text-left p-2.5 rounded-xl border border-gray-200 hover:border-blue-400 hover:bg-blue-50/60 transition-all group cursor-pointer disabled:opacity-50"
                  >
                    <span className="font-semibold text-xs text-blue-900 group-hover:text-blue-700 block">
                      {item.label}
                    </span>
                    <span className="text-[11px] text-gray-500 line-clamp-2 mt-0.5">
                      {item.prompt}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            <div className="bg-blue-50 border border-blue-200 p-5 rounded-2xl space-y-3">
              <h4 className="font-bold text-blue-900 text-sm flex items-center gap-2">
                <span>📋</span> Flujo Dialógico RetroPlani Mate V3
              </h4>
              <ul className="text-xs text-blue-900/90 space-y-1.5 leading-relaxed">
                <li>• <strong>1. Recepción:</strong> Sube tu archivo <code className="bg-white/80 px-1 rounded">.pdf</code> con el botón <strong>📎 Adjuntar PDF</strong> o cárgala en el Validador.</li>
                <li>• <strong>2. Preguntas Diagnósticas:</strong> El asistente formulará hasta 5 preguntas sobre puntos débiles o ambiguos.</li>
                <li>• <strong>3. Reflexión para la Acción:</strong> Revisa coherencia curricular, 3 momentos, DUA, evaluación y errores matemáticos sin reemplazar tu autoría.</li>
              </ul>
              <button
                type="button"
                onClick={() => setActiveTab('VALIDADOR')}
                className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl transition-colors cursor-pointer shadow-sm"
              >
                Ir al Validador de Planificaciones →
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CONTENIDO PESTAÑA 2: VALIDADOR DE PLANIFICACIONES */}
      {activeTab === 'VALIDADOR' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Formulario de Revisión (7 columnas) */}
          <div className="lg:col-span-7 bg-white p-6 rounded-2xl shadow-sm border border-gray-200 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-gray-100 pb-4">
              <div>
                <h2 className="text-lg font-bold text-gray-800">
                  Datos de la Planificación a Evaluar (Pauta UBB)
                </h2>
                <p className="text-xs text-gray-500">
                  Selecciona una planificación guardada de tu portafolio o completa los apartados para aplicar los 14 indicadores.
                </p>
              </div>
            </div>

            {/* Selector de Planificaciones Guardadas */}
            <div className="bg-blue-50/70 border border-blue-200 p-4 rounded-xl">
              <label
                htmlFor="saved-plan-select"
                className="block text-xs font-bold text-blue-900 uppercase tracking-wider mb-1.5"
              >
                📂 Cargar desde Mis Planificaciones Guardadas ({savedPlans.length})
              </label>
              <select
                id="saved-plan-select"
                value={selectedPlanId}
                onChange={(e) => handleSelectSavedPlan(e.target.value)}
                disabled={loadingPlans}
                className="w-full px-3 py-2 text-sm bg-white border border-blue-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600"
              >
                <option value="">
                  {loadingPlans
                    ? 'Cargando tus planificaciones...'
                    : savedPlans.length === 0
                    ? '-- No tienes planificaciones guardadas aún (Modo Manual) --'
                    : '-- Selecciona una planificación de tu portafolio para autocompletar --'}
                </option>
                {savedPlans.map((plan) => (
                  <option key={plan.id} value={plan.id}>
                    [v{plan.version || 1}] {plan.schoolGrade} - {plan.mathAxis} ({plan.status || 'BORRADOR'})
                  </option>
                ))}
              </select>
            </div>

            <form onSubmit={handleEvaluatePlan} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="Curso *"
                  required
                  value={planForm.schoolGrade}
                  onChange={(e) => setPlanForm({ ...planForm, schoolGrade: e.target.value })}
                  placeholder="Ej. 1° Medio A"
                />
                <Select
                  label="Eje Matemático"
                  value={planForm.mathAxis}
                  onChange={(e) => setPlanForm({ ...planForm, mathAxis: e.target.value })}
                  options={MATH_AXIS_OPTIONS}
                />
                <Input
                  label="Duración"
                  value={planForm.duracion}
                  onChange={(e) => setPlanForm({ ...planForm, duracion: e.target.value })}
                  placeholder="90 minutos"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Objetivo de Aprendizaje (OA Curricular MINEDUC) *
                </label>
                <textarea
                  rows="2"
                  required
                  value={planForm.baseLearningObjective}
                  onChange={(e) => setPlanForm({ ...planForm, baseLearningObjective: e.target.value })}
                  placeholder="Ej: OA 14: Desarrollar las reglas de las probabilidades, la regla aditiva, la regla multiplicativa..."
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Objetivo de la Clase (1 Habilidad + Contenido + Actitud)
                  </label>
                  <textarea
                    rows="2"
                    value={planForm.objectives}
                    onChange={(e) => setPlanForm({ ...planForm, objectives: e.target.value })}
                    placeholder="Ej: Aplicar la Ley de Laplace para calcular probabilidades, trabajando de forma activa y reflexiva."
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Objetivo Actitudinal (OAA) / Estrategias DUA y NEE
                  </label>
                  <textarea
                    rows="2"
                    value={planForm.objetivoActitudinal}
                    onChange={(e) => setPlanForm({ ...planForm, objetivoActitudinal: e.target.value })}
                    placeholder="OA B / Acciones DUA de representación, expresión y compromiso..."
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Recursos Didácticos Anexos (Guía, PPT, Geogebra)
                  </label>
                  <input
                    type="text"
                    value={planForm.recursos}
                    onChange={(e) => setPlanForm({ ...planForm, recursos: e.target.value })}
                    placeholder="Ej: Guía de trabajo anexada, PPT, Ticket de salida"
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Instrumento de Evaluación y Pauta de Corrección
                  </label>
                  <input
                    type="text"
                    value={planForm.evaluacion}
                    onChange={(e) => setPlanForm({ ...planForm, evaluacion: e.target.value })}
                    placeholder="Ej: Ticket de salida con pauta de puntajes y escala de apreciación"
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="space-y-3 pt-2 border-t border-gray-100">
                <h3 className="text-sm font-bold text-gray-800 uppercase tracking-wider">
                  Narración de los 3 Momentos de la Clase
                </h3>

                <div>
                  <label className="block text-xs font-bold text-blue-900 mb-1">
                    1. Inicio (Saludo, objetivo, Agenda del día, resumen anterior y activación participativa) *
                  </label>
                  <textarea
                    rows="2"
                    value={planForm.inicio}
                    onChange={(e) => setPlanForm({ ...planForm, inicio: e.target.value })}
                    placeholder="Describe el saludo, agenda del día (DUA 78) y cómo participarán todos los estudiantes en la activación..."
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-blue-900 mb-1">
                    2. Desarrollo (Actividad constructivista, rol de Docente 1 y Docente 2, DUA y errores) *
                  </label>
                  <textarea
                    rows="3"
                    value={planForm.desarrollo}
                    onChange={(e) => setPlanForm({ ...planForm, desarrollo: e.target.value })}
                    placeholder="Describe cómo los estudiantes descubren el concepto, el rol de la dupla docente y cómo abordarás errores matemáticos..."
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-blue-900 mb-1">
                    3. Cierre (Síntesis estudiantil, aclaración de dudas, Ticket de salida y enlace próxima clase) *
                  </label>
                  <textarea
                    rows="2"
                    value={planForm.cierre}
                    onChange={(e) => setPlanForm({ ...planForm, cierre: e.target.value })}
                    placeholder="Describe la síntesis participativa, aplicación del Ticket de salida y cierre de la sesión..."
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="pt-4 flex justify-end">
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={isEvaluating}
                  loadingText="Revisando 14 indicadores UBB..."
                  className="w-full sm:w-auto bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-6 py-3"
                >
                  ✨ Revisar con RetroPlani Mate V3
                </Button>
              </div>
            </form>
          </div>

          {/* Panel de Resultados de la Validación (5 columnas) */}
          <div className="lg:col-span-5 space-y-4">
            {!evaluationResult ? (
              <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-200 text-center space-y-4">
                <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto text-3xl">
                  📋
                </div>
                <h3 className="text-lg font-bold text-gray-800">
                  Retroalimentación Formativa (V3)
                </h3>
                <p className="text-sm text-gray-500 leading-relaxed">
                  Selecciona una planificación o completa los campos a la izquierda y presiona{' '}
                  <strong className="text-indigo-600">"Revisar con RetroPlani Mate V3"</strong>.
                </p>
                <div className="text-xs text-gray-500 bg-gray-50 p-4 rounded-xl text-left space-y-1.5 border border-gray-100">
                  <p className="font-bold text-gray-700">Criterios de la Rúbrica UBB (`02_Rubrica`):</p>
                  <p>• <strong>Objetivo de clase:</strong> 1 Habilidad + Contenido + Actitud (OAA).</p>
                  <p>• <strong>Inicio:</strong> Saludo, objetivo, Agenda del día y activación participativa.</p>
                  <p>• <strong>Desarrollo:</strong> Enfoque constructivista y rol de la dupla docente.</p>
                  <p>• <strong>Cierre y Evaluación:</strong> Síntesis, Ticket de salida y pauta de corrección.</p>
                  <p>• <strong>DUA y Errores:</strong> Acciones concretas de inclusión y abordaje de dificultades.</p>
                </div>
              </div>
            ) : (
              <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200 space-y-5">
                {/* Veredicto Cualitativo */}
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-gray-200">
                  <div className="flex items-center gap-3">
                    <span className="text-3xl">
                      {evaluationResult.aiApproved ? '✅' : '⚠️'}
                    </span>
                    <div>
                      <h3 className="font-bold text-gray-800 text-base">
                        {evaluationResult.aiApproved
                          ? 'Estructura Coherente (Lista para pulir)'
                          : 'Requiere Completar Componentes Clave'}
                      </h3>
                      <span className="text-xs text-gray-500">
                        Basado en la Rúbrica Cualitativa de 14 Indicadores UBB
                      </span>
                    </div>
                  </div>
                  <Badge variant={evaluationResult.aiApproved ? 'success' : 'warning'}>
                    {evaluationResult.aiApproved ? 'Logro Adecuado/Alto' : 'En Desarrollo'}
                  </Badge>
                </div>

                {/* Comentario General */}
                <div>
                  <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Síntesis de la Planificación Revisada
                  </h4>
                  <div className="text-xs text-gray-700 bg-blue-50/60 p-3.5 rounded-xl border border-blue-100">
                    <MathMarkdown content={evaluationResult.feedback} />
                  </div>
                </div>

                {/* Fortalezas */}
                {evaluationResult.strengths?.length > 0 && (
                  <div className="p-4 bg-green-50/70 border border-green-200 rounded-xl">
                    <h4 className="text-xs font-bold text-green-900 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <span>👍</span> Fortalezas Observadas
                    </h4>
                    <ul className="text-xs text-green-900 space-y-1.5 list-disc list-inside">
                      {evaluationResult.strengths.map((str, i) => (
                        <li key={i}>{str}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Aspectos de Mejora por Componente */}
                {evaluationResult.improvements?.length > 0 && (
                  <div className="p-4 bg-amber-50/70 border border-amber-200 rounded-xl">
                    <h4 className="text-xs font-bold text-amber-900 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <span>💡</span> Aspectos de Mejora por Componente
                    </h4>
                    <ul className="text-xs text-amber-900 space-y-1.5 list-disc list-inside">
                      {evaluationResult.improvements.map((imp, i) => (
                        <li key={i}>{imp}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Desglose por Momentos */}
                {evaluationResult.momentAnalysis && (
                  <div className="space-y-2">
                    <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                      Revisión de los 3 Momentos
                    </h4>
                    <div className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs">
                      <span className="font-bold text-blue-900 block">Inicio:</span>
                      <span className="text-gray-600">{evaluationResult.momentAnalysis.inicio}</span>
                    </div>
                    <div className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs">
                      <span className="font-bold text-blue-900 block">Desarrollo:</span>
                      <span className="text-gray-600">{evaluationResult.momentAnalysis.desarrollo}</span>
                    </div>
                    <div className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-xs">
                      <span className="font-bold text-blue-900 block">Cierre:</span>
                      <span className="text-gray-600">{evaluationResult.momentAnalysis.cierre}</span>
                    </div>
                  </div>
                )}

                {/* Preguntas Diagnósticas y de Reflexión para la Acción (Flujo Dialógico V3) */}
                {evaluationResult.diagnosticQuestions?.length > 0 && (
                  <div className="p-4 bg-indigo-50/80 border border-indigo-200 rounded-xl space-y-2">
                    <h4 className="text-xs font-bold text-indigo-950 uppercase tracking-wider flex items-center gap-1.5">
                      <span>🤔</span> Preguntas Diagnósticas y Reflexión para la Acción (V3)
                    </h4>
                    <ul className="text-xs text-indigo-900 space-y-1.5">
                      {evaluationResult.diagnosticQuestions.map((q, i) => (
                        <li key={i} className="leading-relaxed">• {q}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Acciones posteriores */}
                <div className="pt-3 border-t border-gray-100 flex flex-col gap-2">
                  <button
                    type="button"
                    onClick={handleConsultResultInChat}
                    className="w-full py-2.5 px-4 bg-[#003B70] hover:bg-blue-900 text-white text-xs font-semibold rounded-xl transition-colors cursor-pointer"
                  >
                    💬 Dialogar en el Chat sobre estas preguntas de mejora
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/portafolio')}
                    className="w-full py-2 px-4 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl transition-colors cursor-pointer"
                  >
                    Ir a Mi Portafolio
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
