import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

export default function GestionAsignaturas() {
  const [asignaturas, setAsignaturas] = useState([]);
  const [name, setName] = useState('');
  const [cycle, setCycle] = useState('Inicial');
  const navigate = useNavigate();

  const fetchAsignaturas = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/asignaturas');
      if (res.ok) {
        const data = await res.json();
        setAsignaturas(data);
      }
    } catch (error) {
      toast.error('Error cargando asignaturas');
    }
  };

  useEffect(() => {
    // Protección de ruta: Solo Administrador
    const userString = localStorage.getItem('usuarioActivo');
    if (userString) {
      const user = JSON.parse(userString);
      if (user.role && user.role.name !== 'Administrador') {
        toast.error("Acceso denegado: Solo administradores");
        navigate('/dashboard');
        return;
      }
    } else {
        navigate('/');
        return;
    }

    fetchAsignaturas();
  }, [navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      const res = await fetch('http://localhost:8080/api/asignaturas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, cycle })
      });

      if (res.ok) {
        toast.success('Asignatura creada exitosamente');
        setName('');
        fetchAsignaturas();
      } else {
        toast.error('Error al crear la asignatura');
      }
    } catch (error) {
      toast.error('Error de conexión con el servidor');
    }
  };

  const handleDelete = async (id) => {
    try {
      const res = await fetch(`http://localhost:8080/api/asignaturas/${id}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        toast.success('Asignatura eliminada');
        fetchAsignaturas();
      }
    } catch (error) {
      toast.error('Error al eliminar');
    }
  };

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-800 mb-6">Gestión de Asignaturas (Ciclos)</h1>
      
      <div className="bg-white p-6 rounded-lg shadow-md mb-8">
        <h2 className="text-xl font-semibold mb-4">Nueva Asignatura</h2>
        <form onSubmit={handleSubmit} className="flex gap-4 items-end">
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-1">Nombre</label>
            <input 
              type="text" 
              className="w-full border p-2 rounded" 
              placeholder="Ej: Aproximación Escolar"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="w-48">
            <label className="block text-sm font-medium text-gray-700 mb-1">Ciclo</label>
            <select 
              className="w-full border p-2 rounded"
              value={cycle}
              onChange={(e) => setCycle(e.target.value)}
            >
              <option value="Inicial">Inicial</option>
              <option value="Intermedio">Intermedio</option>
              <option value="Avanzado">Avanzado</option>
            </select>
          </div>
          <button type="submit" className="bg-orange-600 text-white px-4 py-2 rounded hover:bg-orange-700 font-bold transition">
            Crear
          </button>
        </form>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="p-4 font-semibold text-gray-600">ID</th>
              <th className="p-4 font-semibold text-gray-600">Asignatura</th>
              <th className="p-4 font-semibold text-gray-600">Ciclo</th>
              <th className="p-4 font-semibold text-gray-600">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {asignaturas.map(asig => (
              <tr key={asig.id} className="border-b hover:bg-gray-50">
                <td className="p-4">{asig.id}</td>
                <td className="p-4">{asig.name}</td>
                <td className="p-4">
                  <span className={`px-2 py-1 rounded-full text-xs font-semibold ${
                    asig.cycle === 'Inicial' ? 'bg-green-100 text-green-800' :
                    asig.cycle === 'Intermedio' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-purple-100 text-purple-800'
                  }`}>
                    {asig.cycle}
                  </span>
                </td>
                <td className="p-4">
                  <button onClick={() => handleDelete(asig.id)} className="text-red-500 hover:text-red-700">Eliminar</button>
                </td>
              </tr>
            ))}
            {asignaturas.length === 0 && (
              <tr>
                <td colSpan="4" className="p-4 text-center text-gray-500">No hay asignaturas registradas</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
