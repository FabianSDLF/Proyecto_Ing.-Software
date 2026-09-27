# Handoff Report — Explorer 2 (Milestone 1, Phase 1)
**Scope:** Controller Authorization, BOLA/IDOR Mitigations, Unsafe Index Assumptions & Code Quality in `PortfolioController` and `ProfessorController`.
**Author:** Explorer 2 (`teamwork_preview_explorer`)
**Target Worker / Next Agent:** Milestone 1 Worker Agent & Sentinel
**Working Directory:** `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_ctrl_2`

---

## 1. Observation

### 1.1. BK-07: IDOR/BOLA in `PortfolioController.descargarDocumento` & `eliminarDocumento`
- **File:** `src/main/java/com/ubb/practicas/controller/PortfolioController.java`
- **Lines 167-190:**
```java
167:     @GetMapping("/documentos/{id}/descargar")
168:     public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
169:         if (principal == null) return ResponseEntity.status(401).build();
170: 
171:         Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
172:         if (docOpt.isEmpty()) {
173:             return ResponseEntity.notFound().build();
174:         }
175: 
176:         PortfolioDocument doc = docOpt.get();
177:         // Nota de seguridad: aquí se debería validar que el principal tenga permiso para ver este documento
178:         
179:         Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());
180:         
181:         String contentType = doc.getContentType();
182:         if (contentType == null) {
183:             contentType = "application/octet-stream";
184:         }
185: 
186:         return ResponseEntity.ok()
187:                 .contentType(MediaType.parseMediaType(contentType))
188:                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
189:                 .body(resource);
190:     }
```
Line 177 contains the verbatim comment: `// Nota de seguridad: aquí se debería validar que el principal tenga permiso para ver este documento`. Any authenticated user can download any document in the system by supplying its numerical ID.

- **Lines 192-205:** In `eliminarDocumento`:
```java
192:     @DeleteMapping("/documentos/{id}")
193:     public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
194:         if (principal == null) return ResponseEntity.status(401).build();
195: 
196:         Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
197:         if (docOpt.isEmpty()) {
198:             return ResponseEntity.notFound().build();
199:         }
200: 
201:         PortfolioDocument doc = docOpt.get();
202:         
203:         try {
204:             fileStorageService.deleteFile(doc.getStoredFileName());
```
Any authenticated user can delete any other student's uploaded document and underlying physical file without authorization checks.

---

### 1.2. BK-04 & BK-03: Unsafe Index 0 Assumptions and Law of Demeter in `PortfolioController`
- **File:** `src/main/java/com/ubb/practicas/controller/PortfolioController.java`
- **Lines 86-93 (`subirEvidencia`):**
```java
86:             List<PracticeAssignment> assignments = practiceAssignmentRepository.findByStudentEmail(principal.getName());
87:             if (assignments.isEmpty()) return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
...
92:             doc.setPracticeAssignment(assignments.get(0));
```
- **Lines 126-130, 144 (`crearPlanificacion`):**
```java
126:             List<PracticeAssignment> assignments = practiceAssignmentRepository.findByStudentEmail(principal.getName());
127:             if (assignments.isEmpty()) return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
...
130:             plan.setPracticeAssignment(assignments.get(0));
...
144:                     doc.setPracticeAssignment(assignments.get(0));
```
`practiceAssignmentRepository.findByStudentEmail(principal.getName())` returns all assignments (historical, finalized, cancelled, active). `assignments.get(0)` arbitrarily binds uploaded evidence/plans to whichever row MySQL happens to return first. Furthermore, if a student only has `FINALIZADA` assignments, `.isEmpty()` is `false`, permitting uploads to inactive assignments while claiming in line 87/127 to check for an active assignment.

- **Line 38 (`eliminarPlanificacion` - Demeter violation):**
```java
38:         if (!plan.getPracticeAssignment().getStudent().getEmail().equals(principal.getName())) {
39:             return ResponseEntity.status(403).build();
40:         }
```
4 dereference levels (`plan -> practiceAssignment -> student -> email`). Throws `NullPointerException` if `practiceAssignment` or `student` is null.

