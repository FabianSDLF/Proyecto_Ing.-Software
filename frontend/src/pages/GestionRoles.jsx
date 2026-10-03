import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import userService from '../services/userService';
import { DataTable, Modal, Input, Select, Button, Badge } from '../components/ui';

export default function GestionRoles() {
    const { user: usuarioActual } = useAuth();
    const [usuarios, setUsuarios] = useState([]);
    const [roles, setRoles] = useState([]);
    const [loadingUsuarios, setLoadingUsuarios] = useState(true);

    // Formulario Modal (Crear o Editar)
    const [showForm, setShowForm] = useState(false);
    const [editingUserId, setEditingUserId] = useState(null);
    const [formData, setFormData] = useState({ fullName: '', email: '', password: '', role: '' });
    const [isSaving, setIsSaving] = useState(false);
    const [filtroRol, setFiltroRol] = useState('TODOS');

    const usuariosFiltrados = filtroRol === 'TODOS'
        ? usuarios
        : usuarios.filter(u => u.role && u.role.name === filtroRol);

    const cargarDatos = useCallback(async (signal) => {
        setLoadingUsuarios(true);
        try {
            const [usersData, rolesData] = await Promise.all([
                userService.getUsers({ signal }),
                userService.getRoles({ signal })
            ]);
            setUsuarios(Array.isArray(usersData) ? usersData : []);
            setRoles(Array.isArray(rolesData) ? rolesData : []);
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('Error al cargar usuarios o roles:', error);
                toast.error(error.message || 'Error al cargar los datos.');
            }
        } finally {
            setLoadingUsuarios(false);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        cargarDatos(controller.signal);
        return () => controller.abort();
    }, [cargarDatos]);

    const handleGuardarUsuario = async (e) => {
        e.preventDefault();

        if (!editingUserId && (!formData.fullName || !formData.email || !formData.password || !formData.role)) {
            toast.error("Todos los campos son obligatorios para crear.");
            return;
        }

        if (editingUserId && (!formData.fullName || !formData.email || !formData.role)) {
            toast.error("Nombre, correo y rol son obligatorios.");
            return;
        }

        setIsSaving(true);
        const loadingToast = toast.loading(editingUserId ? "Actualizando usuario..." : "Creando usuario...");

        try {
            if (editingUserId) {
                await userService.updateUser(editingUserId, {
                    fullName: formData.fullName,
                    email: formData.email,
                    role: { name: formData.role }
                });
                toast.success("Usuario actualizado correctamente", { id: loadingToast });
            } else {
                await userService.createUser({
                    fullName: formData.fullName,
                    email: formData.email,
                    password: formData.password,
                    role: { name: formData.role },
                    active: true
                });
                toast.success("Usuario creado correctamente", { id: loadingToast });
            }

            setShowForm(false);
            setEditingUserId(null);
            setFormData({ fullName: '', email: '', password: '', role: '' });
            cargarDatos();
        } catch (error) {
            console.error('Error al guardar usuario:', error);
            toast.error(error.message || "Error al procesar la solicitud", { id: loadingToast });
        } finally {
            setIsSaving(false);
        }
    };

    const toggleEstado = useCallback(async (id, estadoActual) => {
        const nuevoEstado = estadoActual === null || estadoActual === undefined ? false : !estadoActual;
        const loadingToast = toast.loading(nuevoEstado ? "Activando..." : "Desactivando...");
        try {
            await userService.updateStatus(id, nuevoEstado);
            toast.success(nuevoEstado ? "Usuario activado" : "Usuario desactivado", { id: loadingToast });
            cargarDatos();
        } catch (error) {
            console.error('Error al cambiar estado:', error);
            toast.error(error.message || "Error al cambiar estado", { id: loadingToast });
        }
    }, [cargarDatos]);

    const columns = useMemo(() => [
        {
            header: 'Nombre',
            accessor: 'fullName',
            className: 'font-medium text-gray-900',
        },
        {
            header: 'Email',
            accessor: 'email',
            className: 'text-gray-500',
        },
        {
            header: 'Rol',
            render: (row) => (
                <Badge variant="primary">
                    {row.role?.name || 'Sin Rol'}
                </Badge>
            ),
        },
        {
            header: 'Estado',
            align: 'center',
            render: (row) => (
                <Badge variant={row.active !== false ? 'success' : 'danger'}>
                    {row.active !== false ? 'Activo' : 'Inactivo'}
                </Badge>
            ),
        },
        {
            header: 'Acciones',
            align: 'right',
            render: (row) => (
                <div className="flex items-center justify-end space-x-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                            setEditingUserId(row.id);
                            setFormData({
                                fullName: row.fullName,
                                email: row.email,
                                password: '',
                                role: row.role ? row.role.name : ''
                            });
                            setShowForm(true);
                        }}
                    >
                        Editar
                    </Button>
                    
                    {(!usuarioActual || row.id !== usuarioActual.id) ? (
                        <Button
                            variant={row.active !== false ? 'danger' : 'success'}
                            size="sm"
                            onClick={() => toggleEstado(row.id, row.active)}
                        >
                            {row.active !== false ? 'Desactivar' : 'Activar'}
                        </Button>
                    ) : (
                        <span className="text-xs text-gray-400 cursor-not-allowed px-2" title="No puedes desactivarte a ti mismo">
                            Actual
                        </span>
                    )}
                </div>
            ),
        },
    ], [usuarioActual, toggleEstado]);

    const roleOptions = useMemo(() => (
        roles.map(r => ({ value: r.name, label: r.name }))
    ), [roles]);

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Gestión de Usuarios</h1>
                    <p className="text-gray-500 mt-1">Administra cuentas, roles y estados de los usuarios del sistema.</p>
                </div>
                <Button 
                    variant="primary"
                    onClick={() => {
                        setEditingUserId(null);
                        setFormData({ fullName: '', email: '', password: '', role: '' });
                        setShowForm(true);
                    }}
                >
                    + Nuevo Usuario
                </Button>
            </div>

            {/* Modal de Creación / Edición */}
            <Modal
                isOpen={showForm}
                onClose={() => {
                    setShowForm(false);
                    setEditingUserId(null);
                    setFormData({ fullName: '', email: '', password: '', role: '' });
                }}
                title={editingUserId ? "Editar Usuario" : "Crear Nuevo Usuario"}
                size="md"
                footer={
                    <div className="flex justify-end space-x-3">
                        <Button
                            variant="secondary"
                            onClick={() => setShowForm(false)}
                            disabled={isSaving}
                        >
                            Cancelar
                        </Button>
                        <Button
                            variant="primary"
                            onClick={handleGuardarUsuario}
                            isLoading={isSaving}
                        >
                            {editingUserId ? "Guardar Cambios" : "Crear Usuario"}
                        </Button>
                    </div>
                }
            >
                <form onSubmit={handleGuardarUsuario} className="space-y-4">
                    <Input 
                        label="Nombre Completo"
                        placeholder="Ej. Juan Pérez" 
                        value={formData.fullName} 
                        onChange={e => setFormData({...formData, fullName: e.target.value})} 
                        required
                    />
                    <Input 
                        label="Email Institucional"
                        placeholder="usuario@ubiobio.cl" 
                        type="email"
                        value={formData.email} 
                        onChange={e => setFormData({...formData, email: e.target.value})} 
                        required
                    />
                    {!editingUserId && (
                        <Input 
                            label="Contraseña"
                            placeholder="••••••••" 
                            type="password"
                            value={formData.password} 
                            onChange={e => setFormData({...formData, password: e.target.value})} 
                            required
                        />
                    )}
                    <Select 
                        label="Rol Asignado"
                        options={roleOptions}
                        value={formData.role}
                        onChange={e => setFormData({...formData, role: e.target.value})}
                        disabled={editingUserId && usuarioActual && editingUserId === usuarioActual.id}
                        helperText={editingUserId && usuarioActual && editingUserId === usuarioActual.id ? "No puedes cambiar tu propio rol" : undefined}
                        placeholder="Selecciona un Rol"
                        required
                    />
                </form>
            </Modal>

            {/* Tabla de Usuarios */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
                <div className="flex items-center space-x-3 mb-4">
                    <label className="font-semibold text-gray-700">Filtrar por Rol:</label>
                    <select
                        className="border border-gray-300 bg-white text-gray-800 rounded-lg py-1.5 px-3 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer w-auto shadow-sm"
                        value={filtroRol}
                        onChange={(e) => setFiltroRol(e.target.value)}
                    >
                        <option value="TODOS">Todos los roles</option>
                        {roleOptions.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
                    </select>
                </div>
                <DataTable
                    columns={columns}
                    data={usuariosFiltrados}
                    isLoading={loadingUsuarios}
                    emptyMessage="No se encontraron usuarios registrados."
                    keyField="id"
                />
            </div>

        </div>
    );
}