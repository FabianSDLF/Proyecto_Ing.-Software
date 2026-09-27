## 2026-09-23T19:30:41Z
You are Explorer 1 for Milestone 2 (Phase 2: Layered Architecture, SOLID Services & DIP).

## 🔒 Your Identity & Working Directory
- Identity: teamwork_preview_explorer
- Working directory: c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\explorer_m2_srv_1
- Parent Conversation ID: c3f760d9-58dc-4c34-b179-bc154eee62cf

## Scope & Objective
Investigate and design the complete backend service layer and Dependency Inversion (DIP) refactoring for Phase 2:
1. Identify all controllers currently containing business/persistence logic directly:
   - `PortfolioController`: extract to `PortfolioService` & `LessonPlanService`
   - `AuthController`: extract to `AuthService`
   - `SubjectController`: extract to `SubjectService`
   - `PracticeAssignmentController`: extract to `PracticeAssignmentService`
   - `PracticeCenterController`: extract to `PracticeCenterService`
   - `RoleController`: extract to `RoleService`
   - `UserController`: unroll direct repository queries into `UserService`
2. Formulate clean interfaces and implementations for all 7 services.
3. Design the elimination of all `@Autowired` field injections in controllers and services, replacing them with constructor injection (`private final` fields with `@RequiredArgsConstructor` or explicit constructors).
4. Ensure all Phase 1 security checks (`isAccessibleBy`, role guards) are cleanly preserved within service methods or controller guards.

## Mandatory Files to Read
- ORIGINAL_REQUEST.md: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\5_Desarrollo\Proyecto_IngSoft_Practicas\.agents\ORIGINAL_REQUEST.md`
- Diagnostic report: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\reporte_codigo.md`
- Project plan: `c:\Users\Fabián Sepúlveda\Desktop\UBB\2026-2\Ingenieria Software\Proyecto_Documentacion\PROJECT.md`
- Existing controllers in `src/main/java/com/ubb/practicas/controller/`
