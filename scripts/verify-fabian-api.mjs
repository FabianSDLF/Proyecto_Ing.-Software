// Ejecutar solo contra el backend aislado de revisión en 127.0.0.1:18080.
// Requiere una base de demostración nueva; crea y firma pautas de prueba.
import assert from 'node:assert/strict';
import fs from 'node:fs';
const base='http://127.0.0.1:18080';
const results=[];
async function call(path,{token,body,status=200,method=body?'POST':'GET'}={}) {
  const r=await fetch(base+path,{method,headers:{...(token?{Authorization:`Bearer ${token}`} : {}),...(body?{'Content-Type':'application/json'}:{})},body:body?JSON.stringify(body):undefined});
  const text=await r.text();assert.equal(r.status,status,`${path}: ${r.status} ${text.slice(0,200)}`);
  try{return JSON.parse(text);}catch{return text;}
}
const login=async(email,password)=>(await call('/api/auth/login',{body:{email,password}})).token;
const admin=await login('admin@ubb.cl','admin123');
const all=await call('/api/asignaciones/visibles?history=true',{token:admin});
const a=all.find(x=>x.subject?.professionalRubrics);assert.ok(a,'Asignatura profesional configurada');
const collab=await login(a.collaborator.email,'colab123');
const tutor=await login(a.tutor.email,'tutor123');
const student=await login(a.student.email,a.student.email==='estudiante3@ubb.cl'?'estudiante3123':a.student.email==='estudiante2@ubb.cl'?'estudiante2123':'estudiante123');
const definitions=[...await call('/api/evaluaciones/pautas?assignmentId='+a.id,{token:collab}),...await call('/api/evaluaciones/pautas?assignmentId='+a.id,{token:tutor})];
assert.equal(definitions.length,8);results.push('Catálogo de ocho pautas por participante y asignatura');
const nonProfessional=all.find(x=>!x.subject?.professionalRubrics && x.collaborator?.email===a.collaborator.email);
if(nonProfessional) {
  assert.deepEqual(await call('/api/evaluaciones/pautas?assignmentId='+nonProfessional.id,{token:collab}),[]);
  await call('/api/evaluaciones',{token:collab,body:{practiceAssignmentId:nonProfessional.id,type:'COLAB_ESP_1',rubricVersion:'2025-2-v1',criteria:[]},status:400});
}
const created=[];
for(const def of definitions) {
  const token=def.type.startsWith('TUTOR_')?tutor:collab;
  const payload={practiceAssignmentId:a.id,type:def.type,rubricVersion:def.rubricVersion,score:7,criteria:[{criterionCode:'A1',points:2}],qualitativeComments:'Prueba aislada HU44'};
  const existing=(await call('/api/evaluaciones/asignacion/'+a.id,{token})).find(e=>e.type===def.type);
  assert.ok(!existing?.isFinalized, 'Utiliza una base de prueba nueva: esta pauta ya fue publicada');
  if(existing) payload.expectedRevision=existing.revision;
  const draft=await call('/api/evaluaciones',{token,body:payload});
  assert.equal(draft.score,null);assert.equal(draft.totalPoints,2);
  const read=await call('/api/evaluaciones/'+draft.id,{token});assert.equal(read.criteria[0].points,2);
  await call('/api/evaluaciones/'+draft.id,{token:student,status:403});
  await call('/api/evaluaciones',{token,body:{...payload,expectedRevision:draft.revision,criteria:[{criterionCode:'A1',points:2.5}]},status:400});
  await call('/api/evaluaciones',{token,body:{...payload,expectedRevision:draft.revision,criteria:[{criterionCode:'A1',points:null},{criterionCode:'A1',points:2}]},status:400});
  const complete=await call('/api/evaluaciones',{token,body:{...payload,expectedRevision:draft.revision,criteria:def.indicators.map(i=>({criterionCode:i.criterionCode,points:2}))}});
  assert.equal(complete.score,3);assert.equal(complete.totalPoints,def.indicators.length*2);
  created.push({def,token,payload,evaluation:complete});
}
results.push('Ocho pautas: guardar, recargar, completar, nota 3.0, rechazar decimales/duplicados y privacidad');
const first=created[0];
const concurrent=await Promise.all([1,2].map(async n=>{
  const response=await fetch(base+'/api/evaluaciones',{method:'POST',headers:{Authorization:`Bearer ${first.token}`,'Content-Type':'application/json'},body:JSON.stringify({...first.payload,expectedRevision:first.evaluation.revision,qualitativeComments:`Sesión ${n}`,criteria:first.def.indicators.map(i=>({criterionCode:i.criterionCode,points:2}))})});
  return response.status;
}));
console.log('Guardados simultáneos:',concurrent);
assert.deepEqual(concurrent.sort(),[200,409]);results.push('Concurrencia de borradores: un guardado y un conflicto');
const latest=await call('/api/evaluaciones/'+first.evaluation.id,{token:first.token});
const closes=await Promise.all([1,2].map(async()=>{
  const r=await fetch(base+`/api/evaluaciones/${latest.id}/finalizar`,{method:'POST',headers:{Authorization:`Bearer ${first.token}`,'Content-Type':'application/json'},body:JSON.stringify({expectedRevision:latest.revision,confirmed:true})});return r.status;
}));
console.log('Firmas simultáneas:',closes);assert.deepEqual(closes.sort(),[200,409]);
const published=await call('/api/evaluaciones/'+latest.id,{token:student});assert.equal(published.score,3);assert.equal(published.isFinalized,true);assert.match(published.snapshotHash,/^[a-f0-9]{64}$/);
results.push('Firma/publicación integrada, consulta del estudiante, cierre único e instantánea SHA256');
await call('/api/reportes/estadisticas',{token:tutor,status:403});
await call('/api/reportes/estadisticas?cycle=DESCONOCIDO',{token:admin,status:400});
const stats=await call('/api/reportes/estadisticas?history=true&subjectId='+a.subject.id,{token:admin});
assert.equal(stats.assignments,all.filter(x=>x.subject.id===a.subject.id).length);
assert.equal(stats.configuredInstruments,stats.assignments*8);assert.equal(stats.publishedEvaluations,1);
assert.equal(stats.pendingEvaluations,stats.configuredInstruments-1);assert.equal(stats.gradesByInstrument[0].mean,3);
results.push('Estadísticas institucionales: alcance, muestra, pendiente y solo notas publicadas');
const csv=await call('/api/portafolio/export-csv?history=true&subjectId='+a.subject.id,{token:admin});assert.ok(csv.includes(a.student.fullName));
const none=await call('/api/portafolio/export-csv?history=true&subjectId='+a.subject.id+'&cycle=INICIAL',{token:admin});assert.ok(!none.includes(a.student.fullName));
results.push('CSV respeta historial, ciclo y asignatura');
const contact=await call('/api/ayuda/contacto',{token:student});assert.equal(contact.configured,false);results.push('Ayuda sin contacto configurado no inventa un correo');
fs.mkdirSync('.verification',{recursive:true});
fs.writeFileSync('.verification/fabian-api-results.json',JSON.stringify({passed:results.length,checks:results},null,2));
console.log(JSON.stringify({passed:results.length,checks:results},null,2));