- **Lines 209-215 (`eliminarDocumento` - Demeter & BK-08 NPE risk):**
```java
209:                 List<LessonPlan> plans = lessonPlanRepository.findByPracticeAssignment_Student_Email(principal.getName());
210:                 for (LessonPlan plan : plans) {
211:                     if (plan.getDocument().getId().equals(doc.getId())) {
212:                         lessonPlanRepository.delete(plan);
213:                         break;
214:                     }
215:                 }
```
If a student submitted a plan without an attached file, `plan.getDocument()` is `null`. In line 211, `plan.getDocument().getId()` triggers a fatal `NullPointerException`. In addition, doing an in-memory scan over all plans violates Clean Code and SRP.

---

### 1.3. BK-19, BK-20, BK-21 & BOLA in `ProfessorController`
- **File:** `src/main/java/com/ubb/practicas/controller/ProfessorController.java`
- **Lines 1-46 (entire file):**
```java
1: package com.ubb.practicas.controller;
2: 
3: import com.ubb.practicas.model.PracticeAssignment;
4: import com.ubb.practicas.repository.PracticeAssignmentRepository;
5: import org.springframework.beans.factory.annotation.Autowired;
6: import org.springframework.web.bind.annotation.*;
7: 
8: import java.util.List;
9: 
10: @RestController
11: @CrossOrigin(origins = "http://localhost:5173")
12: @RequestMapping("/api/profesor")
13: public class ProfessorController {
14: 
15:     @Autowired
16:     private PracticeAssignmentRepository practiceAssignmentRepository;
17: 
18:     @Autowired
19:     private com.ubb.practicas.repository.UserRepository userRepository;
20: 
21:     @GetMapping("/{professorId}/estudiantes")
22:     public org.springframework.http.ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId) {
23:         com.ubb.practicas.model.User user = userRepository.findById(professorId).orElse(null);
24:         
25:         if (user == null || user.getRole() == null) {
26:             return org.springframework.http.ResponseEntity.badRequest().body("Usuario no encontrado");
27:         }
28:         
29:         String roleName = user.getRole().getName();
30:         List<PracticeAssignment> assignments;
31:         
32:         if ("Profesor de Asignatura".equals(roleName)) {
33:             assignments = practiceAssignmentRepository.findBySubjectProfessorId(professorId);
34:         } else if ("Profesor Colaborador".equals(roleName)) {
35:             assignments = practiceAssignmentRepository.findByCollaboratorId(professorId);
36:         } else if ("Tutor".equals(roleName)) {
37:             assignments = practiceAssignmentRepository.findByTutorId(professorId);
38:         } else {
39:             return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
40:                     .body("El usuario no tiene un rol docente vǭlido");
41:         }
42:         
43:         return org.springframework.http.ResponseEntity.ok(assignments);
44:     }
45: }
```
- **BK-20 (Line 40):** `"El usuario no tiene un rol docente vǭlido"` contains corrupt character `ǭ` (`\u01ed`).
- **BK-19 (Lines 29-41):** Rigid string comparison chain `if ("Profesor de Asignatura".equals(roleName)) ... else if ("Profesor Colaborador".equals(roleName)) ... else if ("Tutor".equals(roleName))` violates the Open/Closed Principle (OCP).
- **BK-21 (Lines 19, 22, 23, 39, 43):** Fully qualified class names (`com.ubb.practicas.model.User`, `org.springframework.http.ResponseEntity<?>`, `org.springframework.http.HttpStatus.FORBIDDEN`) clutter method signatures instead of using imports.
- **Unregistered Critical Security Defect (BOLA/IDOR):** Line 22 lacks `Principal principal` or `Authentication`. Any logged-in user can query `GET /api/profesor/{professorId}/estudiantes` for any arbitrary `professorId` to dump all student personal information and assignments.
- **Line 11:** Hardcoded redundant `@CrossOrigin(origins = "http://localhost:5173")`.

---

## 2. Logic Chain

1. **BK-07 Authorization Rule:**
   - In `PortfolioDocument`, documents are tied to a `PracticeAssignment`.
   - A `PracticeAssignment` defines the complete ecosystem for a student's practice: `student` (User), `subject.professor` (User), `collaborator` (User), `tutor` (User).
   - Therefore, a document may only be viewed or downloaded if the caller matches one of those four participants OR has the system role `Administrador`.
   - Attempted access by any other user represents an IDOR/BOLA attack and must be met with HTTP 403 Forbidden.
   - By encapsulating `isAccessibleBy(email)` in `PortfolioDocument` and `hasParticipant(email)` in `PracticeAssignment`, the controller delegates business rules to the domain and avoids train wrecks (BK-03).

