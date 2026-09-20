import { useState } from 'react';
import { Link } from 'react-router-dom';
import loginImage from '../assets/loginImage.jpeg';

export default function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [simulatedLink, setSimulatedLink] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage(null);
        setError(null);
        setSimulatedLink(null);

        try {
            const res = await fetch('http://localhost:8080/api/auth/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email })
            });

            const data = await res.json();
            
            if (res.ok) {
                setMessage(data.message);
                setSimulatedLink(data.link);
            } else {
                setError(data.message || 'Error al procesar la solicitud.');
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
                        <h2 className="text-3xl font-bold text-gray-900 mb-2">Recuperar Contraseña</h2>
                        <p className="text-gray-500">Ingresa tu correo institucional para recibir un enlace de recuperación.</p>
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
                                {simulatedLink && (
                                    <div className="mt-4 p-3 bg-white border border-green-200 rounded text-sm break-all">
                                        <p className="font-bold text-gray-700 mb-1">Para fines de prueba, haz clic aquí:</p>
                                        <a href={simulatedLink} className="text-blue-600 hover:underline">{simulatedLink}</a>
                                    </div>
                                )}
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Correo Institucional</label>
                            <input
                                type="email"
                                placeholder="ejemplo@ubiobio.cl"
                                className="w-full border border-gray-300 p-3 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-orange-500 transition-all outline-none"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>

                        <button type="submit" disabled={loading} className="w-full bg-orange-600 text-white p-3 rounded-xl hover:bg-orange-700 font-bold text-lg shadow-md transition-all disabled:opacity-50">
                            {loading ? 'Enviando...' : 'Enviar enlace de recuperación'}
                        </button>
                        
                        <div className="text-center mt-6">
                            <Link to="/" className="text-sm text-blue-600 hover:text-orange-500 font-medium">Volver a Iniciar Sesión</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
