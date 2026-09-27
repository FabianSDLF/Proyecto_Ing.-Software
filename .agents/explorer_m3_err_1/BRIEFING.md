# BRIEFING — 2026-09-24T00:58:30Z

## Mission
Investigate backend error handling across all controllers and services, designing the blueprint for Milestone 3 (Clean Code, Error Handling & Technical Debt) including GlobalExceptionHandler, ApiErrorResponse, custom exceptions, and controller cleanup plan.

## 🔒 My Identity
- Archetype: teamwork_preview_explorer
- Roles: explorer
- Working directory: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_err_1
- Original parent: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Milestone: Milestone 3 (Backend Error Handling & Clean Code)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement or modify source code
- Files for content delivery, Messages for coordination
- Deliver handoff to .agents/explorer_m3_err_1/handoff.md with 5 components
- Target git branch: fabian

## Current Parent
- Conversation ID: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Updated: 2026-09-24T00:58:30Z

## Investigation State
- **Explored paths**:
  - Git repository state (.git/HEAD, .git/logs/refs/heads/fabian)
  - All 8 REST controllers (`AuthController`, `PortfolioController`, `PracticeAssignmentController`, `PracticeCenterController`, `ProfessorController`, `RoleController`, `SubjectController`, `UserController`)
  - All service interfaces and implementations (`UserService`, `UserServiceImpl`, `AuthServiceImpl`, `PortfolioServiceImpl`, `LessonPlanServiceImpl`, `PracticeAssignmentServiceImpl`, `SubjectServiceImpl`, `LocalStorageServiceImpl`, `UserBatchImportServiceImpl`)
  - Existing exceptions in `com.ubb.practicas.exception`
  - Existing controller tests (`AuthControllerTest`, `PortfolioControllerTest`, `ProfessorControllerTest`, `SubjectControllerTest`)
  - Frontend HTTP client (`frontend/src/services/apiClient.js`)
- **Key findings**:
  - Git branch verified as `fabian`, latest commit `f618de60b19ac6f4001279960132c71435e5b405` (Phase 2 completed).
  - Pervasive repetitive try-catch blocks returning unstructured raw strings, inconsistent maps, or empty bodies (BK-26).
  - `UserController` catches generic `Exception e` across 6 methods, blindly mapping business/database failures to 404 Not Found or 400 Bad Request.
  - `UserService` still declares `throws Exception` on 6 methods (BK-33).
  - Existing controller tests expect certain status codes and error messages; design preserves full compatibility while upgrading to structured JSON (`ApiErrorResponse`).
  - Frontend `apiClient.js` is already wired to parse `errorData.message` and `errorData.error` from JSON responses.
- **Unexplored areas**: None for error handling blueprint.

## Key Decisions Made
- Standardize all error responses under `ApiErrorResponse` DTO in `com.ubb.practicas.dto`.
- Create centralized `@RestControllerAdvice` in `com.ubb.practicas.exception.GlobalExceptionHandler`.
- Introduce custom unchecked exceptions hierarchy (`ResourceNotFoundException`, `BadRequestException`, `ResourceConflictException`, `UnauthorizedException`, `ForbiddenException`).
- Eliminate `throws Exception` from `UserService` and remove all redundant try-catches in controllers.

## Artifact Index
- handoff.md — Comprehensive Phase 3 error handling architecture blueprint
- progress.md — Liveness tracker
- DISPATCH.md — Agent dispatch log
