import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';

export default function Perfil() {
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        phone: '',
        password: ''
    });
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);

    // Cargar los datos del perfil al abrir la página
    useEffect(() => {
        const fetchPerfil = async () => {
            const userStr = localStorage.getItem('usuarioActivo');
            const token = localStorage.getItem('token');
            if (!userStr || !token) return;

            try {
                const response = await fetch('http://localhost:8080/api/usuarios/perfil', {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    setFormData({
                        fullName: data.fullName || '',
                        email: data.email || '',
                        phone: data.phone || '',
                        password: '' // No mostramos la clave actual
                    });
                } else {
                    toast.error('Error al cargar tu perfil');
                }
            } catch (error) {
                console.error("Error", error);
                toast.error('Error de conexión');
            } finally {
                setInitialLoading(false);
            }
        };

        fetchPerfil();
    }, []);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        const userStr = localStorage.getItem('usuarioActivo');
        const token = localStorage.getItem('token');

        try {
            const response = await fetch('http://localhost:8080/api/usuarios/perfil', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                const updatedUser = await response.json();
                toast.success('Perfil actualizado correctamente');
                
                // Actualizamos también los datos en el localStorage (por si cambió su nombre)
                const currentUserData = JSON.parse(userStr);
                currentUserData.fullName = updatedUser.fullName;
                localStorage.setItem('usuarioActivo', JSON.stringify(currentUserData));
                
                // Recargar página para que se actualice el avatar del menú
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const text = await response.text();
                toast.error('Error: ' + text);
            }
        } catch (error) {
            console.error("Error", error);
            toast.error('Error al comunicarse con el servidor');
        } finally {
            setLoading(false);
        }
    };

    if (initialLoading) return <div className="p-8 text-gray-500">Cargando tu información...</div>;

    return (
        <div className="max-w-3xl mx-auto p-6">
            <h1 className="text-3xl font-bold text-[#003B70] mb-8">Mi Perfil</h1>
            
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Nombre Completo</label>
                        <input 
                            type="text" 
                            name="fullName"
                            value={formData.fullName}
                            onChange={handleChange}
                            required
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none"
                            placeholder="Ej. Juan Pérez"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Correo Electrónico</label>
                        <input 
                            type="email" 
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none"
                            placeholder="juan@ubb.cl"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono (Opcional)</label>
                        <input 
                            type="text" 
                            name="phone"
                            value={formData.phone}
                            onChange={handleChange}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none"
                            placeholder="+56 9 1234 5678"
                        />
                    </div>

                    <div className="pt-4 border-t border-gray-100">
                        <label className="block text-sm font-medium text-gray-700 mb-1">Nueva Contraseña</label>
                        <p className="text-xs text-gray-500 mb-2">Déjalo en blanco si no quieres cambiar tu contraseña actual.</p>
                        <input 
                            type="password" 
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none"
                            placeholder="••••••••"
                        />
                    </div>

                    <div className="pt-6 flex justify-end">
                        <button 
                            type="submit" 
                            disabled={loading}
                            className="bg-orange-600 hover:bg-orange-700 text-white font-medium py-2 px-6 rounded-lg transition-colors flex items-center shadow-md disabled:opacity-50"
                        >
                            {loading ? 'Guardando...' : 'Guardar Cambios'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
