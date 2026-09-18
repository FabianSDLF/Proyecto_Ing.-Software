import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function Dashboard() {
    const [user, setUser] = useState(null);
    const [stats, setStats] = useState({ asignaciones: 0, centros: 0 });

    useEffect(() => {
        const usuarioGuardado = localStorage.getItem('usuarioActivo');
        if (usuarioGuardado) {
            try {
                const parsedUser = JSON.parse(usuarioGuardado);
                setUser(parsedUser);
                
                // Cargar estadísticas si es Administrador
                if (parsedUser?.role?.name === 'Administrador') {
                    const token = localStorage.getItem('token');
                    
                    fetch('http://localhost:8080/api/asignaciones', { headers: { 'Authorization': `Bearer ${token}` } })
                        .then(res => res.json())
                        .then(data => setStats(s => ({ ...s, asignaciones: data.length })))
                        .catch(err => console.error(err));
                        
                    fetch('http://localhost:8080/api/centros', { headers: { 'Authorization': `Bearer ${token}` } })
                        .then(res => res.json())
                        .then(data => setStats(s => ({ ...s, centros: data.length })))
                        .catch(err => console.error(err));
                }
            } catch (error) {
                console.error("Error parsing user from localStorage:", error);
                localStorage.removeItem('usuarioActivo');
            }
        }
    }, []);

    const userRole = user?.role?.name || '';

    return (
        <div className="space-y-6">
            <header className="mb-8">
                <h1 className="text-4xl font-bold text-gray-800 tracking-tight">
                    Bienvenido, {user?.fullName || 'Usuario'}
                </h1>
                <p className="text-gray-500 mt-2 text-lg">
                    Panel Principal de PITAGORAS {userRole ? `(${userRole})` : ''}
                </p>
            </header>

            {/* Vista de Administrador */}
            {userRole === 'Administrador' && (
                <div className="space-y-6">
                    {/* Estadísticas (HU9) */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="bg-gradient-to-r from-blue-500 to-blue-700 rounded-2xl p-6 text-white shadow-lg flex items-center justify-between">
                            <div>
                                <h3 className="text-lg font-semibold opacity-90">Alumnos Asignados</h3>
                                <p className="text-4xl font-extrabold mt-2">{stats.asignaciones}</p>
                            </div>
                            <svg className="w-16 h-16 opacity-20" fill="currentColor" viewBox="0 0 20 20"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"></path></svg>
                        </div>
                        <div className="bg-gradient-to-r from-green-500 to-green-700 rounded-2xl p-6 text-white shadow-lg flex items-center justify-between">
                            <div>
                                <h3 className="text-lg font-semibold opacity-90">Colegios Activos</h3>
                                <p className="text-4xl font-extrabold mt-2">{stats.centros}</p>
                            </div>
                            <svg className="w-16 h-16 opacity-20" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clipRule="evenodd"></path></svg>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <Link to="/gestion-roles" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Gestin de Usuarios</h3>
                        <p className="text-sm text-gray-500 mt-2">Administra roles y realiza cargas masivas de estudiantes.</p>
                    </Link>

                    <Link to="#" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Centros de Prǭctica</h3>
                        <p className="text-sm text-gray-500 mt-2">Administra colegios, dependencias e informacin CNA.</p>
                    </Link>

                    <Link to="/asignaturas" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Pautas y Evaluacin</h3>
                        <p className="text-sm text-gray-500 mt-2">Configura rǧbricas y ponderaciones de asignaturas.</p>
                    </Link>
                </div>
                </div>
            )}

            {/* Vista de Estudiante */}
            {userRole === 'Estudiante' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border-l-4 border-orange-500 hover:shadow-md transition-shadow">
                        <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                            <svg className="w-6 h-6 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"></path></svg>
                            Mi Portafolio
                        </h3>
                        <p className="text-gray-600 mt-3">Gestiona tus planificaciones, guas y documentos finales. Sube tus entregables para revisin.</p>
                        <button className="mt-4 px-4 py-2 bg-orange-50 text-orange-600 font-semibold rounded-lg hover:bg-orange-100 transition-colors text-sm">
                            Ir al Portafolio
                        </button>
                    </div>

                    <div className="bg-white p-6 rounded-2xl shadow-sm border-l-4 border-blue-600 hover:shadow-md transition-shadow">
                        <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                            <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path></svg>
                            RetroPlani Mate (IA)
                        </h3>
                        <p className="text-gray-600 mt-3">Valida tus planificaciones con el asistente virtual antes de enviarlas al profesor colaborador.</p>
                        <button className="mt-4 px-4 py-2 bg-blue-50 text-blue-600 font-semibold rounded-lg hover:bg-blue-100 transition-colors text-sm">
                            Iniciar Revisin
                        </button>
                    </div>
                </div>
            )}

            {/* Vista de Profesor */}
            {userRole === 'Profesor' && (
                <div className="grid grid-cols-1 gap-6">
                    <Link to="/mis-estudiantes" className="bg-white p-6 rounded-2xl shadow-sm border-l-4 border-green-500 hover:shadow-md transition-shadow">
                        <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                            <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                            Mis Estudiantes Asignados
                        </h3>
                        <p className="text-gray-600 mt-3">Revisa las asignaciones y evalúa el desempeño de tus estudiantes en sus respectivos centros de práctica.</p>
                        <button className="mt-4 px-4 py-2 bg-green-50 text-green-600 font-semibold rounded-lg hover:bg-green-100 transition-colors text-sm">
                            Ver listado de estudiantes
                        </button>
                    </Link>
                </div>
            )}
            
            {/* Fallback */}
            {!['Administrador', 'Estudiante', 'Profesor'].includes(userRole) && userRole !== '' && (
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <p className="text-gray-600">No hay widgets configurados para este rol.</p>
                </div>
            )}
        </div>
    );
}
