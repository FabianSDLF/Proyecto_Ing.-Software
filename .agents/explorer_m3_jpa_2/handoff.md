# Informe de Investigación Técnica: Arquitectura de Persistencia JPA, Entidades y Repositorios (Fase 3)

**Agente:** `explorer_m3_jpa_2`  
**Rol:** teamwork_preview_explorer (Investigación y Diagnóstico de Persistencia JPA)  
**Fecha:** 2026-09-24  
**Rama:** `fabian`  
**Objetivo:** Diagnóstico exhaustivo de las 15 entidades JPA, relaciones, anotaciones Lombok, estrategias de carga (FetchType), mitigación del problema de consultas N+1 con `@EntityGraph`/`JOIN FETCH`, normalización del esquema relacional (nomenclatura) e indexación.

---

## 1. Observation (Observaciones Directas de Código)

A través de la inspección directa de los 15 archivos de entidad en `src/main/java/com/ubb/practicas/model/` y los 15 repositorios en `src/main/java/com/ubb/practicas/repository/`, se verificaron los siguientes hechos empíricos:

### 1.1. Inventario Global de Entidades JPA (15 Modelos)

| N° | Entidad | Archivo | Tabla Actual (`@Table`) | Clave Primaria | Relaciones Existentes |
|:--:|---|---|---|---|---|
| 1 | `AcademicPeriod` | `model/AcademicPeriod.java` | `"Periodos_Academicos"` | `Long id` | Ninguna |
| 2 | `ChatbotLog` | `model/ChatbotLog.java` | `"Registros_Chatbot"` | `Long id` | 1 `@ManyToOne` (`LessonPlan`) |
| 3 | `ChatbotMessage` | `model/ChatbotMessage.java` | `"Mensajes_Chatbot"` | `Long id` | 1 `@ManyToOne` (`ChatbotLog`) |
| 4 | `Evaluation` | `model/Evaluation.java` | `"Evaluaciones"` | `Long id` | 2 `@ManyToOne` (`PracticeAssignment`, `User evaluator`) |
| 5 | `EvaluationCriteria` | `model/EvaluationCriteria.java` | `"Criterios_Evaluacion"` | `Long id` | 1 `@ManyToOne` (`Evaluation`) |
| 6 | `LessonPlan` | `model/LessonPlan.java` | `"Planificaciones"` | `Long id` | 2 `@ManyToOne` (`PracticeAssignment`, `LessonPlan parentPlan`), 1 `@OneToOne` (`PortfolioDocument`) |
| 7 | `Notification` | `model/Notification.java` | `"Notificaciones"` | `Long id` | 1 `@ManyToOne` (`User targetUser`) |
| 8 | `PasswordResetToken` | `model/PasswordResetToken.java` | `"Password_Reset_Tokens"` | `Long id` | 1 `@ManyToOne` (`User`) |
| 9 | `PortfolioDocument` | `model/PortfolioDocument.java` | `"Documentos"` | `Long id` | 1 `@ManyToOne` (`PracticeAssignment`) |
| 10 | `PracticeAssignment` | `model/PracticeAssignment.java` | `"Asignacion_Practicas"` | `Long id` | 6 `@ManyToOne` (`User student`, `Subject subject`, `PracticeCenter center`, `User collaborator`, `User tutor`, `AcademicPeriod period`) |
| 11 | `PracticeCenter` | `model/PracticeCenter.java` | `"Centro_Practicas"` | `Long id` | Ninguna |
| 12 | `Role` | `model/Role.java` | `"roles"` | `Long id` | Ninguna |
| 13 | `StudentPair` | `model/StudentPair.java` | `"Duplas"` | `Long id` | 3 `@ManyToOne` (`PracticeAssignment assignment1`, `assignment2`, `AcademicPeriod period`) |
| 14 | `Subject` | `model/Subject.java` | `"Asignaturas"` | `Long id` | 1 `@ManyToOne` (`User professor`) |
| 15 | `User` | `model/User.java` | `"usuarios"` | `Long id` | 1 `@ManyToOne` (`Role role`) |

### 1.2. Observación de Anotaciones Lombok (`@Data` en las 15 Entidades)

