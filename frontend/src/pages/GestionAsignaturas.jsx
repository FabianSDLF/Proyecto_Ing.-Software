import { useState, useEffect, useCallback, useMemo } from 'react';
import toast from 'react-hot-toast';
import subjectService from '../services/subjectService';
import { DataTable, ConfirmDialog, Input, Select, Button, Badge } from '../components/ui';

export default function GestionAsignaturas() {
  const [asignaturas, setAsignaturas] = useState([]);
  const [name, setName] = useState('');
  const [cycle, setCycle] = useState('INICIAL');
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(true);

  // Estado para ConfirmDialog
  const [subjectToDelete, setSubjectToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchAsignaturas = useCallback(async (signal) => {
    setLoadingData(true);
    try {
      const data = await subjectService.getSubjects({ signal });
      setAsignaturas(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('Error cargando asignaturas:', err);
        toast.error(err.message || 'Error cargando asignaturas');
      }
    } finally {
      setLoadingData(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchAsignaturas(controller.signal);
    return () => controller.abort();
  }, [fetchAsignaturas]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      toast.error('El nombre de la asignatura es obligatorio');
      return;
    }

    setLoading(true);
    try {
      await subjectService.createSubject({ name: name.trim(), cycle });
      toast.success('Asignatura creada exitosamente');
      setName('');
      fetchAsignaturas();
    } catch (err) {
      console.error('Error creando asignatura:', err);
      toast.error(err.message || 'Error al crear la asignatura');
    } finally {
      setLoading(false);
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

  const cycleOptions = [
    { value: 'INICIAL', label: 'Inicial' },
    { value: 'INTERMEDIO', label: 'Intermedio' },
    { value: 'AVANZADO', label: 'Avanzado' },
  ];

  const getCycleBadge = (cycleVal) => {
    const norm = (cycleVal || '').toUpperCase();
    if (norm === 'INICIAL') return <Badge variant="success">Inicial</Badge>;
    if (norm === 'INTERMEDIO') return <Badge variant="warning">Intermedio</Badge>;
    if (norm === 'AVANZADO') return <Badge variant="primary">Avanzado</Badge>;
    return <Badge variant="gray">{cycleVal || 'Sin ciclo'}</Badge>;
  };

  const columns = useMemo(() => [
    {
      header: 'ID',
      accessor: 'id',
      className: 'w-16 text-gray-500 font-mono text-xs',
    },
    {
      header: 'Asignatura',
      accessor: 'name',
      className: 'font-medium text-gray-900',
    },
    {
      header: 'Ciclo',
      render: (row) => getCycleBadge(row.cycle),
    },
    {
      header: 'Acciones',
      align: 'right',
      render: (row) => (
        <Button
          variant="danger"
          size="sm"
          onClick={() => setSubjectToDelete(row)}
        >
          Eliminar
        </Button>
      ),
    },
  ], []);

  return (
    <div className="p-8 max-w-5xl mx-auto space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold text-blue-900 tracking-tight">Gestión de Asignaturas (Ciclos)</h1>
        <p className="text-gray-500 mt-1">Configura las asignaturas correspondientes a los diferentes ciclos de formación práctica.</p>
      </div>

      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <h2 className="text-lg font-bold text-gray-800 mb-4">Nueva Asignatura</h2>
        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div className="md:col-span-1">
            <Input 
              label="Nombre"
              placeholder="Ej: Aproximación Escolar"
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
            <Button 
              type="submit" 
              variant="primary"
              size="md"
              className="w-full bg-orange-600 hover:bg-orange-700 font-bold"
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

      {/* Diálogo de Confirmación Accesible (FE-19) */}
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
    </div>
  );
}
