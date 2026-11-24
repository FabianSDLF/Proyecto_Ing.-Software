import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../services/apiClient';

export default function Ayuda() {
  const { userRole } = useAuth();
  const [contact, setContact] = useState(null);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    apiClient.get('/api/ayuda/contacto', { signal: controller.signal }).then(setContact)
      .catch(err => { if (err.name !== 'AbortError') setError(err.message); });
    return () => controller.abort();
  }, [retry]);
  const student = userRole === 'Estudiante';
  const teacher = ['Tutor','Profesor Colaborador','Profesor de Asignatura'].includes(userRole);
  return <article className="max-w-3xl mx-auto space-y-6">
    <h1 className="text-3xl font-bold text-blue-900">Ayuda y soporte</h1>
    <p>Guía para {userRole || 'tu cuenta'}.</p>
    <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Acceso y recuperación de cuenta</h2><p>Ingresa con el correo registrado. Si olvidaste tu contraseña, cierra sesión y selecciona «¿Olvidaste tu contraseña?» en el inicio de sesión. Sigue el enlace de recuperación recibido en tu correo.</p><p>Revisa tus datos de contacto en <Link className="text-blue-700 underline" to="/perfil">Mi perfil</Link>. Si tu cuenta está inactiva, contacta a coordinación.</p></section>
    {student && <>
      <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Portafolio y documentos</h2><ol className="list-decimal pl-5 space-y-2"><li>Abre <Link to="/portafolio" className="text-blue-700 underline">Mi Portafolio</Link> y selecciona la práctica correcta. Consulta la ficha y el historial.</li><li>Selecciona la sección del documento y pulsa subir. El límite es 20 MB por archivo. Consulta los formatos indicados por cada selector antes de cargar.</li><li>Espera la confirmación de carga y comprueba que el archivo aparece y se puede descargar.</li></ol><p>Las evidencias admiten PDF, Word (.doc, .docx) y presentaciones (.ppt, .pptx). Los documentos finales admiten PDF y Word, hasta 20 MB.</p></section>
      <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Planificación y revisión IA</h2><ol className="list-decimal pl-5 space-y-2"><li>Crea una planificación desde tu portafolio y completa objetivos, actividades, recursos, evaluación e inclusión.</li><li>Guarda el borrador y solicita la revisión en RetroPlani Mate. Revisa las sugerencias; una respuesta de respaldo no equivale a aprobación.</li><li>Después de la aprobación IA de la versión vigente, envíala al docente. La aprobación del docente se registra por separado.</li></ol></section>
      <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Consultar evaluaciones</h2><p>En tu portafolio consulta las evaluaciones firmadas y publicadas: instrumento, visita, indicadores, nota y firma. Los borradores del evaluador son privados. Si aún no aparecen notas, espera la publicación o consulta al evaluador asignado.</p></section>
    </>}
    {teacher && <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Registrar y firmar una evaluación</h2><ol className="list-decimal pl-5 space-y-2"><li>Abre <Link className="text-blue-700 underline" to="/mis-estudiantes">Mis Estudiantes</Link>. Selecciona la pauta, área y visita que correspondan.</li><li>En las pautas profesionales elige un entero de 2 a 7 por indicador. «Sin observar» queda pendiente; puedes guardar y retomar el borrador privado.</li><li>Al completar todos los indicadores, revisa la nota preliminar calculada por la escala oficial al 60% de exigencia. La nota oficial se calcula en el servidor.</li><li>Selecciona firmar y publicar, revisa el resumen y confirma explícitamente. Se registra tu cuenta, fecha y contenido. El estudiante podrá verla y ya no se podrá modificar.</li><li>Si aparece un conflicto de revisión, comprueba la versión recuperada antes de continuar.</li></ol></section>}
    {['Administrador','Coordinador de practicas'].includes(userRole) && <section className="bg-white border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Administración y estadísticas</h2><p>Configura asignaturas, periodos y asignaciones en el menú. En Gestión de Asignaturas activa las ocho pautas únicamente para práctica profesional.</p><p>En <Link className="text-blue-700 underline" to="/reportes">Reportes</Link> filtra periodo, ciclo y asignatura. Estudiantes distintos y asignaciones son medidas diferentes. Exporta CSV con los mismos filtros; los promedios no son notas finales ponderadas.</p></section>}
    <section className="bg-blue-50 border rounded-xl p-4 space-y-2"><h2 className="font-bold text-xl">Contacto de coordinación</h2>
      {error ? <p role="alert">No se pudo consultar el contacto. <button className="underline" onClick={() => { setError(''); setRetry(n => n+1); }}>Reintentar</button></p> : contact === null ? <p role="status">Consultando contacto…</p> : contact.configured ? <><p>{contact.name}</p><a className="block text-blue-800 underline break-all" href={`mailto:${contact.email}`}>{contact.email}</a>{contact.phone && <p>{contact.phone}</p>}</> : <p>El contacto de coordinación aún no está configurado. Los tutoriales siguen disponibles.</p>}
    </section>
  </article>;
}
