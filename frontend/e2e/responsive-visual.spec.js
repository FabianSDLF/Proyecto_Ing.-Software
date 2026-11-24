import { test, expect } from '@playwright/test';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const teacher = { id: 3, fullName: 'María Fernanda Sepúlveda Contreras', email: 'mariafernanda.sepulvedacontreras@departamentopedagogia.ubiobio.cl' };
const assignment = { id: 1, status: 'ACTIVA', student: { id: 2, fullName: 'Sebastián Alejandro González Sepúlveda', email: 'sebastianalejandro.gonzalezsepulveda@alumnos.ubiobio.cl' }, subject: { id: 1, name: 'Práctica Profesional de Pedagogía en Educación Matemática', cycle: 'AVANZADO', professionalRubrics: true, professor: teacher }, period: { id: 1, name: 'Segundo semestre 2026', status: 'ACTIVO' }, center: { id: 1, name: 'Colegio Bicentenario de Excelencia Académica de Chillán', contactEmail: teacher.email, address: 'Avenida Libertador Bernardo O’Higgins 1234', city: 'Chillán' }, tutor: teacher, collaborator: teacher };
const users = Array.from({ length: 14 }, (_, i) => ({ ...assignment.student, id: i + 2, fullName: `${assignment.student.fullName} ${i + 1}`, active: true, role: { name: 'Estudiante' } }));
const notifications = [{ id: 1, read: false, type: 'PLANIFICACION_APROBADA', message: `Tu planificación de Cuarto Medio fue aprobada. Consulta los comentarios en https://campus.ubiobio.cl/planificaciones/revisionprofesionalmatematica2026segundosemestre.`, createdAt: '2026-11-24T12:00:00' }];
const rubrics = JSON.parse(readFileSync(new URL('../../src/main/resources/rubrics/tutor.json', import.meta.url), 'utf8'));
const stats = { students: 48, assignments: 54, centersUsed: 12, publishedEvaluations: 87, pendingEvaluations: 345, configuredInstruments: 8, periods: [assignment.period], subjects: [assignment.subject], gradesByInstrument: [{ instrument: 'TUTOR_ESP_1 · 2025-2-v1', count: 48, mean: 5.7 }], latestPlanStates: { APROBADO: 21, EN_REVISION_DOCENTE: 12 }, series: [], detail: [] };
const sizes = [[320, 740], [390, 844], [768, 1024], [1024, 768], [1440, 900], [844, 390]];

