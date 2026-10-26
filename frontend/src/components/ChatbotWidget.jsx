import { useState, useRef, useEffect } from 'react';
import { chatbotService } from '../services';
import MathMarkdown from './MathMarkdown';

const QUICK_PROMPTS = [
  '¿Cómo formular un inicio motivante?',
  '¿Qué preguntas metacognitivas usar al cierre?',
  '¿Cómo formular un objetivo específico en Bloom?'
];

export default function ChatbotWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      sender: 'bot',
      text: '¡Hola! Soy tu Asistente Pedagógico de PITAGORAS. Puedo ayudarte con las Bases Curriculares de Matemática, formulación de objetivos o momentos de tu clase.',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [healthStatus, setHealthStatus] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    chatbotService.checkHealth().then(setHealthStatus);
  }, []);

  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  const handleSend = async (textToSend) => {
    const text = textToSend || inputValue;
    if (!text || !text.trim() || isLoading) return;

    const userMessage = {
      sender: 'user',
      text: text.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMessage]);
    if (!textToSend) setInputValue('');
    setIsLoading(true);

    try {
      const response = await chatbotService.sendMessage(text.trim());
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: response.answer,
          isMock: response.is_mock,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'bot',
          text: '⚠️ No se pudo contactar al asistente. Asegúrate de que el microservicio de chatbot (puerto 8000) esté encendido.',
          isError: true,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end">
      {isOpen && (
        <div
          role="region"
          aria-label="Ventana del Asistente Pedagógico"
          className="w-96 h-[500px] bg-white rounded-2xl shadow-2xl border border-gray-200 flex flex-col mb-4 overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200"
        >
          {/* Header */}
          <div className="bg-[#003B70] text-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center font-bold text-sm">
                🤖
              </div>
              <div>
                <h3 className="text-sm font-bold leading-tight">Asistente Pedagógico IA</h3>
                <span className="text-xs text-blue-200 flex items-center gap-1">
                  <span
                    className={`w-2 h-2 rounded-full ${
                      healthStatus?.status === 'healthy' ? 'bg-green-400' : 'bg-yellow-400'
                    }`}
                  ></span>
                  {healthStatus?.mode || 'Conectando...'}
                </span>
              </div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1 rounded-lg text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
              aria-label="Cerrar chat"
            >
              ✕
            </button>
          </div>

          {/* Messages Container */}
          <div className="flex-1 p-4 overflow-y-auto space-y-3 bg-gray-50 text-sm">
            {messages.map((msg, idx) => (
              <div
                key={idx}
                className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[85%] p-3 rounded-2xl leading-relaxed ${
                    msg.sender === 'user'
                      ? 'bg-blue-600 text-white rounded-br-none shadow-sm'
                      : msg.isError
                      ? 'bg-red-50 text-red-700 border border-red-200 rounded-bl-none'
                      : 'bg-white text-gray-800 border border-gray-200 rounded-bl-none shadow-sm'
                  }`}
                >
                  <MathMarkdown content={msg.text} isDarkBg={msg.sender === 'user'} />
                </div>
                <span className="text-[10px] text-gray-400 mt-1 px-1">{msg.time}</span>
              </div>
            ))}
            {isLoading && (
              <div className="flex items-center gap-2 p-3 bg-white border border-gray-200 rounded-2xl max-w-[85%] text-gray-500">
                <span className="animate-spin text-sm">⏳</span>
                <span className="text-xs italic">Consultando al tutor pedagógico...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Suggestion Chips */}
          <div className="px-3 py-2 bg-gray-100 border-t border-gray-200 flex gap-2 overflow-x-auto text-xs whitespace-nowrap">
            {QUICK_PROMPTS.map((prompt, i) => (
              <button
                key={i}
                type="button"
                onClick={() => handleSend(prompt)}
                disabled={isLoading}
                className="px-2 py-1 bg-white hover:bg-blue-50 text-blue-700 border border-gray-300 rounded-full transition-colors disabled:opacity-50"
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input Box */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
            className="p-3 bg-white border-t border-gray-200 flex gap-2"
          >
            <input
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Escribe tu consulta pedagógica..."
              disabled={isLoading}
              className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            />
            <button
              type="submit"
              disabled={isLoading || !inputValue.trim()}
              className="px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              ➤
            </button>
          </form>
        </div>
      )}

      {/* Floating Toggle Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-5 py-3.5 bg-[#003B70] hover:bg-[#004A8F] text-white rounded-full shadow-2xl transition-all transform hover:scale-105 active:scale-95 border-2 border-orange-500 group"
        aria-label="Abrir asistente de IA"
      >
        <span className="text-xl">🤖</span>
        <span className="font-semibold text-sm">Tutor Pedagógico IA</span>
      </button>
    </div>
  );
}
