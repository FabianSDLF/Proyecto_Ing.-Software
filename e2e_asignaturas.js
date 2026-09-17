// e2e_asignaturas.js
const { chromium } = require('./frontend/node_modules/playwright');
const assert = require('assert');

(async () => {
  console.log('=== Starting End-to-End Headless Browser Test ===\n');
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // 1. Visit the app and initialize admin session in localStorage
    console.log('1. Navigating to root and setting admin session...');
    await page.goto('http://localhost:5173/');
    await page.evaluate(() => {
      localStorage.setItem('usuarioActivo', JSON.stringify({
        id: 1,
        fullName: 'Administrador E2E',
        email: 'admin.e2e@ubb.cl',
        role: { id: 1, name: 'Administrador' }
      }));
    });

    // 2. Navigate to /asignaturas
    console.log('2. Navigating to /asignaturas view...');
    await page.goto('http://localhost:5173/asignaturas');
    await page.waitForSelector('h1:has-text("Gestión de Asignaturas (Ciclos)")');
    console.log('   Heading "Gestión de Asignaturas (Ciclos)" is visible.');

    // 3. Test validation feedback on empty submission
    console.log('\n3. Testing empty name submission validation...');
    const submitBtn = page.locator('button[type="submit"]');
    await submitBtn.click();
    await page.waitForSelector('text=El nombre de la asignatura es obligatorio', { timeout: 4000 });
    console.log('   Validation toast for empty name was displayed.');

    // 4. Fill in new Asignatura
    const testSubjectName = 'Didáctica Experimental E2E';
    console.log(`\n4. Creating new Asignatura: "${testSubjectName}" with cycle "INTERMEDIO"...`);
    const nameInput = page.locator('input[placeholder="Ej: Aproximación Escolar"]');
    await nameInput.fill(testSubjectName);

    const cycleSelect = page.locator('select');
    await cycleSelect.selectOption('INTERMEDIO');

    // 5. Submit form
    await submitBtn.click();

    // 6. Verify success toast and table row
    console.log('5. Waiting for success notification and table update...');
    await page.waitForSelector('text=Asignatura creada exitosamente', { timeout: 5000 });
    console.log('   Success toast confirmed.');

    const tableRow = page.locator(`tr:has-text("${testSubjectName}")`);
    await tableRow.waitFor({ state: 'visible', timeout: 5000 });
    console.log('   Table row with new subject is visible.');

    // Check badge text
    const badge = tableRow.locator('span');
    const badgeText = await badge.innerText();
    assert.strictEqual(badgeText, 'Intermedio', `Badge text must be 'Intermedio', got '${badgeText}'`);
    console.log('   Cycle badge correctly displays "Intermedio".');

    // 7. Verify backend database persistence via direct API query
    console.log('\n6. Verifying database persistence via GET /api/asignaturas...');
    const res = await fetch('http://localhost:8080/api/asignaturas');
    assert.strictEqual(res.status, 200);
    const list = await res.json();
    const persisted = list.find(s => s.name === testSubjectName);
    assert.ok(persisted, `Subject "${testSubjectName}" must exist in backend database`);
    assert.strictEqual(persisted.cycle, 'INTERMEDIO');
    console.log('   Backend confirmed persistence:', persisted);

    // 8. Test deletion via UI
    console.log('\n7. Deleting subject via UI button with confirmation dialog...');
    page.on('dialog', async dialog => {
      assert.strictEqual(dialog.type(), 'confirm');
      assert.ok(dialog.message().includes('eliminar'));
      await dialog.accept();
    });

    const deleteBtn = tableRow.locator('button:has-text("Eliminar")');
    await deleteBtn.click();

    await page.waitForSelector('text=Asignatura eliminada', { timeout: 5000 });
    await tableRow.waitFor({ state: 'detached', timeout: 5000 });
    console.log('   Subject row removed from table after deletion.');

    // 9. Verify deletion in backend database
    const resAfter = await fetch('http://localhost:8080/api/asignaturas');
    const listAfter = await resAfter.json();
    assert.ok(!listAfter.some(s => s.id === persisted.id), 'Subject must be removed from backend database');
    console.log('   Backend confirmed complete deletion.');

    console.log('\n======================================================');
    console.log('🎉 ALL HEADLESS BROWSER E2E TESTS PASSED SUCCESSFULLY!');
    console.log('======================================================');
  } catch (error) {
    console.error('\n❌ E2E TEST FAILED:', error);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
