import { useState, useEffect, useRef } from 'react';
import { toast } from 'react-hot-toast';

export default function Portafolio() {
    const [activeTab, setActiveTab] = useState('EVIDENCIAS');
    
    // Evidencias
    const [evidencias, setEvidencias] = useState([]);
    const [loadingEvidencias, setLoadingEvidencias] = useState(true);
    const [uploadingEvidencia, setUploadingEvidencia] = useState(false);
    const evidenciaInputRef = useRef(null);

    // Planificaciones
    const [planificaciones, setPlanificaciones] = useState([]);
    const [loadingPlanificaciones, setLoadingPlanificaciones] = useState(true);
    const [showPlanModal, setShowPlanModal] = useState(false);
    const [planForm, setPlanForm] = useState({
        schoolGrade: '',
        mathAxis: 'NUMEROS',
        objective: '',
        file: null
    });
    const [uploadingPlan, setUploadingPlan] = useState(false);

    const fetchEvidencias = async () => {
        const token = localStorage.getItem('token');
        try {
            const res = await fetch('http://localhost:8080/api/portafolio/evidencias', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) setEvidencias(await res.json());
        } finally { setLoadingEvidencias(false); }
    };

    const fetchPlanificaciones = async () => {
        const token = localStorage.getItem('token');
        try {
            const res = await fetch('http://localhost:8080/api/portafolio/planificaciones', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) setPlanificaciones(await res.json());
        } finally { setLoadingPlanificaciones(false); }
    };

    useEffect(() => {
        fetchEvidencias();
        fetchPlanificaciones();
    }, []);

    const handleEvidenciaUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setUploadingEvidencia(true);
        const token = localStorage.getItem('token');
        const formData = new FormData();
        formData.append('file', file);

        try {
            const res = await fetch('http://localhost:8080/api/portafolio/evidencias/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            if (res.ok) {
                toast.success('Evidencia subida');
                fetchEvidencias();
            } else {
                toast.error(await res.text());
            }
        } finally {
            setUploadingEvidencia(false);
            if (evidenciaInputRef.current) evidenciaInputRef.current.value = '';
        }
    };

    const handlePlanSubmit = async (e) => {
        e.preventDefault();
        if (!planForm.file) return toast.error('Debes adjuntar un archivo');

        setUploadingPlan(true);
        const token = localStorage.getItem('token');
        const formData = new FormData();
        formData.append('file', planForm.file);
        formData.append('schoolGrade', planForm.schoolGrade);
        formData.append('mathAxis', planForm.mathAxis);
        formData.append('objective', planForm.objective);

        try {
            const res = await fetch('http://localhost:8080/api/portafolio/planificaciones/upload', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            if (res.ok) {
                toast.success('Planificación creada');
                setShowPlanModal(false);
                setPlanForm({ schoolGrade: '', mathAxis: 'NUMEROS', objective: '', file: null });
                fetchPlanificaciones();
            } else {
                toast.error(await res.text());
            }
        } finally {
            setUploadingPlan(false);
        }
    };

    return (
        <div className="max-w-5xl mx-auto p-6">
            <h1 className="text-3xl font-bold text-[#003B70] mb-6">Mi Portafolio Digital</h1>
            
            <div className="flex gap-4 mb-6 border-b border-gray-200">
                <button 
                    onClick={() => setActiveTab('EVIDENCIAS')}
                    className={`py-2 px-4 font-semibold border-b-2 ${activeTab === 'EVIDENCIAS' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Evidencias de Aprendizaje
                </button>
                <button 
                    onClick={() => setActiveTab('PLANIFICACIONES')}
                    className={`py-2 px-4 font-semibold border-b-2 ${activeTab === 'PLANIFICACIONES' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Planificaciones
                </button>
            </div>

            {activeTab === 'EVIDENCIAS' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-xl font-bold text-gray-800">Tus Evidencias</h2>
                        <input type="file" ref={evidenciaInputRef} onChange={handleEvidenciaUpload} className="hidden" />
                        <button onClick={() => evidenciaInputRef.current.click()} disabled={uploadingEvidencia} className="bg-orange-600 hover:bg-orange-700 text-white font-medium py-2 px-4 rounded-lg">
                            {uploadingEvidencia ? 'Subiendo...' : 'Subir Archivo'}
                        </button>
                    </div>
                    
                    {loadingEvidencias ? <p>Cargando...</p> : evidencias.length === 0 ? <p className="text-gray-500 text-center py-10">No hay evidencias subidas.</p> : (
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="border-b-2 border-gray-100">
                                    <th className="py-3 px-4 font-semibold text-gray-600">Nombre del Archivo</th>
                                    <th className="py-3 px-4 font-semibold text-gray-600">Fecha</th>
                                </tr>
                            </thead>
                            <tbody>
                                {evidencias.map(doc => (
                                    <tr key={doc.id} className="border-b border-gray-50">
                                        <td className="py-4 px-4 text-blue-600 font-medium">{doc.fileName}</td>
                                        <td className="py-4 px-4 text-gray-500">{new Date(doc.uploadDate).toLocaleDateString()}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {activeTab === 'PLANIFICACIONES' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-xl font-bold text-gray-800">Tus Planificaciones</h2>
                        <button onClick={() => setShowPlanModal(true)} className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg">
                            + Nueva Planificación
                        </button>
                    </div>

                    {loadingPlanificaciones ? <p>Cargando...</p> : planificaciones.length === 0 ? <p className="text-gray-500 text-center py-10">No has creado planificaciones.</p> : (
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="border-b-2 border-gray-100">
                                    <th className="py-3 px-4 font-semibold text-gray-600">Fecha</th>
                                    <th className="py-3 px-4 font-semibold text-gray-600">Curso</th>
                                    <th className="py-3 px-4 font-semibold text-gray-600">Eje Matemático</th>
                                    <th className="py-3 px-4 font-semibold text-gray-600">Estado</th>
                                    <th className="py-3 px-4 font-semibold text-gray-600">Archivo</th>
                                </tr>
                            </thead>
                            <tbody>
                                {planificaciones.map(plan => (
                                    <tr key={plan.id} className="border-b border-gray-50">
                                        <td className="py-4 px-4 text-gray-600">{new Date(plan.createdAt).toLocaleDateString()}</td>
                                        <td className="py-4 px-4 text-gray-600">{plan.schoolGrade}</td>
                                        <td className="py-4 px-4 text-gray-600">{plan.mathAxis}</td>
                                        <td className="py-4 px-4"><span className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm font-semibold">{plan.status}</span></td>
                                        <td className="py-4 px-4 text-blue-600 font-medium">{plan.document?.fileName}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {/* Modal Nueva Planificación */}
            {showPlanModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white p-8 rounded-xl max-w-md w-full">
                        <h3 className="text-2xl font-bold mb-4">Nueva Planificación</h3>
                        <form onSubmit={handlePlanSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium mb-1">Curso</label>
                                <input type="text" required value={planForm.schoolGrade} onChange={e => setPlanForm({...planForm, schoolGrade: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500" placeholder="Ej. 1° Medio A"/>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Eje Matemático</label>
                                <select value={planForm.mathAxis} onChange={e => setPlanForm({...planForm, mathAxis: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500">
                                    <option value="NUMEROS">Números</option>
                                    <option value="ALGEBRA">Álgebra</option>
                                    <option value="GEOMETRIA">Geometría</option>
                                    <option value="PROBABILIDAD_ESTADISTICA">Probabilidad y Estadística</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Objetivo de Aprendizaje</label>
                                <textarea required value={planForm.objective} onChange={e => setPlanForm({...planForm, objective: e.target.value})} className="w-full border border-gray-300 rounded-lg p-3 outline-none focus:border-blue-500" rows="3"></textarea>
                            </div>
                            <div>
                                <label className="block text-sm font-medium mb-1">Documento Adjunto</label>
                                <div 
                                    className="border-2 border-dashed border-gray-300 rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer hover:bg-blue-50 hover:border-blue-400 transition-colors"
                                    onClick={() => document.getElementById('file-upload-plan').click()}
                                >
                                    <svg className="w-10 h-10 text-blue-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path></svg>
                                    <span className="text-sm text-gray-700 font-medium">Haz clic aquí para buscar tu archivo</span>
                                    {planForm.file && <span className="mt-2 text-sm text-green-600 font-bold bg-green-50 px-3 py-1 rounded-full">{planForm.file.name}</span>}
                                </div>
                                <input id="file-upload-plan" type="file" required onChange={e => setPlanForm({...planForm, file: e.target.files[0]})} className="hidden"/>
                            </div>
                            <div className="flex justify-end gap-3 pt-4">
                                <button type="button" onClick={() => setShowPlanModal(false)} className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg">Cancelar</button>
                                <button type="submit" disabled={uploadingPlan} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
                                    {uploadingPlan ? 'Guardando...' : 'Crear Planificación'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
