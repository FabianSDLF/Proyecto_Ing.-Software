import { useState } from 'react';
import { useNavigate } from 'react-router-dom'; // Herramienta para saltar a otra página

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
        <div className="flex min-h-screen items-center justify-center bg-gray-100">
            <div className="w-full max-w-md bg-white p-8 rounded-lg shadow-md">
                <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">
                    Sistema de Prácticas Pedagógicas
                </h2>

                {/* Le decimos al formulario que al enviarse, ejecute la función handleLogin */}
                <form onSubmit={handleLogin} className="space-y-4">

                    {/* Si hay un error, dibuja este texto rojo */}
                    {errorMensaje && <p className="text-red-500 text-sm text-center font-bold">{errorMensaje}</p>}

                    {/* Input de Correo conectado a la memoria (useState) */}
                    <input
                        type="email"
                        placeholder="Correo institucional"
                        className="w-full border p-2 rounded"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />

                    {/* Input de Contraseña conectado a la memoria (useState) */}
                    <input
                        type="password"
                        placeholder="Contraseña"
                        className="w-full border p-2 rounded"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />

                    <button type="submit" className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700">
                        Iniciar Sesión
                    </button>
                </form>

            </div>
        </div>
    );
}