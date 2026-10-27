/**
 * PITAGORAS - Servicio Cliente para Microservicio de Chatbot e IA Pedagógica
 */

const RAW_CHATBOT_URL = import.meta.env.VITE_CHATBOT_URL;
const CHATBOT_BASE_URL = (RAW_CHATBOT_URL !== undefined && RAW_CHATBOT_URL !== '')
  ? RAW_CHATBOT_URL.replace(/\/+$/, '')
  : '/chatbot';

export const chatbotService = {
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
      headers: { 'Content-Type': 'application/json' },
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
  async sendMessage(message, sessionId = 'web-session') {
    const response = await fetch(`${CHATBOT_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, session_id: sessionId })
    });

    if (!response.ok) {
      throw new Error(`Error en el chatbot (${response.status})`);
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
    formData.append('session_id', sessionId);

    const response = await fetch(`${CHATBOT_BASE_URL}/api/chat-archivo`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`Error al procesar el archivo en el chatbot (${response.status})`);
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
      cierre: planData.cierre || ''
    };

    const response = await fetch(`${CHATBOT_BASE_URL}/api/evaluar-planificacion`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(`No se pudo conectar con el servicio de IA (${response.status})`);
    }
    return await response.json();
  }
};

export default chatbotService;
