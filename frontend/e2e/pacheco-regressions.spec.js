import { test, expect } from '@playwright/test';

const report = { students: 6, assignments: 6, centersUsed: 4, publishedEvaluations: 1, pendingEvaluations: 8, configuredInstruments: 8, periods: [], subjects: [], gradesByInstrument: [], latestPlanStates: { BORRADOR: 1 }, detail: [], series: [] };

for (const [width, height] of [[320,740], [768,1024], [844,390], [1280,720], [1440,900]]) {
  test(`cierre de sesión siempre accesible ${width}x${height}`, async ({ page }) => {
    await page.setViewportSize({ width, height });
    await page.addInitScript(() => {
      localStorage.setItem('token', 'session-de-prueba');
      localStorage.setItem('usuarioActivo', JSON.stringify({ id: 1, fullName: 'Cuenta de coordinación', email: 'coord@example.test', role: { name: 'Coordinador de practicas' } }));
    });
    await page.route('**/api/**', route => route.fulfill({ json: route.request().url().includes('/reportes/estadisticas') ? report : [] }));
    await page.route('**/health', route => route.fulfill({ json: { status: 'ok' } }));
    await page.goto('/dashboard');
    await expect(page.getByText('Estudiantes distintos')).toBeVisible();
    const menu = page.getByRole('button', { name: 'Abrir menú', exact: true });
    if (width < 1280) await menu.click();
    const sidebar = page.getByRole('complementary', { name: 'Menú principal' });
    const logout = sidebar.getByRole('button', { name: 'Cerrar sesión', exact: true });
    await expect(logout).toBeInViewport({ ratio: 1 });
    if (width >= 1280) {
      await page.getByRole('heading', { name: 'Notificaciones', exact: true }).scrollIntoViewIfNeeded();
      await expect(logout).toBeInViewport({ ratio: 1 });
      expect(await sidebar.evaluate(el => el.getBoundingClientRect().height)).toBeLessThanOrEqual(height);
    } else {
      const help = sidebar.getByRole('link', { name: 'Ayuda y soporte', exact: true });
      await help.scrollIntoViewIfNeeded();
      await expect(help).toBeInViewport({ ratio: 1 });
      await expect(logout).toBeInViewport({ ratio: 1 });
    }
    await page.screenshot({ path: test.info().outputPath('sidebar.png') });
    await logout.click();
    await expect(page.getByRole('button', { name: 'Iniciar Sesión', exact: true })).toBeVisible();
  });
}