- **Presencia universal de `@Data`:** El 100% de las 15 entidades (línea 8 de `User.java`, línea 9 de `PracticeAssignment.java`, línea 9 de `LessonPlan.java`, línea 8 de `PortfolioDocument.java`, línea 8 de `Evaluation.java`, etc.) utiliza exclusivamente la anotación `@Data` de Lombok.
- **Riesgo crítico de recursión infinita en `LessonPlan.java` (BK-37):**
  - Línea 35-37: `@ManyToOne private LessonPlan parentPlan;`
  - Dado que `@Data` genera automáticamente `toString()`, `equals()` y `hashCode()` evaluando todos los campos sin exclusión, invocar `toString()` o almacenar instancias de `LessonPlan` con `parentPlan` asignado en colecciones `Set` o `Map` detona un ciclo recursivo infinito que culmina en `java.lang.StackOverflowError`.
- **Fuga de datos sensibles en `User.java`:**
  - Líneas 19-20: `@JsonProperty(...) private String passwordHash;`
  - Aunque Jackson oculte `passwordHash` al serializar JSON si se especifica `WRITE_ONLY`, el método `toString()` generado por `@Data` evalúa e imprime `passwordHash` en texto plano en cualquier log de depuración o consola.

### 1.3. Auditoría de Relaciones y Estrategias de Carga (`FetchType`)

En la especificación Jakarta Persistence (JPA):
- El valor predeterminado de `fetch` para `@ManyToOne` es `FetchType.EAGER`.
- El valor predeterminado de `fetch` para `@OneToOne` es `FetchType.EAGER`.
- El valor predeterminado de `fetch` para `@OneToMany` es `FetchType.LAZY`.
- El valor predeterminado de `fetch` para `@ManyToMany` es `FetchType.LAZY`.

Al contrastar la base de código real:
- **Total de relaciones mapeadas:** 19 relaciones.
- **Relaciones con `FetchType.LAZY` explícito:** Solo 3 relaciones:
  - `ChatbotLog.java:14`: `@ManyToOne(fetch = FetchType.LAZY)`
  - `ChatbotMessage.java:15`: `@ManyToOne(fetch = FetchType.LAZY)`
  - `EvaluationCriteria.java:14`: `@ManyToOne(fetch = FetchType.LAZY)`
- **Relaciones con `FetchType.EAGER` por omisión (16 relaciones deficientes):**
  1. `Evaluation.java:15`: `@ManyToOne` hacia `PracticeAssignment`
  2. `Evaluation.java:18`: `@ManyToOne` hacia `User evaluator`
  3. `LessonPlan.java:16`: `@ManyToOne` hacia `PracticeAssignment`
  4. `LessonPlan.java:19`: `@OneToOne(cascade = CascadeType.ALL)` hacia `PortfolioDocument`
  5. `LessonPlan.java:35`: `@ManyToOne` hacia `LessonPlan parentPlan`
  6. `Notification.java:14`: `@ManyToOne` hacia `User targetUser`
  7. `PasswordResetToken.java:17`: `@ManyToOne` hacia `User`
  8. `PortfolioDocument.java:15`: `@ManyToOne` hacia `PracticeAssignment`
  9. `PracticeAssignment.java:15`: `@ManyToOne` hacia `User student`
  10. `PracticeAssignment.java:18`: `@ManyToOne` hacia `Subject subject`
  11. `PracticeAssignment.java:21`: `@ManyToOne` hacia `PracticeCenter center`
  12. `PracticeAssignment.java:24`: `@ManyToOne` hacia `User collaborator`
  13. `PracticeAssignment.java:27`: `@ManyToOne` hacia `User tutor`
  14. `PracticeAssignment.java:30`: `@ManyToOne` hacia `AcademicPeriod period`
  15. `StudentPair.java:13,16,19`: 3 `@ManyToOne` hacia `assignment1`, `assignment2`, `period`
  16. `Subject.java:19`: `@ManyToOne` hacia `User professor`
  17. `User.java:22`: `@ManyToOne` hacia `Role role`

### 1.4. Impacto del Carga Eager en Cascada y Explosión N+1 (TR-12)