2. **BK-04 Active Assignment Resolution:**
   - A student may have multiple assignments across semesters (e.g. `PRC-100` Finalizada, `PRC-200` Activa).
   - Unconditionally selecting `assignments.get(0)` corrupts historical integrity and allows submissions to inactive practices.
   - Solution: Query `findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA)` in `PracticeAssignmentRepository`.
   - For backwards compatibility with `Portafolio.jsx` and `NuevaPlanificacion.jsx` (which do not yet transmit `assignmentId`), the backend resolves the student's active assignment automatically, while accepting an optional `@RequestParam(value = "assignmentId", required = false)` for API clients that specify the assignment explicitly.

3. **BK-03 Law of Demeter & BK-08 NPE Elimination:**
   - `plan.getPracticeAssignment().getStudent().getEmail().equals(principal.getName())` is replaced with `plan.isOwnedBy(userEmail)`.
   - In `eliminarDocumento`, the in-memory iteration `for (LessonPlan plan : plans) { if (plan.getDocument().getId().equals(doc.getId())) ... }` will throw NPE whenever a plan has no attachment (`plan.getDocument() == null`).
   - Solution: Introduce `Optional<LessonPlan> findByDocumentId(Long documentId)` in `LessonPlanRepository`. Deleting becomes a clean, one-line database-driven operation: `lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete);`.

4. **BK-19 & BK-20 Teaching Role Strategy & Encoding:**
   - Introducing enum `TeachingRole` with a functional fetcher `BiFunction<PracticeAssignmentRepository, Long, List<PracticeAssignment>>` encapsulates role-to-query mapping cleanly.
   - `TeachingRole.fromRoleName(roleName)` replaces the brittle `if-else` chain, adhering strictly to OCP.
   - Correcting `"vǭlido"` to `"válido"` fixes character encoding corruption.
   - Adding `Principal principal` and verifying `caller.getId().equals(professorId) || callerIsAdmin` eliminates the IDOR vulnerability in `ProfessorController`.

---

## 3. Caveats

- **Frontend Impact:** The frontend does not currently provide `assignmentId` in `Portafolio.jsx` or `NuevaPlanificacion.jsx`. Therefore, the backend must keep `assignmentId` optional and fall back to finding the active practice assignment.
- **Phase 2 Services Transition:** A full transition of `PortfolioController` to `PortfolioService` and `LessonPlanService` is slated for Phase 2 (P1). In Phase 1 (Critical Security & DLP), we implement domain helper methods and repository queries to immediately patch BOLA, Demeter, and Index 0 bugs without risking regressions.
- **Admin Privileges:** Administrators are granted overarching read and delete privileges across all endpoints to ensure administrative oversight and maintenance capabilities.

---

## 4. Conclusion & Concrete Code Remediation Plan

The Worker agent must apply the following concrete changes:

### 4.1. Domain Model Changes

#### A. In `src/main/java/com/ubb/practicas/model/PracticeAssignment.java`
Add domain participant verification methods:
```java
    public boolean isStudent(String email) {
        return email != null && this.student != null && email.equalsIgnoreCase(this.student.getEmail());
    }

    public boolean isProfessor(String email) {
        return email != null && this.subject != null && this.subject.getProfessor() != null 
                && email.equalsIgnoreCase(this.subject.getProfessor().getEmail());
    }

    public boolean isCollaborator(String email) {
        return email != null && this.collaborator != null && email.equalsIgnoreCase(this.collaborator.getEmail());
    }

    public boolean isTutor(String email) {
        return email != null && this.tutor != null && email.equalsIgnoreCase(this.tutor.getEmail());
    }

    public boolean hasParticipant(String email) {
        if (email == null) return false;
        return isStudent(email) || isProfessor(email) || isCollaborator(email) || isTutor(email);
    }
```

#### B. In `src/main/java/com/ubb/practicas/model/PortfolioDocument.java`
Add ownership and accessibility methods:
```java
    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }

    public boolean isAccessibleBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.hasParticipant(email);
    }
```

#### C. In `src/main/java/com/ubb/practicas/model/LessonPlan.java`
Add ownership verification:
```java
    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }

    public boolean hasDocument(Long documentId) {
        return documentId != null && this.document != null && documentId.equals(this.document.getId());
    }
```

