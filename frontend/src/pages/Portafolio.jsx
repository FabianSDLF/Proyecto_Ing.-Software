import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';
import portfolioService from '../services/portfolioService';
import { DataTable, ConfirmDialog, Button, Badge, Select, Modal, Input } from '../components/ui';

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

    // Historial de Versiones (HU30)
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [versionHistory, setVersionHistory] = useState([]);
    const [selectedPlanForHistory, setSelectedPlanForHistory] = useState(null);

    // Detalle Solo Lectura (HU30)
    const [showDetailModal, setShowDetailModal] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailPlan, setDetailPlan] = useState(null);

    // Edición Versionada (HU30)
    const [showEditModal, setShowEditModal] = useState(false);
    const [savingEdit, setSavingEdit] = useState(false);
    const [selectedPlanToEdit, setSelectedPlanToEdit] = useState(null);
    const [editForm, setEditForm] = useState({
        schoolGrade: '',
        mathAxis: 'NUMEROS',
        baseLearningObjective: '',
        objectives: '',
        classActivities: '',
        file: null,
    });

    // Documentos de Término (HU19)
    const MAX_FINAL_DOC_SIZE = 20 * 1024 * 1024; // 20 MB (20,971,520 bytes)
    const ALLOWED_FINAL_DOC_EXTENSIONS = ['.pdf', '.doc', '.docx'];
    const FINAL_DOC_ACCEPT = ALLOWED_FINAL_DOC_EXTENSIONS.join(','); // ".pdf,.doc,.docx"

    const [finalDocuments, setFinalDocuments] = useState([]);
    const [loadingFinalDocuments, setLoadingFinalDocuments] = useState(false);
    const [uploadingFinalDoc, setUploadingFinalDoc] = useState(false);
    const [finalDocType, setFinalDocType] = useState('INFORME_FINAL');
    const [selectedFinalFile, setSelectedFinalFile] = useState(null);
    const [showReplaceConfirm, setShowReplaceConfirm] = useState(false);
    const finalFileInputRef = useRef(null);

    // ConfirmDialog State para eliminación
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

    const fetchFinalDocuments = useCallback(async (signal) => {
        setLoadingFinalDocuments(true);
        try {
            const data = await portfolioService.getFinalDocuments({ signal });
            setFinalDocuments(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando documentos de término:', err);
                toast.error(err.message || 'Error cargando documentos de término');
            }
        } finally {
            setLoadingFinalDocuments(false);
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

    const handleFinalFileChange = (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 1. Validación de tamaño límite: máximo 20 MB (20,971,520 bytes)
        if (file.size > MAX_FINAL_DOC_SIZE) {
            toast.error('El archivo excede el tamaño máximo permitido de 20 MB');
            e.target.value = '';
            setSelectedFinalFile(null);
            return;
        }

        // 2. Validación de formato: solo .pdf, .doc, .docx
        const dotIdx = file.name.lastIndexOf('.');
        const ext = dotIdx !== -1 ? file.name.substring(dotIdx).toLowerCase() : '';
        if (!ALLOWED_FINAL_DOC_EXTENSIONS.includes(ext)) {
            toast.error('Formato no permitido. Solo se aceptan archivos PDF (.pdf) y Word (.doc, .docx)');
            e.target.value = '';
            setSelectedFinalFile(null);
            return;
        }

        setSelectedFinalFile(file);
    };

    const handleUploadFinalDocument = async (replaceConfirmed = false) => {
        if (!selectedFinalFile) {
            toast.error('Debes seleccionar un archivo para subir');
            return;
        }

        // Si ya existe un documento final y no está confirmada la sustitución, abrir confirmación
        if (finalDocuments.length > 0 && !replaceConfirmed) {
            setShowReplaceConfirm(true);
            return;
        }

        setUploadingFinalDoc(true);
        const loadingToast = toast.loading('Subiendo documento de término...');

        try {
            const formData = new FormData();
            formData.append('file', selectedFinalFile);
            formData.append('type', finalDocType);
            if (replaceConfirmed) {
                formData.append('replace', 'true');
                formData.append('replaceConfirmed', 'true');
            }

            await portfolioService.uploadFinalDocument(formData);
            toast.success('Documento de término subido exitosamente', { id: loadingToast });
            setSelectedFinalFile(null);
            if (finalFileInputRef.current) finalFileInputRef.current.value = '';
            await fetchFinalDocuments();
        } catch (err) {
            console.error('Error al subir documento de término:', err);
            toast.error(err.message || 'Error al subir documento de término', { id: loadingToast });
        } finally {
            setUploadingFinalDoc(false);
        }
    };

    const handleConfirmReplacement = async () => {
        setShowReplaceConfirm(false);
        if (selectedFinalFile) {
            await handleUploadFinalDocument(true);
        } else {
            finalFileInputRef.current?.click();
        }
    };

    const handleCancelReplacement = () => {
        setShowReplaceConfirm(false);
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

    const handleOpenHistory = async (plan) => {
        setSelectedPlanForHistory(plan);
        setHistoryLoading(true);
        try {
            const data = await portfolioService.getLessonPlanHistory(plan.id);
            setVersionHistory(Array.isArray(data) ? data : []);
            setShowHistoryModal(true);
        } catch (err) {
            toast.error(err.message || 'Error al obtener historial');
        } finally {
            setHistoryLoading(false);
        }
    };

    const handleOpenDetail = async (versionItem) => {
        setShowHistoryModal(false);
        setDetailLoading(true);
        try {
            const data = await portfolioService.getLessonPlanById(versionItem.id);
            setDetailPlan(data || versionItem);
            setShowDetailModal(true);
        } catch (err) {
            toast.error(err.message || 'Error al cargar detalle');
        } finally {
            setDetailLoading(false);
        }
    };

    const handleOpenEdit = (plan) => {
        setSelectedPlanToEdit(plan);
        setEditForm({
            schoolGrade: plan.schoolGrade || '',
            mathAxis: plan.mathAxis || 'NUMEROS',
            baseLearningObjective: plan.baseLearningObjective || plan.objective || '',
            objectives: plan.objectives || plan.specificObjectives || '',
            classActivities: typeof plan.contentJson === 'string' ? plan.contentJson : JSON.stringify(plan.contentJson || {}),
            file: null,
        });
        setShowEditModal(true);
    };

    const handleSaveEdit = async (e) => {
        e.preventDefault();
        if (!selectedPlanToEdit) return;
        setSavingEdit(true);
        try {
            await portfolioService.createLessonPlanVersion(selectedPlanToEdit.id, editForm);
            toast.success('Nueva versión creada exitosamente');
            setShowEditModal(false);
            fetchPlanificaciones();
        } catch (err) {
            toast.error(err.message || 'Error al guardar versión');
        } finally {
            setSavingEdit(false);
        }
    };

    const planColumns = useMemo(() => [
        {
            header: 'Versión',
            align: 'center',
            render: (row) => (
                <Badge variant="info">
                    v{row.version || 1}
                </Badge>
            ),
        },
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
                <Badge variant={row.status === 'APROBADA' || row.status === 'APROBADO' ? 'success' : row.status === 'RECHAZADA' || row.status === 'RECHAZADO' ? 'danger' : 'gray'}>
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
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenHistory(row)}
                    >
                        Historial
                    </Button>
                    <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleOpenEdit(row)}
                    >
                        Editar
                    </Button>
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
    ], [handleDownload]);

    return (
        <div className="max-w-5xl mx-auto p-6 space-y-6">
            <div>
                <h1 className="text-3xl font-extrabold text-[#003B70] tracking-tight">Mi Portafolio Digital</h1>
                <p className="text-gray-500 mt-1">Sube y organiza tus evidencias de aprendizaje, planificaciones didácticas y documento final de término.</p>
            </div>
            
            <div className="flex gap-4 border-b border-gray-200">
                <button 
                    type="button"
                    onClick={() => {
                        setActiveTab('EVIDENCIAS');
                        fetchEvidencias();
                    }}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors ${activeTab === 'EVIDENCIAS' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Evidencias de Aprendizaje
                </button>
                <button 
                    type="button"
                    onClick={() => {
                        setActiveTab('PLANIFICACIONES');
                        fetchPlanificaciones();
                    }}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors ${activeTab === 'PLANIFICACIONES' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Planificaciones
                </button>
                <button 
                    type="button"
                    onClick={() => {
                        setActiveTab('TERMINO');
                        fetchFinalDocuments();
                    }}
                    className={`py-2 px-4 font-semibold border-b-2 transition-colors ${activeTab === 'TERMINO' ? 'border-orange-600 text-orange-600' : 'border-transparent text-gray-500 hover:text-gray-700'}`}
                >
                    Documento de Término
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

            {activeTab === 'TERMINO' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                    <div>
                        <h2 className="text-xl font-bold text-gray-800">Documento de Término</h2>
                        <p className="text-sm text-gray-500 mt-1">
                            Sube tu Informe Final o Proyecto de Intervención de práctica profesional. Solo se permite un documento final activo por estudiante (máximo 20 MB en formato PDF o Word).
                        </p>
                    </div>

                    {/* Tarjeta de Documento Existente si ya existe */}
                    {finalDocuments.length > 0 && (
                        <div className="space-y-4">
                            <div className="text-sm font-semibold text-gray-700">Documento de término registrado:</div>
                            {finalDocuments.map((doc) => (
                                <div 
                                    key={doc.id}
                                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-5 bg-orange-50/40 border border-orange-200 rounded-xl gap-4"
                                >
                                    <div className="space-y-1.5">
                                        <div className="flex items-center gap-2">
                                            <Badge variant={doc.type === 'INFORME_FINAL' ? 'warning' : 'info'}>
                                                {doc.type === 'INFORME_FINAL' ? 'Informe Final' : 'Proyecto de Intervención'}
                                            </Badge>
                                            <span className="text-xs text-gray-500">
                                                Subido el {new Date(doc.uploadDate).toLocaleDateString()}
                                            </span>
                                        </div>
                                        <h3 className="text-base font-bold text-gray-900 break-all">
                                            {doc.fileName}
                                        </h3>
                                        <p className="text-xs text-gray-500">
                                            Tamaño: {doc.sizeBytes ? `${(doc.sizeBytes / (1024 * 1024)).toFixed(2)} MB` : 'Desconocido'}
                                        </p>
                                    </div>

                                    <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={() => handleDownload(doc)}
                                        >
                                            Descargar
                                        </Button>
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            onClick={() => setShowReplaceConfirm(true)}
                                            className="bg-orange-600 hover:bg-orange-700"
                                        >
                                            Reemplazar Documento
                                        </Button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Formulario de Subida (Disponible en estado vacío o para seleccionar nuevo archivo de reemplazo) */}
                    <div className="border border-gray-200 rounded-xl p-5 space-y-4 bg-gray-50/50">
                        <h3 className="text-base font-semibold text-gray-800">
                            {finalDocuments.length > 0 ? 'Cargar nuevo archivo para reemplazo' : 'Subir Documento'}
                        </h3>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <Select
                                label="Tipo de Documento"
                                id="final-doc-type"
                                value={finalDocType}
                                onChange={(e) => setFinalDocType(e.target.value)}
                                options={[
                                    { value: 'INFORME_FINAL', label: 'Informe Final' },
                                    { value: 'PROYECTO_INTERVENCION', label: 'Proyecto de Intervención' },
                                ]}
                                placeholder={null}
                            />

                            <div>
                                <label htmlFor="final-document-file" className="block text-sm font-medium text-gray-700 mb-1.5">
                                    Seleccionar archivo
                                </label>
                                <input
                                    type="file"
                                    id="final-document-file"
                                    ref={finalFileInputRef}
                                    onChange={handleFinalFileChange}
                                    accept={FINAL_DOC_ACCEPT}
                                    aria-label="Seleccionar archivo"
                                    className="block w-full text-sm text-gray-500 file:mr-4 file:py-2.5 file:px-4 file:rounded-xl file:border-0 file:text-sm file:font-semibold file:bg-orange-50 file:text-orange-700 hover:file:bg-orange-100 border border-gray-300 rounded-xl bg-white p-1 cursor-pointer"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Formatos permitidos: .pdf, .doc, .docx. Tamaño máximo: 20 MB.
                                </p>
                            </div>
                        </div>

                        {selectedFinalFile && (
                            <div className="text-xs text-gray-600 bg-white p-3 rounded-lg border border-gray-200 flex items-center justify-between">
                                <span>Archivo seleccionado: <strong>{selectedFinalFile.name}</strong> ({(selectedFinalFile.size / (1024 * 1024)).toFixed(2)} MB)</span>
                                <button 
                                    type="button" 
                                    onClick={() => {
                                        setSelectedFinalFile(null);
                                        if (finalFileInputRef.current) finalFileInputRef.current.value = '';
                                    }}
                                    className="text-red-500 hover:text-red-700 text-xs font-semibold ml-2"
                                >
                                    Quitar
                                </button>
                            </div>
                        )}

                        <div className="flex justify-end pt-2">
                            <Button
                                variant="primary"
                                onClick={() => handleUploadFinalDocument(false)}
                                isLoading={uploadingFinalDoc}
                                className="bg-orange-600 hover:bg-orange-700"
                            >
                                Subir Documento
                            </Button>
                        </div>
                    </div>

                    {/* Modal de Confirmación de Reemplazo (ConfirmDialog) */}
                    <ConfirmDialog
                        isOpen={showReplaceConfirm}
                        onClose={handleCancelReplacement}
                        onCancel={handleCancelReplacement}
                        onConfirm={handleConfirmReplacement}
                        title="Reemplazar Documento"
                        message="¿Deseas reemplazar el documento de término existente? Solo puedes registrar un documento final activo por periodo de práctica. El archivo previo será sustituido."
                        confirmText="Reemplazar"
                        cancelText="Cancelar"
                        variant="warning"
                        isLoading={uploadingFinalDoc}
                    />
                </div>
            )}

            {/* Diálogo de Confirmación Accesible para eliminación */}
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

            {/* Modal de Historial de Versiones (HU30) */}
            <Modal
                isOpen={showHistoryModal}
                onClose={() => setShowHistoryModal(false)}
                title="Historial de Versiones"
                description={selectedPlanForHistory ? `Historial de cambios para planificación de ${selectedPlanForHistory.schoolGrade}` : ''}
                size="lg"
                footer={
                    <div className="flex justify-end">
                        <Button variant="secondary" onClick={() => setShowHistoryModal(false)}>
                            Cerrar
                        </Button>
                    </div>
                }
            >
                {historyLoading ? (
                    <div className="py-8 text-center text-gray-500">Cargando historial de versiones...</div>
                ) : versionHistory.length === 0 ? (
                    <div className="py-8 text-center text-gray-500">No hay versiones registradas.</div>
                ) : (
                    <div className="space-y-4">
                        {versionHistory.map((v) => (
                            <div
                                key={v.id}
                                className="p-4 rounded-xl border border-gray-200 bg-gray-50/50 flex flex-col md:flex-row md:items-center justify-between gap-4"
                            >
                                <div className="space-y-1.5 flex-1">
                                    <div className="flex items-center gap-2">
                                        <Badge variant="info">
                                            Versión {v.version}
                                        </Badge>
                                        <Badge variant={v.status === 'APROBADA' || v.status === 'APROBADO' ? 'success' : v.status === 'RECHAZADA' || v.status === 'RECHAZADO' ? 'danger' : 'warning'}>
                                            {v.status || 'BORRADOR'}
                                        </Badge>
                                        <span className="text-xs text-gray-500">
                                            {new Date(v.createdAt).toLocaleDateString()}
                                        </span>
                                    </div>
                                    <p className="text-sm font-semibold text-gray-800">
                                        {v.schoolGrade} — {v.mathAxis}
                                    </p>
                                    <p className="text-xs text-gray-600 line-clamp-2">
                                        {v.baseLearningObjective || v.objective}
                                    </p>
                                    {v.teacherFeedback && (
                                        <div className="mt-2 p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-900">
                                            <span className="font-bold">Retroalimentación docente: </span>
                                            {v.teacherFeedback}
                                        </div>
                                    )}
                                </div>
                                <div className="shrink-0">
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => handleOpenDetail(v)}
                                    >
                                        Ver Detalle
                                    </Button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </Modal>

            {/* Modal de Detalle Modo Solo Lectura (HU30) */}
            <Modal
                isOpen={showDetailModal}
                onClose={() => setShowDetailModal(false)}
                title={`Detalle de Planificación (v${detailPlan?.version || 1})`}
                showCloseButton={false}
                size="lg"
                footer={
                    <div className="flex justify-end">
                        <Button variant="secondary" onClick={() => setShowDetailModal(false)}>
                            Cerrar
                        </Button>
                    </div>
                }
            >
                <div className="p-3 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl text-sm mb-4">
                    Modo solo lectura: Las versiones históricas son inmutables y no pueden editarse ni restaurarse.
                </div>

                <div className="space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 mb-1">Curso</label>
                        <input
                            type="text"
                            value={detailPlan?.schoolGrade || ''}
                            disabled
                            readOnly
                            className="w-full text-sm bg-gray-100 border border-gray-200 rounded-lg p-2.5 text-gray-700 cursor-not-allowed"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 mb-1">Eje Matemático</label>
                        <input
                            type="text"
                            value={detailPlan?.mathAxis || ''}
                            disabled
                            readOnly
                            className="w-full text-sm bg-gray-100 border border-gray-200 rounded-lg p-2.5 text-gray-700 cursor-not-allowed"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 mb-1">Objetivo Basal de Aprendizaje</label>
                        <textarea
                            value={detailPlan?.baseLearningObjective || detailPlan?.objective || ''}
                            disabled
                            readOnly
                            rows={3}
                            className="w-full text-sm bg-gray-100 border border-gray-200 rounded-lg p-2.5 text-gray-700 cursor-not-allowed resize-none"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 mb-1">Objetivos Específicos</label>
                        <textarea
                            value={detailPlan?.objectives || detailPlan?.specificObjectives || ''}
                            disabled
                            readOnly
                            rows={3}
                            className="w-full text-sm bg-gray-100 border border-gray-200 rounded-lg p-2.5 text-gray-700 cursor-not-allowed resize-none"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 mb-1">Actividades de la Clase</label>
                        <textarea
                            value={typeof detailPlan?.contentJson === 'string' ? detailPlan.contentJson : JSON.stringify(detailPlan?.contentJson || detailPlan?.classActivities || {})}
                            disabled
                            readOnly
                            rows={3}
                            className="w-full text-sm bg-gray-100 border border-gray-200 rounded-lg p-2.5 text-gray-700 cursor-not-allowed resize-none"
                        />
                    </div>
                    {detailPlan?.teacherFeedback && (
                        <div>
                            <label className="block text-xs font-semibold text-gray-600 mb-1">Retroalimentación Docente</label>
                            <textarea
                                value={detailPlan.teacherFeedback}
                                disabled
                                readOnly
                                rows={2}
                                className="w-full text-sm bg-amber-50/70 border border-amber-200 rounded-lg p-2.5 text-amber-900 cursor-not-allowed resize-none"
                            />
                        </div>
                    )}
                </div>
            </Modal>

            {/* Modal de Edición Versionada (HU30) */}
            <Modal
                isOpen={showEditModal}
                onClose={() => setShowEditModal(false)}
                title="Editar Planificación (Nueva Versión)"
                size="lg"
            >
                <form onSubmit={handleSaveEdit} className="space-y-4">
                    <Input
                        label="Curso"
                        name="schoolGrade"
                        value={editForm.schoolGrade}
                        onChange={(e) => setEditForm(prev => ({ ...prev, schoolGrade: e.target.value }))}
                        required
                    />

                    <Select
                        label="Eje Matemático"
                        name="mathAxis"
                        value={editForm.mathAxis}
                        onChange={(e) => setEditForm(prev => ({ ...prev, mathAxis: e.target.value }))}
                        options={[
                            { value: 'NUMEROS', label: 'Números' },
                            { value: 'ALGEBRA', label: 'Álgebra' },
                            { value: 'GEOMETRIA', label: 'Geometría' },
                            { value: 'PROBABILIDAD_ESTADISTICA', label: 'Probabilidad y Estadística' },
                        ]}
                    />

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1.5">
                            Objetivo Basal de Aprendizaje <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={editForm.baseLearningObjective}
                            onChange={(e) => setEditForm(prev => ({ ...prev, baseLearningObjective: e.target.value }))}
                            rows={3}
                            className="w-full text-sm border border-gray-300 rounded-xl p-2.5 text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1.5">
                            Objetivos Específicos
                        </label>
                        <textarea
                            value={editForm.objectives}
                            onChange={(e) => setEditForm(prev => ({ ...prev, objectives: e.target.value }))}
                            rows={3}
                            className="w-full text-sm border border-gray-300 rounded-xl p-2.5 text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1.5">
                            Actividades de la Clase
                        </label>
                        <textarea
                            value={editForm.classActivities}
                            onChange={(e) => setEditForm(prev => ({ ...prev, classActivities: e.target.value }))}
                            rows={3}
                            className="w-full text-sm border border-gray-300 rounded-xl p-2.5 text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1.5">
                            Nuevo Archivo Adjunto (Opcional)
                        </label>
                        <input
                            type="file"
                            accept={acceptAttr}
                            onChange={(e) => setEditForm(prev => ({ ...prev, file: e.target.files[0] }))}
                            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 border border-gray-300 rounded-xl p-1"
                        />
                    </div>

                    <div className="flex justify-end gap-2 pt-4 border-t border-gray-100">
                        <Button
                            type="button"
                            variant="secondary"
                            onClick={() => setShowEditModal(false)}
                        >
                            Cancelar
                        </Button>
                        <Button
                            type="submit"
                            variant="primary"
                            isLoading={savingEdit}
                        >
                            Guardar Nueva Versión
                        </Button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
