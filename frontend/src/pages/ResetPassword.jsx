import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import loginImage from '../assets/loginImage.jpeg';

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

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
            const res = await fetch('http://localhost:8080/api/auth/reset-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token, newPassword })
            });

            const data = await res.json();
            
            if (res.ok) {
                setMessage(data.message);
                setTimeout(() => navigate('/'), 3000); // Redirigir al login después de 3s
            } else {
                setError(data.message || 'Error al restablecer la contraseña.');
            }
        } catch (err) {
            setError('Error de conexión con el servidor.');
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
                            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-4">
                                <p className="text-red-700 text-sm font-semibold">{error}</p>
                            </div>
                        )}
                        
                        {message && (
                            <div className="bg-green-50 border-l-4 border-green-500 p-4 mb-4">
                                <p className="text-green-700 text-sm font-semibold">{message}</p>
                                <p className="text-xs text-gray-600 mt-2">Redirigiendo al inicio de sesión...</p>
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Nueva Contraseña</label>
                            <input
                                type="password"
                                placeholder="••••••••"
                                className="w-full border border-gray-300 p-3 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-orange-500 transition-all outline-none"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Confirmar Nueva Contraseña</label>
                            <input
                                type="password"
                                placeholder="••••••••"
                                className="w-full border border-gray-300 p-3 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-orange-500 transition-all outline-none"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                            />
                        </div>

                        <button type="submit" disabled={loading || message !== null} className="w-full bg-orange-600 text-white p-3 rounded-xl hover:bg-orange-700 font-bold text-lg shadow-md transition-all disabled:opacity-50">
                            {loading ? 'Guardando...' : 'Guardar y Entrar'}
                        </button>
                        
                        <div className="text-center mt-6">
                            <Link to="/" className="text-sm text-blue-600 hover:text-orange-500 font-medium">Cancelar</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
