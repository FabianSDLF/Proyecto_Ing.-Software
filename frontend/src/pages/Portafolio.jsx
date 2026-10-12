import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import portfolioService from '../services/portfolioService';
import { DataTable, ConfirmDialog, Button, Badge } from '../components/ui';

export default function Portafolio() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('EVIDENCIAS');
    
    // Evidencias
    const [evidencias, setEvidencias] = useState([]);
    const [loadingEvidencias, setLoadingEvidencias] = useState(true);
    const [uploadingEvidencia, setUploadingEvidencia] = useState(false);
    const evidenciaInputRef = useRef(null);

    // Planificaciones
    const [planificaciones, setPlanificaciones] = useState([]);
    const [loadingPlanificaciones, setLoadingPlanificaciones] = useState(true);

    // ConfirmDialog State
    const [itemToDelete, setItemToDelete] = useState(null); // { id, name, isPlan }
    const [isDeleting, setIsDeleting] = useState(false);

    const allowedExtensions = ['.pdf', '.doc', '.docx', '.ppt', '.pptx'];
    const acceptAttr = allowedExtensions.join(',');

    const fetchEvidencias = useCallback(async (signal) => {
        setLoadingEvidencias(true);
        try {
            const data = await portfolioService.getDocuments({ signal });
            setEvidencias(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando evidencias:', err);
                toast.error(err.message || 'Error cargando evidencias');
            }
        } finally {
            setLoadingEvidencias(false);
        }
    }, []);

    const fetchPlanificaciones = useCallback(async (signal) => {
        setLoadingPlanificaciones(true);
        try {
            const data = await portfolioService.getLessonPlans({ signal });
            setPlanificaciones(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando planificaciones:', err);
                toast.error(err.message || 'Error cargando planificaciones');
            }
        } finally {
            setLoadingPlanificaciones(false);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        fetchEvidencias(controller.signal);
        fetchPlanificaciones(controller.signal);
        return () => controller.abort();
    }, [fetchEvidencias, fetchPlanificaciones]);

    const validateFile = (file) => {
        if (!file) return false;
        const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        if (!allowedExtensions.includes(ext)) {
            toast.error(`Formato no permitido. Usa: ${allowedExtensions.join(', ')}`);
            return false;
        }
        return true;
    };

    const handleEvidenciaUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!validateFile(file)) {
            if (evidenciaInputRef.current) evidenciaInputRef.current.value = '';
            return;
        }

        setUploadingEvidencia(true);
        const loadingToast = toast.loading('Subiendo evidencia...');

        try {
            await portfolioService.uploadEvidence(file);
            toast.success('Evidencia subida exitosamente', { id: loadingToast });
            fetchEvidencias();
        } catch (err) {
            console.error('Error al subir evidencia:', err);
            toast.error(err.message || 'Error al subir archivo', { id: loadingToast });
        } finally {
            setUploadingEvidencia(false);
            if (evidenciaInputRef.current) evidenciaInputRef.current.value = '';
        }
    };

    const handleDownload = async (doc) => {
        if (!doc || !doc.fileUrl) return;
        const toastId = toast.loading('Descargando archivo...');
        
        try {
            await portfolioService.downloadDocument(doc.fileUrl, doc.fileName);
            toast.success('Descarga completada', { id: toastId });
        } catch (error) {
            console.error('Error al descargar:', error);
            toast.error(error.message || 'No se pudo descargar el archivo', { id: toastId });
        }
    };
    
    const confirmDelete = async () => {
        if (!itemToDelete) return;

        setIsDeleting(true);
        try {
            if (itemToDelete.isPlan) {
                await portfolioService.deleteLessonPlan(itemToDelete.id);
                toast.success('Planificación eliminada exitosamente');
                fetchPlanificaciones();
            } else {
                await portfolioService.deleteDocument(itemToDelete.id);
                toast.success('Evidencia eliminada exitosamente');
                fetchEvidencias();
            }
            setItemToDelete(null);
        } catch (error) {
            console.error('Error al eliminar:', error);
            toast.error(error.message || 'Error al eliminar');
        } finally {
            setIsDeleting(false);
        }
    };

    const evidenciaColumns = useMemo(() => [
        {
            header: 'Nombre del Archivo',
            accessor: 'fileName',
            className: 'text-gray-800 font-medium',
        },
        {
            header: 'Fecha',
            render: (row) => new Date(row.uploadDate).toLocaleDateString(),
            className: 'text-gray-500',
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                <div className="flex justify-end space-x-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDownload(row)}
                    >
                        Descargar
                    </Button>
                    <Button
                        variant="danger"
                        size="sm"
                        onClick={() => setItemToDelete({ id: row.id, name: row.fileName, isPlan: false })}
                    >
                        Eliminar
                    </Button>
                </div>
            ),
        },
    ], []);

    const planColumns = useMemo(() => [
        {
            header: 'Fecha',
            render: (row) => new Date(row.createdAt).toLocaleDateString(),
            className: 'text-gray-600',
        },
        {
            header: 'Curso',
            accessor: 'schoolGrade',
            className: 'text-gray-600',
        },
        {
            header: 'Eje Matemático',
            accessor: 'mathAxis',
            className: 'text-gray-600',
        },
        {
            header: 'Estado',
            align: 'center',
            render: (row) => (
                <Badge variant={row.status === 'APROBADA' ? 'success' : row.status === 'RECHAZADA' ? 'danger' : 'gray'}>
                    {row.status || 'BORRADOR'}
                </Badge>
            ),
        },
        {
            header: 'Archivo',
            render: (row) => row.document?.fileName || <span className="text-gray-400 italic">Sin archivo</span>,
            className: 'truncate max-w-xs text-gray-700',
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                <div className="flex justify-end space-x-2">
                    {row.document && (
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleDownload(row.document)}
                        >
                            Descargar
                        </Button>
                    )}
                    <Button
                        variant="danger"
                        size="sm"
                        onClick={() => setItemToDelete({ id: row.id, name: `Planificación (${row.schoolGrade})`, isPlan: true })}
                    >
                        Eliminar
                    </Button>
                </div>
            ),
        },
    ], []);

    return (
        <div className="max-w-5xl mx-auto p-6 space-y-6">
            <div>
                <h1 className="text-3xl font-extrabold text-[#003B70] tracking-tight">Mi Portafolio Digital</h1>
                <p className="text-gray-500 mt-1">Sube y organiza tus evidencias de aprendizaje y planificaciones didácticas.</p>
            </div>
            
            <div className="flex gap-4 border-b border-gray-200">
                <button 
                    onClick={() => setActiveTab('EVIDENCIAS')}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors ${activeTab === 'EVIDENCIAS' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Evidencias de Aprendizaje
                </button>
                <button 
                    onClick={() => setActiveTab('PLANIFICACIONES')}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors ${activeTab === 'PLANIFICACIONES' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Planificaciones
                </button>
            </div>

            {activeTab === 'EVIDENCIAS' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                    <div className="flex justify-between items-center">
                        <h2 className="text-xl font-bold text-gray-800">Tus Evidencias</h2>
                        <input 
                            type="file" 
                            ref={evidenciaInputRef} 
                            onChange={handleEvidenciaUpload} 
                            accept={acceptAttr} 
                            className="hidden" 
                        />
                        <Button 
                            variant="primary"
                            onClick={() => evidenciaInputRef.current?.click()} 
                            isLoading={uploadingEvidencia}
                            className="bg-orange-600 hover:bg-orange-700"
                        >
                            Subir Archivo
                        </Button>
                    </div>
                    
                    <DataTable
                        columns={evidenciaColumns}
                        data={evidencias}
                        isLoading={loadingEvidencias}
                        emptyMessage="No hay evidencias subidas."
                        keyField="id"
                    />
                </div>
            )}

            {activeTab === 'PLANIFICACIONES' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                    <div className="flex justify-between items-center">
                        <h2 className="text-xl font-bold text-gray-800">Tus Planificaciones</h2>
                        <Button 
                            variant="primary"
                            onClick={() => navigate('/portafolio/nueva-planificacion')} 
                            className="bg-blue-600 hover:bg-blue-700"
                        >
                            + Nueva Planificación
                        </Button>
                    </div>

                    <DataTable
                        columns={planColumns}
                        data={planificaciones}
                        isLoading={loadingPlanificaciones}
                        emptyMessage="No has creado planificaciones."
                        keyField="id"
                    />
                </div>
            )}

            {/* Diálogo de Confirmación Accesible (FE-19) */}
            <ConfirmDialog
                isOpen={Boolean(itemToDelete)}
                onClose={() => setItemToDelete(null)}
                onConfirm={confirmDelete}
                title={itemToDelete?.isPlan ? "Eliminar Planificación" : "Eliminar Evidencia"}
                message={`¿¿Seguro que deseas eliminar "${itemToDelete?.name}"? Esta acción no se puede deshacer.`}
                confirmText="Eliminar"
                cancelText="Cancelar"
                variant="danger"
                isLoading={isDeleting}
            />
        </div>
    );
}
