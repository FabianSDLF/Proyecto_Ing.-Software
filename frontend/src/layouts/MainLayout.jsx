import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import logoPitagoras from '../assets/logoUBB.png';

export default function MainLayout() {
  const { user, userRole, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length > 1) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0][0].toUpperCase();
  };

  const isActive = (path) => location.pathname === path;

  const baseLinkClass =
    'flex items-center gap-3 px-4 py-3 rounded-xl transition-colors font-medium';
  const activeLinkClass = 'bg-orange-600 text-white shadow-md';
  const inactiveLinkClass = 'text-gray-300 hover:bg-[#004A8F] hover:text-white';

  return (
    <div className="flex min-h-screen bg-gray-50">
      {/* Menú Lateral (Sidebar) */}
      <aside className="w-64 bg-[#003B70] text-white p-5 flex flex-col shadow-xl z-10 relative">
        <div className="flex items-center gap-3 mb-10 mt-2 px-2">
          <img src={logoPitagoras} alt="Logo Pitagoras" className="w-10 h-10 object-contain" />
          <h2 className="text-2xl font-bold tracking-widest text-orange-500">PITAGORAS</h2>
        </div>

        <nav className="flex flex-col space-y-2 flex-grow">
          <Link
            to="/dashboard"
            className={`${baseLinkClass} ${isActive('/dashboard') ? activeLinkClass : inactiveLinkClass}`}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
              ></path>
            </svg>
            Inicio
          </Link>

          {/* Opciones exclusivas de Estudiantes */}
          {userRole === 'Estudiante' && (
            <Link
              to="/portafolio"
              className={`${baseLinkClass} ${isActive('/portafolio') ? activeLinkClass : inactiveLinkClass}`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                ></path>
              </svg>
              Mi Portafolio
            </Link>
          )}

          {/* Opciones exclusivas de Administrador */}
          {userRole === 'Coordinador de practicas' && (
            <div className="mt-8">
              <p className="text-xs font-semibold text-blue-300 uppercase tracking-wider mb-4 px-4 opacity-80">
                Administración
              </p>

              <div className="flex flex-col space-y-2">
                <Link
                  to="/carga-masiva"
                  className={`${baseLinkClass} ${isActive('/carga-masiva') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  Carga Masiva
                </Link>

                <Link
                  to="/gestion-roles"
                  className={`${baseLinkClass} ${isActive('/gestion-roles') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                    ></path>
                  </svg>
                  Gestión de Usuarios
                </Link>

                <Link
                  to="/ciclos"
                  className={`${baseLinkClass} ${isActive('/ciclos') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h10M4 18h6"></path>
                  </svg>
                  Ciclos de Práctica
                </Link>

                <Link
                  to="/asignaturas"
                  className={`${baseLinkClass} ${isActive('/asignaturas') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                    ></path>
                  </svg>
                  Asignaturas
                </Link>

                <Link
                  to="/asignaciones"
                  className={`${baseLinkClass} ${isActive('/asignaciones') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                    ></path>
                  </svg>
                  Asignaciones
                </Link>

                <Link
                    to="/reportes"
                    className={`${baseLinkClass} ${isActive('/reportes') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
                  Reportes
                </Link>
              </div>
            </div>
          )}

          {/* Opciones exclusivas de Docentes */}
          {['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(userRole) && (
            <div className="mt-8">
              <p className="text-xs font-semibold text-blue-300 uppercase tracking-wider mb-4 px-4 opacity-80">
                Docencia
              </p>

              <div className="flex flex-col space-y-2">
                <Link
                  to="/mis-estudiantes"
                  className={`${baseLinkClass} ${isActive('/mis-estudiantes') ? activeLinkClass : inactiveLinkClass}`}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  Mis Estudiantes
                </Link>
              </div>
            </div>
          )}
        </nav>

        {/* Sección del Usuario */}
        <div className="mt-auto border-t border-blue-800 pt-4">
          {user && (
            <div className="flex items-center justify-between">
              <Link
                to="/perfil"
                className="flex items-center gap-3 overflow-hidden cursor-pointer hover:opacity-80 transition-opacity"
              >
                <div className="flex-shrink-0 w-10 h-10 bg-orange-600 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-inner">
                  {getInitials(user.fullName)}
                </div>
                <div className="flex flex-col truncate">
                  <span className="text-sm font-semibold text-white truncate">
                    {user.fullName || 'Usuario'}
                  </span>
                  <span className="text-xs text-blue-300 truncate">{userRole || 'Sin Rol'}</span>
                </div>
              </Link>

              <button
                onClick={handleLogout}
                className="ml-2 p-2 text-gray-400 hover:text-red-400 hover:bg-gray-800 hover:bg-opacity-40 rounded-lg transition-colors cursor-pointer"
                title="Cerrar Sesión"
                aria-label="Cerrar sesión"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                  ></path>
                </svg>
              </button>
            </div>
          )}
        </div>
      </aside>

      {/* Contenedor Principal */}
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  );
}
