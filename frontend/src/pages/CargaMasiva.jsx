import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import userService from '../services/userService';
import { Button, Modal, Input, Select } from '../components/ui';

export default function CargaMasiva() {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);

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

        try {
            await userService.uploadBatch(file);
            toast.dismiss(loadingToast);
            toast.success("¡Usuarios cargados con éxito!");
            setFile(null);
            e.target.reset();
        } catch (err) {
            toast.dismiss(loadingToast);
            toast.error(err.message || "Error al procesar el archivo masivo.");
        } finally {
            setLoading(false);
        }
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
        <div className="p-8 max-w-4xl mx-auto space-y-8">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Carga e Inscripción de Usuarios</h1>
                    <p className="text-gray-500 mt-1">Importa nóminas masivas o registra cuentas individuales para estudiantes y docentes.</p>
                </div>
                <div className="flex items-center gap-3">
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

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
                <div className="flex items-center mb-6">
                    <div className="bg-blue-100 p-3 rounded-full mr-4">
                        <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">Importar Usuarios Masivamente (CSV / Excel)</h2>
                        <p className="text-gray-500 mt-1">Sube un archivo .csv o .xlsx para registrar alumnos y profesores automáticamente.</p>
                    </div>
                </div>

                <form onSubmit={handleUpload} className="flex flex-col space-y-5 mt-4">
                    <div className="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center hover:bg-gray-50 transition-colors">
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
