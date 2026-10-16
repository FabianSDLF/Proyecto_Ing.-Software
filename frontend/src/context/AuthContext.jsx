import { createContext, useContext, useState, useCallback, useMemo } from 'react';
import authService from '../services/authService';

const STORAGE_KEY_TOKEN = 'token';
const STORAGE_KEY_USER = 'usuarioActivo';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Lazy initializer to read localStorage synchronously without cascading useEffect re-renders (FE-12)
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEY_TOKEN) || null);
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(STORAGE_KEY_USER);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      localStorage.removeItem(STORAGE_KEY_USER);
      localStorage.removeItem(STORAGE_KEY_TOKEN);
      return null;
    }
  });

  const [isLoading, setIsLoading] = useState(false);

  const isAuthenticated = useMemo(() => Boolean(token && user), [token, user]);
  const userRole = useMemo(() => user?.role?.name || null, [user]);

  // Specific role evaluation helpers (TS-06, FE-03)
  const isAdmin = useMemo(() => userRole === 'Coordinador de practicas', [userRole]);
  const isStudent = useMemo(() => userRole === 'Estudiante', [userRole]);
  const isProfessor = useMemo(
    () => ['Profesor de Asignatura', 'Profesor Colaborador', 'Tutor'].includes(userRole),
    [userRole]
  );

  /**
   * Log in user, persist credentials, and update reactive state
   */
  const login = useCallback(async (credentials) => {
    setIsLoading(true);
    try {
      const response = await authService.login(credentials);
      // Backend returns { token, user: UserResponse }
      const authToken = response.token;
      const authUser = response.user;

      localStorage.setItem(STORAGE_KEY_TOKEN, authToken);
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(authUser));

      setToken(authToken);
      setUser(authUser);
      return authUser;
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Log out user, purge BOTH token and usuarioActivo from localStorage (TR-04)
   */
  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_USER);
    setToken(null);
    setUser(null);
  }, []);

  /**
   * Update user profile reactively without window.location.reload() (FE-04)
   */
  const updateUserProfile = useCallback((updatedUserData) => {
    setUser((prev) => {
      const merged = { ...prev, ...updatedUserData };
      localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(merged));
      return merged;
    });
  }, []);

  /**
   * Role validation helper
   */
  const hasRole = useCallback(
    (allowedRoles) => {
      if (!userRole) return false;
      const rolesArray = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
      return rolesArray.includes(userRole);
    },
    [userRole]
  );

  const contextValue = useMemo(
    () => ({
      user,
      token,
      userRole,
      isAuthenticated,
      isLoading,
      isAdmin,
      isStudent,
      isProfessor,
      login,
      logout,
      updateUserProfile,
      hasRole,
    }),
    [user, token, userRole, isAuthenticated, isLoading, isAdmin, isStudent, isProfessor, login, logout, updateUserProfile, hasRole]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser utilizado dentro de un <AuthProvider />');
  }
  return context;
}

export default AuthContext;
