import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';

export default function GestionRoles() {
    const [usuarios, setUsuarios] = useState([]);
    const [roles, setRoles] = useState([]);
    const [loadingUsuarios, setLoadingUsuarios] = useState(true);

    useEffect(() => {
        cargarUsuarios();
        cargarRoles();
    }, []);

    const cargarUsuarios = async () => {
        setLoadingUsuarios(true);
        try {
            const res = await fetch('http://localhost:8080/api/usuarios');
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
            const res = await fetch('http://localhost:8080/api/roles');
            if (res.ok) {
                const data = await res.json();
                setRoles(data);
            }
        } catch (error) {
            toast.error("Error al cargar roles");
        }
    };

    const cambiarRol = async (usuarioId, newRoleName) => {
        const loadingToast = toast.loading("Actualizando rol...");
        try {
            const respuesta = await fetch(`http://localhost:8080/api/usuarios/${usuarioId}/rol`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ roleName: newRoleName })
            });

            if (respuesta.ok) {
                toast.dismiss(loadingToast);
                toast.success("Rol actualizado con éxito");
                
                // Actualizar estado local
                setUsuarios(usuarios.map(u => {
                    if (u.id === usuarioId) {
                        return { ...u, role: roles.find(r => r.name === newRoleName) };
                    }
                    return u;
                }));
            } else {
                const textoError = await respuesta.text();
                toast.dismiss(loadingToast);
                toast.error("Error al actualizar rol: " + textoError);
            }
        } catch (err) {
            toast.dismiss(loadingToast);
            toast.error("Error de conexión con el servidor");
        }
    };

    return (
        <div className="p-8 max-w-6xl mx-auto space-y-8">
            <h1 className="text-4xl font-extrabold text-blue-900 tracking-tight">Gestión de Roles</h1>

            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100">
                <div className="flex items-center mb-6">
                    <div className="bg-green-100 p-3 rounded-full mr-4">
                        <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                    </div>
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">Usuarios del Sistema</h2>
                        <p className="text-gray-500 mt-1">Asigna y modifica los roles de los usuarios registrados.</p>
                    </div>
                </div>

                <div className="overflow-x-auto">
                    {loadingUsuarios ? (
                        <p className="text-center text-gray-500">Cargando usuarios...</p>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Nombre Completo</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Rol Actual</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Modificar Rol</th>
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
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            <select
                                                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
                                                value={usuario.role ? usuario.role.name : ''}
                                                onChange={(e) => cambiarRol(usuario.id, e.target.value)}
                                            >
                                                <option value="" disabled>Seleccione un rol...</option>
                                                {roles.map(rol => (
                                                    <option key={rol.id} value={rol.name}>
                                                        {rol.name}
                                                    </option>
                                                ))}
                                            </select>
                                        </td>
                                    </tr>
                                ))}
                                {usuarios.length === 0 && (
                                    <tr>
                                        <td colSpan="4" className="px-6 py-4 text-center text-sm text-gray-500">
                                            No hay usuarios registrados.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
