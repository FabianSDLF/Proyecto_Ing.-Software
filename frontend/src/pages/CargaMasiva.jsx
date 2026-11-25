import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import userService from '../services/userService';
import { Button, Modal, Input, Select } from '../components/ui';

export default function CargaMasiva() {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [importResult, setImportResult] = useState(null);

    // Creación manual individual de usuario (HU1)
    const [isManualModalOpen, setIsManualModalOpen] = useState(false);
    const [roles, setRoles] = useState([]);
    const [savingUser, setSavingUser] = useState(false);
    const [manualForm, setManualForm] = useState({
        fullName: '',
        email: '',
        password: '',
        role: 'Estudiante',
    });

    useEffect(() => {
        const controller = new AbortController();
        userService.getRoles({ signal: controller.signal })
            .then((data) => {
                if (Array.isArray(data) && data.length > 0) {
                    setRoles(data);
                }
            })
            .catch(() => {
                setRoles([
                    { name: 'Estudiante' },
                    { name: 'Profesor de Asignatura' },
                    { name: 'Tutor' },
                    { name: 'Profesor Colaborador' },
                    { name: 'Coordinador de practicas' },
                ]);
            });
        return () => controller.abort();
    }, []);

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) {
            toast.error("Por favor, selecciona un archivo CSV o Excel.");
            return;
        }

        const loadingToast = toast.loading("Procesando usuarios...");
        setLoading(true);
        setImportResult(null);

        try {
            const response = await userService.uploadBatch(file);
            toast.dismiss(loadingToast);
            const resultData = response && typeof response === 'object' ? response : null;
            setImportResult(resultData);
            if (resultData?.failureCount > 0) {
                toast(resultData.message || `Importación parcial con ${resultData.failureCount} filas rechazadas.`, { icon: '⚠️' });
            } else {
                toast.success(resultData?.message || "¡Usuarios cargados con éxito!");
            }
            setFile(null);
            e.target.reset();
        } catch (err) {
            toast.dismiss(loadingToast);
            toast.error(err.message || "Error al procesar el archivo masivo.");
        } finally {
            setLoading(false);
        }
    };

    const handleDownloadTemplate = () => {
        const csvTemplate = "Nombre,Correo,Contraseña,Rol\n"
            + "Ejemplo Estudiante,estudiante.ejemplo@ubiobio.cl,Temporal2026!,Estudiante\n"
            + "Ejemplo Tutor,tutor.ejemplo@ubiobio.cl,Temporal2026!,Tutor\n";
        const blob = new Blob(["\uFEFF" + csvTemplate], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'plantilla_carga_usuarios.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    const handleCreateManualUser = async (e) => {
        e.preventDefault();
        if (!manualForm.fullName.trim() || !manualForm.email.trim() || !manualForm.password || !manualForm.role) {
            toast.error("Todos los campos son obligatorios.");
            return;
        }

        setSavingUser(true);
        try {
            await userService.createUser({
                fullName: manualForm.fullName.trim(),
                email: manualForm.email.trim(),
                password: manualForm.password,
                role: { name: manualForm.role },
                active: true,
            });
            toast.success(`Usuario "${manualForm.fullName}" registrado exitosamente.`);
            setIsManualModalOpen(false);
            setManualForm({ fullName: '', email: '', password: '', role: 'Estudiante' });
        } catch (err) {
            toast.error(err.message || "Error al crear el usuario.");
        } finally {
            setSavingUser(false);
        }
    };

    const roleOptions = roles.length > 0
        ? roles.map(r => ({ value: r.name, label: r.name }))
        : [
            { value: 'Estudiante', label: 'Estudiante' },
            { value: 'Profesor de Asignatura', label: 'Profesor de Asignatura' },
            { value: 'Tutor', label: 'Tutor' },
            { value: 'Profesor Colaborador', label: 'Profesor Colaborador' },
            { value: 'Coordinador de practicas', label: 'Coordinador de practicas' },
        ];

    return (
        <div className="page-shell max-w-3xl">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="page-title">Carga e Inscripción de Usuarios</h1>
                    <p className="text-gray-500 mt-1">Importa nóminas masivas o registra cuentas individuales para estudiantes y docentes.</p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                    <Link
                        to="/gestion-roles"
                        className="px-4 py-2 text-sm font-semibold text-blue-800 bg-blue-50 hover:bg-blue-100 rounded-lg border border-blue-200 transition-colors"
                    >
                        Ver Listado de Usuarios
                    </Link>
                    <Button
                        variant="primary"
                        onClick={() => setIsManualModalOpen(true)}
                    >
                        + Nuevo Usuario Manual
                    </Button>
                </div>
            </div>

            <div className="bg-white p-4 sm:p-8 rounded-2xl shadow-lg border border-gray-100">
                <div className="flex flex-wrap sm:flex-nowrap items-center gap-3 mb-6">
                    <div className="bg-blue-100 p-3 rounded-full shrink-0">
                        <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">Importar Usuarios Masivamente (CSV / Excel)</h2>
                        <p className="text-gray-500 mt-1">Sube un archivo .csv o .xlsx para registrar alumnos y profesores automáticamente.</p>
                    </div>
                </div>

                <div className="mb-4 p-3.5 rounded-xl bg-blue-50/70 border border-blue-200 text-xs text-blue-950 space-y-2">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <p className="font-semibold">
                            Estructura obligatoria (4 columnas con fila de encabezado):
                        </p>
                        <button
                            type="button"
                            onClick={handleDownloadTemplate}
                            className="text-blue-700 hover:text-blue-900 underline font-semibold cursor-pointer"
                        >
                            Descargar plantilla CSV
                        </button>
                    </div>
                    <p className="font-mono bg-white/80 px-2.5 py-1.5 rounded border border-blue-100">
                        Nombre, Correo, Contraseña, Rol
                    </p>
                    <p className="text-blue-800">
                        Roles válidos: Estudiante, Profesor de Asignatura, Tutor, Profesor Colaborador, Coordinador de practicas.
                    </p>
                </div>

                <form onSubmit={handleUpload} className="flex flex-col space-y-5 mt-4">
                    <div className="border-2 border-dashed border-gray-300 rounded-xl p-3 sm:p-6 text-center hover:bg-gray-50 transition-colors">
                        <input
                            type="file"
                            accept=".csv, .xlsx"
                            onChange={(e) => setFile(e.target.files?.[0] || null)}
                            className="w-full text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
                        />
                    </div>
                    <Button
                        type="submit"
                        variant="primary"
                        size="lg"
                        className="w-full bg-orange-600 hover:bg-orange-700 font-bold"
                        isLoading={loading}
                    >
                        Procesar Usuarios
                    </Button>
                </form>

                {importResult && (
                    <div
                        role="status"
                        className={`mt-5 p-4 rounded-xl border text-sm space-y-2 ${
                            importResult.failureCount > 0
                                ? 'bg-amber-50 border-amber-300 text-amber-950'
                                : 'bg-emerald-50 border-emerald-300 text-emerald-950'
                        }`}
                    >
                        <p className="font-bold">{importResult.message}</p>
                        <div className="flex flex-wrap gap-4 text-xs">
                            <span>Procesados: <strong>{importResult.totalRecords ?? 0}</strong></span>
                            <span>Creados: <strong>{importResult.createdCount ?? 0}</strong></span>
                            <span>Actualizados: <strong>{importResult.updatedCount ?? 0}</strong></span>
                            <span>Rechazados: <strong>{importResult.failureCount ?? 0}</strong></span>
                        </div>
                        {Array.isArray(importResult.errors) && importResult.errors.length > 0 && (
                            <div className="pt-2 border-t border-amber-200">
                                <p className="font-semibold text-xs mb-1">Detalle de filas rechazadas:</p>
                                <ul className="list-disc list-inside space-y-1 text-xs">
                                    {importResult.errors.map((errItem, idx) => (
                                        <li key={idx}>{errItem}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Modal de Creación Manual Individual (HU1) */}
            <Modal
                isOpen={isManualModalOpen}
                onClose={() => setIsManualModalOpen(false)}
                title="Registrar Nuevo Usuario Individual"
                size="md"
            >
                <form onSubmit={handleCreateManualUser} className="space-y-4">
                    <Input
                        label="Nombre Completo"
                        placeholder="Ej: Valentina Soto"
                        value={manualForm.fullName}
                        onChange={(e) => setManualForm(prev => ({ ...prev, fullName: e.target.value }))}
                        required
                    />
                    <Input
                        label="Correo Institucional o de Contacto"
                        type="email"
                        placeholder="usuario@ubiobio.cl"
                        value={manualForm.email}
                        onChange={(e) => setManualForm(prev => ({ ...prev, email: e.target.value }))}
                        required
                    />
                    <Input
                        label="Contraseña Inicial"
                        type="password"
                        placeholder="••••••••"
                        value={manualForm.password}
                        onChange={(e) => setManualForm(prev => ({ ...prev, password: e.target.value }))}
                        required
                    />
                    <Select
                        label="Rol en el Sistema"
                        options={roleOptions}
                        value={manualForm.role}
                        onChange={(e) => setManualForm(prev => ({ ...prev, role: e.target.value }))}
                        placeholder={null}
                        required
                    />
                    <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
                        <Button
                            type="button"
                            variant="secondary"
                            onClick={() => setIsManualModalOpen(false)}
                            disabled={savingUser}
                        >
                            Cancelar
                        </Button>
                        <Button
                            type="submit"
                            variant="primary"
                            isLoading={savingUser}
                        >
                            Crear Usuario
                        </Button>
                    </div>
                </form>
            </Modal>
        </div>
    );
}
