// verify_api.js
const assert = require('assert');

const BASE_URL = 'http://localhost:8080/api/asignaturas';

async function runTests() {
  console.log('--- Starting Programmatic Verification of Asignaturas API ---\n');
  const createdIds = [];

  try {
    // 1. Initial GET
    console.log('1. Checking GET /api/asignaturas (initial state)...');
    const resInit = await fetch(BASE_URL);
    assert.strictEqual(resInit.status, 200, 'Initial GET should return 200');
    const listInit = await resInit.json();
    console.log(`   Initial subjects count: ${listInit.length}`);

    // 2. Acceptance Criteria 1 & 2: POST with valid JSON payload -> 200 OK & entity
    console.log('\n2. Testing POST /api/asignaturas with cycle "INICIAL"...');
    const payload1 = { name: 'Aproximacion Escolar', cycle: 'INICIAL' };
    const resPost1 = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload1)
    });
    assert.strictEqual(resPost1.status, 200, 'POST should return 200 OK');
    const entity1 = await resPost1.json();
    console.log('   Response entity:', entity1);
    assert.ok(entity1.id, 'Created entity must have an ID');
    assert.strictEqual(entity1.name, 'Aproximacion Escolar');
    assert.strictEqual(entity1.cycle, 'INICIAL');
    createdIds.push(entity1.id);

    // 3. Acceptance Criteria 3: Subsequent GET returns the newly created Asignatura
    console.log('\n3. Testing subsequent GET /api/asignaturas contains newly created entity...');
    const resGet1 = await fetch(BASE_URL);
    assert.strictEqual(resGet1.status, 200, 'GET should return 200');
    const list1 = await resGet1.json();
    const found1 = list1.find(item => item.id === entity1.id);
    assert.ok(found1, `Entity with ID ${entity1.id} must exist in GET /api/asignaturas`);
    assert.strictEqual(found1.name, 'Aproximacion Escolar');
    assert.strictEqual(found1.cycle, 'INICIAL');
    console.log('   Verified entity persists in database:', found1);

    // 4. Testing backward-compatible capitalized cycle "Inicial"
    console.log('\n4. Testing POST /api/asignaturas with capitalized cycle "Inicial"...');
    const payload2 = { name: 'Práctica Temprana', cycle: 'Inicial' };
    const resPost2 = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload2)
    });
    assert.strictEqual(resPost2.status, 200, 'POST with "Inicial" should return 200 OK');
    const entity2 = await resPost2.json();
    assert.strictEqual(entity2.cycle, 'INICIAL', 'Capitalized "Inicial" must deserialize to INICIAL');
    assert.strictEqual(entity2.name, 'Práctica Temprana');
    createdIds.push(entity2.id);
    console.log('   Successfully deserialized "Inicial" to INICIAL:', entity2);

    // 5. Testing lowercase cycle with whitespace "  intermedio  "
    console.log('\n5. Testing POST with lowercase cycle and whitespace "  intermedio  "...');
    const payload3 = { name: 'Práctica Intermedia', cycle: '  intermedio  ' };
    const resPost3 = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload3)
    });
    assert.strictEqual(resPost3.status, 200, 'POST with trimmed lowercase should return 200 OK');
    const entity3 = await resPost3.json();
    assert.strictEqual(entity3.cycle, 'INTERMEDIO');
    createdIds.push(entity3.id);
    console.log('   Successfully deserialized lowercase trimmed cycle:', entity3);

    // 6. Testing cycle "AVANZADO" with special Spanish characters and accents
    console.log('\n6. Testing POST with cycle "AVANZADO" and Spanish characters/accents...');
    const payload4 = { name: 'Didáctica de la Geometría & Álgebra (Ñ)', cycle: 'AVANZADO' };
    const resPost4 = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload4)
    });
    assert.strictEqual(resPost4.status, 200, 'POST with accents should return 200 OK');
    const entity4 = await resPost4.json();
    assert.strictEqual(entity4.name, 'Didáctica de la Geometría & Álgebra (Ñ)');
    assert.strictEqual(entity4.cycle, 'AVANZADO');
    createdIds.push(entity4.id);
    console.log('   Successfully created subject with accents:', entity4);

    // 7. Testing validation: empty name rejection
    console.log('\n7. Testing POST validation with empty/whitespace name (should be 400)...');
    const resPostEmpty = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: '   ', cycle: 'INICIAL' })
    });
    assert.strictEqual(resPostEmpty.status, 400, 'Empty name should return 400 Bad Request');
    console.log('   Correctly rejected empty name with status 400');

    // 7b. Testing concurrent creation of multiple subjects
    console.log('\n7b. Testing concurrent POST requests via Promise.all...');
    const concurrentPayloads = [
      { name: 'Álgebra Lineal I', cycle: 'INICIAL' },
      { name: 'Cálculo Diferencial', cycle: 'INICIAL' },
      { name: 'Didáctica de la Probabilidad', cycle: 'INTERMEDIO' },
      { name: 'Práctica Profesional Avanzada', cycle: 'AVANZADO' }
    ];
    const concurrentResponses = await Promise.all(
      concurrentPayloads.map(async p => {
        const res = await fetch(BASE_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(p)
        });
        assert.strictEqual(res.status, 200, `Concurrent creation of ${p.name} should return 200`);
        return res.json();
      })
    );
    for (const entity of concurrentResponses) {
      assert.ok(entity.id, 'Concurrent entity must have an ID');
      createdIds.push(entity.id);
    }
    console.log(`   Successfully created and verified ${concurrentResponses.length} concurrent subjects.`);

    // 8. Testing validation: null name rejection
    console.log('\n8. Testing POST validation with null name (should be 400)...');
    const resPostNullName = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cycle: 'INICIAL' })
    });
    assert.strictEqual(resPostNullName.status, 400, 'Null name should return 400 Bad Request');
    console.log('   Correctly rejected null name with status 400');

    // 9. Testing validation: invalid cycle rejection
    console.log('\n9. Testing POST validation with invalid cycle (should be 400)...');
    const resPostInvalidCycle = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Asignatura Invalida', cycle: 'CICLO_INEXISTENTE' })
    });
    assert.strictEqual(resPostInvalidCycle.status, 400, 'Invalid cycle should return 400 Bad Request');
    console.log('   Correctly rejected invalid cycle with status 400');

    // 9b. Testing ID overwrite protection: client ID must be ignored and not overwrite existing entity
    console.log('\n9b. Testing ID overwrite protection (client ID must be ignored)...');
    const targetEntityId = createdIds[0];
    const resPostWithId = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: targetEntityId, name: 'Asignatura Nueva Con ID Pasado', cycle: 'AVANZADO' })
    });
    assert.strictEqual(resPostWithId.status, 200, 'POST with client ID should succeed and generate fresh ID');
    const createdWithIgnoredId = await resPostWithId.json();
    assert.notStrictEqual(createdWithIgnoredId.id, targetEntityId, 'Created ID must not equal the passed existing ID');
    createdIds.push(createdWithIgnoredId.id);

    // Verify original entity was not modified
    const resGetOrig = await fetch(BASE_URL);
    const listOrig = await resGetOrig.json();
    const origFound = listOrig.find(i => i.id === targetEntityId);
    assert.strictEqual(origFound.name, 'Aproximacion Escolar', 'Original entity name must not be overwritten');
    console.log('   Correctly preserved existing entity and generated new ID:', createdWithIgnoredId.id);

    // 9c. Testing code length validation (> 50 chars should be 400)
    console.log('\n9c. Testing code length validation (> 50 chars should be 400)...');
    const resPostLongCode = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Asignatura Codigo Largo', cycle: 'INICIAL', code: 'C'.repeat(51) })
    });
    assert.strictEqual(resPostLongCode.status, 400, 'Code > 50 chars must return 400 Bad Request');
    console.log('   Correctly rejected code exceeding 50 chars with status 400');

    // 9d. Testing valid code persistence and trimming
    console.log('\n9d. Testing valid code persistence and trimming...');
    const resPostValidCode = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Asignatura Codigo Valido', cycle: 'INICIAL', code: '  MAT-201  ' })
    });
    assert.strictEqual(resPostValidCode.status, 200, 'Valid code should return 200');
    const entityWithCode = await resPostValidCode.json();
    assert.strictEqual(entityWithCode.code, 'MAT-201', 'Code should be trimmed');
    createdIds.push(entityWithCode.id);
    console.log('   Successfully created subject with trimmed code:', entityWithCode);

    // 9e. Testing non-existent professor validation (should be 400)
    console.log('\n9e. Testing non-existent professor validation (should be 400)...');
    const resPostBadProf = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Asignatura Prof Inexistente', cycle: 'INICIAL', professor: { id: 999999 } })
    });
    assert.strictEqual(resPostBadProf.status, 400, 'Non-existent professor must return 400 Bad Request');
    console.log('   Correctly rejected non-existent professor with status 400');

    // 10. Testing DELETE
    console.log('\n10. Testing DELETE /api/asignaturas/:id for created items...');
    for (const id of createdIds) {
      const resDel = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
      assert.strictEqual(resDel.status, 200, `DELETE ${id} should return 200 OK`);
      console.log(`   Deleted subject ID ${id}`);
    }

    // 11. Testing DELETE non-existent returns 404
    console.log('\n11. Testing DELETE non-existent ID (should be 404)...');
    const resDelNonExistent = await fetch(`${BASE_URL}/999999`, { method: 'DELETE' });
    assert.strictEqual(resDelNonExistent.status, 404, 'DELETE non-existent should return 404 Not Found');
    console.log('   Correctly returned 404 for non-existent ID');

    // 12. Final GET verification
    console.log('\n12. Verifying final GET /api/asignaturas (clean state)...');
    const resFinal = await fetch(BASE_URL);
    const listFinal = await resFinal.json();
    for (const id of createdIds) {
      assert.ok(!listFinal.some(item => item.id === id), `Entity ${id} should no longer be present`);
    }
    console.log('   All test entities confirmed removed.');

    console.log('\n========================================');
    console.log('✅ ALL 12 VERIFICATION CHECKS PASSED!');
    console.log('========================================');
  } catch (err) {
    console.error('\n❌ VERIFICATION TEST FAILED:', err);
    process.exit(1);
  }
}

runTests();
