import { useState } from 'react';
import { useNavigate } from 'react-router-dom'; // Herramienta para saltar a otra página
import loginImage from '../assets/loginImage.jpeg';

export default function Login() {
    // 1. Memoria de la pantalla: Aquí guardamos lo que el usuario tipea
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMensaje, setErrorMensaje] = useState('');

    const navigate = useNavigate();

    // 2. La función que se dispara al presionar el botón
    const handleLogin = async (e) => {
        e.preventDefault(); // Evita que la página web titile/recargue
        setErrorMensaje(''); // Limpiamos errores antiguos

        try {
            const respuesta = await fetch('http://localhost:8080/api/usuarios/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, password: password })
            });

            if (respuesta.ok) {
                // 1. Extraemos los datos del usuario que Java nos envió
                const usuario = await respuesta.json();

                // (Opcional pero recomendado) Guardamos el usuario en la memoria del navegador
                // para que las otras pantallas sepan quién inició sesión.
                localStorage.setItem('usuarioActivo', JSON.stringify(usuario));

                // 2. Leemos el nombre del rol y redirigimos
                // Nota: Asegúrate de usar 'nombre' o 'name' según como lo dejaste en tu clase Role de Java.
                const rolDelUsuario = usuario.role.name;

                if (rolDelUsuario === 'Administrador') {
                    navigate('/dashboard'); // Ahora el admin va al inicio y usa la sidebar
                }
                else if (rolDelUsuario === 'Estudiante') {
                    navigate('/dashboard');
                }
                else if (rolDelUsuario === 'Profesor') {
                    // Si luego haces un panel de profesor, lo pones aquí
                    navigate('/dashboard'); // Por ahora lo mandamos al dashboard también
                }
                else {
                    navigate('/'); // Si no tiene rol, lo dejamos en el login
                }

            } else {
                setErrorMensaje('Correo o contraseña incorrectos');
            }
        } catch (err) {
            setErrorMensaje('El servidor de Java está apagado o no responde');
        }
    };

    return (
        <div className="flex min-h-screen bg-gray-50">
            {/* Mitad Izquierda: Imagen Ilustrativa */}
            <div className="hidden md:flex md:w-1/2 bg-[#003B70] items-center justify-center relative overflow-hidden">
                {/* Capa de opacidad y color institucional */}
                <div className="absolute inset-0 bg-[#003B70] opacity-60 z-10"></div>
                <img 
                    src={loginImage} 
                    alt="Universidad del Bío-Bío" 
                    className="absolute inset-0 object-cover w-full h-full z-0"
                />
                
                {/* Texto sobre la imagen */}
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
                            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-4">
                                <p className="text-red-700 text-sm font-semibold">{errorMensaje}</p>
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

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Contraseña</label>
                            <input
                                type="password"
                                placeholder="••••••••"
                                className="w-full border border-gray-300 p-3 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-orange-500 transition-all outline-none"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                            <div className="text-right mt-2">
                                <a href="#" className="text-sm text-blue-600 hover:text-orange-500">¿Olvidaste tu contraseña?</a>
                            </div>
                        </div>

                        <button type="submit" className="w-full bg-orange-600 text-white p-3 rounded-xl hover:bg-orange-700 font-bold text-lg shadow-md hover:shadow-lg transition-all active:scale-95 mt-4">
                            Iniciar Sesión
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
}