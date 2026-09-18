import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

export default function GestionUsuarios() {
    const [usuarios, setUsuarios] = useState([]);
    const [roles, setRoles] = useState([]);
    const [loadingUsuarios, setLoadingUsuarios] = useState(true);

    // Formulario (Crear o Editar)
    const [showForm, setShowForm] = useState(false);
    const [editingUserId, setEditingUserId] = useState(null);
    const [formData, setFormData] = useState({ fullName: '', email: '', password: '', role: '' });
    const [usuarioActual, setUsuarioActual] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const storedUser = localStorage.getItem('usuarioActivo');
        if (storedUser) {
            try {
                const parsedUser = JSON.parse(storedUser);
                if (parsedUser?.role?.name !== 'Administrador') {
                    toast.error("No tienes permiso para ver esta página.");
                    navigate('/dashboard');
                    return;
                }
                setUsuarioActual(parsedUser);
            } catch (error) {
                navigate('/');
                return;
            }
        } else {
            navigate('/');
            return;
        }
        cargarUsuarios();
        cargarRoles();
    }, [navigate]);

    const cargarUsuarios = async () => {
        setLoadingUsuarios(true);
        try {
            const res = await fetch('http://localhost:8080/api/usuarios', { headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') } });
            if (res.ok) {
                const data = await res.json();
                setUsuarios(data);
            }
        } catch (error) {
            toast.error("Error al cargar usuarios");
        }
        setLoadingUsuarios(false);
    };

    const cargarRoles = async () => {
        try {
            const res = await fetch('http://localhost:8080/api/roles', { headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') } });
            if (res.ok) {
                const data = await res.json();
                setRoles(data);
            }
        } catch (error) {
            toast.error("Error al cargar roles");
        }
    };

    const handleCrearUsuario = async () => {
        if (!formData.fullName || !formData.email || !formData.password || !formData.role) {
            toast.error("Todos los campos son obligatorios para crear.");
            return;
        }
        
        const nuevoUsuario = {
            fullName: formData.fullName,
            email: formData.email,
            passwordHash: formData.password,
            active: true,
            role: { name: formData.role }
        };

        const loadingToast = toast.loading("Creando usuario...");
        try {
            const res = await fetch('http://localhost:8080/api/usuarios', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                body: JSON.stringify(nuevoUsuario)
            });

            if (res.ok) {
                toast.success("Usuario creado correctamente", { id: loadingToast });
                setShowForm(false);
                setFormData({ fullName: '', email: '', password: '', role: '' });
                cargarUsuarios();
            } else {
                toast.error("Error al crear usuario", { id: loadingToast });
            }
        } catch (error) {
            toast.error("Error de red", { id: loadingToast });
        }
    };

    const handleEditarUsuario = async () => {
        const payload = {
            fullName: formData.fullName,
            email: formData.email,
            role: { name: formData.role }
        };

        const loadingToast = toast.loading("Actualizando usuario...");
        try {
            const res = await fetch(`http://localhost:8080/api/usuarios/${editingUserId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                toast.success("Usuario actualizado correctamente", { id: loadingToast });
                setShowForm(false);
                setEditingUserId(null);
                setFormData({ fullName: '', email: '', password: '', role: '' });
                cargarUsuarios();
            } else {
                toast.error("Error al actualizar usuario", { id: loadingToast });
            }
        } catch (error) {
            toast.error("Error de red", { id: loadingToast });
        }
    };

    const toggleEstado = async (id, estadoActual) => {
        const nuevoEstado = estadoActual === null || estadoActual === undefined ? false : !estadoActual;
        const loadingToast = toast.loading(nuevoEstado ? "Activando..." : "Desactivando...");
        try {
            const res = await fetch(`http://localhost:8080/api/usuarios/${id}/estado`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token') },
                body: JSON.stringify({ active: nuevoEstado })
            });

            if (res.ok) {
                toast.success(nuevoEstado ? "Usuario activado" : "Usuario desactivado", { id: loadingToast });
                cargarUsuarios();
            } else {
                toast.error("Error al cambiar estado", { id: loadingToast });
            }
        } catch (error) {
            toast.error("Error de red", { id: loadingToast });
        }
    };

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            <div className="flex justify-between items-center">
                <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Gestión de Usuarios</h1>
                <button 
                    onClick={() => {
                        setEditingUserId(null);
                        setFormData({ fullName: '', email: '', password: '', role: '' });
                        setShowForm(!showForm);
                    }}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded-lg shadow"
                >
                    {showForm ? "Cerrar Formulario" : "+ Nuevo Usuario"}
                </button>
            </div>

            {showForm && (
                <div className="bg-white p-6 rounded-2xl shadow border border-gray-100">
                    <h2 className="text-xl font-bold mb-4">{editingUserId ? "Editar Usuario" : "Crear Usuario"}</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <input 
                            className="border p-2 rounded w-full" 
                            placeholder="Nombre Completo" 
                            value={formData.fullName} 
                            onChange={e => setFormData({...formData, fullName: e.target.value})} 
                        />
                        <input 
                            className="border p-2 rounded w-full" 
                            placeholder="Email" 
                            type="email"
                            value={formData.email} 
                            onChange={e => setFormData({...formData, email: e.target.value})} 
                        />
                        {!editingUserId && (
                            <input 
                                className="border p-2 rounded w-full" 
                                placeholder="Contraseña" 
                                type="password"
                                value={formData.password} 
                                onChange={e => setFormData({...formData, password: e.target.value})} 
                            />
                        )}
                        <select 
                            className="border p-2 rounded w-full bg-white disabled:bg-gray-100 disabled:text-gray-500"
                            value={formData.role}
                            onChange={e => setFormData({...formData, role: e.target.value})}
                            disabled={editingUserId && usuarioActual && editingUserId === usuarioActual.id}
                            title={editingUserId && usuarioActual && editingUserId === usuarioActual.id ? "No puedes cambiar tu propio rol" : ""}
                        >
                            <option value="">Selecciona un Rol</option>
                            {roles.map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
                        </select>
                    </div>
                    <div className="mt-4 flex justify-end">
                        <button 
                            onClick={editingUserId ? handleEditarUsuario : handleCrearUsuario}
                            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow"
                        >
                            {editingUserId ? "Guardar Cambios" : "Crear Usuario"}
                        </button>
                    </div>
                </div>
            )}

            <div className="bg-white rounded-2xl shadow border border-gray-100 overflow-hidden">
                <div className="overflow-x-auto">
                    {loadingUsuarios ? (
                        <p className="text-center text-gray-500 py-8">Cargando usuarios...</p>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nombre</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rol</th>
                                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Estado</th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Acciones</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {usuarios.map(usuario => (
                                    <tr key={usuario.id}>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{usuario.fullName}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{usuario.email}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                                                {usuario.role ? usuario.role.name : 'Sin Rol'}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-center">
                                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${usuario.active !== false ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                                {usuario.active !== false ? 'Activo' : 'Inactivo'}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-2">
                                            <button 
                                                onClick={() => {
                                                    setEditingUserId(usuario.id);
                                                    setFormData({
                                                        fullName: usuario.fullName,
                                                        email: usuario.email,
                                                        password: '',
                                                        role: usuario.role ? usuario.role.name : ''
                                                    });
                                                    setShowForm(true);
                                                }}
                                                className="text-blue-600 hover:text-blue-900"
                                            >
                                                Editar
                                            </button>
                                            
                                            {(!usuarioActual || usuario.id !== usuarioActual.id) ? (
                                                <button 
                                                    onClick={() => toggleEstado(usuario.id, usuario.active)}
                                                    className={`${usuario.active !== false ? 'text-red-600 hover:text-red-900' : 'text-green-600 hover:text-green-900'}`}
                                                >
                                                    {usuario.active !== false ? 'Desactivar' : 'Activar'}
                                                </button>
                                            ) : (
                                                <span className="text-gray-400 cursor-not-allowed" title="No puedes desactivarte a ti mismo">
                                                    Desactivar
                                                </span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
