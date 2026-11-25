/**
 * PITAGORAS - Servicio Cliente para Microservicio de Chatbot e IA Pedagógica
 */

const RAW_CHATBOT_URL = import.meta.env.VITE_CHATBOT_URL;
const CHATBOT_BASE_URL = (RAW_CHATBOT_URL !== undefined && RAW_CHATBOT_URL !== '')
  ? RAW_CHATBOT_URL.replace(/\/+$/, '')
  : '/chatbot';

const createSessionId = () => typeof crypto.randomUUID === 'function' ? crypto.randomUUID()
  : Array.from(crypto.getRandomValues(new Uint8Array(16)), n => n.toString(16).padStart(2, '0')).join('');
let session = createSessionId();
const authenticatedHeaders = () => {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const formatChatbotError = async (response, fallbackMessage) => {
  const err = await response.json().catch(() => ({}));
  if (response.status === 401 || response.status === 403) {
    return err.detail || 'Sesión inválida o expirada. Inicia sesión nuevamente para usar el asistente de IA.';
  }
  return err.detail || `${fallbackMessage} (${response.status})`;
};

export const chatbotService = {
  resetSession() { session = createSessionId(); },
  /**
   * Verificar estado del servicio del Chatbot
   */
  async checkHealth() {
    try {
      const response = await fetch(`${CHATBOT_BASE_URL}/health`);
      if (!response.ok) return { status: 'error', gemini_api_configured: false };
      return await response.json();
    } catch {
      return { status: 'unavailable', gemini_api_configured: false };
    }
  },

  /**
   * Configurar o actualizar la GOOGLE_API_KEY en caliente
   * @param {string} apiKey
   */
  async configureApiKey(apiKey) {
    const response = await fetch(`${CHATBOT_BASE_URL}/api/config/apikey`, {
      method: 'POST',
      headers: { ...authenticatedHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ api_key: apiKey })
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.detail || `Error al configurar API Key (${response.status})`);
    }
    return await response.json();
  },

  /**
   * Enviar consulta pedagógica al chatbot tutor
   * @param {string} message
   * @param {string} [sessionId]
   */
  async sendMessage(message, sessionId = 'web') {
    const response = await fetch(`${CHATBOT_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: { ...authenticatedHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, session_id: session + ":" + sessionId })
    });

    if (!response.ok) {
      throw new Error(await formatChatbotError(response, 'Error en el chatbot'));
    }
    return await response.json();
  },

  /**
   * Enviar archivo de planificación (.pdf / .txt) para revisión según Flujo Dialógico RetroPlani Mate V3
   * @param {File} file
   * @param {string} [message]
   * @param {string} [sessionId]
   */
  async sendFileMessage(file, message = '', sessionId = 'web-session') {
    const formData = new FormData();
    formData.append('file', file);
    if (message) formData.append('message', message);
    formData.append('session_id', session + ':' + sessionId);

    const response = await fetch(`${CHATBOT_BASE_URL}/api/chat-archivo`, {
      method: 'POST',
      headers: authenticatedHeaders(),
      body: formData
    });

    if (!response.ok) {
      throw new Error(await formatChatbotError(response, 'Error al procesar el archivo en el chatbot'));
    }
    return await response.json();
  },

  /**
   * Solicitar evaluación pedagógica formativa de una planificación didáctica
   * @param {Object} planData
   */
  async evaluateLessonPlan(planData) {
    const payload = {
      schoolGrade: planData.schoolGrade || '',
      mathAxis: planData.mathAxis || 'NUMEROS',
      baseLearningObjective: planData.baseLearningObjective || planData.objective || '',
      specificObjectives: planData.objectives || planData.specificObjectives || '',
      objetivoActitudinal: planData.objetivoActitudinal || '',
      duracion: planData.duracion || '90 minutos',
      recursos: planData.recursos || '',
      evaluacion: planData.evaluacion || '',
      inicio: planData.inicio || '',
      desarrollo: planData.desarrollo || '',
      cierre: planData.cierre || '',
      inclusion: planData.inclusion || ''
    };

    const response = await fetch(`${CHATBOT_BASE_URL}/api/evaluar-planificacion`, {
      method: 'POST',
      headers: { ...authenticatedHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(await formatChatbotError(response, 'No se pudo conectar con el servicio de IA'));
    }
    return await response.json();
  }
};

export default chatbotService;
