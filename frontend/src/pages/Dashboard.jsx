import { useEffect, useState } from 'react';

export default function Dashboard() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const usuarioGuardado = localStorage.getItem('usuarioActivo');
        if (usuarioGuardado) {
            setUser(JSON.parse(usuarioGuardado));
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
                    Panel Principal de PITAGORAS ({userRole})
                </p>
            </header>

            {/* Vista de Administrador */}
            {userRole === 'Administrador' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Gestión de Usuarios</h3>
                        <p className="text-sm text-gray-500 mt-2">Administra roles y realiza cargas masivas de estudiantes.</p>
                    </div>

                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Centros de Práctica</h3>
                        <p className="text-sm text-gray-500 mt-2">Administra colegios, dependencias e información CNA.</p>
                    </div>

                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                        <div className="w-14 h-14 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
                        </div>
                        <h3 className="text-lg font-bold text-gray-800">Pautas y Evaluación</h3>
                        <p className="text-sm text-gray-500 mt-2">Configura rúbricas y ponderaciones de asignaturas.</p>
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
                        <p className="text-gray-600 mt-3">Gestiona tus planificaciones, guías y documentos finales. Sube tus entregables para revisión.</p>
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
                            Iniciar Revisión
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}