- En `PracticeAssignment.java`, al consultar asignaciones mediante `practiceAssignmentRepository.findAll()` o `findByCollaboratorId()`:
  - Para cada fila de asignación recuperada, Hibernate ejecuta inmediatamente 6 consultas SQL adicionales para poblar `student`, `subject`, `center`, `collaborator`, `tutor` y `period`.
  - Como `student`, `collaborator`, `tutor` y el profesor de `subject` son entidades `User`, y `User.role` es `EAGER`, Hibernate ejecuta consultas adicionales para cargar cada `Role`.
  - En un escenario con 50 asignaciones, una sola invocación a `GET /api/asignaciones` desata más de **300 consultas SQL individuales**.
- En `PortfolioDocument.java`, cada documento carga eagerly su `PracticeAssignment`, la cual a su vez arrastra sus 6 asociaciones y roles.

### 1.5. Auditoría de Repositorios Spring Data JPA

- **Retorno de entidades sin contenedor `Optional<T>` (BK-44):**
  - `UserRepository.java:10`: `User findByEmail(String email);`
  - `RoleRepository.java:10`: `Role findByName(String roleName);`
  - Obliga a verificaciones manuales de nulidad, predisponiendo al sistema a `NullPointerException`.
- **Consultas derivadas de excesiva profundidad (BK-45):**
  - `PortfolioDocumentRepository.java:10`: `findByPracticeAssignment_Student_Email(String email)`
  - `PortfolioDocumentRepository.java:11`: `findByPracticeAssignment_Student_EmailAndType(String email, DocumentType type)`
  - `LessonPlanRepository.java:10`: `findByPracticeAssignment_Student_Email(String email)`
  - Generan nombres de métodos frágiles que acoplan el repositorio a 3 niveles de navegación relacional.
- **Ausencia total de `@EntityGraph` o consultas JPQL `JOIN FETCH`:**
  - Ni un solo repositorio de la aplicación declara `@EntityGraph` ni `@Query("... JOIN FETCH ...")`.

### 1.6. Heterogeneidad en Nombres de Tablas, Columnas e Índices (BK-38)

- **Caos en `@Table(name = "...")`:**
  - Tablas en minúscula: `usuarios`, `roles`.
  - Tablas en PascalCase singular/plural: `Asignaturas`, `Planificaciones`, `Documentos`, `Evaluaciones`, `Notificaciones`, `Duplas`.
  - Tablas en PascalCase con guiones bajos: `Centro_Practicas`, `Asignacion_Practicas`, `Criterios_Evaluacion`, `Periodos_Academicos`, `Registros_Chatbot`, `Mensajes_Chatbot`, `Password_Reset_Tokens`.
  - Tablas en inglés (`Password_Reset_Tokens`) vs español (`usuarios`, `Planificaciones`).
- **Inconsistencia en Claves Foráneas (`@JoinColumn`):**
  - CamelCase con sufijo: `lessonPlan_id` (`ChatbotLog.java:15`), `chatbotLog_id` (`ChatbotMessage.java:16`), `targetUser_id` (`Notification.java:15`).
  - snake_case con sufijo: `practice_assignment_id`, `student_id`, `subject_id`, `center_id`, `collaborator_id`, `tutor_id`, `period_id`, `document_id`, `parent_plan_id`, `role_id`.
- **Discrepancia en propiedad booleana (BK-42):**
  - `Notification.java:20-21`: `@Column(name = "is_read") private Boolean read;`
- **Ausencia de Índices en Base de Datos:**
  - Cero declaraciones `@Table(indexes = { ... })` en las 15 entidades. Claves foráneas como `practice_assignment_id`, `student_id`, `user_id` y atributos de filtrado frecuente (`status`, `is_read`, `type`) carecen de índices declarados, ocasionando escaneos completos de tabla (*Full Table Scans*).

---

## 2. Logic Chain (Cadena de Razonamiento Lógico)

1. **Premisa 1 (Lombok `@Data` vs Ciclo de Vida JPA):**
   - `@Data` genera `equals()` y `hashCode()` considerando todos los campos del objeto.
   - En JPA, una entidad recién instanciada tiene `id == null`. Al persistirse, la base de datos le asigna un `id` autonumérico (mutación de estado). Si una entidad se agrega a un `HashSet` antes de persistirse, su `hashCode` cambiará tras la persistencia, haciendo que `set.contains(entity)` falle o que no pueda recuperarse.
   - Además, al comparar proxies de Hibernate generados perezosamente, `@Data` accede a campos directos mediante `this.field` en lugar de `getGetter()`, produciendo fallos de identidad o forzando la resolución prematura de relaciones LAZY.
   - Por tanto, la regla de oro de JPA establece: *Nunca usar `@Data` en entidades JPA*. Se debe emplear `@Getter`, `@Setter` y `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` incluyendo únicamente el `@Id` (o clave natural inmutable), con constructores explícitos `@NoArgsConstructor` y `@AllArgsConstructor`.

