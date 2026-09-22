import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import CargaMasiva from './pages/CargaMasiva';
import GestionRoles from './pages/GestionRoles';
import GestionAsignaturas from './pages/GestionAsignaturas';
import Asignaciones from './pages/Asignaciones';
import MisEstudiantes from './pages/MisEstudiantes';
import Perfil from './pages/Perfil';
import Portafolio from './pages/Portafolio';
import NuevaPlanificacion from './pages/NuevaPlanificacion';
import MainLayout from './layouts/MainLayout';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

function App() {
    return (
        <BrowserRouter>
            <Toaster position="top-right" reverseOrder={false} />

            <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
                <Route path="/reset-password" element={<ResetPassword />} />

                <Route element={<MainLayout />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/perfil" element={<Perfil />} />
                    <Route path="/portafolio" element={<Portafolio />} />
                    <Route path="/portafolio/nueva-planificacion" element={<NuevaPlanificacion />} />
                    <Route path="/carga-masiva" element={<CargaMasiva />} />
                    <Route path="/gestion-roles" element={<GestionRoles />} />
                    <Route path="/asignaturas" element={<GestionAsignaturas />} />
                    <Route path="/asignaciones" element={<Asignaciones />} />
                    <Route path="/mis-estudiantes" element={<MisEstudiantes />} />
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;