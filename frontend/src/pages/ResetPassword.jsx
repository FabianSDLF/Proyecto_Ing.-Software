import { useState, useRef, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import { Input, Button } from '../components/ui';
import loginImage from '../assets/loginImage.jpeg';

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const timerRef = useRef(null);

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current);
            }
        };
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!token) {
            setError('No se proporcionó un token válido en la URL.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setError('Las contraseñas no coinciden.');
            return;
        }

        if (newPassword.length < 6) {
            setError('La contraseña debe tener al menos 6 caracteres.');
            return;
        }

        setLoading(true);
        setError(null);
        setMessage(null);

        try {
            const data = await authService.resetPassword({ token, newPassword });
            setMessage(data.message || 'Contraseña restablecida exitosamente.');
            timerRef.current = setTimeout(() => navigate('/'), 3000);
        } catch (err) {
            setError(err.message || 'Error al restablecer la contraseña.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-screen bg-gray-50">
            <div className="hidden md:flex md:w-1/2 bg-[#003B70] items-center justify-center relative overflow-hidden">
                <div className="absolute inset-0 bg-[#003B70] opacity-60 z-10"></div>
                <img src={loginImage} alt="Universidad del Bío-Bío" className="absolute inset-0 object-cover w-full h-full z-0" />
                <div className="relative z-20 text-white p-12 text-center">
                    <h1 className="text-5xl font-extrabold mb-6 tracking-widest text-orange-500">PITAGORAS</h1>
                    <p className="text-xl font-light text-gray-200">Plataforma de Gestión Académica y Portafolio Digital</p>
                </div>
            </div>

            <div className="flex flex-1 items-center justify-center w-full md:w-1/2">
                <div className="w-full max-w-md bg-white p-10 rounded-2xl shadow-2xl">
                    <div className="text-center mb-8">
                        <h2 className="text-3xl font-bold text-gray-900 mb-2">Crear Nueva Contraseña</h2>
                        <p className="text-gray-500">Ingresa tu nueva contraseña para acceder al sistema.</p>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-6">
                        {error && (
                            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-4 rounded-r">
                                <p className="text-red-700 text-sm font-semibold">{error}</p>
                            </div>
                        )}
                        
                        {message && (
                            <div className="bg-green-50 border-l-4 border-green-500 p-4 mb-4 rounded-r">
                                <p className="text-green-700 text-sm font-semibold">{message}</p>
                                <p className="text-xs text-gray-600 mt-2">Redirigiendo al inicio de sesión...</p>
                            </div>
                        )}

                        <Input
                            label="Nueva Contraseña"
                            type="password"
                            placeholder="••••••••"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                        />

                        <Input
                            label="Confirmar Nueva Contraseña"
                            type="password"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                        />

                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            className="w-full bg-orange-600 hover:bg-orange-700 font-bold"
                            isLoading={loading}
                        >
                            Guardar nueva contraseña
                        </Button>
                        
                        <div className="text-center mt-6">
                            <Link to="/" className="text-sm text-blue-600 hover:text-orange-500 font-medium">Volver a Iniciar Sesión</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