---

### 4.2. Repository Interface Additions

#### A. In `src/main/java/com/ubb/practicas/repository/PracticeAssignmentRepository.java`
Add query method for active assignments:
```java
    List<PracticeAssignment> findByStudentEmailAndStatus(String email, com.ubb.practicas.model.enums.AssignmentStatus status);
```

#### B. In `src/main/java/com/ubb/practicas/repository/LessonPlanRepository.java`
Add query method to locate plans by document ID:
```java
    java.util.Optional<LessonPlan> findByDocumentId(Long documentId);
```

#### C. In `src/main/java/com/ubb/practicas/repository/PortfolioDocumentRepository.java`
Add query method for document types:
```java
    List<PortfolioDocument> findByPracticeAssignment_Student_EmailAndType(String email, com.ubb.practicas.model.enums.DocumentType type);
```

---

### 4.3. New Enum: `TeachingRole` (Solves BK-19)
Create new file `src/main/java/com/ubb/practicas/model/enums/TeachingRole.java`:
```java
package com.ubb.practicas.model.enums;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PracticeAssignmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public enum TeachingRole {
    PROFESOR_ASIGNATURA("Profesor de Asignatura", PracticeAssignmentRepository::findBySubjectProfessorId),
    PROFESOR_COLABORADOR("Profesor Colaborador", PracticeAssignmentRepository::findByCollaboratorId),
    TUTOR("Tutor", PracticeAssignmentRepository::findByTutorId);

    private final String roleName;
    private final BiFunction<PracticeAssignmentRepository, Long, List<PracticeAssignment>> assignmentFetcher;

    TeachingRole(String roleName, BiFunction<PracticeAssignmentRepository, Long, List<PracticeAssignment>> assignmentFetcher) {
        this.roleName = roleName;
        this.assignmentFetcher = assignmentFetcher;
    }

    public String getRoleName() {
        return roleName;
    }

    public static Optional<TeachingRole> fromRoleName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.roleName.equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public List<PracticeAssignment> findAssignments(PracticeAssignmentRepository repository, Long professorId) {
        return this.assignmentFetcher.apply(repository, professorId);
    }
}
```

---

### 4.4. Full Replacement for `ProfessorController.java`
Replace `src/main/java/com/ubb/practicas/controller/ProfessorController.java` with:
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.TeachingRole;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profesor")
public class ProfessorController {

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{professorId}/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesPorProfesor(@PathVariable Long professorId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User targetProfessor = userRepository.findById(professorId).orElse(null);
        if (targetProfessor == null || targetProfessor.getRole() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        User caller = userRepository.findByEmail(principal.getName());
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = caller.getRole() != null && "Administrador".equalsIgnoreCase(caller.getRole().getName());
        boolean isSelf = caller.getId() != null && caller.getId().equals(professorId);

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado: no tiene permisos para consultar los estudiantes de este docente");
        }

        Optional<TeachingRole> teachingRole = TeachingRole.fromRoleName(targetProfessor.getRole().getName());
        if (teachingRole.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El usuario no tiene un rol docente válido");
        }

        List<PracticeAssignment> assignments = teachingRole.get().findAssignments(practiceAssignmentRepository, professorId);
        return ResponseEntity.ok(assignments);
    }
}
```

---

### 4.5. Full Replacement for `PortfolioController.java`
Replace `src/main/java/com/ubb/practicas/controller/PortfolioController.java` with:
```java
package com.ubb.practicas.controller;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.DocumentType;
import com.ubb.practicas.model.enums.MathAxis;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.LessonPlanRepository;
import com.ubb.practicas.repository.PortfolioDocumentRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portafolio")
public class PortfolioController {

    @Autowired
    private PortfolioDocumentRepository portfolioDocumentRepository;

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Autowired
    private LessonPlanRepository lessonPlanRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    // --- Helper Methods ---

