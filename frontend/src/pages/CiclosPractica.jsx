import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import cycleService from '../services/cycleService';
import { Modal, Input, Button, Badge, Spinner } from '../components/ui';

/**
 * HU5: Registro de los tres ciclos de práctica (Inicial, Intermedio, Avanzado).
 * El Administrador consulta los ciclos, sus asignaturas asociadas y edita sus datos descriptivos.
 */
const CYCLE_STYLES = {
  INICIAL: { badge: 'success', accent: 'border-t-green-500' },
  INTERMEDIO: { badge: 'warning', accent: 'border-t-yellow-500' },
  AVANZADO: { badge: 'primary', accent: 'border-t-orange-500' },
};

const toRoman = (n) => {
  const map = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII'];
  return map[n] || String(n);
};

export const formatSemesters = (start, end) => {
  if (!start && !end) return 'Semestres sin definir';
  if (start && end && start !== end) return `Semestres ${toRoman(start)} a ${toRoman(end)}`;
  return `Semestre ${toRoman(start || end)}`;
};

const EMPTY_FORM = { name: '', description: '', startSemester: '', endSemester: '' };

export default function CiclosPractica() {
  const [cycles, setCycles] = useState([]);
  const [loading, setLoading] = useState(true);

  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const fetchCycles = useCallback(async (signal) => {
    setLoading(true);
    try {
      const data = await cycleService.getCycles({ signal });
      setCycles(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('Error cargando ciclos:', err);
        toast.error(err.message || 'Error cargando los ciclos de práctica');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchCycles(controller.signal);
    return () => controller.abort();
  }, [fetchCycles]);

  const openEdit = (cycle) => {
    setEditing(cycle);
    setErrors({});
    setForm({
      name: cycle.name || '',
      description: cycle.description || '',
      startSemester: cycle.startSemester ?? '',
      endSemester: cycle.endSemester ?? '',
    });
  };

  const closeEdit = () => {
    if (saving) return;
    setEditing(null);
    setForm(EMPTY_FORM);
    setErrors({});
  };

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = 'El nombre del ciclo es obligatorio';
    else if (form.name.trim().length > 100) e.name = 'Máximo 100 caracteres';
    if (form.description.length > 1000) e.description = 'Máximo 1000 caracteres';
    const start = form.startSemester === '' ? null : Number(form.startSemester);
    const end = form.endSemester === '' ? null : Number(form.endSemester);
    if (start !== null && (start < 1 || start > 12)) e.startSemester = 'Debe estar entre 1 y 12';
    if (end !== null && (end < 1 || end > 12)) e.endSemester = 'Debe estar entre 1 y 12';
    if (start !== null && end !== null && !e.startSemester && !e.endSemester && start > end) {
      e.endSemester = 'Debe ser mayor o igual al semestre de inicio';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async (event) => {
    event.preventDefault();
    if (!editing || !validate()) return;

    setSaving(true);
    try {
      const updated = await cycleService.updateCycle(editing.code, {
        ...form,
        name: form.name.trim(),
        description: form.description.trim(),
      });
      setCycles((prev) => prev.map((c) => (c.code === updated.code ? updated : c)));
      toast.success('Ciclo actualizado exitosamente');
      setEditing(null);
    } catch (err) {
      console.error('Error actualizando ciclo:', err);
      toast.error(err.message || 'Error al actualizar el ciclo');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Ciclos de Práctica</h1>
        <p className="text-gray-500 mt-1">
          Estructura de la formación práctica de la carrera. Cada asignatura se asocia a uno de estos tres ciclos.
        </p>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <Spinner />
        </div>
      ) : cycles.length === 0 ? (
        <div className="bg-white p-8 rounded-2xl border border-gray-100 text-center text-gray-500">
          No hay ciclos registrados.
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {cycles.map((cycle) => {
            const style = CYCLE_STYLES[cycle.code] || { badge: 'neutral', accent: 'border-t-gray-400' };
            return (
              <article
                key={cycle.code}
                data-testid={`cycle-card-${cycle.code}`}
                className={`bg-white rounded-2xl shadow-sm border border-gray-100 border-t-4 ${style.accent} p-6 flex flex-col`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
                      Ciclo {cycle.displayOrder}
                    </span>
                    <h2 className="text-xl font-bold text-gray-900">{cycle.name}</h2>
                  </div>
                  <Badge variant={style.badge}>{cycle.code}</Badge>
                </div>

                <p className="text-sm font-medium text-blue-800 mt-2">
                  {formatSemesters(cycle.startSemester, cycle.endSemester)}
                </p>

                <p className="text-sm text-gray-600 mt-3 whitespace-pre-line flex-grow">
                  {cycle.description || <span className="italic text-gray-400">Sin descripción</span>}
                </p>

                <div className="mt-5 pt-4 border-t border-gray-100">
                  <h3 className="text-sm font-semibold text-gray-700 mb-2">
                    Asignaturas ({cycle.subjectCount ?? 0})
                  </h3>
                  {cycle.subjects && cycle.subjects.length > 0 ? (
                    <ul className="space-y-1">
                      {cycle.subjects.map((s) => (
                        <li key={s.id} className="text-sm text-gray-700 flex items-center gap-2">
                          <span className="w-1.5 h-1.5 rounded-full bg-gray-400 flex-shrink-0" aria-hidden="true" />
                          {s.name}
                          {s.code && <span className="text-xs text-gray-400 font-mono">({s.code})</span>}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-sm italic text-gray-400">Sin asignaturas asociadas</p>
                  )}
                </div>

                <Button variant="outline" size="sm" className="mt-5" onClick={() => openEdit(cycle)}>
                  Editar ciclo
                </Button>
              </article>
            );
          })}
        </div>
      )}

      <Modal
        isOpen={Boolean(editing)}
        onClose={closeEdit}
        title={editing ? `Editar ${editing.name}` : ''}
        description="El código y el orden del ciclo son fijos. Solo se editan sus datos descriptivos."
      >
        <form onSubmit={handleSave} className="space-y-4" noValidate>
          <Input
            label="Nombre"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            error={errors.name}
            maxLength={100}
            required
          />
          <div>
            <label htmlFor="cycle-description" className="block text-sm font-medium text-gray-700 mb-1.5">
              Descripción
            </label>
            <textarea
              id="cycle-description"
              rows={4}
              maxLength={1000}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full border border-gray-300 rounded-xl p-3 outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 text-gray-900"
            />
            {errors.description && (
              <p className="mt-1.5 text-xs font-semibold text-red-600">{errors.description}</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Semestre de inicio"
              type="number"
              min={1}
              max={12}
              value={form.startSemester}
              onChange={(e) => setForm({ ...form, startSemester: e.target.value })}
              error={errors.startSemester}
            />
            <Input
              label="Semestre de término"
              type="number"
              min={1}
              max={12}
              value={form.endSemester}
              onChange={(e) => setForm({ ...form, endSemester: e.target.value })}
              error={errors.endSemester}
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button variant="outline" onClick={closeEdit} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" isLoading={saving}>
              Guardar cambios
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
