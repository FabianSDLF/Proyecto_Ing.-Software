import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Input, Button } from '../components/ui';
import loginImage from '../assets/loginImage.jpeg';

export default function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMensaje, setErrorMensaje] = useState('');

    const navigate = useNavigate();
    const { login, logout, isLoading } = useAuth();

    useEffect(() => {
        logout();
    }, [logout]);

    const handleLogin = async (e) => {
        e.preventDefault();
        setErrorMensaje('');

        try {
            await login({ email, password });
            navigate('/dashboard');
        } catch (err) {
            setErrorMensaje(err.message || 'Correo o contraseña incorrectos');
        }
    };

    return (
        <div className="flex min-h-screen bg-gray-50">
            {/* Mitad Izquierda: Imagen Ilustrativa */}
            <div className="hidden md:flex md:w-1/2 bg-[#003B70] items-center justify-center relative overflow-hidden">
                <div className="absolute inset-0 bg-[#003B70] opacity-60 z-10"></div>
                <img 
                    src={loginImage} 
                    alt="Universidad del Bío-Bío" 
                    className="absolute inset-0 object-cover w-full h-full z-0"
                />
                
                <div className="relative z-20 text-white p-12 text-center">
                    <h1 className="text-5xl font-extrabold mb-6 tracking-widest text-orange-500">PITAGORAS</h1>
                    <p className="text-xl font-light text-gray-200">Plataforma de Gestión Académica y Portafolio Digital</p>
                </div>
            </div>

            {/* Mitad Derecha: Formulario de Login */}
            <div className="flex flex-1 items-center justify-center w-full md:w-1/2">
                <div className="w-full max-w-md bg-white p-10 rounded-2xl shadow-2xl">
                    <div className="text-center mb-10">
                        <h2 className="text-3xl font-bold text-gray-900 mb-2">Bienvenido</h2>
                        <p className="text-gray-500">Ingresa tus credenciales para continuar</p>
                    </div>

                    <form onSubmit={handleLogin} className="space-y-6">
                        {errorMensaje && (
                            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-4 rounded-r">
                                <p className="text-red-700 text-sm font-semibold">{errorMensaje}</p>
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

                        <div>
                            <Input
                                label="Contraseña"
                                type="password"
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                            <div className="text-right mt-2">
                                <Link to="/forgot-password" className="text-sm text-blue-600 hover:text-orange-500">
                                    ¿Olvidaste tu contraseña?
                                </Link>
                            </div>
                        </div>

                        <Button
                            type="submit"
                            variant="primary"
                            size="lg"
                            className="w-full mt-4 bg-orange-600 hover:bg-orange-700 font-bold"
                            isLoading={isLoading}
                        >
                            Iniciar Sesión
                        </Button>
                    </form>
                </div>
            </div>
        </div>
    );
}
