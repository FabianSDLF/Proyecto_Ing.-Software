# BRIEFING — 2026-09-23T19:06:15Z

## Mission
Investigate and design remediation plan for Phase 1 controller authorization & BOLA/IDOR findings (BK-07, BK-04, BK-03, BK-19, BK-20) in PortfolioController and ProfessorController.

## 🔒 My Identity
- Archetype: teamwork_preview_explorer
- Roles: Explorer 2 (Milestone 1 - Phase 1: Controller Authorization, IDOR/BOLA, Robustness)
- Working directory: c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m1_ctrl_2
- Original parent: c3f760d9-58dc-4c34-b179-bc154eee62cf
- Milestone: Milestone 1 (Phase 1)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Do NOT write or edit source code files (only agent files in working directory)
- Provide exact line-by-line evidence, exact diffs/snippets for workers
- Deliver 5-component handoff report (Observation, Logic Chain, Caveats, Conclusion, Verification Method)

## Current Parent
- Conversation ID: c3f760d9-58dc-4c34-b179-bc154eee62cf
- Updated: 2026-09-23T19:00:00Z

## Investigation State
- **Explored paths**:
  - `src/main/java/com/ubb/practicas/controller/PortfolioController.java` (lines 1-225)
  - `src/main/java/com/ubb/practicas/controller/ProfessorController.java` (lines 1-46)
  - `src/main/java/com/ubb/practicas/controller/PracticeAssignmentController.java` (lines 1-80)
  - `src/main/java/com/ubb/practicas/model/` (`PracticeAssignment.java`, `PortfolioDocument.java`, `LessonPlan.java`, `Subject.java`, `User.java`, `Role.java`, `AssignmentStatus.java`)
  - `src/main/java/com/ubb/practicas/repository/` (`PracticeAssignmentRepository.java`, `PortfolioDocumentRepository.java`, `LessonPlanRepository.java`, `UserRepository.java`)
  - `src/main/java/com/ubb/practicas/security/` (`SecurityConfig.java`, `JwtAuthFilter.java`, `JwtUtil.java`)
  - `frontend/src/pages/` (`Portafolio.jsx`, `NuevaPlanificacion.jsx`, `MisEstudiantes.jsx`)
- **Key findings**:
  - BK-07: In `PortfolioController.descargarDocumento` (lines 168-190), any authenticated caller can download any student document by ID without authorization check (explicit TODO comment at line 177). Also `eliminarDocumento` (lines 192-223) allows any user to delete any document.
  - BK-04: In `PortfolioController.subirEvidencia` (lines 86-92) and `crearPlanificacion` (lines 126-130, 144), `assignments.get(0)` assumes student has exactly one assignment without checking status or active period.
  - BK-03: In `PortfolioController` lines 38, 69, 77, 86, 126, 209-215, Law of Demeter violations with deep getter chains causing severe NPE risk when `plan.getDocument()` is null.
  - BK-19 & BK-20: In `ProfessorController` line 40 has corrupted encoding `"vǭlido"` (should be `"válido"`). Lines 29-41 have hardcoded `if-else` role name checks violating OCP.
  - Critical Unregistered BOLA in `ProfessorController.obtenerEstudiantesPorProfesor` (lines 21-44): accepts `@PathVariable Long professorId` without checking if caller is that professor or an admin.
- **Unexplored areas**: None within Milestone 1 controller scope. Ready for handoff synthesis.

## Key Decisions Made
- Encapsulate ownership and participant checks in domain entities (`PracticeAssignment.hasParticipant`, `PortfolioDocument.isAccessibleBy`, `LessonPlan.isOwnedBy`) to resolve Demeter violations and avoid controller coupling.
- Introduce `TeachingRole` enum with functional lookup strategy to resolve BK-19 (OCP).
- Support optional `assignmentId` parameter while automatically resolving the active practice assignment for backward compatibility to solve BK-04.
- Provide comprehensive MockMvc test outlines to enable immediate verification.

## Artifact Index
- DISPATCH.md — Recorded dispatch instructions
- BRIEFING.md — Persistent context & state
- progress.md — Liveness & status tracking
- handoff.md — Final 5-component handoff report
