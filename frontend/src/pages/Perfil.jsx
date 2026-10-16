import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import authService from '../services/authService';
import { Input, Button, Spinner } from '../components/ui';

export default function Perfil() {
    const { updateUserProfile } = useAuth();
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        phone: '',
        password: ''
    });
    const [loading, setLoading] = useState(false);
    const [initialLoading, setInitialLoading] = useState(true);

    useEffect(() => {
        const controller = new AbortController();

        async function fetchPerfil() {
            try {
                const data = await authService.getProfile({ signal: controller.signal });
                if (data) {
                    setFormData({
                        fullName: data.fullName || '',
                        email: data.email || '',
                        phone: data.phone || '',
                        password: ''
                    });
                }
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.error('Error cargando perfil:', error);
                    toast.error(error.message || 'Error al cargar tu perfil');
                }
            } finally {
                setInitialLoading(false);
            }
        }

        fetchPerfil();

        return () => controller.abort();
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

        try {
            const updatedUser = await authService.updateProfile(formData);
            toast.success('Perfil actualizado correctamente');
            
            // FE-04: Actualizar el estado global sin recargar el navegador
            updateUserProfile(updatedUser);
            setFormData(prev => ({ ...prev, password: '' }));
        } catch (error) {
            console.error('Error al actualizar perfil:', error);
            toast.error(error.message || 'Error al actualizar el perfil');
        } finally {
            setLoading(false);
        }
    };

    if (initialLoading) {
        return (
            <div className="flex items-center justify-center p-12">
                <Spinner size="lg" color="primary" />
                <span className="ml-3 text-gray-500">Cargando tu información...</span>
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto p-6">
            <h1 className="text-3xl font-bold text-[#003B70] mb-8">Mi Perfil</h1>
            
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <form onSubmit={handleSubmit} className="space-y-6">
                    <Input 
                        label="Nombre Completo"
                        type="text" 
                        name="fullName"
                        value={formData.fullName}
                        onChange={handleChange}
                        required
                        placeholder="Ej. Juan Pérez"
                    />

                    <Input 
                        label="Correo Electrónico"
                        type="email" 
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        required
                        placeholder="juan@ubb.cl"
                    />

                    <Input 
                        label="Teléfono (Opcional)"
                        type="text" 
                        name="phone"
                        value={formData.phone}
                        onChange={handleChange}
                        placeholder="+56 9 1234 5678"
                    />

                    <div className="pt-4 border-t border-gray-100">
                        <Input 
                            label="Nueva Contraseña"
                            helperText="Déjalo en blanco si no quieres cambiar tu contraseña actual."
                            type="password" 
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder="••••••••"
                        />
                    </div>

                    <div className="pt-6 flex justify-end">
                        <Button 
                            type="submit" 
                            variant="primary"
                            size="md"
                            isLoading={loading}
                            className="bg-orange-600 hover:bg-orange-700 font-medium px-6 shadow-md"
                        >
                            Guardar Cambios
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}
