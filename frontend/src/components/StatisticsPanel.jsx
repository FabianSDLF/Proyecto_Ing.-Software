import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../services/apiClient';

export default function StatisticsPanel() {
  const [period, setPeriod] = useState('VIGENTE');
  const [cycle, setCycle] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [retry, setRetry] = useState(0);
  const params = { ...(period === 'TODOS' ? { history: true } : period !== 'VIGENTE' ? { periodId: period } : {}), ...(cycle ? { cycle } : {}), ...(subjectId ? { subjectId } : {}) };
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    const filters = { ...(period === 'TODOS' ? { history: true } : period !== 'VIGENTE' ? { periodId: period } : {}), ...(cycle ? { cycle } : {}), ...(subjectId ? { subjectId } : {}) };
    apiClient.get('/api/reportes/estadisticas', { params: filters, signal: controller.signal })
      .then(result => { if (!controller.signal.aborted) setData(result); })
      .catch(err => { if (err.name !== 'AbortError') setError(err.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [period, cycle, subjectId, retry]);
  const download = async () => {
    setExporting(true);
    try { await apiClient.download('/api/portafolio/export-csv', 'estadisticas_practicas.csv', { params }); }
    catch (err) { setError(err.message); }
    finally { setExporting(false); }
  };
  return <section className="space-y-5 min-w-0" aria-label="Estadísticas institucionales">
    <h2 className="text-2xl font-bold text-blue-900">Estadísticas institucionales</h2>
    <div className="flex flex-wrap items-end gap-3">
      <label>Periodo<select aria-label="Periodo" className="block border p-2 rounded max-w-full" value={period} onChange={e => setPeriod(e.target.value)}>
        <option value="VIGENTE">Prácticas vigentes</option><option value="TODOS">Todo el historial</option>
        {data?.periods?.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
      </select></label>
      <label>Ciclo<select aria-label="Ciclo" className="block border p-2 rounded" value={cycle} onChange={e => setCycle(e.target.value)}>
        <option value="">Todos los ciclos</option>{['INICIAL','INTERMEDIO','AVANZADO'].map(c => <option key={c}>{c}</option>)}
      </select></label>
      <label className="min-w-0">Asignatura<select aria-label="Asignatura" className="block border p-2 rounded max-w-full" value={subjectId} onChange={e => setSubjectId(e.target.value)}>
        <option value="">Todas las asignaturas</option>{data?.subjects?.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
      </select></label>
      <button type="button" disabled={loading || exporting} onClick={download} className="min-h-11 bg-blue-700 text-white rounded px-4 disabled:opacity-50">{exporting ? 'Exportando…' : 'Descargar CSV'}</button>
    </div>
    {error && <div role="alert">{error} <button type="button" className="underline" onClick={() => setRetry(n => n+1)}>Reintentar</button></div>}
    {loading ? <p role="status">Cargando estadísticas…</p> : !error && data && <>
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
        {[["Estudiantes distintos", data.students],["Asignaciones",data.assignments],["Centros utilizados",data.centersUsed],["Evaluaciones publicadas",data.publishedEvaluations],["Pautas pendientes",data.pendingEvaluations],["Instrumentos configurados",data.configuredInstruments]].map(([label,value]) =>
          <div key={label} className="bg-white border rounded-xl p-4"><p>{label}</p><strong className="text-3xl text-blue-900">{value}</strong></div>)}
      </div>
      <p className="text-sm text-gray-600">Pendientes: solo las ocho pautas de las asignaturas configuradas como práctica profesional. Los borradores no cuentan como publicados.</p>
      {!data.assignments && <p>No hay datos para los filtros seleccionados.</p>}
      <div className="bg-white border rounded-xl p-4 space-y-3">
        <h3 className="font-bold">Promedio simple por instrumento y versión</h3>
        <p className="text-sm">Notas oficiales; se informa el tamaño de muestra. No representa una nota final ponderada.</p>
        {!data.gradesByInstrument?.length && <p>Sin notas publicadas.</p>}
        {data.gradesByInstrument?.map(g => <div key={g.instrument} className="space-y-1"><p className="break-words">{g.instrument}: <strong>{Number(g.mean).toFixed(1)}</strong> · n={g.count}</p><meter min="2" max="7" value={g.mean} className="w-full h-5" aria-label={`Promedio ${g.instrument}`} /></div>)}
      </div>
      <div className="bg-white border rounded-xl p-4 space-y-2"><h3 className="font-bold">Última versión de cada planificación</h3>
        <p className="text-sm">APROBADO_IA es revisión automática; APROBADO es aprobación docente.</p>
        {Object.entries(data.latestPlanStates || {}).map(([state,count]) => <p key={state}>{state}: <strong>{count}</strong></p>)}
        {!Object.keys(data.latestPlanStates || {}).length && <p>Sin planificaciones.</p>}
      </div>
      <details className="bg-white border rounded-xl p-4"><summary className="cursor-pointer font-bold">Serie por periodo e instrumento</summary>
        <p className="text-sm my-2">Selecciona todo el historial para comparar periodos de un mismo instrumento y versión.</p>
        <ul className="space-y-2">{data.series?.map(g => <li key={g.instrument} className="break-words">{g.instrument}: {Number(g.mean).toFixed(1)} · n={g.count}</li>)}</ul>
      </details>
      <details open className="bg-white border rounded-xl p-4"><summary className="cursor-pointer font-bold">Detalle de asignaciones</summary>
        <div className="overflow-x-auto mt-3"><table className="w-full text-sm"><thead><tr>{['Estudiante','Asignatura','Periodo','Centro','Publicadas','Detalle'].map(h => <th key={h} className="p-2 text-left">{h}</th>)}</tr></thead>
          <tbody>{data.detail?.map(a => <tr key={a.id} className="border-t"><td className="p-2">{a.student}</td><td className="p-2">{a.subject}</td><td className="p-2">{a.period}</td><td className="p-2">{a.center}</td><td className="p-2">{a.publishedEvaluations}</td><td className="p-2"><Link className="text-blue-700 underline" to={`/portafolio/estudiante/${a.id}`}>Ver portafolio</Link></td></tr>)}</tbody></table></div>
      </details>
    </>}
  </section>;
}
