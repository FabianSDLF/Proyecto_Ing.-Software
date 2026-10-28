import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import portfolioService from '../services/portfolioService';
import { DataTable, ConfirmDialog, Button, Badge, Select, Modal, Input } from '../components/ui';

export default function Portafolio({ isReadOnly = false }) {
    const navigate = useNavigate();
    const { assignmentId } = useParams();
    const location = useLocation();
    const modoSupervision = isReadOnly || Boolean(assignmentId);
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
        duracion: '90',
        baseLearningObjective: '',
        objectives: '',
        objetivoActitudinal: '',
        inicio: '',
        desarrollo: '',
        cierre: '',
        recursos: '',
        evaluacion: '',
        classActivities: '',
        file: null,
    });

    const parsePlanContent = useCallback((rawContent) => {
        const empty = {
            duracion: '90',
            objetivoActitudinal: '',
            inicio: '',
            desarrollo: '',
            cierre: '',
            recursos: '',
            evaluacion: '',
        };
        if (!rawContent) return empty;

        let parsed = rawContent;
        if (typeof rawContent === 'string') {
            const trimmed = rawContent.trim();
            if (!trimmed) return empty;
            try {
                parsed = JSON.parse(trimmed);
            } catch {
                return { ...empty, desarrollo: trimmed };
            }
        }

        if (!parsed || typeof parsed !== 'object') {
            return { ...empty, desarrollo: String(rawContent) };
        }

        const formatMomento = (val) => {
            if (!val) return '';
            if (typeof val === 'string') return val;
            if (typeof val === 'object') {
                const desc = val.descripcion || val.actividad || '';
                const mins = val.minutos ? `[${val.minutos} min] ` : '';
                return `${mins}${desc}`.trim();
            }
            return String(val);
        };

        const calcDuracion = () => {
            if (parsed.duracion) return String(parsed.duracion);
            const mInicio = Number(parsed.inicio?.minutos) || 0;
            const mDes = Number(parsed.desarrollo?.minutos) || 0;
            const mCierre = Number(parsed.cierre?.minutos) || 0;
            const total = mInicio + mDes + mCierre;
            return total > 0 ? String(total) : '90';
        };

        const calcRecursos = () => {
            if (typeof parsed.recursos === 'string' && parsed.recursos.trim()) {
                return parsed.recursos;
            }
            if (Array.isArray(parsed.recursos)) {
                return parsed.recursos.join(', ');
            }
            const setRec = new Set();
            [parsed.inicio?.recursos, parsed.desarrollo?.recursos, parsed.cierre?.recursos].forEach((arr) => {
                if (Array.isArray(arr)) {
                    arr.forEach((r) => {
                        if (r) setRec.add(r);
                    });
                }
            });
            return Array.from(setRec).join(', ');
        };

        return {
            duracion: calcDuracion(),
            objetivoActitudinal: parsed.objetivoActitudinal || '',
            inicio: formatMomento(parsed.inicio),
            desarrollo: formatMomento(parsed.desarrollo),
            cierre: formatMomento(parsed.cierre),
            recursos: calcRecursos(),
            evaluacion: parsed.evaluacion || parsed.adaptaciones_pie || '',
        };
    }, []);

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

    // Estados para el Modal de Retroalimentación Formativa
    const [feedbackModal, setFeedbackModal] = useState({
        isOpen: false,
        tipo: null, // 'DOCUMENTO' | 'PLAN'
        item: null,
        comentario: '',
        readOnly: false,
        guardando: false,
    });

    const parseComentarios = useCallback((item) => {
        const raw = item?.teacherFeedback;
        if (!raw || typeof raw !== 'string' || !raw.trim()) return [];
        const trimmed = raw.trim();
        if (trimmed.startsWith('[')) {
            try {
                const parsed = JSON.parse(trimmed);
                if (Array.isArray(parsed)) {
                    return parsed.filter(c => c && typeof c.text === 'string' && c.text.trim());
                }
            } catch {
                // fallback a comentario simple
            }
        }
        return [{
            id: 'legacy-1',
            authorName: 'Docente Supervisor',
            authorRole: 'Docente',
            authorEmail: '',
            text: trimmed,
            date: item?.feedbackDate || item?.updatedAt || item?.createdAt || null,
        }];
    }, []);

    const formatFeedbackPlainText = useCallback((item) => {
        const list = parseComentarios(item);
        if (list.length === 0) return '';
        if (list.length === 1 && list[0].id === 'legacy-1') return list[0].text;
        return list.map(c => `[${c.authorName || 'Docente'} - ${c.authorRole || 'Docente'}]: ${c.text}`).join('\n\n');
    }, [parseComentarios]);

    const abrirModalFeedback = (item, tipo, readOnly = false) => {
        setFeedbackModal({
            isOpen: true,
            tipo,
            item,
            comentario: '',
            readOnly,
            guardando: false,
        });
    };

    const cerrarModalFeedback = () => {
        if (feedbackModal.guardando) return;
        setFeedbackModal({
            isOpen: false,
            tipo: null,
            item: null,
            comentario: '',
            readOnly: false,
            guardando: false,
        });
    };

    const guardarRetroalimentacion = async () => {
        if (!feedbackModal.item || feedbackModal.readOnly) return;
        const texto = feedbackModal.comentario.trim();
        if (!texto) {
            toast.error('Escribe un comentario antes de publicarlo.');
            return;
        }
        if (texto.length > 2000) {
            toast.error('El comentario no puede superar los 2000 caracteres.');
            return;
        }

        setFeedbackModal((prev) => ({ ...prev, guardando: true }));
        try {
            let actualizado = null;
            if (feedbackModal.tipo === 'PLAN') {
                actualizado = await portfolioService.agregarRetroalimentacionPlan(
                    feedbackModal.item.id,
                    texto
                );
                await fetchPlanificaciones();
            } else {
                actualizado = await portfolioService.agregarRetroalimentacionDocumento(
                    feedbackModal.item.id,
                    texto
                );
                await fetchEvidencias();
                await fetchFinalDocuments();
            }
            toast.success('Comentario personal registrado correctamente.');
            setFeedbackModal((prev) => ({
                ...prev,
                item: actualizado || prev.item,
                comentario: '',
                guardando: false,
            }));
        } catch (error) {
            console.error(error);
            toast.error(error?.message || 'No se pudo guardar el comentario.');
            setFeedbackModal((prev) => ({ ...prev, guardando: false }));
        }
    };

    // Respaldo de datos de la asignación (desde MisEstudiantes o desde las entidades si el docente recarga con F5)
    const assignment = location.state?.assignment
        || planificaciones[0]?.practiceAssignment
        || evidencias[0]?.practiceAssignment
        || finalDocuments[0]?.practiceAssignment;

    const allowedExtensions = ['.pdf', '.doc', '.docx', '.ppt', '.pptx'];
    const acceptAttr = allowedExtensions.join(',');

    const fetchEvidencias = useCallback(async (signal) => {
        setLoadingEvidencias(true);
        try {
            const data = modoSupervision && assignmentId
                ? await portfolioService.obtenerEvidenciasPorAsignacion(assignmentId, { signal })
                : await portfolioService.getDocuments({ signal });
            setEvidencias(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando evidencias:', err);
                toast.error(err.message || 'Error cargando evidencias');
            }
        } finally {
            setLoadingEvidencias(false);
        }
    }, [modoSupervision, assignmentId]);

    const fetchPlanificaciones = useCallback(async (signal) => {
        setLoadingPlanificaciones(true);
        try {
            const data = modoSupervision && assignmentId
                ? await portfolioService.obtenerPlanificacionesPorAsignacion(assignmentId, { signal })
                : await portfolioService.getLessonPlans({ signal });
            setPlanificaciones(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando planificaciones:', err);
                toast.error(err.message || 'Error cargando planificaciones');
            }
        } finally {
            setLoadingPlanificaciones(false);
        }
    }, [modoSupervision, assignmentId]);

    const fetchFinalDocuments = useCallback(async (signal) => {
        setLoadingFinalDocuments(true);
        try {
            const data = modoSupervision && assignmentId
                ? await portfolioService.obtenerDocumentosFinalesPorAsignacion(assignmentId, { signal })
                : await portfolioService.getFinalDocuments({ signal });
            setFinalDocuments(Array.isArray(data) ? data : []);
        } catch (err) {
            if (err.name !== 'AbortError') {
                console.error('Error cargando documentos de término:', err);
                toast.error(err.message || 'Error cargando documentos de término');
            }
        } finally {
            setLoadingFinalDocuments(false);
        }
    }, [modoSupervision, assignmentId]);

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

    const handleDownload = useCallback(async (doc) => {
        if (!doc || !doc.fileUrl) return;
        const toastId = toast.loading('Descargando archivo...');

        try {
            await portfolioService.downloadDocument(doc.fileUrl, doc.fileName);
            toast.success('Descarga completada', { id: toastId });
        } catch (error) {
            console.error('Error al descargar:', error);
            toast.error(error.message || 'No se pudo descargar el archivo', { id: toastId });
        }
    }, []);

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

    const renderBotonRetroalimentacion = (row, tipo = 'DOCUMENTO') => {
        const comentarios = parseComentarios(row);
        const total = comentarios.length;
        const tieneComentario = total > 0;

        if (modoSupervision) {
            return (
                <Button
                    size="sm"
                    variant={tieneComentario ? 'secondary' : 'primary'}
                    onClick={() => abrirModalFeedback(row, tipo, false)}
                    title="Ver hilo de comentarios y agregar comentario personal"
                >
                    {tieneComentario ? `💬 Comentarios (${total})` : '💬 Comentar'}
                </Button>
            );
        }

        if (tieneComentario) {
            return (
                <Button
                    size="sm"
                    variant="outline"
                    onClick={() => abrirModalFeedback(row, tipo, true)}
                    aria-label="Ver comentarios docentes"
                >
                    💬 Ver comentarios ({total})
                </Button>
            );
        }

        return (
            <Button
                size="sm"
                variant="outline"
                disabled
                className="opacity-60 cursor-default"
            >
                Sin comentarios
            </Button>
        );
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
                <div className="flex items-center justify-end space-x-2">
                    {renderBotonRetroalimentacion(row, 'DOCUMENTO')}
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDownload(row)}
                    >
                        Descargar
                    </Button>
                    {!modoSupervision && (
                        <Button
                            variant="danger"
                            size="sm"
                            onClick={() => setItemToDelete({ id: row.id, name: row.fileName, isPlan: false })}
                        >
                            Eliminar
                        </Button>
                    )}
                </div>
            ),
        },
    ], [handleDownload, modoSupervision]);

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
        const structured = parsePlanContent(plan.contentJson || plan.classActivities);
        setEditForm({
            schoolGrade: plan.schoolGrade || '',
            mathAxis: plan.mathAxis || 'NUMEROS',
            duracion: structured.duracion || '90',
            baseLearningObjective: plan.baseLearningObjective || plan.objective || '',
            objectives: plan.objectives || plan.specificObjectives || '',
            objetivoActitudinal: structured.objetivoActitudinal || '',
            inicio: structured.inicio || '',
            desarrollo: structured.desarrollo || '',
            cierre: structured.cierre || '',
            recursos: structured.recursos || '',
            evaluacion: structured.evaluacion || '',
            classActivities: JSON.stringify(structured),
            file: null,
        });
        setShowEditModal(true);
    };

    const handleSaveEdit = async (e) => {
        e.preventDefault();
        if (!selectedPlanToEdit) return;
        setSavingEdit(true);
        try {
            const structuredActivities = JSON.stringify({
                objetivoActitudinal: editForm.objetivoActitudinal || '',
                duracion: editForm.duracion || '90',
                recursos: editForm.recursos || '',
                evaluacion: editForm.evaluacion || '',
                inicio: editForm.inicio || '',
                desarrollo: editForm.desarrollo || '',
                cierre: editForm.cierre || '',
            });
            const payload = {
                ...editForm,
                classActivities: structuredActivities,
            };
            await portfolioService.createLessonPlanVersion(selectedPlanToEdit.id, payload);
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
                <div className="flex items-center justify-end space-x-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenDetail(row)}
                    >
                        Ver Detalle
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenHistory(row)}
                    >
                        Historial
                    </Button>
                    {!modoSupervision && (
                        <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleOpenEdit(row)}
                        >
                            Editar
                        </Button>
                    )}
                    {renderBotonRetroalimentacion(row, 'PLAN')}
                    {row.document && (
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleDownload(row.document)}
                        >
                            Descargar
                        </Button>
                    )}
                    {!modoSupervision && (
                        <Button
                            variant="danger"
                            size="sm"
                            onClick={() => setItemToDelete({ id: row.id, name: `Planificación (${row.schoolGrade})`, isPlan: true })}
                        >
                            Eliminar
                        </Button>
                    )}
                </div>
            ),
        },
    ], [handleDownload, modoSupervision]);

    return (
        <div className="max-w-5xl mx-auto p-6 space-y-6">
            {/* Banner superior cuando un Docente está supervisando */}
            {modoSupervision && (
                <div className="bg-blue-50 border border-blue-200 rounded-2xl p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div>
                        <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 mb-1">
                            Modo Supervisión de Portafolio
                        </span>
                        <h2 className="text-xl font-bold text-blue-900">
                            Estudiante: {assignment?.student?.fullName || 'Estudiante'}
                        </h2>
                        <p className="text-sm text-blue-700">
                            {assignment?.subject?.name || 'Práctica'} • {assignment?.center?.name || 'Centro de práctica'}
                        </p>
                    </div>
                    <Button variant="secondary" size="sm" onClick={() => navigate('/mis-estudiantes')}>
                        ← Volver a Mis Estudiantes
                    </Button>
                </div>
            )}

            <div>
                <h1 className="text-3xl font-extrabold text-[#003B70] tracking-tight">
                    {modoSupervision ? 'Portafolio del Estudiante' : 'Mi Portafolio Digital'}
                </h1>
                <p className="text-gray-500 mt-1">
                    {modoSupervision
                        ? 'Revisa el avance de evidencias, planificaciones y documentos antes de evaluar.'
                        : 'Sube y organiza tus evidencias de aprendizaje, planificaciones didácticas y documento final de término.'}
                </p>
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
                    Informe
                </button>
            </div>

            {activeTab === 'EVIDENCIAS' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                    <div className="flex justify-between items-center">
                        <h2 className="text-xl font-bold text-gray-800">
                            {modoSupervision ? 'Evidencias del Estudiante' : 'Tus Evidencias'}
                        </h2>
                        <input
                            type="file"
                            ref={evidenciaInputRef}
                            onChange={handleEvidenciaUpload}
                            accept={acceptAttr}
                            className="hidden"
                        />
                        {!modoSupervision && (
                            <Button
                                variant="primary"
                                onClick={() => evidenciaInputRef.current?.click()}
                                isLoading={uploadingEvidencia}
                                className="bg-orange-600 hover:bg-orange-700"
                            >
                                Subir Archivo
                            </Button>
                        )}
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
                        <h2 className="text-xl font-bold text-gray-800">
                            {modoSupervision ? 'Planificaciones del Estudiante' : 'Tus Planificaciones'}
                        </h2>
                        {!modoSupervision && (
                            <Button
                                variant="primary"
                                onClick={() => navigate('/portafolio/nueva-planificacion')}
                                className="bg-blue-600 hover:bg-blue-700"
                            >
                                + Nueva Planificación
                            </Button>
                        )}
                    </div>

                    <DataTable
                        columns={planColumns}
                        data={planificaciones}
                        isLoading={loadingPlanificaciones}
                        emptyMessage="No se han registrado planificaciones."
                        keyField="id"
                    />
                </div>
            )}

            {activeTab === 'TERMINO' && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                    <div>
                        <h2 className="text-xl font-bold text-gray-800">Informe</h2>
                        <p className="text-sm text-gray-500 mt-1">
                            {modoSupervision
                                ? 'Consulta o descarga el Informe Final entregado por el estudiante.'
                                : 'Sube tu Informe Final o Proyecto de Intervención de práctica profesional. Solo se permite un documento final activo por estudiante (máximo 20 MB en formato PDF o Word).'}
                        </p>
                    </div>

                    {loadingFinalDocuments ? (
                        <div className="py-8 text-center text-gray-500">Cargando Informe...</div>
                    ) : finalDocuments.length === 0 && modoSupervision ? (
                        <div className="py-8 text-center text-gray-500 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                            El estudiante aún no ha subido el Informe
                        </div>
                    ) : null}

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
                                        {renderBotonRetroalimentacion(doc, 'DOCUMENTO')}
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={() => handleDownload(doc)}
                                        >
                                            Descargar
                                        </Button>
                                        {!modoSupervision && (
                                            <Button
                                                variant="primary"
                                                size="sm"
                                                onClick={() => setShowReplaceConfirm(true)}
                                                className="bg-orange-600 hover:bg-orange-700"
                                            >
                                                Reemplazar Documento
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Formulario de Subida (Solo visible para el Estudiante) */}
                    {!modoSupervision && (
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
                    )}

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
                message={`¿Seguro que deseas eliminar "${itemToDelete?.name}"? Esta acción no se puede deshacer.`}
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
                                        <div className="mt-2 p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-900 whitespace-pre-wrap">
                                            <span className="font-bold">Retroalimentación docente: </span>
                                            {formatFeedbackPlainText(v)}
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
                {detailLoading ? (
                    <div className="py-8 text-center text-gray-500">Cargando detalle de la planificación...</div>
                ) : (() => {
                    const detailContent = parsePlanContent(detailPlan?.contentJson || detailPlan?.classActivities);
                    return (
                        <div className="max-h-[75vh] overflow-y-auto pr-1 space-y-5">
                            <div className="p-3 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl text-sm flex flex-wrap items-center justify-between gap-2">
                                <span>Modo solo lectura: Las versiones históricas son inmutables y no pueden editarse ni restaurarse.</span>
                                <div className="flex items-center gap-2">
                                    <Badge variant="info">v{detailPlan?.version || 1}</Badge>
                                    <Badge variant={detailPlan?.status === 'APROBADA' || detailPlan?.status === 'APROBADO' ? 'success' : detailPlan?.status === 'RECHAZADA' || detailPlan?.status === 'RECHAZADO' ? 'danger' : 'warning'}>
                                        {detailPlan?.status || 'BORRADOR'}
                                    </Badge>
                                </div>
                            </div>

                            {/* 1. Datos Generales */}
                            <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">1. Datos Generales</h4>
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Curso / Nivel Escolar</label>
                                        <input
                                            type="text"
                                            value={detailPlan?.schoolGrade || ''}
                                            disabled
                                            readOnly
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 font-medium cursor-not-allowed"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Eje Matemático</label>
                                        <input
                                            type="text"
                                            value={detailPlan?.mathAxis || ''}
                                            disabled
                                            readOnly
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 font-medium cursor-not-allowed"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Duración Estimada (min)</label>
                                        <input
                                            type="text"
                                            value={detailContent.duracion ? `${detailContent.duracion} min` : '90 min'}
                                            disabled
                                            readOnly
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 font-medium cursor-not-allowed"
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 2. Objetivos de Aprendizaje */}
                            <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">2. Objetivos de Aprendizaje</h4>
                                <div>
                                    <label className="block text-xs font-semibold text-gray-600 mb-1">Objetivo de Aprendizaje Basal (OA)</label>
                                    <textarea
                                        value={detailPlan?.baseLearningObjective || detailPlan?.objective || ''}
                                        disabled
                                        readOnly
                                        rows={2}
                                        className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                    />
                                </div>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Objetivos Específicos de la Clase</label>
                                        <textarea
                                            value={detailPlan?.objectives || detailPlan?.specificObjectives || 'Sin especificar'}
                                            disabled
                                            readOnly
                                            rows={3}
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Objetivo Actitudinal (OAT)</label>
                                        <textarea
                                            value={detailContent.objetivoActitudinal || 'Sin especificar'}
                                            disabled
                                            readOnly
                                            rows={3}
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* 3. Estructura Didáctica de la Clase */}
                            <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">3. Estructura Didáctica de la Clase</h4>
                                <div className="border-l-4 border-indigo-400 pl-3">
                                    <label className="block text-xs font-bold text-indigo-900 mb-1">Inicio (Activación y Motivación)</label>
                                    <textarea
                                        value={detailContent.inicio || 'Sin actividades de inicio registradas'}
                                        disabled
                                        readOnly
                                        rows={2}
                                        className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                    />
                                </div>
                                <div className="border-l-4 border-blue-500 pl-3">
                                    <label className="block text-xs font-bold text-blue-900 mb-1">Desarrollo (Modelamiento y Práctica)</label>
                                    <textarea
                                        value={detailContent.desarrollo || 'Sin actividades de desarrollo registradas'}
                                        disabled
                                        readOnly
                                        rows={3}
                                        className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                    />
                                </div>
                                <div className="border-l-4 border-emerald-500 pl-3">
                                    <label className="block text-xs font-bold text-emerald-900 mb-1">Cierre (Síntesis y Metacognición)</label>
                                    <textarea
                                        value={detailContent.cierre || 'Sin actividades de cierre registradas'}
                                        disabled
                                        readOnly
                                        rows={2}
                                        className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                    />
                                </div>
                            </div>

                            {/* 4. Recursos, Evaluación y Archivo Adjunto */}
                            <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">4. Recursos y Evaluación</h4>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Recursos Materiales</label>
                                        <textarea
                                            value={detailContent.recursos || 'Sin especificar'}
                                            disabled
                                            readOnly
                                            rows={2}
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-600 mb-1">Instrumentos de Evaluación / PIE</label>
                                        <textarea
                                            value={detailContent.evaluacion || 'Sin especificar'}
                                            disabled
                                            readOnly
                                            rows={2}
                                            className="w-full text-sm bg-white border border-gray-200 rounded-lg p-2.5 text-gray-800 cursor-not-allowed resize-none"
                                        />
                                    </div>
                                </div>
                                {detailPlan?.document && (
                                    <div className="flex items-center justify-between bg-white border border-gray-200 rounded-lg p-2.5 text-xs text-gray-700">
                                        <span>Archivo adjunto: <strong>{detailPlan.document.fileName}</strong></span>
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={() => handleDownload(detailPlan.document)}
                                        >
                                            Descargar Adjunto
                                        </Button>
                                    </div>
                                )}
                            </div>

                            {/* 5. Retroalimentación Docente */}
                            {detailPlan?.teacherFeedback && (
                                <div className="p-4 bg-amber-50/70 border border-amber-200 rounded-xl space-y-2">
                                    <label className="block text-xs font-bold uppercase tracking-wider text-amber-900">Retroalimentación Docente</label>
                                    <textarea
                                        value={formatFeedbackPlainText(detailPlan)}
                                        disabled
                                        readOnly
                                        rows={3}
                                        className="w-full text-sm bg-white/90 border border-amber-200 rounded-lg p-2.5 text-amber-950 cursor-not-allowed resize-none"
                                    />
                                </div>
                            )}
                        </div>
                    );
                })()}
            </Modal>

            {/* Modal de Edición Versionada (HU30) */}
            <Modal
                isOpen={showEditModal}
                onClose={() => setShowEditModal(false)}
                title={`Editar Planificación (Generar Versión v${(selectedPlanToEdit?.version || 1) + 1})`}
                size="lg"
            >
                <form onSubmit={handleSaveEdit} className="max-h-[75vh] overflow-y-auto pr-1 space-y-5">
                    <div className="p-3 bg-indigo-50 border border-indigo-200 text-indigo-900 rounded-xl text-xs">
                        Al guardar los cambios se creará automáticamente la <strong>Versión v{(selectedPlanToEdit?.version || 1) + 1}</strong> manteniendo intacta la versión anterior en el historial.
                    </div>

                    {/* 1. Datos Generales */}
                    <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">1. Datos Generales</h4>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
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
                                    { value: 'NUMEROS', label: 'Números y Operaciones' },
                                    { value: 'ALGEBRA', label: 'Álgebra y Funciones' },
                                    { value: 'GEOMETRIA', label: 'Geometría' },
                                    { value: 'PROBABILIDAD_ESTADISTICA', label: 'Probabilidad y Estadística' },
                                    { value: 'PROBABILIDAD', label: 'Probabilidad' },
                                ]}
                            />

                            <Input
                                label="Duración (Minutos)"
                                name="duracion"
                                type="number"
                                value={editForm.duracion}
                                onChange={(e) => setEditForm(prev => ({ ...prev, duracion: e.target.value }))}
                            />
                        </div>
                    </div>

                    {/* 2. Objetivos */}
                    <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">2. Objetivos de Aprendizaje</h4>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Objetivo Basal de Aprendizaje (OA) <span className="text-red-500">*</span>
                            </label>
                            <textarea
                                value={editForm.baseLearningObjective}
                                onChange={(e) => setEditForm(prev => ({ ...prev, baseLearningObjective: e.target.value }))}
                                rows={2}
                                className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                required
                            />
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Objetivos Específicos de la Clase
                                </label>
                                <textarea
                                    value={editForm.objectives}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, objectives: e.target.value }))}
                                    rows={3}
                                    className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Objetivo Actitudinal (OAT)
                                </label>
                                <textarea
                                    value={editForm.objetivoActitudinal}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, objetivoActitudinal: e.target.value }))}
                                    rows={3}
                                    className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                />
                            </div>
                        </div>
                    </div>

                    {/* 3. Estructura de la Clase */}
                    <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">3. Estructura de la Clase (Momentos Didácticos)</h4>
                        <div className="border-l-4 border-indigo-400 pl-3">
                            <label className="block text-xs font-bold text-indigo-900 mb-1">
                                1. Inicio (Activación de conocimientos previos y motivación)
                            </label>
                            <textarea
                                value={editForm.inicio}
                                onChange={(e) => setEditForm(prev => ({ ...prev, inicio: e.target.value }))}
                                rows={2}
                                className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                        </div>
                        <div className="border-l-4 border-blue-500 pl-3">
                            <label className="block text-xs font-bold text-blue-900 mb-1">
                                2. Desarrollo (Modelamiento, práctica guiada e independiente)
                            </label>
                            <textarea
                                value={editForm.desarrollo}
                                onChange={(e) => setEditForm(prev => ({ ...prev, desarrollo: e.target.value }))}
                                rows={3}
                                className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                        </div>
                        <div className="border-l-4 border-emerald-500 pl-3">
                            <label className="block text-xs font-bold text-emerald-900 mb-1">
                                3. Cierre (Síntesis y metacognición)
                            </label>
                            <textarea
                                value={editForm.cierre}
                                onChange={(e) => setEditForm(prev => ({ ...prev, cierre: e.target.value }))}
                                rows={2}
                                className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    {/* 4. Recursos, Evaluación y Archivo */}
                    <div className="p-4 bg-gray-50/80 border border-gray-200 rounded-xl space-y-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-blue-900">4. Anexos, Recursos y Evaluación</h4>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1">
                                    Recursos Materiales
                                </label>
                                <textarea
                                    value={editForm.recursos}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, recursos: e.target.value }))}
                                    rows={2}
                                    className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 mb-1">
                                    Instrumentos de Evaluación / PIE
                                </label>
                                <textarea
                                    value={editForm.evaluacion}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, evaluacion: e.target.value }))}
                                    rows={2}
                                    className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1.5">
                                Nuevo Archivo Adjunto (Opcional — si no adjuntas uno nuevo, se conserva el documento de la versión anterior)
                            </label>
                            <input
                                type="file"
                                accept={acceptAttr}
                                onChange={(e) => setEditForm(prev => ({ ...prev, file: e.target.files[0] }))}
                                className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 border border-gray-300 rounded-xl p-1 bg-white"
                            />
                        </div>
                    </div>

                    <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
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

            <Modal
                isOpen={feedbackModal.isOpen}
                onClose={cerrarModalFeedback}
                title={
                    feedbackModal.readOnly
                        ? 'Comentarios y Retroalimentación Docente'
                        : 'Hilo de Retroalimentación Personal'
                }
            >
                <div className="space-y-4">
                    {feedbackModal.item && (
                        <div className="bg-gray-50 p-3 rounded-lg border border-gray-200 text-sm text-gray-700">
                            <span className="font-semibold">Elemento: </span>
                            {feedbackModal.item.title || feedbackModal.item.fileName || (feedbackModal.item.schoolGrade ? `Planificación (${feedbackModal.item.schoolGrade})` : 'Documento del portafolio')}
                        </div>
                    )}

                    {/* Hilo de comentarios personales */}
                    <div className="space-y-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500">
                            Comentarios registrados ({parseComentarios(feedbackModal.item).length})
                        </h4>
                        {parseComentarios(feedbackModal.item).length === 0 ? (
                            <div className="p-4 bg-gray-50 border border-dashed border-gray-200 rounded-lg text-center text-xs text-gray-500">
                                Aún no se han registrado comentarios en este elemento.
                            </div>
                        ) : (
                            <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
                                {parseComentarios(feedbackModal.item).map((c, idx) => (
                                    <div
                                        key={c.id || idx}
                                        className="p-3.5 bg-blue-50/50 border border-blue-100 rounded-xl space-y-1.5"
                                    >
                                        <div className="flex flex-wrap items-center justify-between gap-2">
                                            <div className="flex items-center gap-2">
                                                <span className="font-bold text-xs text-blue-900">
                                                    {c.authorName || 'Docente'}
                                                </span>
                                                <Badge
                                                    variant={
                                                        c.authorRole?.toLowerCase().includes('tutor')
                                                            ? 'info'
                                                            : c.authorRole?.toLowerCase().includes('colaborador')
                                                            ? 'warning'
                                                            : 'success'
                                                    }
                                                >
                                                    {c.authorRole || 'Docente'}
                                                </Badge>
                                            </div>
                                            {c.date && (
                                                <span className="text-[11px] text-gray-500">
                                                    {new Date(c.date).toLocaleString('es-CL', {
                                                        dateStyle: 'short',
                                                        timeStyle: 'short',
                                                    })}
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-sm text-gray-800 whitespace-pre-wrap leading-relaxed">
                                            {c.text}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {!feedbackModal.readOnly && (
                        <div className="pt-2 border-t border-gray-100">
                            <label
                                htmlFor="teacher-feedback-textarea"
                                className="block text-sm font-medium text-gray-700 mb-1"
                            >
                                Agregar nuevo comentario personal
                            </label>
                            <textarea
                                id="teacher-feedback-textarea"
                                rows={4}
                                maxLength={2000}
                                value={feedbackModal.comentario}
                                onChange={(e) =>
                                    setFeedbackModal((prev) => ({ ...prev, comentario: e.target.value }))
                                }
                                placeholder="Escribe aquí tu comentario personal (se agregará al hilo con tu nombre y rol)..."
                                className="w-full rounded-lg border border-gray-300 p-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                disabled={feedbackModal.guardando}
                            />
                            <div className="flex justify-end mt-1">
                                <span
                                    className={`text-xs ${
                                        feedbackModal.comentario.length >= 1950 ? 'text-red-600 font-semibold' : 'text-gray-500'
                                    }`}
                                >
                                    {feedbackModal.comentario.length} / 2000 caracteres
                                </span>
                            </div>
                        </div>
                    )}

                    <div className="flex justify-end gap-2 pt-3 border-t border-gray-200">
                        <Button
                            variant="secondary"
                            onClick={cerrarModalFeedback}
                            disabled={feedbackModal.guardando}
                        >
                            Cerrar
                        </Button>
                        {!feedbackModal.readOnly && (
                            <Button
                                variant="primary"
                                onClick={guardarRetroalimentacion}
                                disabled={feedbackModal.guardando || !feedbackModal.comentario.trim()}
                            >
                                {feedbackModal.guardando ? 'Publicando...' : 'Publicar Comentario'}
                            </Button>
                        )}
                    </div>
                </div>
            </Modal>
        </div>
    );
}