2. **Premisa 2 (Prevención de Recursión y Exclusiones de ToString):**
   - En relaciones bidireccionales o autorreferenciales (ej. `LessonPlan.parentPlan`), el método `toString()` generado por `@Data` invoca recursivamente `parentPlan.toString()`, causando `StackOverflowError`.
   - Asimismo, las relaciones `@ManyToOne` y `@OneToOne` no deben ser navegadas en `toString()`, pues si la sesión de Hibernate está cerrada, detonan `LazyInitializationException`.
   - Por tanto, todo campo relacional foráneo debe anotarse obligatoriamente con `@ToString.Exclude`, y en `User.java`, el campo `passwordHash` debe ser excluido para no vulnerar la confidencialidad de credenciales en logs (OWASP A02).

3. **Premisa 3 (Erradicación de N+1 mediante `FetchType.LAZY`):**
   - Las 16 relaciones `@ManyToOne` y `@OneToOne` que actualmente tienen carga EAGER fuerzan a Hibernate a disparar consultas adicionales inmediatamente al leer cualquier registro.
   - Cambiar todas las asociaciones a `FetchType.LAZY` garantiza que Hibernate no recupere entidades asociadas a menos que se soliciten explícitamente.

4. **Premisa 4 (Carga Eficiente de Grafos con `@EntityGraph` y `JOIN FETCH`):**
   - Si una asociación es `LAZY` y el controlador o la vista requieren sus atributos (ej. en `GET /api/asignaciones` donde la tabla muestra el nombre del estudiante, carrera, centro y profesor), iterar o serializar la lista disparará N consultas individuales (N+1 diferido) o lanzará `LazyInitializationException` si la sesión de persistencia no está abierta en la capa web.
   - La solución arquitectónica óptima no es volver a EAGER, sino definir planes de carga específicos para las consultas que lo necesitan utilizando `@EntityGraph` o `JOIN FETCH` en los repositorios. Esto genera un único `SELECT ... LEFT OUTER JOIN` que recupera el árbol de entidades en un solo viaje de ida y vuelta a MySQL (*single round-trip*).

5. **Premisa 5 (Estandarización del Esquema Relacional e Indexación):**
   - La heterogeneidad entre snake_case, PascalCase, Spanglish y camelCase atenta contra la portabilidad del esquema entre motores SQL (MySQL en Windows es insensible a mayúsculas por defecto, mientras que en Linux/Docker `lower_case_table_names=0` distingue mayúsculas y minúsculas estrictamente, provocando fallos en despliegues containerizados).
   - Estandarizar todas las tablas a minúsculas con `snake_case` plural y todas las columnas foráneas a `snake_case` estricto asegura consistencia absoluta.
   - La creación de índices en claves foráneas y columnas de filtrado frecuente reduce la complejidad de consulta de $O(N)$ a $O(\log N)$.

---

## 3. Caveats (Advertencias, Supuestos y Áreas Fuera de Alcance)

1. **Compatibilidad con Datos Preexistentes:**
   - La normalización de nombres de tabla (`@Table(name = "...")`) y columnas (`@JoinColumn(name = "...")`) impactará directamente en las tablas generadas en MySQL.
   - Dado que en la Fase 1 se configuró `spring.jpa.hibernate.ddl-auto=update`, Hibernate creará las nuevas tablas estandarizadas pero podría no eliminar automáticamente las tablas antiguas con nombres descontinuados (ej. `Periodos_Academicos`).
   - Para producción se requerirá formalizar un script de migración Flyway (`V3__standardize_schema_names.sql`). Para desarrollo local bajo `update`, las nuevas entidades operarán transparentemente.
2. **Serialización JSON vs Entidades:**
   - Varias respuestas de controladores retornan la entidad directamente (`ResponseEntity.ok(assignment)`). Al cambiar a `LAZY`, si se accede a campos no inicializados fuera de una transacción, Jackson podría serializar proxies vacíos o fallar con `InvalidDefinitionException`. Por ello, es imperativo que las consultas de servicio utilicen los métodos de repositorio con `@EntityGraph` implementados aquí o que se acompañe del mapeo a DTOs (coordinado con `explorer_m3_err_1`).
