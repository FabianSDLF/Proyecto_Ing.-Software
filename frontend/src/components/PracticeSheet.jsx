import { useEffect, useState } from 'react';
import apiClient from '../services/apiClient';

export function isCurrentPractice(assignment) {
  return assignment?.status === 'ACTIVA' && (!assignment.period || assignment.period.status === 'ACTIVO');
}

export default function PracticeSheet({ assignmentId }) {
  const [assignments, setAssignments] = useState([]);
  const [history, setHistory] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    const request = assignmentId
      ? apiClient.get(`/api/asignaciones/${assignmentId}/ficha`, { signal: controller.signal }).then(a => [a])
      : apiClient.get('/api/asignaciones/visibles', { signal: controller.signal, params: { history: true } });
    request.then(data => setAssignments(Array.isArray(data) ? data : []))
      .catch(err => { if (err.name !== 'AbortError') setError(err.message || 'No se pudo cargar la ficha.'); });
    return () => controller.abort();
  }, [assignmentId]);
  const visible = assignmentId || history ? assignments : assignments.filter(isCurrentPractice);
  return <section className="space-y-3" aria-label="Ficha de práctica">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-xl font-bold text-blue-900">Mi Equipo y Centro de Práctica</h2>
      {!assignmentId && <label className="text-sm">Mostrar <select className="border rounded p-2 ml-2" value={history ? 'historial' : 'vigente'} onChange={e => setHistory(e.target.value === 'historial')}>
        <option value="vigente">Prácticas vigentes</option><option value="historial">Todo el historial</option>
      </select></label>}
    </div>
    {error && <p role="alert" className="text-red-700">{error}</p>}
    {!error && visible.length === 0 && <p className="text-gray-500">Sin prácticas en esta vista.</p>}
    {visible.map(a => <article key={a.id} className="surface p-4 sm:p-6 space-y-3">
      <h3 className="font-bold">{a.subject?.name || 'Práctica'} · {a.period?.name || 'Sin periodo'} · {isCurrentPractice(a) ? 'Vigente' : 'Histórica'}</h3>
      <div className="grid gap-4 md:grid-cols-2">
        <div><h4 className="font-semibold">Establecimiento</h4>
          <p>{a.center?.name || 'Pendiente de asignación'}</p>
          <p>RBD: {a.center?.rbd || 'Sin registrar'}</p>
          <p>{[a.center?.address, a.center?.district, a.center?.city].filter(Boolean).join(', ') || 'Dirección sin registrar'}</p>
          <p>Contacto: {a.center?.contactName || 'Sin registrar'}</p>
          <p>{a.center?.contactEmail} {a.center?.phone}</p>
        </div>
        <div><h4 className="font-semibold">Equipo docente</h4>
          {[["Profesor de Asignatura", a.subject?.professor], ["Profesor Colaborador", a.collaborator], ["Tutor", a.tutor]].map(([label, user]) =>
            <div className="mb-2" key={label}><p>{label}: {user?.fullName || 'Pendiente de asignación'}</p><p className="text-sm text-gray-600">{user?.email || ''}</p></div>)}
        </div>
      </div>
    </article>)}
  </section>;
}
