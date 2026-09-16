import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import CargaMasiva from './pages/CargaMasiva';
import GestionRoles from './pages/GestionRoles';
import GestionAsignaturas from './pages/GestionAsignaturas';
import MainLayout from './layouts/MainLayout';

function App() {
    return (
        <BrowserRouter>
            <Toaster position="top-right" reverseOrder={false} />

            <Routes>
                <Route path="/" element={<Login />} />

                <Route element={<MainLayout />}>
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/carga-masiva" element={<CargaMasiva />} />
                    <Route path="/gestion-roles" element={<GestionRoles />} />
                    <Route path="/asignaturas" element={<GestionAsignaturas />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default App;