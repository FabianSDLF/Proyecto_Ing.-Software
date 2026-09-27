# BRIEFING — 2026-09-24T00:57:30Z

## Mission
Investigate all 15 JPA entities, repositories, and relationships for Phase 3 (Clean Code, Error Handling & Technical Debt): Lombok annotations, FetchTypes, N+1 patterns, query optimization, and table/column mappings.

## 🔒 My Identity
- Archetype: explorer
- Roles: teamwork_preview_explorer
- Working directory: c:/Users/Fabián Sepúlveda/Desktop/UBB/2026-2/Ingenieria Software/Proyecto_Documentacion/5_Desarrollo/Proyecto_IngSoft_Practicas/.agents/explorer_m3_jpa_2
- Original parent: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Milestone: Phase 3 (Clean Code, Error Handling & Technical Debt)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Detailed inspection of JPA entities, repositories, Lombok annotations, FetchType, EntityGraph/JOIN FETCH, and DB naming/indexes

## Current Parent
- Conversation ID: 403313ad-9e49-4fa6-96a7-fe91e61771bb
- Updated: 2026-09-24T00:57:30Z

## Investigation State
- **Explored paths**:
  - `src/main/java/com/ubb/practicas/model/` (all 15 entity files)
  - `src/main/java/com/ubb/practicas/repository/` (all 15 repository interfaces)
  - `src/main/java/com/ubb/practicas/service/impl/` (`PortfolioServiceImpl`, `LessonPlanServiceImpl`, `PracticeAssignmentServiceImpl`, `UserServiceImpl`, `AuthServiceImpl`)
  - `src/main/java/com/ubb/practicas/security/` (`JwtAuthFilter.java`)
  - `reporte_codigo.md` (BK-29 to BK-48, TR-12)
  - `PROJECT.md`, `ORIGINAL_REQUEST.md`, `application.properties`
- **Key findings**:
  - 100% of 15 entities use `@Data` (BK-36). `LessonPlan.parentPlan` risks `StackOverflowError` (BK-37); `User.passwordHash` risks leaking in logs. Replaced with `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString(exclude=...)`, and `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on `@Id`.
  - 16 of 19 relationships defaulted to `FetchType.EAGER`, causing severe N+1 query storms (TR-12). All converted to `FetchType.LAZY`.
  - Repositories lack `@EntityGraph` and `JOIN FETCH`. Detailed blueprint designed for `PracticeAssignmentRepository`, `PortfolioDocumentRepository`, `LessonPlanRepository`, `UserRepository`, `SubjectRepository`, `RoleRepository` to fetch required association graphs in a single roundtrip.
  - Inconsistent schema table and column naming (BK-38) and missing database indexes cataloged and resolved.
- **Unexplored areas**: None within JPA scope.

## Key Decisions Made
- Concluded investigation and drafted complete actionable blueprint in `handoff.md`.

## Artifact Index
- handoff.md — Comprehensive JPA, Entity, and Repository analysis report
