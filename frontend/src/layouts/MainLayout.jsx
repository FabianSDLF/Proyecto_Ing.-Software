import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';

export default function MainLayout() {
    const [userRole, setUserRole] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const usuarioGuardado = localStorage.getItem('usuarioActivo');
        if (usuarioGuardado) {
            const usuario = JSON.parse(usuarioGuardado);
            if (usuario && usuario.role) {
                setUserRole(usuario.role.name);
            }
        } else {
            // Si no hay usuario, mandarlo al login
            navigate('/');
        }
    }, [navigate]);

    return (
        <div className="flex min-h-screen bg-gray-50">

            {/* Menú Lateral (Sidebar) */}
            <aside className="w-64 bg-blue-900 text-white p-5 flex flex-col">
                <h2 className="text-2xl font-bold mb-8">Prácticas UBB</h2>
                <nav className="flex flex-col space-y-2 flex-grow">
                    <Link to="/dashboard" className="hover:bg-blue-800 p-3 rounded">Inicio</Link>
                    <Link to="#" className="hover:bg-blue-800 p-3 rounded">Mi Portafolio</Link>
                    
                    {/* Opciones exclusivas de Administrador */}
                    {userRole === 'Administrador' && (
                        <>
                            <div className="pt-4 mt-4 border-t border-blue-800"></div>
                            <span className="text-xs font-semibold text-blue-400 uppercase tracking-wider mb-2 px-3">Administración</span>
                            <Link to="/carga-masiva" className="hover:bg-blue-800 p-3 rounded">Carga Masiva</Link>
                            <Link to="/gestion-roles" className="hover:bg-blue-800 p-3 rounded">Gestión de Roles</Link>
                        </>
                    )}
                </nav>
                <Link to="/" className="bg-red-600 hover:bg-red-700 text-center p-3 rounded mt-auto" onClick={() => localStorage.removeItem('usuarioActivo')}>
                    Cerrar Sesión
                </Link>
            </aside>

            {/* Contenedor Derecho (Donde van las páginas) */}
            <main className="flex-1 p-8">
                <Outlet />
            </main>

        </div>
    );
}