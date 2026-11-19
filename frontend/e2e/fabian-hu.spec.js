import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';

const rubrics = JSON.parse(readFileSync(new URL('../../src/main/resources/rubrics/tutor.json', import.meta.url), 'utf8'));
const assignment = { id: 1, status: 'ACTIVA', student: { id: 2, fullName: 'Estudiante de prueba', email: 'estudiante@example.test' }, subject: { id: 1, name: 'Práctica Profesional', cycle: 'AVANZADO', professionalRubrics: true }, period: { id: 1, name: '2026-2', status: 'ACTIVO' }, center: { name: 'Centro de prueba' } };

async function session(page, role = 'Tutor') {
  await page.route('**/health', route => route.fulfill({ json: { status: 'ok' } }));
  await page.addInitScript(role => {
    localStorage.setItem('token', 'test-session');
    localStorage.setItem('usuarioActivo', JSON.stringify({ id: 1, fullName: 'Cuenta de prueba', email: 'prueba@example.test', role: { name: role } }));
  }, role);
}
async function openMenu(page) {
  const button = page.getByRole('button', { name: 'Abrir menú' });
  if (await button.isVisible()) await button.click();
}
async function noPageOverflow(page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
}

test('HU40: navegación, ayuda sin contacto y foco accesibles', async ({ page }) => {
  await session(page);
  await page.route('**/chatbot/health', route => route.fulfill({ json: { status: 'ok' } }));
  await page.route('**/api/ayuda/contacto', route => route.fulfill({ json: { configured: false } }));
  await page.goto('/ayuda');
  await expect(page.getByRole('heading', { name: 'Ayuda y soporte' })).toBeVisible();
  await expect(page.getByText(/aún no está configurado/)).toBeVisible();
  await openMenu(page);
  await page.getByRole('link', { name: 'Ayuda y soporte', exact: true }).click();
  await noPageOverflow(page);
  await page.screenshot({ path: test.info().outputPath('ayuda.png'), fullPage: true });
});

test('HU44: capturar, guardar parcialmente, reabrir y proteger cambios sin guardar', async ({ page }) => {
  await session(page);
  await page.route('**/chatbot/health', route => route.fulfill({ json: { status: 'ok' } }));
  let saved = null;
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.includes('/pautas/tutor')) return route.fulfill({ json: rubrics });
    if (url.pathname.includes('/asignaciones/visibles')) return route.fulfill({ json: [assignment] });
    if (url.pathname.includes('/evaluaciones/asignacion/')) return route.fulfill({ json: saved ? [saved] : [] });
    if (url.pathname === '/api/evaluaciones' && route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      saved = { ...body, id: 10, revision: (saved?.revision || 0)+1, score: null,
        criteria: body.criteria.map(c => ({ ...c, ...rubrics[0].indicators.find(i => i.criterionCode === c.criterionCode) })) };
      return route.fulfill({ json: saved });
    }
    return route.fulfill({ json: [] });
  });
  await page.goto('/mis-estudiantes');
  await expect(page.getByRole('button', { name: 'Evaluar (Pauta)' })).toBeVisible();
  await noPageOverflow(page);
  await page.getByRole('button', { name: 'Evaluar (Pauta)' }).click();
  const point = page.getByRole('combobox', { name: 'Puntaje indicador A1' });
  await point.selectOption('5');
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await expect(page.getByRole('dialog', { name: 'Cambios sin guardar' })).toBeVisible();
  await page.getByRole('button', { name: 'Seguir editando' }).click();
  await expect(point).toHaveValue('5');
  await page.getByRole('button', { name: 'Guardar Borrador' }).click();
  await expect(page.getByText('Revisión #1')).toBeVisible();
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
  await page.getByRole('button', { name: /Continuar Pauta/ }).click();
  await expect(page.getByRole('combobox', { name: 'Puntaje indicador A1' })).toHaveValue('5');
  await expect(page.getByRole('combobox', { name: 'Puntaje indicador A2' })).toHaveValue('');
  await expect(page.getByText('Observados: 1 / 19')).toBeVisible();
  await noPageOverflow(page);
  await page.screenshot({ path: test.info().outputPath('pauta.png'), fullPage: true });
});

test('HU35: filtros y exportación CSV mantienen el mismo alcance', async ({ page }) => {
  await session(page, 'Coordinador de practicas');
  const filters = [];
  await page.route('**/api/reportes/estadisticas**', route => {
    filters.push(new URL(route.request().url()).searchParams.toString());
    return route.fulfill({ json: { students: 1, assignments: 2, centersUsed: 1, publishedEvaluations: 1, configuredInstruments: 8, pendingEvaluations: 7,
      periods: [], subjects: [], gradesByInstrument: [{ instrument: 'TUTOR_ESP_1 · 2025-2-v1', count: 1, mean: 5 }], latestPlanStates: { APROBADO_IA: 1 }, series: [], detail: [] } });
  });
  await page.route('**/api/portafolio/export-csv**', route => route.fulfill({ contentType: 'text/csv', body: 'Asignacion;Estudiante\r\n1;Prueba' }));
  await page.goto('/reportes');
  await expect(page.getByRole('heading', { name: 'Estadísticas institucionales' })).toBeVisible();
  await page.getByLabel('Periodo', { exact: true }).selectOption('TODOS');
  await page.getByLabel('Ciclo', { exact: true }).selectOption('AVANZADO');
  await expect(page.getByRole('button', { name: 'Descargar CSV' })).toBeEnabled();
  const request = page.waitForRequest('**/api/portafolio/export-csv**');
  await page.getByRole('button', { name: 'Descargar CSV' }).click();
  const url = new URL((await request).url());
  expect(url.searchParams.get('history')).toBe('true'); expect(url.searchParams.get('cycle')).toBe('AVANZADO');
  expect(filters.at(-1)).toContain('cycle=AVANZADO');
  await noPageOverflow(page);
  await page.screenshot({ path: test.info().outputPath('reportes.png'), fullPage: true });
});

test('HU40: ficha y acceso a planificación en móvil y tablet', async ({ page }) => {
  await session(page, 'Estudiante');
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    if (path.includes('/asignaciones/')) return route.fulfill({ json: [assignment] });
    return route.fulfill({ json: [] });
  });
  await page.goto('/portafolio');
  await expect(page.getByRole('heading', { name: /Mi Portafolio/ })).toBeVisible();
  await noPageOverflow(page);
  await page.goto('/portafolio/nueva-planificacion');
  await expect(page.getByRole('heading', { name: /Planificación/ }).first()).toBeVisible();
  await noPageOverflow(page);
  await page.screenshot({ path: test.info().outputPath('planificacion.png'), fullPage: true });
});
