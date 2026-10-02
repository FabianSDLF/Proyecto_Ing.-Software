## 2026-09-23T22:27:40Z

You are reviewer_m3_1, a teamwork_preview_reviewer.
Your working directory is:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/reviewer_m3_1

CRITICAL MANDATORY INSTRUCTION:
You MUST read ORIGINAL_REQUEST.md before starting work:
c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/ORIGINAL_REQUEST.md

Reference files:
- Worker Handoff: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/worker_m3_1/handoff.md
- PROJECT.md: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/PROJECT.md
- Diagnostic Report: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/reporte_codigo.md

Objective:
Perform thorough, objective code review of Phase 3 Backend changes committed to branch 'fabian':
1. Inspect `com.ubb.practicas.exception.GlobalExceptionHandler`, `ApiErrorResponse`, and custom domain exceptions. Verify proper HTTP status codes, structured error payload, and lack of information leakage.
2. Inspect all 8 REST controllers (`AuthController`, `PortfolioController`, `PracticeAssignmentController`, `PracticeCenterController`, `ProfessorController`, `RoleController`, `SubjectController`, `UserController`). Verify that redundant try-catch blocks were removed and clean DTO/entity responses are returned.
3. Inspect all 15 JPA entities in `src/main/java/com/ubb/practicas/model/`: verify Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@EqualsAndHashCode.Include` on `@Id`). Verify `@ToString.Exclude` on associations and `passwordHash`. Verify `fetch = FetchType.LAZY` on all `@ManyToOne` and `@OneToOne`.
4. Inspect Repositories in `src/main/java/com/ubb/practicas/repository/`: verify `@EntityGraph` or `JOIN FETCH` queries to eliminate N+1 query cascades.
5. Verify backend build by running `./mvnw compile` and `./mvnw test-compile` (or equivalent).
6. Issue an explicit verdict: APPROVE or REQUEST_CHANGES.