3. **Repositorios Sin Uso Directo (BK-46):**
   - Existen 7 repositorios (`AcademicPeriodRepository`, `ChatbotLogRepository`, `ChatbotMessageRepository`, `EvaluationCriteriaRepository`, `EvaluationRepository`, `NotificationRepository`, `StudentPairRepository`) correspondientes a módulos no expuestos en los controladores actuales. Se mantienen en el inventario saneados con `@Getter`/`@Setter` y `FetchType.LAZY` para evitar deuda técnica latente cuando se habiliten sus endpoints.

---

## 4. Conclusion & Actionable Blueprint (Especificación Detallada de Cambios)

A continuación se detalla la especificación exacta de cambios para cada entidad y repositorio.

### 4.1. Matriz de Transformación de Entidades JPA (15 Modelos)

#### 1. `com.ubb.practicas.model.AcademicPeriod`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
- **Mapeo Relacional:**
  - `@Table(name = "academic_periods")` (anterior: `"Periodos_Academicos"`)
  - Índices: `@Table(name = "academic_periods", indexes = { @Index(name = "idx_period_year_sem", columnList = "year, semesterNumber") })`

#### 2. `com.ubb.practicas.model.ChatbotLog`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `lessonPlan`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `private LessonPlan lessonPlan;`: mantener `FetchType.LAZY`
  - Clave foránea: corregir `@JoinColumn(name = "lesson_plan_id")` (anterior: `"lessonPlan_id"`)
- **Mapeo Relacional:**
  - `@Table(name = "chatbot_logs", indexes = { @Index(name = "idx_chatbot_log_plan", columnList = "lesson_plan_id") })` (anterior: `"Registros_Chatbot"`)

#### 3. `com.ubb.practicas.model.ChatbotMessage`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `chatbotLog`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `private ChatbotLog chatbotLog;`: mantener `FetchType.LAZY`
  - Clave foránea: corregir `@JoinColumn(name = "chatbot_log_id")` (anterior: `"chatbotLog_id"`)
- **Mapeo Relacional:**
  - `@Table(name = "chatbot_messages", indexes = { @Index(name = "idx_chatbot_msg_log", columnList = "chatbot_log_id") })` (anterior: `"Mensajes_Chatbot"`)

#### 4. `com.ubb.practicas.model.Evaluation`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campos `practiceAssignment`, `evaluator`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `practiceAssignment`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
  - `evaluator`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
- **Tipado & Precisión (BK-39):**
  - Cambiar `private Float score;` a `private java.math.BigDecimal score;` o `private Double score;`
- **Mapeo Relacional:**
  - `@Table(name = "evaluaciones", indexes = { @Index(name = "idx_eval_assignment", columnList = "practice_assignment_id"), @Index(name = "idx_eval_evaluator", columnList = "evaluator_id") })`

#### 5. `com.ubb.practicas.model.EvaluationCriteria`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `evaluation`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `evaluation`: mantener `FetchType.LAZY`
- **Tipado & Precisión (BK-39):**
  - Cambiar `private Float points;` a `private java.math.BigDecimal points;`
- **Mapeo Relacional:**
  - `@Table(name = "criterios_evaluacion", indexes = { @Index(name = "idx_crit_eval", columnList = "evaluation_id") })`

#### 6. `com.ubb.practicas.model.LessonPlan`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campos `practiceAssignment`, `document`, `parentPlan`: agregar `@ToString.Exclude`
  - **CRÍTICO (BK-37):** Al usar `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` y `@ToString.Exclude` en `parentPlan`, se elimina de raíz el `StackOverflowError` por recursión cíclica.