    private PracticeAssignment resolveAssignment(String email, Long explicitAssignmentId) {
        if (explicitAssignmentId != null) {
            Optional<PracticeAssignment> opt = practiceAssignmentRepository.findById(explicitAssignmentId);
            if (opt.isPresent()) {
                PracticeAssignment a = opt.get();
                if (a.isStudent(email) && a.getStatus() == AssignmentStatus.ACTIVA) {
                    return a;
                }
            }
            return null;
        }

        List<PracticeAssignment> activeAssignments = practiceAssignmentRepository.findByStudentEmailAndStatus(email, AssignmentStatus.ACTIVA);
        if (activeAssignments.isEmpty()) {
            return null;
        }
        return activeAssignments.stream()
                .max((a1, a2) -> Long.compare(a1.getId() != null ? a1.getId() : 0L, a2.getId() != null ? a2.getId() : 0L))
                .orElse(activeAssignments.get(0));
    }

    private boolean isUserAdmin(String email) {
        User user = userRepository.findByEmail(email);
        return user != null && user.getRole() != null && "Administrador".equalsIgnoreCase(user.getRole().getName());
    }

    // --- Endpoints ---

    @GetMapping("/evidencias")
    public ResponseEntity<?> obtenerMisEvidencias(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<PortfolioDocument> documentos = portfolioDocumentRepository.findByPracticeAssignment_Student_EmailAndType(
                principal.getName(), DocumentType.EVIDENCIA_APRENDIZAJE
        );
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/planificaciones")
    public ResponseEntity<?> obtenerMisPlanificaciones(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<LessonPlan> planificaciones = lessonPlanRepository.findByPracticeAssignment_Student_Email(principal.getName());
        return ResponseEntity.ok(planificaciones);
    }

    @GetMapping("/documentos/{id}/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !doc.isAccessibleBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());
            String contentType = doc.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/evidencias/upload")
    public ResponseEntity<?> subirEvidencia(
            Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            PracticeAssignment assignment = resolveAssignment(principal.getName(), assignmentId);
            if (assignment == null) {
                return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
            }

            String storedFileName = fileStorageService.storeFile(file);

            PortfolioDocument doc = new PortfolioDocument();
            doc.setPracticeAssignment(assignment);
            doc.setType(DocumentType.EVIDENCIA_APRENDIZAJE);
            doc.setFileName(file.getOriginalFilename());
            doc.setStoredFileName(storedFileName);
            doc.setContentType(file.getContentType());
            doc.setSizeBytes(file.getSize());
            doc.setUploadDate(LocalDateTime.now());

            portfolioDocumentRepository.save(doc);

            doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
            portfolioDocumentRepository.save(doc);

            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al subir archivo: " + e.getMessage());
        }
    }

    @PostMapping("/planificaciones/upload")
    public ResponseEntity<?> crearPlanificacion(
            Principal principal,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam("schoolGrade") String schoolGrade,
            @RequestParam("mathAxis") String mathAxis,
            @RequestParam("objective") String objective,
            @RequestParam(value = "specificObjectives", required = false) String specificObjectives,
            @RequestParam(value = "classActivities", required = false) String classActivities) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            PracticeAssignment assignment = resolveAssignment(principal.getName(), assignmentId);
            if (assignment == null) {
                return ResponseEntity.badRequest().body("No tienes una asignación de práctica activa.");
            }

            LessonPlan plan = new LessonPlan();
            plan.setPracticeAssignment(assignment);
            plan.setStatus(PlanStatus.BORRADOR);
            plan.setSchoolGrade(schoolGrade);
            plan.setMathAxis(MathAxis.valueOf(mathAxis));
            plan.setBaseLearningObjective(objective);
            plan.setObjectives(specificObjectives);
            plan.setContentJson(classActivities);
            plan.setCreatedAt(LocalDateTime.now());
            plan.setUpdatedAt(LocalDateTime.now());

            if (file != null && !file.isEmpty() && file.getSize() > 0) {
                if (!"planificacion_sin_archivo.txt".equals(file.getOriginalFilename())) {
                    String storedFileName = fileStorageService.storeFile(file);
                    PortfolioDocument doc = new PortfolioDocument();
                    doc.setPracticeAssignment(assignment);
                    doc.setType(DocumentType.PLANIFICACION);
                    doc.setFileName(file.getOriginalFilename());
                    doc.setStoredFileName(storedFileName);
                    doc.setContentType(file.getContentType());
                    doc.setSizeBytes(file.getSize());
                    doc.setUploadDate(LocalDateTime.now());

                    portfolioDocumentRepository.save(doc);
                    doc.setFileUrl("/api/portafolio/documentos/" + doc.getId() + "/descargar");
                    portfolioDocumentRepository.save(doc);

                    plan.setDocument(doc);
                }
            }

            lessonPlanRepository.save(plan);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/planificaciones/{id}")
    public ResponseEntity<?> eliminarPlanificacion(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<LessonPlan> planOpt = lessonPlanRepository.findById(id);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LessonPlan plan = planOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !plan.isOwnedBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            if (plan.getDocument() != null) {
                fileStorageService.deleteFile(plan.getDocument().getStoredFileName());
            }
            lessonPlanRepository.delete(plan);
            return ResponseEntity.ok("Planificación eliminada");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar planificación: " + e.getMessage());
        }
    }