for (const [width, height] of sizes) {
  test(`revisión visual ${width}x${height}`, async ({ page }) => {
    test.setTimeout(120000);
    await page.setViewportSize({ width, height });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/api/**', route => {
      const p = new URL(route.request().url()).pathname;
      let json = [];
      if (p === '/api/notificaciones') json = notifications;
      else if (p === '/api/usuarios/perfil') json = teacher;
      else if (p === '/api/notificaciones/destinatarios' || p.startsWith('/api/usuarios')) json = users;
      else if (p.includes('/pautas/tutor')) json = rubrics;
      else if (p === '/api/reportes/estadisticas') json = stats;
      else if (p.endsWith('/ficha')) json = assignment;
      else if (p.startsWith('/api/asignaciones')) json = [assignment];
      else if (p === '/api/periodos') json = [assignment.period];
      else if (p === '/api/asignaturas') json = [assignment.subject];
      else if (p === '/api/centros') json = [assignment.center];
      else if (p === '/api/roles') json = [{ id: 2, name: 'Estudiante' }];
      else if (p.includes('/planificaciones')) json = [{ id: 1, schoolGrade: 'Cuarto Medio', mathAxis: 'ALGEBRA_Y_FUNCIONES', status: 'EN_REVISION_DOCENTE', objective: 'Resolver problemas de funciones en contextos cotidianos', version: 1, practiceAssignment: assignment }];
      return route.fulfill({ json });
    });
    await page.route('**/health', route => route.fulfill({ json: { status: 'ok' } }));
    const folder = resolve('..', '.verification', 'visual', process.env.VISUAL_STAGE || 'current', `${width}x${height}`);
    mkdirSync(folder, { recursive: true });
    const measurements = [];
    async function capture(name) {
      await page.evaluate(() => document.fonts.ready);
      await page.screenshot({ path: resolve(folder, `${name}.png`), fullPage: await page.getByRole('dialog').count() === 0 });
      const geometry = await page.evaluate(() => ({
        width: innerWidth, documentWidth: document.documentElement.scrollWidth,
        overflow: [...document.querySelectorAll('main *, [role="dialog"] *')].filter(el => {
          const r = el.getBoundingClientRect();
          if (!r.width || !r.height || (r.left >= -1 && r.right <= innerWidth + 1)) return false;
          for (let parent = el.parentElement; parent; parent = parent.parentElement) {
            if (['auto', 'scroll', 'hidden'].includes(getComputedStyle(parent).overflowX)) return false;
          }
          return true;
        }).slice(0, 10).map(el => ({ tag: el.tagName, text: el.textContent.slice(0, 75) })),
      }));
      measurements.push({ name, ...geometry });
      expect.soft(geometry.documentWidth, `${name}: ancho de página`).toBeLessThanOrEqual(width + 1);
      expect.soft(geometry.overflow, `${name}: elementos fuera de pantalla`).toEqual([]);
    }
    async function open(role, path) {
      await page.evaluate(({ role, teacher }) => {
        localStorage.setItem('token', 'visual-test');
        localStorage.setItem('usuarioActivo', JSON.stringify({ ...teacher, role: { name: role } }));
      }, { role, teacher });
      await page.goto(path);
      await expect(page.locator('main')).toBeVisible();
    }
    await page.goto('/');
    await expect(page.getByRole('button', { name: 'Iniciar Sesión' })).toBeVisible();
    await capture('login');
    await page.goto('/forgot-password');
    await expect(page.getByRole('button', { name: 'Enviar enlace de recuperación' })).toBeVisible();
    await capture('recuperar-clave');
    await page.goto('/reset-password?token=visual-test');
    await expect(page.getByRole('button', { name: 'Guardar nueva contraseña' })).toBeVisible();
    await capture('nueva-clave');
    await open('Estudiante', '/dashboard');
    await expect(page.getByText('Mi Equipo y Centro de Práctica')).toBeVisible();
    await capture('inicio-estudiante');
    const menu = page.getByRole('button', { name: 'Abrir menú' });
    if (await menu.isVisible()) await menu.click();
    await page.getByRole('button', { name: /^Notificaciones/ }).click();
    await expect(page.getByRole('dialog', { name: 'Notificaciones' })).toBeVisible();
    await capture('notificaciones');
    expect.soft(await page.locator('main').getAttribute('inert'), 'cerrar menú al abrir notificaciones').toBeNull();
    const message = page.getByRole('dialog').locator('p').filter({ hasText: 'Tu planificación' });
    expect.soft(await message.evaluate(el => el.scrollWidth <= el.clientWidth), 'mensaje completo dentro del modal').toBe(true);
    await page.keyboard.press('Tab');
    expect.soft(await page.getByRole('dialog').evaluate(el => el.contains(document.activeElement)), 'foco dentro de notificaciones').toBe(true);
    await page.getByRole('button', { name: 'Cerrar modal' }).click();
    await open('Estudiante', '/portafolio');
    await page.getByRole('button', { name: 'Planificaciones', exact: true }).click();
    await expect(page.getByText('Cuarto Medio')).toBeVisible();
    await capture('portafolio');
    await open('Coordinador de practicas', '/dashboard');
    await expect(page.getByText('Estudiantes distintos')).toBeVisible();
    await capture('inicio-coordinacion');
    await open('Administrador', '/gestion-roles');
    await expect(page.getByRole('button', { name: 'Siguiente', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Siguiente', exact: true }).scrollIntoViewIfNeeded();
    await expect.soft(page.getByRole('button', { name: 'Siguiente', exact: true })).toBeInViewport({ ratio: 1 });
    await capture('usuarios');
    await page.getByRole('button', { name: /Nuevo Usuario/ }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await capture('usuario-formulario');
    await page.getByRole('button', { name: 'Cerrar modal' }).click();
    await open('Coordinador de practicas', '/reportes');
    await expect(page.getByText('Estudiantes distintos')).toBeVisible();
    await capture('reportes');
    await open('Tutor', '/reportes');
    await expect(page.getByRole('heading', { name: 'Reporte Institucional' })).toBeVisible();
    await capture('reportes-docente');
    await open('Estudiante', '/perfil');
    await expect(page.getByRole('button', { name: /Guardar/ })).toBeVisible();
    await capture('perfil');
    for (const [path, title] of [['/carga-masiva', /Carga e Inscripción/], ['/asignaturas', /Gestión de Asignaturas/], ['/ciclos', /Ciclos de Práctica/]]) {
      await open('Administrador', path);
      await expect(page.getByRole('heading', { name: title })).toBeVisible();
      await capture(path.slice(1));
    }
    await open('Administrador', '/asignaciones');
    await expect(page.getByRole('tab', { name: /Asignaciones Individuales/ })).toBeVisible();
    await capture('asignaciones');
    await open('Estudiante', '/portafolio/nueva-planificacion');
    await expect(page.getByRole('heading', { name: /Planificación/ }).first()).toBeVisible();
    await capture('nueva-planificacion');
    await open('Estudiante', '/retroplani');
    await expect(page.getByPlaceholder(/Pregunta sobre fracciones/)).toBeVisible();
    await capture('retroplani');
    await open('Tutor', '/mis-estudiantes');
    await page.getByRole('button', { name: 'Evaluar (Pauta)' }).click();
    await expect(page.getByRole('combobox', { name: 'Puntaje indicador A1' })).toBeVisible();
    await capture('evaluacion');
    await page.getByRole('button', { name: 'Cancelar', exact: true }).scrollIntoViewIfNeeded();
    await capture('evaluacion-acciones');
    writeFileSync(resolve(folder, 'mediciones.json'), JSON.stringify({ measurements, errors }, null, 2));
    expect(errors).toEqual([]);
  });
}
