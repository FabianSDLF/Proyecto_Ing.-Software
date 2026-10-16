import { Navigate, Outlet } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import Spinner from './ui/Spinner';

/**
 * Role-based authorization route guard (FE-03, FE-05)
 * @param {string[]} allowedRoles - List of permitted role names
 * @param {string} [redirectTo='/dashboard'] - Destination fallback
 */
export default function RoleRoute({ allowedRoles = [], redirectTo = '/dashboard' }) {
  const { userRole, isAuthenticated, isLoading } = useAuth();
  const hasNotified = useRef(false);

  const isAuthorized = allowedRoles.includes(userRole);

  useEffect(() => {
    if (!isLoading && isAuthenticated && !isAuthorized && !hasNotified.current) {
      toast.error('Acceso denegado: No tienes permisos para acceder a esta sección.');
      hasNotified.current = true;
    }
  }, [isLoading, isAuthenticated, isAuthorized]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-gray-600">
        <Spinner size="lg" color="orange" />
        <p className="mt-4 font-medium text-sm">Comprobando permisos...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (!isAuthorized) {
    return <Navigate to={redirectTo} replace />;
  }

  return <Outlet />;
}