    @DeleteMapping("/documentos/{id}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<PortfolioDocument> docOpt = portfolioDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortfolioDocument doc = docOpt.get();
        String userEmail = principal.getName();
        boolean isAdmin = isUserAdmin(userEmail);

        if (!isAdmin && !doc.isOwnedBy(userEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            fileStorageService.deleteFile(doc.getStoredFileName());

            // Si es planificación, eliminar la entidad LessonPlan vinculada de manera segura
            if (doc.getType() == DocumentType.PLANIFICACION) {
                lessonPlanRepository.findByDocumentId(doc.getId()).ifPresent(lessonPlanRepository::delete);
            }

            portfolioDocumentRepository.delete(doc);
            return ResponseEntity.ok("Documento eliminado");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar documento: " + e.getMessage());
        }
    }
}
```

---

## 5. Verification Method

### 5.1. Compilation Verification
Execute from project root (`Proyecto_IngSoft_Practicas`):
```powershell
.\mvnw.cmd clean compile -DskipTests
```
Expected result: `BUILD SUCCESS` with zero compilation warnings or errors.

### 5.2. Automated Controller Unit Tests
The Worker should add `src/test/java/com/ubb/practicas/controller/PortfolioControllerTest.java` and `src/test/java/com/ubb/practicas/controller/ProfessorControllerTest.java` to test the exact security contracts:

1. **PortfolioController Security Tests:**
   - Call `GET /api/portafolio/documentos/{id}/descargar` with Mock Principal matching the student owner -> Expect `200 OK`.
   - Call `GET /api/portafolio/documentos/{id}/descargar` with Mock Principal matching the subject professor -> Expect `200 OK`.
   - Call `GET /api/portafolio/documentos/{id}/descargar` with Mock Principal with role `Administrador` -> Expect `200 OK`.
   - Call `GET /api/portafolio/documentos/{id}/descargar` with Mock Principal of an unrelated student -> Expect `403 Forbidden` (**Verifies BK-07**).
   - Call `DELETE /api/portafolio/documentos/{id}` with unrelated user -> Expect `403 Forbidden`.
   - Call `DELETE /api/portafolio/documentos/{id}` for a plan that has no document attachment (`null`) -> Expect `200 OK` and ensure NO `NullPointerException` occurs (**Verifies BK-08 & BK-03**).
   - Call `POST /api/portafolio/evidencias/upload` when user has multiple assignments (one `FINALIZADA` and one `ACTIVA`) -> Verify that the document binds to the `ACTIVA` assignment (**Verifies BK-04**).

2. **ProfessorController Security & Robustness Tests:**
   - Call `GET /api/profesor/{id}/estudiantes` where caller ID != `professorId` and role != Admin -> Expect `403 Forbidden` (**Verifies IDOR mitigation**).
   - Call `GET /api/profesor/{id}/estudiantes` where target user has non-teaching role -> Expect `403 Forbidden` with body `"El usuario no tiene un rol docente válido"` containing character `á` (**Verifies BK-20 & BK-19**).
   - Call `GET /api/profesor/{id}/estudiantes` as the assigned professor -> Expect `200 OK` with list of assignments.

Run tests:
```powershell
.\mvnw.cmd test
```

### 5.3. Invalidation Conditions
The fix would be invalidated if:
- An unassociated student can download another student's portfolio document (returns 200 instead of 403).
- An unauthorized user can delete another student's document (returns 200 instead of 403).
- An assignment with status `FINALIZADA` is bound to a new upload when an `ACTIVA` assignment exists.
- Deleting a document linked to a plan without an attachment crashes with `NullPointerException`.
- `ProfessorController` returns `vǭlido` with corrupted characters or throws on non-matching literal strings.
