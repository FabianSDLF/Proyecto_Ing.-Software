import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';
import MainLayout from './layouts/MainLayout';

// Public pages
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

// Protected pages
import Dashboard from './pages/Dashboard';
import Perfil from './pages/Perfil';
import Portafolio from './pages/Portafolio';
import NuevaPlanificacion from './pages/NuevaPlanificacion';
import CargaMasiva from './pages/CargaMasiva';
import GestionRoles from './pages/GestionRoles';
import GestionAsignaturas from './pages/GestionAsignaturas';
import Asignaciones from './pages/Asignaciones';
import MisEstudiantes from './pages/MisEstudiantes';
import Reportes from './pages/Reportes';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" reverseOrder={false} />

        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Authenticated Routes wrapped in ProtectedRoute */}
          <Route element={<ProtectedRoute />}>
            <Route element={<MainLayout />}>
              {/* Common Routes */}
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/perfil" element={<Perfil />} />

              {/* Student Routes */}
              <Route element={<RoleRoute allowedRoles={['Estudiante']} />}>
                <Route path="/portafolio" element={<Portafolio />} />
                <Route path="/portafolio/nueva-planificacion" element={<NuevaPlanificacion />} />
              </Route>

              {/* Administrator Routes */}
              <Route element={<RoleRoute allowedRoles={['Coordinador de practicas']} />}>
                <Route path="/carga-masiva" element={<CargaMasiva />} />
                <Route path="/gestion-roles" element={<GestionRoles />} />
                <Route path="/asignaturas" element={<GestionAsignaturas />} />
                <Route path="/asignaciones" element={<Asignaciones />} />
              </Route>

              {/* Teacher Routes */}
              <Route element={<RoleRoute allowedRoles={['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor']} />}>
                <Route path="/mis-estudiantes" element={<MisEstudiantes />} />
              </Route>
              <Route path="/portafolio/estudiante/:assignmentId" element={<Portafolio isReadOnly={true} />} />

              {/* Reportes va AQUÍ ADENTRO, arriba de los dos </Route> de cierre */}
              <Route element={<RoleRoute allowedRoles={['Coordinador de practicas']} />}>
                <Route path="/reportes" element={<Reportes />} />
              </Route>

            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