- **Relaciones & FetchType:**
  - `practiceAssignment`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
  - `document`: cambiar a `@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)`
  - `parentPlan`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "planificaciones", indexes = { @Index(name = "idx_plan_assignment", columnList = "practice_assignment_id"), @Index(name = "idx_plan_document", columnList = "document_id") })`

#### 7. `com.ubb.practicas.model.Notification`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `targetUser`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `targetUser`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
  - Clave foránea: corregir `@JoinColumn(name = "target_user_id")` (anterior: `"targetUser_id"`)
- **Propiedad Booleana (BK-42):**
  - Mantener `@Column(name = "is_read") private Boolean isRead;` o `private Boolean read;` con accesores `getIsRead()` / `setIsRead()`.
- **Mapeo Relacional:**
  - `@Table(name = "notificaciones", indexes = { @Index(name = "idx_notif_target_read", columnList = "target_user_id, is_read") })`

#### 8. `com.ubb.practicas.model.PasswordResetToken`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `user`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `user`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "password_reset_tokens", indexes = { @Index(name = "idx_prt_token", columnList = "token", unique = true), @Index(name = "idx_prt_user", columnList = "user_id") })`

#### 9. `com.ubb.practicas.model.PortfolioDocument`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `practiceAssignment`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `practiceAssignment`: cambiar a `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "documentos", indexes = { @Index(name = "idx_doc_assignment_type", columnList = "practice_assignment_id, type") })`

#### 10. `com.ubb.practicas.model.PracticeAssignment`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campos `student`, `subject`, `center`, `collaborator`, `tutor`, `period`: agregar `@ToString.Exclude` en los 6 campos.
- **Relaciones & FetchType (TR-12):**
  - Cambiar los 6 campos `@ManyToOne` a `fetch = FetchType.LAZY`:
    ```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private PracticeCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborator_id")
    private User collaborator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id")
    private User tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private AcademicPeriod period;
    ```
- **Mapeo Relacional:**
  - `@Table(name = "asignacion_practicas", indexes = {`
    - `@Index(name = "idx_pa_student_status", columnList = "student_id, status")`,
    - `@Index(name = "idx_pa_collaborator", columnList = "collaborator_id")`,
    - `@Index(name = "idx_pa_tutor", columnList = "tutor_id")`,
    - `@Index(name = "idx_pa_subject", columnList = "subject_id")`
    `})`

#### 11. `com.ubb.practicas.model.PracticeCenter`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
- **Mapeo Relacional:**
  - `@Table(name = "centros_practica")` (anterior: `"Centro_Practicas"`)

#### 12. `com.ubb.practicas.model.Role`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
- **Mapeo Relacional:**
  - `@Table(name = "roles", indexes = { @Index(name = "idx_role_name", columnList = "name", unique = true) })`

#### 13. `com.ubb.practicas.model.StudentPair`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campos `assignment1`, `assignment2`, `period`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `assignment1`: `@ManyToOne(fetch = FetchType.LAZY)`
  - `assignment2`: `@ManyToOne(fetch = FetchType.LAZY)`
  - `period`: `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "duplas", indexes = { @Index(name = "idx_pair_a1", columnList = "assignment1_id"), @Index(name = "idx_pair_a2", columnList = "assignment2_id") })`

#### 14. `com.ubb.practicas.model.Subject`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campo `professor`: agregar `@ToString.Exclude`
- **Relaciones & FetchType:**
  - `professor`: `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "asignaturas", indexes = { @Index(name = "idx_subject_professor", columnList = "professor_id") })`

#### 15. `com.ubb.practicas.model.User`
- **Lombok:**
  - Remover: `@Data`
  - Agregar: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - Campo `@Id`: agregar `@EqualsAndHashCode.Include`
  - Campos `role`, `passwordHash`: agregar `@ToString.Exclude` (oculta la credencial hash de logs).
- **Relaciones & FetchType:**
  - `role`: `@ManyToOne(fetch = FetchType.LAZY)`
- **Mapeo Relacional:**
  - `@Table(name = "usuarios", indexes = { @Index(name = "idx_user_email", columnList = "email", unique = true), @Index(name = "idx_user_role", columnList = "role_id") })`

---

### 4.2. Especificación de Consultas Optimizadas en Repositorios (`@EntityGraph` & `JOIN FETCH`)

Para erradicar definitivamente las consultas N+1 sin incurrir en `LazyInitializationException`, se especifican los siguientes métodos en los repositorios de Spring Data JPA:

#### 1. `PracticeAssignmentRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeAssignmentRepository extends JpaRepository<PracticeAssignment, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"student", "student.role", "subject", "subject.professor", "center", "collaborator", "tutor", "period"})
    Optional<PracticeAssignment> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findBySubjectProfessorId(Long professorId);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findByCollaboratorId(Long collaboratorId);

    @EntityGraph(attributePaths = {"student", "student.role", "subject", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findByTutorId(Long tutorId);

    @EntityGraph(attributePaths = {"student", "subject", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findByStudentEmail(String email);

    @EntityGraph(attributePaths = {"student", "subject", "center", "collaborator", "tutor", "period"})
    List<PracticeAssignment> findByStudentEmailAndStatus(String email, AssignmentStatus status);

    boolean existsBySubjectId(Long subjectId);
}
```

#### 2. `PortfolioDocumentRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.enums.DocumentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioDocumentRepository extends JpaRepository<PortfolioDocument, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {
        "practiceAssignment",
        "practiceAssignment.student",
        "practiceAssignment.collaborator",
        "practiceAssignment.tutor",
        "practiceAssignment.subject",
        "practiceAssignment.subject.professor"
    })
    Optional<PortfolioDocument> findById(@NonNull Long id);

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email")
    List<PortfolioDocument> findByPracticeAssignment_Student_Email(@Param("email") String email);

    @Query("SELECT d FROM PortfolioDocument d " +
           "JOIN FETCH d.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email AND d.type = :type")
    List<PortfolioDocument> findByPracticeAssignment_Student_EmailAndType(
            @Param("email") String email,
            @Param("type") DocumentType type
    );
}
```

#### 3. `LessonPlanRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonPlanRepository extends JpaRepository<LessonPlan, Long> {

    @Query("SELECT lp FROM LessonPlan lp " +
           "LEFT JOIN FETCH lp.document " +
           "JOIN FETCH lp.practiceAssignment pa " +
           "JOIN FETCH pa.student s " +
           "WHERE s.email = :email")
    List<LessonPlan> findByPracticeAssignment_Student_Email(@Param("email") String email);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"practiceAssignment", "practiceAssignment.student", "document"})
    Optional<LessonPlan> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"practiceAssignment", "document"})
    Optional<LessonPlan> findByDocumentId(Long documentId);
}
```

#### 4. `UserRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    User findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findOptionalByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    List<User> findByRoleName(String roleName);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"role"})
    List<User> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(@NonNull Long id);
}
```

#### 5. `SubjectRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"professor"})
    List<Subject> findAll();

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"professor"})
    Optional<Subject> findById(@NonNull Long id);
}
```

#### 6. `RoleRepository.java`
```java
package com.ubb.practicas.repository;

import com.ubb.practicas.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByName(String roleName);

    Optional<Role> findOptionalByName(String roleName);
}
```

---

## 5. Verification Method (Método de Verificación Independiente)

El equipo de implementación (`worker`) y el auditor podrán verificar la correcta aplicación de estas soluciones mediante los siguientes pasos:

### 5.1. Verificación de Compilación y Sintaxis
Ejecutar el comando de compilación de Maven para validar que todos los tipos, repositorios, accesores (`@Getter`, `@Setter`) y anotaciones de Lombok compilan limpiamente:
```bash
./mvnw test-compile
```
*Criterio de Aceptación:* `BUILD SUCCESS`, sin errores de símbolos perdidos por la remoción de `@Data`.

### 5.2. Verificación de Prevención de StackOverflowError y Carga Lazy
Ejecutar la suite de pruebas unitarias y de integración de controladores y servicios:
```bash
./mvnw test
```
*Criterio de Aceptación:* Ningún `StackOverflowError` al instanciar y probar planes de clase (`LessonPlan`) con planes padre simulados, y ningún `LazyInitializationException` en flujos de lectura de asignaciones, documentos y planes de lección.

### 5.3. Inspección del Log de Consultas SQL (Verificación Anti-N+1)
Con las propiedades configuradas en `application.properties`:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```
Ejecutar una consulta a `GET /api/asignaciones` o invocar `practiceAssignmentRepository.findAll()`:
- **Antes:** Se observaba 1 consulta a `Asignacion_Practicas` seguida de decenas de consultas individuales a `usuarios`, `Asignaturas`, `Centro_Practicas`, `Periodos_Academicos` y `roles`.
- **Después:** Debe observarse una **ÚNICA consulta SQL** con sentencias `LEFT OUTER JOIN` vinculando `student`, `role`, `subject`, `professor`, `center`, `collaborator`, `tutor` y `period`.
