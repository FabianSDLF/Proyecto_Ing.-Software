import { useState } from 'react';
import { Link } from 'react-router-dom';
import authService from '../services/authService';
import { Input, Button } from '../components/ui';
import loginImage from '../assets/loginImage.jpeg';

export default function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage(null);
        setError(null);

        try {
            const data = await authService.forgotPassword(email);
            setMessage(data.message || 'Si el correo corresponde a una cuenta registrada, se enviarán instrucciones de recuperación.');
        } catch (err) {
            setError(err.message || 'Error al procesar la solicitud.');
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
                        <h2 className="text-3xl font-bold text-gray-900 mb-2">Recuperar Contraseña</h2>
                        <p className="text-gray-500">Ingresa tu correo institucional para recibir un enlace de recuperación.</p>
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
                            </div>
                        )}

                        <Input
                            label="Correo Institucional"
                            type="email"
                            placeholder="ejemplo@ubiobio.cl"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />

                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            className="w-full bg-orange-600 hover:bg-orange-700 font-bold"
                            isLoading={loading}
                        >
                            Enviar enlace de recuperación
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
