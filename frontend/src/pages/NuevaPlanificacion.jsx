import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

export default function NuevaPlanificacion() {
    const navigate = useNavigate();
    const [uploadingPlan, setUploadingPlan] = useState(false);
    
    // Mismo estado que Portafolio, pero expandido
    const [planForm, setPlanForm] = useState({
        schoolGrade: '',
        mathAxis: 'NUMEROS',
        baseLearningObjective: '',
        objectives: '',
        // Campos que irn en contentJson
        objetivoActitudinal: '',
        duracion: '',
        recursos: '',
        evaluacion: '',
        inicio: '',
        desarrollo: '',
        cierre: '',
        file: null
    });

    const [hasActiveAssignment, setHasActiveAssignment] = useState(true);

    const handlePlanSubmit = async (e) => {
        e.preventDefault();
        setUploadingPlan(true);
        const token = localStorage.getItem('token');
        
        const contentJson = JSON.stringify({
            objetivoActitudinal: planForm.objetivoActitudinal,
            duracion: planForm.duracion,
            recursos: planForm.recursos,
            evaluacion: planForm.evaluacion,
            inicio: planForm.inicio,
            desarrollo: planForm.desarrollo,
            cierre: planForm.cierre
        });

        const formData = new FormData();
        if (planForm.file) {
            formData.append('file', planForm.file);
        } else {
            // Backend should handle optional file. If not, we can pass an empty blob.
            // We'll modify backend to make it optional.
            formData.append('file', new Blob([""], { type: "text/plain" }), "planificacion_sin_archivo.txt");
        }
        
        formData.append('schoolGrade', planForm.schoolGrade);
        formData.append('mathAxis', planForm.mathAxis);
        formData.append('objective', planForm.baseLearningObjective);
        formData.append('specificObjectives', planForm.objectives);
        formData.append('classActivities', contentJson);

        try {
            const res = await fetch('http://localhost:8080/api/portafolio/planificaciones/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            if (res.ok) {
                toast.success('Planificación creada con éxito');
                navigate('/portafolio');
            } else {
                const text = await res.text();
                toast.error(text || 'Error al crear planificación');
            }
        } catch (error) {
            toast.error("Error de conexión con el servidor");
        }
        setUploadingPlan(false);
    };

    return (
        <div className="p-8 max-w-4xl mx-auto space-y-8 pb-20">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/portafolio')} className="p-2 bg-gray-200 rounded-full hover:bg-gray-300 transition-colors">
                    <svg className="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path></svg>
                </button>
                <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Crear Nueva Planificación</h1>
            </div>

            <form onSubmit={handlePlanSubmit} className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 space-y-6">
                
                <h3 className="text-xl font-bold text-gray-800 border-b pb-2">Datos Generales</h3>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Curso (Ej. 1 Medio A)</label>
                        <input type="text" required value={planForm.schoolGrade} onChange={e => setPlanForm({...planForm, schoolGrade: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" placeholder="Curso..." />
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Eje Matemático</label>
                        <select value={planForm.mathAxis} onChange={e => setPlanForm({...planForm, mathAxis: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200">
                            <option value="NUMEROS">Números</option>
                            <option value="ALGEBRA">Álgebra</option>
                            <option value="GEOMETRIA">Geometría</option>
                            <option value="PROBABILIDAD_ESTADISTICA">Probabilidad y Estadística</option>
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Duración de la Clase (Ej. 90 minutos)</label>
                        <input type="text" required value={planForm.duracion} onChange={e => setPlanForm({...planForm, duracion: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" placeholder="90 minutos..." />
                    </div>
                </div>

                <h3 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Objetivos</h3>

                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Objetivo de Aprendizaje Basal (OA)</label>
                        <textarea required value={planForm.baseLearningObjective} onChange={e => setPlanForm({...planForm, baseLearningObjective: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[80px]" placeholder="Desarrollar las reglas de las probabilidades..."></textarea>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Objetivo de la Clase (Específico)</label>
                        <textarea required value={planForm.objectives} onChange={e => setPlanForm({...planForm, objectives: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[80px]" placeholder="Aplicar la Ley de Laplace..."></textarea>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Objetivo Actitudinal (OAT)</label>
                        <textarea required value={planForm.objetivoActitudinal} onChange={e => setPlanForm({...planForm, objetivoActitudinal: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[80px]" placeholder="Gestionar de manera activa el propio aprendizaje..."></textarea>
                    </div>
                </div>

                <h3 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Estructura de la Clase</h3>
                
                <div className="space-y-6">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Inicio</label>
                        <textarea required value={planForm.inicio} onChange={e => setPlanForm({...planForm, inicio: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[100px]" placeholder="Saludo, activación de conocimientos previos..."></textarea>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Desarrollo</label>
                        <textarea required value={planForm.desarrollo} onChange={e => setPlanForm({...planForm, desarrollo: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[150px]" placeholder="Actividades principales, trabajo en grupo..."></textarea>
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Cierre</label>
                        <textarea required value={planForm.cierre} onChange={e => setPlanForm({...planForm, cierre: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200 min-h-[100px]" placeholder="Sntesis, evaluación formativa..."></textarea>
                    </div>
                </div>

                <h3 className="text-xl font-bold text-gray-800 border-b pb-2 pt-6">Anexos y Recursos</h3>
                
                <div className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Recursos Materiales</label>
                            <input type="text" value={planForm.recursos} onChange={e => setPlanForm({...planForm, recursos: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" placeholder="Proyector, guas de trabajo..." />
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-2">Instrumentos de Evaluación</label>
                            <input type="text" value={planForm.evaluacion} onChange={e => setPlanForm({...planForm, evaluacion: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200" placeholder="Rbrica, Puesta en comn grupal..." />
                        </div>
                    </div>
                    
                    <div className="pt-4">
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Archivo Adjunto (Opcional - Gua, Material)</label>
                        <div 
                            className="border-2 border-dashed border-gray-300 rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-blue-50 hover:border-blue-400 transition-colors bg-gray-50"
                            onClick={() => document.getElementById('file-upload-plan').click()}
                        >
                            <svg className="w-10 h-10 text-blue-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                            <span className="text-sm text-gray-700 font-medium">Haz clic aquí para buscar o cambiar tu archivo (Opcional)</span>
                            {planForm.file && <span className="mt-2 text-sm text-green-600 font-bold bg-green-50 px-3 py-1 rounded-full">{planForm.file.name}</span>}
                        </div>
                        <input id="file-upload-plan" type="file" onChange={e => {
                            if (e.target.files[0]) setPlanForm({...planForm, file: e.target.files[0]})
                        }} className="hidden"/>
                    </div>
                </div>

                <div className="flex justify-end gap-4 pt-8 border-t">
                    <button type="button" onClick={() => navigate('/portafolio')} className="px-6 py-3 font-semibold text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">Cancelar</button>
                    <button type="submit" disabled={uploadingPlan} className="px-6 py-3 font-semibold bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors shadow-md">
                        {uploadingPlan ? 'Guardando...' : 'Guardar Planificación'}
                    </button>
                </div>
            </form>
        </div>
    );
}
