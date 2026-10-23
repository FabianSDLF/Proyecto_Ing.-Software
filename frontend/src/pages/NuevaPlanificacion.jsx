import { useState } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import { portfolioService, chatbotService } from '../services';
import { Button, Input, Select, Modal, Badge } from '../components/ui';

const MATH_AXIS_OPTIONS = [
  { value: 'NUMEROS', label: 'Números' },
  { value: 'ALGEBRA', label: 'Álgebra' },
  { value: 'GEOMETRIA', label: 'Geometría' },
  { value: 'PROBABILIDAD_ESTADISTICA', label: 'Probabilidad y Estadística' }
];

export default function NuevaPlanificacion() {
  const navigate = useNavigate();
  const [uploadingPlan, setUploadingPlan] = useState(false);
  const [isEvaluatingPlan, setIsEvaluatingPlan] = useState(false);
  const [evaluationModalOpen, setEvaluationModalOpen] = useState(false);
  const [evaluationData, setEvaluationData] = useState(null);

  const [planForm, setPlanForm] = useState({
    schoolGrade: '',
    mathAxis: 'NUMEROS',
    baseLearningObjective: '',
    objectives: '',
    objetivoActitudinal: '',
    duracion: '',
    recursos: '',
    evaluacion: '',
    inicio: '',
    desarrollo: '',
    cierre: '',
    file: null
  });

  const handlePlanSubmit = async (e) => {
    e.preventDefault();
    setUploadingPlan(true);

    const contentJson = {
      objetivoActitudinal: planForm.objetivoActitudinal,
      duracion: planForm.duracion,
      recursos: planForm.recursos,
      evaluacion: planForm.evaluacion,
      inicio: planForm.inicio,
      desarrollo: planForm.desarrollo,
      cierre: planForm.cierre
    };

    try {
      await portfolioService.createLessonPlan({
        schoolGrade: planForm.schoolGrade,
        mathAxis: planForm.mathAxis,
        objective: planForm.baseLearningObjective,
        specificObjectives: planForm.objectives,
        classActivities: contentJson,
        file: planForm.file || undefined
      });

      toast.success('Planificación creada con éxito');
      navigate('/portafolio');
    } catch (error) {
      toast.error(error.message || 'Error al crear planificación');
    } finally {
      setUploadingPlan(false);
    }
  };

  const handleEvaluateWithAI = async () => {
    if (!planForm.schoolGrade.trim() || !planForm.baseLearningObjective.trim()) {
      toast.error('Completa al menos el Curso y el Objetivo de Aprendizaje antes de evaluar.');
      return;
    }
    if (!planForm.inicio.trim() && !planForm.desarrollo.trim() && !planForm.cierre.trim()) {
      toast.error('Ingresa información en los momentos de la clase (Inicio, Desarrollo o Cierre).');
      return;
    }

    setIsEvaluatingPlan(true);
    try {
      const res = await chatbotService.evaluateLessonPlan(planForm);
      setEvaluationData(res);
      setEvaluationModalOpen(true);
      toast.success('¡Evaluación pedagógica completada!');
    } catch (err) {
      toast.error(err.message || 'Error al conectar con el Asistente IA.');
    } finally {
      setIsEvaluatingPlan(false);
    }
  };

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8 pb-20">
      <div className="flex items-center gap-4 mb-8">
        <button
          type="button"
          onClick={() => navigate('/portafolio')}
          aria-label="Volver al portafolio"
          className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <svg className="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>
        <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Crear Nueva Planificación</h1>
      </div>

      <form onSubmit={handlePlanSubmit} className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 space-y-6">
        <h2 className="text-xl font-bold text-gray-800 border-b pb-2">Datos Generales</h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Input
            label="Curso (Ej. 1° Medio A)"
            required
            value={planForm.schoolGrade}
            onChange={(e) => setPlanForm({ ...planForm, schoolGrade: e.target.value })}
            placeholder="Curso..."
          />
          <Select
            label="Eje Matemático"
            value={planForm.mathAxis}
            onChange={(e) => setPlanForm({ ...planForm, mathAxis: e.target.value })}
            options={MATH_AXIS_OPTIONS}
          />
          <Input
            label="Duración de la Clase (Ej. 90 minutos)"
            required
            value={planForm.duracion}
            onChange={(e) => setPlanForm({ ...planForm, duracion: e.target.value })}
            placeholder="90 minutos..."
          />
        </div>

        <h2 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Objetivos</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Objetivo de Aprendizaje Basal (OA) <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.baseLearningObjective}
              onChange={(e) => setPlanForm({ ...planForm, baseLearningObjective: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[80px]"
              placeholder="Desarrollar las reglas de las probabilidades..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Objetivo de la Clase (Específico) <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.objectives}
              onChange={(e) => setPlanForm({ ...planForm, objectives: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[80px]"
              placeholder="Aplicar la Ley de Laplace..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Objetivo Actitudinal (OAT) <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.objetivoActitudinal}
              onChange={(e) => setPlanForm({ ...planForm, objetivoActitudinal: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[80px]"
              placeholder="Gestionar de manera activa el propio aprendizaje..."
            />
          </div>
        </div>

        <h2 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Estructura de la Clase</h2>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Inicio <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.inicio}
              onChange={(e) => setPlanForm({ ...planForm, inicio: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[100px]"
              placeholder="Saludo, activación de conocimientos previos..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Desarrollo <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.desarrollo}
              onChange={(e) => setPlanForm({ ...planForm, desarrollo: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[150px]"
              placeholder="Actividades principales, trabajo en grupo..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Cierre <span className="text-red-500">*</span>
            </label>
            <textarea
              required
              value={planForm.cierre}
              onChange={(e) => setPlanForm({ ...planForm, cierre: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 min-h-[100px]"
              placeholder="Síntesis, evaluación formativa..."
            />
          </div>
        </div>

        <h2 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Anexos y Recursos</h2>

        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Input
              label="Recursos Materiales"
              value={planForm.recursos}
              onChange={(e) => setPlanForm({ ...planForm, recursos: e.target.value })}
              placeholder="Proyector, guías de trabajo..."
            />
            <Input
              label="Instrumentos de Evaluación"
              value={planForm.evaluacion}
              onChange={(e) => setPlanForm({ ...planForm, evaluacion: e.target.value })}
              placeholder="Rúbrica, Puesta en común grupal..."
            />
          </div>

          <div className="pt-4">
            <span className="block text-sm font-medium text-gray-700 mb-2">
              Archivo Adjunto (Opcional - Guía, Material)
            </span>
            <div
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  document.getElementById('file-upload-plan').click();
                }
              }}
              className="border-2 border-dashed border-gray-300 rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-blue-50 hover:border-blue-400 transition-colors bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
              onClick={() => document.getElementById('file-upload-plan').click()}
            >
              <svg className="w-10 h-10 text-blue-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <span className="text-sm text-gray-700 font-medium">Haz clic aquí para buscar o cambiar tu archivo (Opcional)</span>
              {planForm.file && (
                <span className="mt-2 text-sm text-green-600 font-bold bg-green-50 px-3 py-1 rounded-full">
                  {planForm.file.name}
                </span>
              )}
            </div>
            <input
              id="file-upload-plan"
              type="file"
              onChange={(e) => {
                if (e.target.files[0]) setPlanForm({ ...planForm, file: e.target.files[0] });
              }}
              className="hidden"
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-8 border-t flex-wrap">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/portafolio')}
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={handleEvaluateWithAI}
            isLoading={isEvaluatingPlan}
            loadingText="Analizando con IA..."
            className="bg-indigo-600 hover:bg-indigo-700 text-white"
          >
            ✨ Validar con Asistente IA
          </Button>
          <Button
            type="submit"
            variant="secondary"
            isLoading={uploadingPlan}
            loadingText="Guardando..."
          >
            Guardar Planificación
          </Button>
        </div>
      </form>

      {/* Modal de Retroalimentación Pedagógica del Asistente IA */}
      {evaluationData && (
        <Modal
          isOpen={evaluationModalOpen}
          onClose={() => setEvaluationModalOpen(false)}
          title="Evaluación Pedagógica del Asistente IA"
          size="lg"
          footer={
            <div className="flex justify-between w-full items-center">
              <span className="text-xs text-gray-500">
                {evaluationData.is_mock ? 'Modo: Evaluación Heurística Local' : 'Modo: Gemini IA Live'}
              </span>
              <Button
                variant="primary"
                onClick={() => setEvaluationModalOpen(false)}
              >
                Entendido, ajustar planificación
              </Button>
            </div>
          }
        >
          <div className="space-y-6">
            {/* Cabecera de Estado y Nota */}
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-xl border border-gray-200">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{evaluationData.aiApproved ? '✅' : '⚠️'}</span>
                <div>
                  <h4 className="font-bold text-gray-800">
                    {evaluationData.aiApproved ? 'Planificación Coherente' : 'Requiere Ajustes Pedagógicos'}
                  </h4>
                  <p className="text-xs text-gray-500">
                    Evaluado según criterios curriculares de Educación Media (MINEDUC)
                  </p>
                </div>
              </div>
              <div className="text-right">
                <span className="text-xs uppercase tracking-wider text-gray-500 block font-semibold">
                  Nota Estimada
                </span>
                <span
                  className={`text-2xl font-black ${
                    evaluationData.score >= 5.5
                      ? 'text-green-600'
                      : evaluationData.score >= 4.0
                      ? 'text-yellow-600'
                      : 'text-red-600'
                  }`}
                >
                  {evaluationData.score.toFixed(1)} / 7.0
                </span>
              </div>
            </div>

            {/* Resumen General */}
            <div>
              <h5 className="text-sm font-bold text-gray-700 uppercase tracking-wide mb-1">
                Retroalimentación General
              </h5>
              <p className="text-sm text-gray-600 leading-relaxed bg-blue-50/50 p-3 rounded-lg border border-blue-100">
                {evaluationData.feedback}
              </p>
            </div>

            {/* Fortalezas y Mejoras */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 bg-green-50/60 border border-green-200 rounded-xl">
                <h5 className="text-sm font-bold text-green-800 flex items-center gap-1.5 mb-2">
                  <span>👍</span> Fortalezas Observadas
                </h5>
                <ul className="text-xs text-green-900 space-y-1.5 list-disc list-inside">
                  {evaluationData.strengths?.map((str, idx) => (
                    <li key={idx}>{str}</li>
                  ))}
                </ul>
              </div>

              <div className="p-4 bg-amber-50/60 border border-amber-200 rounded-xl">
                <h5 className="text-sm font-bold text-amber-800 flex items-center gap-1.5 mb-2">
                  <span>💡</span> Sugerencias de Mejora
                </h5>
                <ul className="text-xs text-amber-900 space-y-1.5 list-disc list-inside">
                  {evaluationData.improvements?.map((imp, idx) => (
                    <li key={idx}>{imp}</li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Análisis por Momentos de la Clase */}
            {evaluationData.momentAnalysis && (
              <div>
                <h5 className="text-sm font-bold text-gray-700 uppercase tracking-wide mb-2">
                  Análisis de Momentos Didácticos
                </h5>
                <div className="space-y-2 text-xs">
                  <div className="p-3 bg-white border border-gray-200 rounded-lg">
                    <span className="font-bold text-blue-900 block mb-0.5">Inicio:</span>
                    <span className="text-gray-600">{evaluationData.momentAnalysis.inicio}</span>
                  </div>
                  <div className="p-3 bg-white border border-gray-200 rounded-lg">
                    <span className="font-bold text-blue-900 block mb-0.5">Desarrollo:</span>
                    <span className="text-gray-600">{evaluationData.momentAnalysis.desarrollo}</span>
                  </div>
                  <div className="p-3 bg-white border border-gray-200 rounded-lg">
                    <span className="font-bold text-blue-900 block mb-0.5">Cierre:</span>
                    <span className="text-gray-600">{evaluationData.momentAnalysis.cierre}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}


