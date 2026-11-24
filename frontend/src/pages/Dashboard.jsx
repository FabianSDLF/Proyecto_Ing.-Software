import PracticeSheet from '../components/PracticeSheet';
import StatisticsPanel from '../components/StatisticsPanel';
import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import notificacionService from '../services/notificacionService';
import userService from '../services/userService';
import { Badge, Button } from '../components/ui';

export default function Dashboard() {
    const { user, userRole } = useAuth();
    const [notificacionesUsuario, setNotificacionesUsuario] = useState([]);
    const [cargandoNotificaciones, setCargandoNotificaciones] = useState(false);
    const [mensajeComunicado, setMensajeComunicado] = useState('');
    const [correoDestinatario, setCorreoDestinatario] = useState('');
    const [enviarATodos, setEnviarATodos] = useState(true);
    const [listaDestinatarios, setListaDestinatarios] = useState([]);
    const [cargandoDestinatarios, setCargandoDestinatarios] = useState(false);
    const [enviandoComunicado, setEnviandoComunicado] = useState(false);

    const puedePublicarComunicado =
        userRole === 'Coordinador de practicas' || userRole === 'Profesor de Asignatura';

    const cargarNotificacionesUsuario = useCallback(async (signal) => {
        if (!user) return;
        setCargandoNotificaciones(true);
        try {
            const datos = await notificacionService.obtenerMisNotificaciones({ signal });
            setNotificacionesUsuario(Array.isArray(datos) ? datos : []);
        } catch (error) {
            if (error?.name !== 'AbortError') {
                console.error('Error cargando notificaciones del usuario:', error);
            }
        } finally {
            setCargandoNotificaciones(false);
        }
    }, [user]);

    const cargarDestinatarios = useCallback(async (signal) => {
        if (!user || !puedePublicarComunicado) return;
        setCargandoDestinatarios(true);
        try {
            const datos = await notificacionService.obtenerDestinatariosDisponibles({ signal });
            setListaDestinatarios(Array.isArray(datos) ? datos : []);
        } catch (error) {
            if (error?.name !== 'AbortError') {
                try {
                    const usuariosRespaldo = await userService.getUsers({ signal });
                    setListaDestinatarios(Array.isArray(usuariosRespaldo) ? usuariosRespaldo : []);
                } catch (errorRespaldo) {
                    if (errorRespaldo?.name !== 'AbortError') {
                        console.error('Error cargando destinatarios:', errorRespaldo);
                    }
                }
            }
        } finally {
            setCargandoDestinatarios(false);
        }
    }, [user, puedePublicarComunicado]);

    useEffect(() => {
        if (!user) return;
        const controlador = new AbortController();
        cargarNotificacionesUsuario(controlador.signal);
        cargarDestinatarios(controlador.signal);
        return () => controlador.abort();
    }, [user, cargarNotificacionesUsuario, cargarDestinatarios]);

    const marcarNotificacionLeidaDashboard = async (notificacionId) => {
        try {
            await notificacionService.marcarComoLeida(notificacionId);
            setNotificacionesUsuario((estadoAnterior) =>
                estadoAnterior.map((elemento) =>
                    elemento.id === notificacionId ? { ...elemento, read: true } : elemento
                )
            );
        } catch (error) {
            toast.error(error?.message || 'No se pudo marcar la notificación como leída');
        }
    };

    const marcarTodasNotificacionesDashboard = async () => {
        try {
            await notificacionService.marcarTodasComoLeidas();
            setNotificacionesUsuario((estadoAnterior) =>
                estadoAnterior.map((elemento) => ({ ...elemento, read: true }))
            );
            toast.success('Todas las notificaciones marcadas como leídas');
        } catch (error) {
            toast.error(error?.message || 'No se pudieron actualizar las notificaciones');
        }
    };

    const enviarComunicadoSemestre = async (evento) => {
        evento.preventDefault();
        if (!mensajeComunicado.trim()) {
            toast.error('Debes escribir un mensaje para el comunicado.');
            return;
        }
        if (!enviarATodos && !correoDestinatario.trim()) {
            toast.error('Debes seleccionar un usuario destinatario.');
            return;
        }
        setEnviandoComunicado(true);
        try {
            await notificacionService.publicarComunicado({
                mensaje: mensajeComunicado.trim(),
                correoDestinatario: enviarATodos ? 'TODOS' : correoDestinatario.trim(),
            });
            toast.success('Comunicado enviado correctamente');
            setMensajeComunicado('');
            setCorreoDestinatario('');
            setEnviarATodos(true);
            await cargarNotificacionesUsuario();
        } catch (error) {
            toast.error(error?.message || 'No se pudo enviar el comunicado');
        } finally {
            setEnviandoComunicado(false);
        }
    };

    const obtenerEtiquetaNotificacion = (tipoNotificacion) => {
        if (tipoNotificacion === 'PLANIFICACION_APROBADA') return 'Planificación Aprobada';
        if (tipoNotificacion === 'ALERTA_SISTEMA') return 'Comunicado del Sistema';
        if (tipoNotificacion === 'REVISION_PENDIENTE') return 'Revisión Pendiente';
        if (tipoNotificacion === 'EVALUACION_PUBLICADA') return 'Evaluación Publicada';
        return 'Notificación';
    };

    const obtenerVarianteNotificacion = (tipoNotificacion) => {
        if (tipoNotificacion === 'PLANIFICACION_APROBADA') return 'success';
        if (tipoNotificacion === 'ALERTA_SISTEMA') return 'info';
        if (tipoNotificacion === 'REVISION_PENDIENTE') return 'warning';
        return 'primary';
    };

    const alertasPlanificacionAprobada = notificacionesUsuario.filter(
        (notificacionActual) => notificacionActual.type === 'PLANIFICACION_APROBADA'
    );
    const notificacionesNoLeidas = notificacionesUsuario.filter(
        (notificacionActual) => !notificacionActual.read
    );



    return (
        <div className="page-shell">
            <header className="mb-8">
                <h1 className="page-title">
                    Bienvenido, {user?.fullName || 'Usuario'}
                </h1>
                <p className="text-gray-500 mt-2 text-lg">
                    Panel Principal de PITAGORAS {userRole ? `(${userRole})` : ''}
                </p>
            </header>

            {userRole === 'Estudiante' && alertasPlanificacionAprobada.length > 0 && (
                <div className="bg-green-50 border-l-4 border-green-500 rounded-2xl p-5 shadow-sm space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <h2 className="text-lg font-bold text-green-900">
                            Tu planificación fue aprobada: ya puedes aplicarla en el colegio
                        </h2>
                        <Link
                            to="/portafolio"
                            className="px-3 py-1.5 bg-green-600 text-white text-xs font-bold rounded-lg hover:bg-green-700 transition-colors"
                        >
                            Ver en Mi Portafolio
                        </Link>
                    </div>
                    <div className="space-y-2">
                        {alertasPlanificacionAprobada.map((alertaActual) => (
                            <div
                                key={alertaActual.id}
                                className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-white/80 border border-green-200 rounded-xl px-4 py-2.5 text-sm text-green-900"
                            >
                                <span>{alertaActual.message}</span>
                                {!alertaActual.read && (
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => marcarNotificacionLeidaDashboard(alertaActual.id)}
                                    >
                                        Marcar leída
                                    </Button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Vista de Administrador */}
            {['Coordinador de practicas', 'Administrador'].includes(userRole) && (
                <div className="space-y-6">
                    <StatisticsPanel />

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <Link to="/gestion-roles" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                            <div className="w-14 h-14 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center mb-4">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-800">Gestión de Usuarios</h3>
                            <p className="text-sm text-gray-500 mt-2">Administra roles y realiza cargas masivas de estudiantes.</p>
                        </Link>

                        <Link to="/asignaciones" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                            <div className="w-14 h-14 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center mb-4">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-800">Asignaciones</h3>
                            <p className="text-sm text-gray-500 mt-2">Gestiona asignaciones de estudiantes a centros de práctica.</p>
                        </Link>

                        <Link to="/asignaturas" className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center justify-center text-center hover:shadow-md transition-shadow">
                            <div className="w-14 h-14 bg-green-100 text-green-600 rounded-full flex items-center justify-center mb-4">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path></svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-800">Gestión de Asignaturas</h3>
                            <p className="text-sm text-gray-500 mt-2">Configura asignaturas y profesores responsables.</p>
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
                        <p className="text-gray-600 mt-3">Gestiona tus planificaciones, guías y documentos finales. Sube tus entregables para revisión.</p>
                        <Link to="/portafolio" className="inline-block mt-4 px-4 py-2 bg-orange-50 text-orange-600 font-semibold rounded-lg hover:bg-orange-100 transition-colors text-sm">
                            Ir al Portafolio
                        </Link>
                    </div>

                    <div className="bg-white p-6 rounded-2xl shadow-sm border-l-4 border-blue-600 hover:shadow-md transition-shadow">
                        <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                            <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path></svg>
                            RetroPlani Mate (IA)
                        </h3>
                        <p className="text-gray-600 mt-3">Valida tus planificaciones con el asistente virtual antes de enviarlas al profesor colaborador.</p>
                        <Link to="/retroplani" className="inline-block mt-4 px-4 py-2 bg-blue-50 text-blue-600 font-semibold rounded-lg hover:bg-blue-100 transition-colors text-sm">
                            Ir a RetroPlani Mate (IA)
                        </Link>
                    </div>
                </div>
            )}

            {userRole === 'Estudiante' && <PracticeSheet />}

            {/* Vista de Docentes */}
            {['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(userRole) && (
                <div className="grid grid-cols-1 gap-6">
                    <Link to="/mis-estudiantes" className="bg-white p-6 rounded-2xl shadow-sm border-l-4 border-green-500 hover:shadow-md transition-shadow">
                        <h3 className="text-xl font-bold text-gray-800 flex items-center gap-2">
                            <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                            Mis Estudiantes Asignados
                        </h3>
                        <p className="text-gray-600 mt-3">Revisa las asignaciones y evalúa el desempeño de tus estudiantes en sus respectivos centros de práctica.</p>
                        <span className="inline-block mt-4 px-4 py-2 bg-green-50 text-green-600 font-semibold rounded-lg text-sm">
                            Ver listado de estudiantes
                        </span>
                    </Link>
                </div>
            )}
            
            {/* Fallback */}
            {!['Administrador', 'Coordinador de practicas', 'Estudiante', 'Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(userRole) && userRole !== '' && (
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <p className="text-gray-600">No hay widgets configurados para este rol.</p>
                </div>
            )}

            {puedePublicarComunicado && (
                <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 space-y-4">
                    <div className="border-b border-gray-100 pb-4">
                        <h2 className="text-xl font-bold text-gray-800">
                            Publicar Comunicado del Semestre
                        </h2>
                        <p className="text-sm text-gray-500 mt-1">
                            Envía avisos o comunicados para coordinar las actividades de práctica.
                        </p>
                    </div>

                    <form onSubmit={enviarComunicadoSemestre} className="space-y-4">
                        <div className="flex items-center gap-2">
                            <input
                                id="enviar-a-todos"
                                type="checkbox"
                                checked={enviarATodos}
                                onChange={(evento) => {
                                    const marcado = evento.target.checked;
                                    setEnviarATodos(marcado);
                                    if (marcado) {
                                        setCorreoDestinatario('');
                                    }
                                }}
                                className="h-4 w-4 text-orange-600 border-gray-300 rounded focus:ring-orange-500 cursor-pointer"
                            />
                            <label htmlFor="enviar-a-todos" className="text-sm font-medium text-gray-700 cursor-pointer">
                                Enviar a todos los usuarios activos
                            </label>
                        </div>

                        {!enviarATodos && (
                            <div>
                                <label htmlFor="selector-destinatario" className="block text-sm font-medium text-gray-700 mb-1">
                                    Seleccionar destinatario
                                </label>
                                <select
                                    id="selector-destinatario"
                                    required={!enviarATodos}
                                    value={correoDestinatario}
                                    onChange={(evento) => setCorreoDestinatario(evento.target.value)}
                                    disabled={cargandoDestinatarios}
                                    className="w-full text-sm border border-gray-300 rounded-xl p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-orange-500 focus:outline-none"
                                >
                                    <option value="">
                                        {cargandoDestinatarios ? 'Cargando usuarios...' : 'Seleccione un usuario de la base de datos'}
                                    </option>
                                    {listaDestinatarios.map((usuarioDestino) => (
                                        <option key={usuarioDestino.id || usuarioDestino.email} value={usuarioDestino.email}>
                                            {usuarioDestino.fullName} ({usuarioDestino.email}) - {usuarioDestino.role?.name || 'Usuario'}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}

                        <div>
                            <label htmlFor="mensaje-comunicado" className="block text-sm font-medium text-gray-700 mb-1">
                                Mensaje del comunicado
                            </label>
                            <textarea
                                id="mensaje-comunicado"
                                rows="3"
                                required
                                value={mensajeComunicado}
                                onChange={(evento) => setMensajeComunicado(evento.target.value)}
                                placeholder="Escribe el aviso o comunicado para coordinar las actividades del semestre..."
                                className="w-full text-sm border border-gray-300 rounded-xl p-3 bg-white text-gray-800 focus:ring-2 focus:ring-orange-500 focus:outline-none"
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button type="submit" variant="primary" size="sm" isLoading={enviandoComunicado}>
                                Enviar Comunicado
                            </Button>
                        </div>
                    </form>
                </section>
            )}

            <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 space-y-5">
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 pb-4">
                    <div className="flex items-center gap-3">
                        <h2 className="text-xl font-bold text-gray-800">
                            Notificaciones
                        </h2>
                        {notificacionesNoLeidas.length > 0 && (
                            <Badge variant="primary" size="sm">
                                {notificacionesNoLeidas.length} sin leer
                            </Badge>
                        )}
                    </div>
                    {notificacionesNoLeidas.length > 0 && (
                        <Button variant="outline" size="sm" onClick={marcarTodasNotificacionesDashboard}>
                            Marcar todas como leídas
                        </Button>
                    )}
                </div>

                {cargandoNotificaciones ? (
                    <div className="py-6 text-center text-sm text-gray-500">Cargando notificaciones...</div>
                ) : notificacionesUsuario.length === 0 ? (
                    <div className="py-6 text-center text-sm text-gray-500 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                        No tienes notificaciones registradas.
                    </div>
                ) : (
                    <div className="space-y-3">
                        {notificacionesUsuario.map((notificacionActual) => (
                            <div
                                key={notificacionActual.id}
                                className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                                    notificacionActual.read
                                        ? 'bg-gray-50 border-gray-200'
                                        : 'bg-white border-gray-200 border-l-4 border-l-orange-500'
                                }`}
                            >
                                <div className="space-y-1">
                                    <div className="flex flex-wrap items-center gap-2">
                                        <Badge
                                            variant={obtenerVarianteNotificacion(notificacionActual.type)}
                                            size="sm"
                                        >
                                            {obtenerEtiquetaNotificacion(notificacionActual.type)}
                                        </Badge>
                                        {!notificacionActual.read && (
                                            <span className="text-[11px] font-bold text-orange-600 uppercase">
                                                Nueva
                                            </span>
                                        )}
                                        {notificacionActual.createdAt && (
                                            <span className="text-xs text-gray-500">
                                                {new Date(notificacionActual.createdAt).toLocaleString('es-CL')}
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-sm text-gray-800 whitespace-pre-wrap">
                                        {notificacionActual.message}
                                    </p>
                                </div>
                                {!notificacionActual.read && (
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => marcarNotificacionLeidaDashboard(notificacionActual.id)}
                                    >
                                        Marcar leída
                                    </Button>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
}
