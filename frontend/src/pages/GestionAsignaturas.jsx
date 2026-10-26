import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import subjectService from '../services/subjectService';
import userService from '../services/userService';
import { DataTable, ConfirmDialog, Modal, Input, Select, Button, Badge } from '../components/ui';

export default function GestionAsignaturas() {
  const [activeTab, setActiveTab] = useState('ASIGNATURAS'); // 'ASIGNATURAS' | 'PERIODOS'

  // Asignaturas
  const [asignaturas, setAsignaturas] = useState([]);
  const [profesoresAsignatura, setProfesoresAsignatura] = useState([]);
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [cycle, setCycle] = useState('INICIAL');
  const [professorId, setProfessorId] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);

  // Edición de Asignatura
  const [editingSubject, setEditingSubject] = useState(null);
  const [editSubjectForm, setEditSubjectForm] = useState({
    name: '',
    code: '',
    cycle: 'INICIAL',
    professorId: '',
  });
  const [savingSubjectEdit, setSavingSubjectEdit] = useState(false);

  // Eliminación de Asignatura
  const [subjectToDelete, setSubjectToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Periodos Académicos (Ciclos Semestrales)
  const [periodos, setPeriodos] = useState([]);
  const [loadingPeriodos, setLoadingPeriodos] = useState(false);
  const [savingPeriodo, setSavingPeriodo] = useState(false);
  const [periodForm, setPeriodForm] = useState({
    name: '2026-2',
    year: new Date().getFullYear(),
    semesterNumber: 2,
    startDate: '',
    endDate: '',
    status: 'ACTIVO',
  });
  const [editingPeriod, setEditingPeriod] = useState(null);
  const [periodToDelete, setPeriodToDelete] = useState(null);
  const [deletingPeriod, setDeletingPeriod] = useState(false);

  const fetchAsignaturas = useCallback(async (signal) => {
    setLoadingData(true);
    try {
      const [subjRes, profRes] = await Promise.allSettled([
        subjectService.getSubjects({ signal }),
        userService.getUsersByRole?.('Profesor de Asignatura', { signal }),
      ]);
      if (subjRes.status === 'fulfilled') {
        setAsignaturas(Array.isArray(subjRes.value) ? subjRes.value : []);
      }
      if (profRes.status === 'fulfilled') {
        setProfesoresAsignatura(Array.isArray(profRes.value) ? profRes.value : []);
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('Error cargando asignaturas:', err);
        toast.error(err.message || 'Error cargando asignaturas');
      }
    } finally {
      setLoadingData(false);
    }
  }, []);

  const fetchPeriodos = useCallback(async (signal) => {
    if (!subjectService.getPeriods) return;
    setLoadingPeriodos(true);
    try {
      const data = await subjectService.getPeriods({ signal });
      setPeriodos(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('Error cargando periodos académicos:', err);
        toast.error(err.message || 'Error cargando periodos académicos');
      }
    } finally {
      setLoadingPeriodos(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchAsignaturas(controller.signal);
    fetchPeriodos(controller.signal);
    return () => controller.abort();
  }, [fetchAsignaturas, fetchPeriodos]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      toast.error('El nombre de la asignatura es obligatorio');
      return;
    }

    setLoading(true);
    try {
      await subjectService.createSubject({
        name: name.trim(),
        code: code.trim() || null,
        cycle,
        professorId: professorId || null,
      });
      toast.success('Asignatura creada exitosamente');
      setName('');
      setCode('');
      setProfessorId('');
      fetchAsignaturas();
    } catch (err) {
      console.error('Error creando asignatura:', err);
      toast.error(err.message || 'Error al crear la asignatura');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEditSubject = (subj) => {
    setEditingSubject(subj);
    setEditSubjectForm({
      name: subj.name || '',
      code: subj.code || '',
      cycle: subj.cycle || 'INICIAL',
      professorId: subj.professor?.id || '',
    });
  };

  const handleSaveEditSubject = async (e) => {
    e.preventDefault();
    if (!editingSubject) return;
    setSavingSubjectEdit(true);
    try {
      await subjectService.updateSubject(editingSubject.id, editSubjectForm);
      toast.success('Asignatura actualizada correctamente');
      setEditingSubject(null);
      fetchAsignaturas();
    } catch (err) {
      toast.error(err.message || 'Error al actualizar asignatura');
    } finally {
      setSavingSubjectEdit(false);
    }
  };

  const confirmDelete = async () => {
    if (!subjectToDelete) return;

    setIsDeleting(true);
    try {
      await subjectService.deleteSubject(subjectToDelete.id);
      toast.success('Asignatura eliminada exitosamente');
      setSubjectToDelete(null);
      fetchAsignaturas();
    } catch (err) {
      console.error('Error eliminando asignatura:', err);
      toast.error(err.message || 'Error al eliminar la asignatura');
    } finally {
      setIsDeleting(false);
    }
  };

  // Handlers de Periodos Académicos
  const handleSavePeriod = async (e) => {
    e.preventDefault();
    if (!periodForm.name.trim()) {
      toast.error('El nombre del periodo es obligatorio (ej. 2026-2)');
      return;
    }
    setSavingPeriodo(true);
    try {
      const payload = {
        name: periodForm.name.trim(),
        year: Number(periodForm.year) || new Date().getFullYear(),
        semesterNumber: Number(periodForm.semesterNumber) || 1,
        startDate: periodForm.startDate || null,
        endDate: periodForm.endDate || null,
        status: periodForm.status || 'ACTIVO',
      };
      if (editingPeriod) {
        await subjectService.updatePeriod(editingPeriod.id, payload);
        toast.success('Periodo académico actualizado exitosamente');
        setEditingPeriod(null);
      } else {
        await subjectService.createPeriod(payload);
        toast.success('Periodo académico creado exitosamente');
      }
      setPeriodForm({
        name: '',
        year: new Date().getFullYear(),
        semesterNumber: 1,
        startDate: '',
        endDate: '',
        status: 'ACTIVO',
      });
      fetchPeriodos();
    } catch (err) {
      toast.error(err.message || 'Error al guardar periodo académico');
    } finally {
      setSavingPeriodo(false);
    }
  };

  const handleEditPeriodClick = (p) => {
    setEditingPeriod(p);
    setPeriodForm({
      name: p.name || '',
      year: p.year || new Date().getFullYear(),
      semesterNumber: p.semesterNumber || 1,
      startDate: p.startDate || '',
      endDate: p.endDate || '',
      status: p.status || 'ACTIVO',
    });
  };

  const handleTogglePeriodStatus = async (p, newStatus) => {
    try {
      await subjectService.changePeriodStatus(p.id, newStatus);
      toast.success(`Periodo ${p.name} cambiado a ${newStatus}`);
      fetchPeriodos();
    } catch (err) {
      toast.error(err.message || 'Error al cambiar estado del periodo');
    }
  };

  const confirmDeletePeriod = async () => {
    if (!periodToDelete) return;
    setDeletingPeriod(true);
    try {
      await subjectService.deletePeriod(periodToDelete.id);
      toast.success('Periodo académico eliminado exitosamente');
      setPeriodToDelete(null);
      fetchPeriodos();
    } catch (err) {
      toast.error(err.message || 'No se pudo eliminar el periodo');
    } finally {
      setDeletingPeriod(false);
    }
  };

  const cycleOptions = [
    { value: 'INICIAL', label: 'Inicial' },
    { value: 'BASICO', label: 'Básico' },
    { value: 'INTERMEDIO', label: 'Intermedio' },
    { value: 'AVANZADO', label: 'Avanzado' },
  ];

  const professorOptions = useMemo(() => (
    profesoresAsignatura.map(p => ({ value: p.id, label: `${p.fullName} (${p.email})` }))
  ), [profesoresAsignatura]);

  const getCycleBadge = (cycleVal) => {
    const norm = (cycleVal || '').toUpperCase();
    if (norm === 'INICIAL' || norm === 'BASICO') return <Badge variant="success">{cycleVal}</Badge>;
    if (norm === 'INTERMEDIO') return <Badge variant="warning">Intermedio</Badge>;
    if (norm === 'AVANZADO') return <Badge variant="primary">Avanzado</Badge>;
    return <Badge variant="gray">{cycleVal || 'Sin ciclo'}</Badge>;
  };

  const columns = useMemo(() => [
    {
      header: 'Código',
      render: (row) => row.code ? <span className="font-mono text-xs font-semibold text-blue-900 bg-blue-50 px-2 py-1 rounded">{row.code}</span> : <span className="text-gray-400 italic text-xs">S/C</span>,
    },
    {
      header: 'Asignatura',
      accessor: 'name',
      className: 'font-medium text-gray-900',
    },
    {
      header: 'Ciclo Formativo',
      render: (row) => getCycleBadge(row.cycle),
    },
    {
      header: 'Profesor Responsable',
      render: (row) => row.professor ? (
        <div>
          <p className="text-sm font-medium text-gray-800">{row.professor.fullName}</p>
          <p className="text-xs text-gray-500">{row.professor.email}</p>
        </div>
      ) : (
        <span className="text-xs text-gray-400 italic">Sin asignar</span>
      ),
    },
    {
      header: 'Acciones',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => handleOpenEditSubject(row)}
          >
            Editar
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => setSubjectToDelete(row)}
          >
            Eliminar
          </Button>
        </div>
      ),
    },
  ], []);

  const periodColumns = useMemo(() => [
    {
      header: 'Periodo',
      render: (row) => <span className="font-bold text-blue-900">{row.name}</span>,
    },
    {
      header: 'Año / Semestre',
      render: (row) => `${row.year || '-'} — Semestre ${row.semesterNumber || '-'}`,
    },
    {
      header: 'Vigencia',
      render: (row) => (
        <span className="text-xs text-gray-600">
          {row.startDate || 'Sin inicio'} → {row.endDate || 'Sin término'}
        </span>
      ),
    },
    {
      header: 'Estado',
      align: 'center',
      render: (row) => (
        <Badge variant={row.status === 'ACTIVO' ? 'success' : row.status === 'CERRADO' ? 'danger' : 'gray'}>
          {row.status || 'N/A'}
        </Badge>
      ),
    },
    {
      header: 'Acciones',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-2">
          {row.status !== 'ACTIVO' ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleTogglePeriodStatus(row, 'ACTIVO')}
            >
              Activar
            </Button>
          ) : (
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleTogglePeriodStatus(row, 'CERRADO')}
            >
              Cerrar Semestre
            </Button>
          )}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => handleEditPeriodClick(row)}
          >
            Editar
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => setPeriodToDelete(row)}
          >
            Eliminar
          </Button>
        </div>
      ),
    },
  ], []);

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Gestión de Asignaturas y Ciclos Académicos</h1>
        <p className="text-gray-500 mt-1">Configura las asignaturas por ciclo formativo, sus profesores responsables y los periodos semestrales activos.</p>
      </div>

      {/* Pestañas */}
      <div className="flex border-b border-gray-200 gap-4">
        <button
          type="button"
          onClick={() => setActiveTab('ASIGNATURAS')}
          className={`py-2 px-4 font-semibold border-b-2 transition-colors cursor-pointer ${
            activeTab === 'ASIGNATURAS'
              ? 'border-orange-600 text-orange-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          Asignaturas y Ciclos Formativos
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('PERIODOS')}
          className={`py-2 px-4 font-semibold border-b-2 transition-colors cursor-pointer ${
            activeTab === 'PERIODOS'
              ? 'border-orange-600 text-orange-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          Periodos / Semestres Académicos
        </button>
      </div>

      {activeTab === 'ASIGNATURAS' && (
        <>
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <h2 className="text-lg font-bold text-gray-800 mb-4">Nueva Asignatura</h2>
            <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-5 gap-4 items-end">
              <div>
                <Input
                  label="Código"
                  placeholder="Ej: MAT-301"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
              </div>
              <div className="md:col-span-2">
                <Input
                  label="Nombre"
                  placeholder="Ej: Práctica Pedagógica I"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
              <div>
                <Select
                  label="Ciclo"
                  options={cycleOptions}
                  value={cycle}
                  onChange={(e) => setCycle(e.target.value)}
                  required
                />
              </div>
              <div>
                <Select
                  label="Profesor Responsable"
                  options={professorOptions}
                  value={professorId}
                  onChange={(e) => setProfessorId(e.target.value)}
                  placeholder="-- Opcional --"
                />
              </div>
              <div className="md:col-span-5 flex justify-end">
                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  className="bg-orange-600 hover:bg-orange-700 font-bold"
                  isLoading={loading}
                >
                  Crear Asignatura
                </Button>
              </div>
            </form>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
            <DataTable
              columns={columns}
              data={asignaturas}
              isLoading={loadingData}
              emptyMessage="No hay asignaturas registradas."
              keyField="id"
            />
          </div>
        </>
      )}

      {activeTab === 'PERIODOS' && (
        <>
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-800">
                {editingPeriod ? `Editando Periodo: ${editingPeriod.name}` : 'Nuevo Periodo Académico (Semestre)'}
              </h2>
              {editingPeriod && (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setEditingPeriod(null);
                    setPeriodForm({
                      name: '2026-2',
                      year: new Date().getFullYear(),
                      semesterNumber: 2,
                      startDate: '',
                      endDate: '',
                      status: 'ACTIVO',
                    });
                  }}
                >
                  Cancelar Edición
                </Button>
              )}
            </div>
            <form onSubmit={handleSavePeriod} className="grid grid-cols-1 md:grid-cols-6 gap-4 items-end">
              <div>
                <Input
                  label="Nombre (Ej: 2026-2)"
                  value={periodForm.name}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="2026-2"
                  required
                />
              </div>
              <div>
                <Input
                  label="Año"
                  type="number"
                  value={periodForm.year}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, year: e.target.value }))}
                  required
                />
              </div>
              <div>
                <Select
                  label="Semestre"
                  value={periodForm.semesterNumber}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, semesterNumber: e.target.value }))}
                  options={[
                    { value: 1, label: '1° Semestre' },
                    { value: 2, label: '2° Semestre' },
                  ]}
                  placeholder={null}
                />
              </div>
              <div>
                <Input
                  label="Fecha Inicio"
                  type="date"
                  value={periodForm.startDate}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, startDate: e.target.value }))}
                />
              </div>
              <div>
                <Input
                  label="Fecha Término"
                  type="date"
                  value={periodForm.endDate}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, endDate: e.target.value }))}
                />
              </div>
              <div>
                <Select
                  label="Estado"
                  value={periodForm.status}
                  onChange={(e) => setPeriodForm(prev => ({ ...prev, status: e.target.value }))}
                  options={[
                    { value: 'ACTIVO', label: 'Activo (Vigente)' },
                    { value: 'CERRADO', label: 'Cerrado' },
                    { value: 'ARCHIVADO', label: 'Archivado' },
                  ]}
                  placeholder={null}
                />
              </div>
              <div className="md:col-span-6 flex justify-end">
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={savingPeriodo}
                  className="bg-orange-600 hover:bg-orange-700 font-bold"
                >
                  {editingPeriod ? 'Guardar Cambios del Periodo' : 'Crear Periodo Académico'}
                </Button>
              </div>
            </form>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4">
            <DataTable
              columns={periodColumns}
              data={periodos}
              isLoading={loadingPeriodos}
              emptyMessage="No hay periodos académicos registrados."
              keyField="id"
            />
          </div>
        </>
      )}

      {/* Modal Editar Asignatura */}
      <Modal
        isOpen={Boolean(editingSubject)}
        onClose={() => setEditingSubject(null)}
        title="Editar Asignatura"
        size="md"
      >
        <form onSubmit={handleSaveEditSubject} className="space-y-4">
          <Input
            label="Código"
            value={editSubjectForm.code}
            onChange={(e) => setEditSubjectForm(prev => ({ ...prev, code: e.target.value }))}
            placeholder="Ej: MAT-301"
          />
          <Input
            label="Nombre de la Asignatura"
            value={editSubjectForm.name}
            onChange={(e) => setEditSubjectForm(prev => ({ ...prev, name: e.target.value }))}
            required
          />
          <Select
            label="Ciclo Formativo"
            options={cycleOptions}
            value={editSubjectForm.cycle}
            onChange={(e) => setEditSubjectForm(prev => ({ ...prev, cycle: e.target.value }))}
            placeholder={null}
          />
          <Select
            label="Profesor de Asignatura Responsable"
            options={professorOptions}
            value={editSubjectForm.professorId}
            onChange={(e) => setEditSubjectForm(prev => ({ ...prev, professorId: e.target.value }))}
            placeholder="-- Sin asignar --"
          />
          <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
            <Button type="button" variant="secondary" onClick={() => setEditingSubject(null)}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" isLoading={savingSubjectEdit}>
              Guardar Cambios
            </Button>
          </div>
        </form>
      </Modal>

      {/* Diálogo de Confirmación para eliminar Asignatura */}
      <ConfirmDialog
        isOpen={Boolean(subjectToDelete)}
        onClose={() => setSubjectToDelete(null)}
        onConfirm={confirmDelete}
        title="Eliminar Asignatura"
        message={`¿Está seguro de que desea eliminar la asignatura "${subjectToDelete?.name}"? Esta acción no se puede deshacer.`}
        confirmText="Eliminar Asignatura"
        cancelText="Cancelar"
        variant="danger"
        isLoading={isDeleting}
      />

      {/* Diálogo de Confirmación para eliminar Periodo */}
      <ConfirmDialog
        isOpen={Boolean(periodToDelete)}
        onClose={() => setPeriodToDelete(null)}
        onConfirm={confirmDeletePeriod}
        title="Eliminar Periodo Académico"
        message={`¿Está seguro de que desea eliminar el periodo "${periodToDelete?.name}"?`}
        confirmText="Eliminar Periodo"
        cancelText="Cancelar"
        variant="danger"
        isLoading={deletingPeriod}
      />
    </div>
  );
}
