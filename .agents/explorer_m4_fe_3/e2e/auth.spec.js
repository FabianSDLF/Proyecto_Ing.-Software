import { test, expect } from '@playwright/test';

test.describe('Authentication & Protected Routing E2E', () => {
  test('redirects unauthenticated user from /dashboard to login /', async ({ page }) => {
    await page.goto('/dashboard');
    // ProtectedRoute should redirect to login root
    await expect(page).toHaveURL(/\//);
    await expect(page.getByRole('button', { name: /iniciar sesión/i })).toBeVisible();
  });

  test('successful login redirects to /dashboard and renders role widget', async ({ page }) => {
    // Intercept backend login call for reliable mock in E2E
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'mock-e2e-jwt-token',
          user: {
            id: 1,
            fullName: 'Fabián Sepúlveda',
            email: 'admin@ubiobio.cl',
            role: { id: 1, name: 'Administrador' },
          },
        }),
      });
    });

    await page.route('**/api/asignaciones', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.route('**/api/centros', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
    });

    await page.goto('/');

    await page.fill('input[type="email"]', 'admin@ubiobio.cl');
    await page.fill('input[type="password"]', 'password123');
    await page.click('button[type="submit"]');

    // Should navigate to dashboard
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByText(/Bienvenido, Fabián Sepúlveda/i)).toBeVisible();
    await expect(page.getByText(/Gestión de Usuarios/i)).toBeVisible();
  });

  test('logout removes credentials and redirects to login screen', async ({ page }) => {
    // Seed authenticated session in localStorage
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'e2e-valid-token');
      window.localStorage.setItem(
        'usuarioActivo',
        JSON.stringify({
          id: 1,
          fullName: 'Profesor Guía',
          email: 'profesor@ubiobio.cl',
          role: { id: 3, name: 'Profesor Colaborador' },
        })
      );
    });

    await page.goto('/dashboard');
    await expect(page.getByText(/Mis Estudiantes Asignados/i)).toBeVisible();

    // Trigger logout from MainLayout header/sidebar
    const logoutBtn = page.getByRole('button', { name: /cerrar sesión/i });
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
    }

    await expect(page).toHaveURL(/\//);

    // Verify localStorage cleared (TR-04)
    const token = await page.evaluate(() => localStorage.getItem('token'));
    const user = await page.evaluate(() => localStorage.getItem('usuarioActivo'));
    expect(token).toBeNull();
    expect(user).toBeNull();
  });
});
