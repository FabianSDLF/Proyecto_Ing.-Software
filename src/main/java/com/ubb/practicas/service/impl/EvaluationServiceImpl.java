package com.ubb.practicas.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.dto.EvaluationFinalizeDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.exception.UserNotFoundException;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.EvaluationCriteria;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.AchievementLevel;
import com.ubb.practicas.model.enums.EvaluationType;
import com.ubb.practicas.repository.EvaluationCriteriaRepository;
import com.ubb.practicas.repository.EvaluationRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.AssignmentScope;
import com.ubb.practicas.service.AuditService;
import com.ubb.practicas.service.EvaluationRubricCatalog;
import com.ubb.practicas.service.EvaluationRubricCatalog.IndicatorDefinition;
import com.ubb.practicas.service.EvaluationRubricCatalog.RubricDefinition;
import com.ubb.practicas.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final UserRepository userRepository;
    private EvaluationRubricCatalog rubricCatalog;
    private AuditService auditService;
    private ObjectMapper objectMapper;

    @Autowired
    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCriteriaRepository evaluationCriteriaRepository,
            PracticeAssignmentRepository practiceAssignmentRepository,
            UserRepository userRepository,
            EvaluationRubricCatalog rubricCatalog,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationCriteriaRepository = evaluationCriteriaRepository;
        this.practiceAssignmentRepository = practiceAssignmentRepository;
        this.userRepository = userRepository;
        this.rubricCatalog = rubricCatalog;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationCriteriaRepository evaluationCriteriaRepository,
            PracticeAssignmentRepository practiceAssignmentRepository,
            UserRepository userRepository
    ) {
        this(evaluationRepository, evaluationCriteriaRepository, practiceAssignmentRepository, userRepository, null, null, null);
    }

    private ObjectMapper mapper() {
        if (this.objectMapper == null) {
            this.objectMapper = new ObjectMapper();
        }
        return this.objectMapper;
    }

    private EvaluationRubricCatalog catalog() {
        if (this.rubricCatalog == null) {
            EvaluationRubricCatalog fallback = new EvaluationRubricCatalog(mapper(), null, practiceAssignmentRepository);
            fallback.init();
            this.rubricCatalog = fallback;
        }
        return this.rubricCatalog;
    }

    @Override
    @Transactional
    public Evaluation crearEvaluacion(EvaluationCreateDTO dto, String evaluatorEmail) {
        if (dto == null) {
            throw new BadRequestException("Los datos de la evaluación son obligatorios.");
        }
        if (Boolean.TRUE.equals(dto.getIsFinalized())) {
            throw new BadRequestException(
                    "El guardado de borrador no permite cerrar ni firmar la evaluación. Utilice POST /api/evaluaciones/{id}/finalizar."
            );
        }
        if (dto.getPracticeAssignmentId() == null) {
            throw new BadRequestException("El identificador de la asignación de práctica es obligatorio.");
        }

        User evaluator = requireActiveUser(evaluatorEmail);
        PracticeAssignment assignment = lockOrFindAssignment(dto.getPracticeAssignmentId());
        EvaluationType evalType = dto.getType() != null ? dto.getType() : EvaluationType.PAUTA_OBSERVACION_COLABORADOR;

        validateEvaluatorAuthorization(evaluator, assignment, evalType);

        Optional<Evaluation> existingOpt = evaluationRepository.findByPracticeAssignmentIdAndType(
                assignment.getId(), evalType
        );

        Evaluation evaluation;
        if (existingOpt.isPresent()) {
            Evaluation existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getIsFinalized())) {
                throw new ResourceConflictException(
                        "Esta evaluación ya fue finalizada y firmada digitalmente; se encuentra registrada como calificación oficial e inamovible en el sistema."
                );
            }
            if (existing.getEvaluator() != null
                    && existing.getEvaluator().getEmail() != null
                    && !existing.getEvaluator().getEmail().equalsIgnoreCase(evaluatorEmail)) {
                throw new AccessDeniedException("Este borrador es privado y pertenece a otro evaluador.");
            }
            Long currentRev = existing.getRevision() != null ? existing.getRevision() : 1L;
            if (dto.getExpectedRevision() == null || !dto.getExpectedRevision().equals(currentRev)) {
                throw new ResourceConflictException(
                        "Conflicto de concurrencia: la pauta se encuentra en la revisión " + currentRev
                                + " y la solicitud indicó expectedRevision=" + dto.getExpectedRevision() + ". Recargue la pauta actual."
                );
            }
            evaluation = existing;
            evaluation.setRevision(currentRev + 1L);
            evaluationCriteriaRepository.deleteByEvaluationId(existing.getId());
        } else {
            if (dto.getExpectedRevision() != null) {
                throw new ResourceConflictException(
                        "No existe un borrador previo para la revisión esperada " + dto.getExpectedRevision() + "."
                );
            }
            evaluation = new Evaluation();
            evaluation.setPracticeAssignment(assignment);
            evaluation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            evaluation.setRevision(1L);
        }

        List<EvaluationCriteria> criteriaToSave = new ArrayList<>();

        if (evalType.isProfessionalRubric()) {
            RubricDefinition rubric = catalog().requireRubric(evalType, dto.getRubricVersion());
            Map<String, IndicatorDefinition> officialIndicators = rubric.indicatorMap();
            Set<String> seenCodes = new HashSet<>();

            if (dto.getCriteria() != null) {
                for (EvaluationCriteriaDTO c : dto.getCriteria()) {
                    if (c == null) continue;
                    String code = resolveCriterionCode(c);
                    if (c.getPoints() == null) {
                        // Indicador aún sin observar en el borrador: se valida el código si viene informado y no se convierte en 0.
                        if (code != null && !officialIndicators.containsKey(code)) {
                            throw new BadRequestException("El indicador '" + code + "' no pertenece a la pauta " + evalType + ".");
                        }
                        continue;
                    }
                    if (code == null || !officialIndicators.containsKey(code)) {
                        throw new BadRequestException("El indicador '" + code + "' no pertenece a la pauta oficial " + evalType + ".");
                    }
                    if (!seenCodes.add(code)) {
                        throw new BadRequestException("El indicador '" + code + "' está duplicado en la pauta.");
                    }
                    if (!EvaluationRubricCatalog.isAllowedPointValue(c.getPoints())) {
                        throw new BadRequestException(
                                "El puntaje del indicador '" + code + "' debe ser un entero entre 2 y 7."
                        );
                    }

                    IndicatorDefinition ind = officialIndicators.get(code);
                    int intPoints = Math.round(c.getPoints());
                    EvaluationCriteria entity = new EvaluationCriteria();
                    entity.setCriterionCode(code);
                    entity.setDimension(ind.getDimension());
                    entity.setDisplayOrder(ind.getOrder());
                    entity.setCriteriaName(ind.getText());
                    entity.setAchievementLevel(c.getAchievementLevel() != null ? c.getAchievementLevel() : mapAchievementLevel(intPoints));
                    entity.setPoints((float) intPoints);
                    entity.setJustification(c.getJustification());
                    criteriaToSave.add(entity);
                }
            }

            criteriaToSave.sort(Comparator.comparing(
                    ec -> ec.getDisplayOrder() != null ? ec.getDisplayOrder() : Integer.MAX_VALUE
            ));

            int totalPoints = criteriaToSave.stream().mapToInt(ec -> Math.round(ec.getPoints())).sum();
            evaluation.setRubricVersion(rubric.getRubricVersion());
            evaluation.setTotalPoints(totalPoints);
            evaluation.setMaxPoints(rubric.getMaxPoints());

            // El cliente nunca impone la nota oficial. Solo si la pauta tiene todos los indicadores se calcula nota preliminar.
            if (criteriaToSave.size() == rubric.getIndicatorCount()) {
                evaluation.setScore(EvaluationRubricCatalog.calculateOfficialGrade(totalPoints, rubric.getMaxPoints()));
            } else {
                evaluation.setScore(null);
            }
        } else {
            if (dto.getScore() != null) {
                if (!Float.isFinite(dto.getScore()) || dto.getScore() < 1.0f || dto.getScore() > 7.0f) {
                    throw new BadRequestException("La calificación debe estar comprendida entre 1.0 y 7.0.");
                }
            }
            evaluation.setScore(dto.getScore());
            if (dto.getCriteria() != null) {
                int idx = 1;
                for (EvaluationCriteriaDTO c : dto.getCriteria()) {
                    if (c == null) continue;
                    EvaluationCriteria criteria = new EvaluationCriteria();
                    criteria.setCriterionCode(resolveCriterionCode(c));
                    criteria.setDimension(c.getDimension());
                    criteria.setDisplayOrder(c.getOrder() != null ? c.getOrder() : idx);
                    criteria.setCriteriaName(c.getCriteriaName() != null ? c.getCriteriaName() : c.getText());
                    criteria.setAchievementLevel(c.getAchievementLevel());
                    criteria.setPoints(c.getPoints());
                    criteria.setJustification(c.getJustification());
                    criteriaToSave.add(criteria);
                    idx++;
                }
            }
        }

        evaluation.setEvaluator(evaluator);
        evaluation.setType(evalType);
        evaluation.setQualitativeComments(dto.getQualitativeComments());
        evaluation.setIsFinalized(false);
        evaluation.setDigitalSignature(null);
        evaluation.setEvaluationDate(LocalDateTime.now(ZoneOffset.UTC));

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        List<EvaluationCriteria> persistedCriteria = new ArrayList<>();
        for (EvaluationCriteria criteria : criteriaToSave) {
            criteria.setEvaluation(savedEvaluation);
            persistedCriteria.add(evaluationCriteriaRepository.save(criteria));
        }

        return enrichEvaluation(savedEvaluation, persistedCriteria);
    }

    @Override
    @Transactional
    public Evaluation finalizarEvaluacion(Long evaluationId, EvaluationFinalizeDTO dto, String evaluatorEmail) {
        if (evaluationId == null) {
            throw new BadRequestException("El identificador de la evaluación es obligatorio.");
        }
        if (dto == null || !Boolean.TRUE.equals(dto.getConfirmed())) {
            throw new BadRequestException("Debe confirmar explícitamente el cierre y firma de la evaluación (confirmed=true).");
        }
        if (dto.getExpectedRevision() == null) {
            throw new BadRequestException("Debe indicar la revisión esperada (expectedRevision) para firmar la evaluación.");
        }

        User evaluator = requireActiveUser(evaluatorEmail);

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));

        PracticeAssignment assignment = evaluation.getPracticeAssignment();
        if (assignment != null && assignment.getId() != null) {
            assignment = lockOrFindAssignment(assignment.getId());
        } else {
            throw new ResourceNotFoundException("Asignación de práctica asociada no encontrada");
        }

        if (Boolean.TRUE.equals(evaluation.getIsFinalized())) {
            throw new ResourceConflictException(
                    "Esta evaluación ya fue finalizada y firmada digitalmente; se encuentra registrada como calificación oficial e inamovible en el sistema."
            );
        }

        Long currentRev = evaluation.getRevision() != null ? evaluation.getRevision() : 1L;
        if (!dto.getExpectedRevision().equals(currentRev)) {
            throw new ResourceConflictException(
                    "Conflicto de concurrencia: la pauta cambió (revisión actual: " + currentRev
                            + ", recibida: " + dto.getExpectedRevision() + "). Recargue la evaluación antes de firmar."
            );
        }

        if (evaluation.getEvaluator() == null
                || evaluation.getEvaluator().getEmail() == null
                || !evaluation.getEvaluator().getEmail().equalsIgnoreCase(evaluatorEmail)) {
            throw new AccessDeniedException("Solo el evaluador propietario del borrador puede firmarlo y publicarlo.");
        }

        EvaluationType evalType = evaluation.getType();
        validateEvaluatorAuthorization(evaluator, assignment, evalType);

        List<EvaluationCriteria> savedCriteria = new ArrayList<>(
                Optional.ofNullable(evaluationCriteriaRepository.findByEvaluationId(evaluation.getId()))
                        .orElse(List.of())
        );

        RubricDefinition officialRubric = null;
        if (evalType.isProfessionalRubric()) {
            officialRubric = catalog().requireRubric(evalType, evaluation.getRubricVersion());
            Map<String, IndicatorDefinition> officialIndicators = officialRubric.indicatorMap();

            if (savedCriteria.size() != officialIndicators.size()) {
                throw new BadRequestException(
                        "La pauta está incompleta: se registraron " + savedCriteria.size()
                                + " de " + officialIndicators.size() + " indicadores oficiales requeridos para firmar."
                );
            }

            Set<String> seenCodes = new HashSet<>();
            int totalPoints = 0;

            for (EvaluationCriteria c : savedCriteria) {
                String code = c.getCriterionCode() != null && !c.getCriterionCode().isBlank()
                        ? c.getCriterionCode().trim()
                        : null;
                if (code == null || !officialIndicators.containsKey(code)) {
                    throw new BadRequestException(
                            "El indicador '" + code + "' no pertenece a la plantilla oficial " + evalType + "."
                    );
                }
                if (!seenCodes.add(code)) {
                    throw new BadRequestException("Indicador duplicado en el registro de evaluación: " + code);
                }
                if (!EvaluationRubricCatalog.isAllowedPointValue(c.getPoints())) {
                    throw new BadRequestException(
                            "El indicador '" + code + "' tiene un puntaje fuera de la escala permitida (2 a 7)."
                    );
                }
                IndicatorDefinition ind = officialIndicators.get(code);
                c.setDimension(ind.getDimension());
                c.setDisplayOrder(ind.getOrder());
                c.setCriteriaName(ind.getText());
                totalPoints += Math.round(c.getPoints());
            }

            savedCriteria.sort(Comparator.comparing(
                    ec -> ec.getDisplayOrder() != null ? ec.getDisplayOrder() : Integer.MAX_VALUE
            ));

            int maxPoints = officialRubric.getMaxPoints();
            float officialScore = EvaluationRubricCatalog.calculateOfficialGrade(totalPoints, maxPoints);

            evaluation.setRubricVersion(officialRubric.getRubricVersion());
            evaluation.setTotalPoints(totalPoints);
            evaluation.setMaxPoints(maxPoints);
            evaluation.setScore(officialScore);
        } else {
            if (evaluation.getScore() == null
                    || !Float.isFinite(evaluation.getScore())
                    || evaluation.getScore() < 1.0f
                    || evaluation.getScore() > 7.0f) {
                throw new BadRequestException("La evaluación debe contar con una calificación válida entre 1.0 y 7.0 para finalizarse.");
            }
        }

        LocalDateTime closedAtUtc = LocalDateTime.now(ZoneOffset.UTC);
        long closedRevision = currentRev + 1L;
        String signerName = evaluator.getFullName() != null && !evaluator.getFullName().isBlank()
                ? evaluator.getFullName().trim()
                : evaluator.getEmail();
        String signerRole = evaluator.getRole() != null ? evaluator.getRole().getName() : null;

        evaluation.setIsFinalized(true);
        evaluation.setRevision(closedRevision);
        evaluation.setFinalizedAt(closedAtUtc);
        evaluation.setEvaluationDate(closedAtUtc);
        evaluation.setFinalizedById(evaluator.getId());
        evaluation.setFinalizedByEmail(evaluator.getEmail());
        evaluation.setFinalizedByName(signerName);
        evaluation.setFinalizedByRole(signerRole);
        evaluation.setDigitalSignature(signerName + " <" + evaluator.getEmail() + ">");

        String snapshotJson = buildCanonicalSnapshotJson(evaluation, officialRubric, savedCriteria, closedAtUtc);
        String snapshotHash = sha256Hex(snapshotJson);
        evaluation.setSnapshotJson(snapshotJson);
        evaluation.setSnapshotHash(snapshotHash);

        Evaluation saved = evaluationRepository.save(evaluation);

        if (auditService != null) {
            auditService.record(
                    evaluator.getEmail(),
                    "FINALIZAR_PUBLICAR_EVALUACION",
                    "/api/evaluaciones/" + saved.getId() + "/finalizar [type=" + saved.getType()
                            + ", score=" + saved.getScore() + ", revision=" + saved.getRevision()
                            + ", sha256=" + snapshotHash + "]",
                    200,
                    "INTERNAL"
            );
        }

        return enrichEvaluation(saved, savedCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Evaluation obtenerEvaluacionPorId(Long evaluationId, String callerEmail) {
        User caller = requireActiveUser(callerEmail);
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
        PracticeAssignment assignment = evaluation.getPracticeAssignment();
        if (assignment == null) {
            throw new ResourceNotFoundException("Asignación asociada no encontrada");
        }
        if (!canReadEvaluation(evaluation, assignment, caller)) {
            throw new AccessDeniedException("No tiene permisos para consultar esta evaluación.");
        }
        List<EvaluationCriteria> criteria = evaluationCriteriaRepository.findByEvaluationId(evaluation.getId());
        return enrichEvaluation(evaluation, criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId, String callerEmail) {
        User caller = requireActiveUser(callerEmail);
        PracticeAssignment assignment = practiceAssignmentRepository.findById(practiceAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        if (!AssignmentScope.isAdmin(caller) && !assignment.hasParticipant(callerEmail)) {
            throw new AccessDeniedException("No pertenece a esta práctica");
        }

        final PracticeAssignment targetAssignment = assignment;
        return evaluationRepository.findByPracticeAssignmentId(practiceAssignmentId).stream()
                .filter(e -> canReadEvaluation(e, targetAssignment, caller))
                .map(e -> enrichEvaluation(e, evaluationCriteriaRepository.findByEvaluationId(e.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId) {
        return evaluationRepository.findByPracticeAssignmentId(practiceAssignmentId).stream()
                .map(e -> enrichEvaluation(e, evaluationCriteriaRepository.findByEvaluationId(e.getId())))
                .toList();
    }

    private User requireActiveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Sesión no autenticada");
        }
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("Usuario no encontrado");
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new AccessDeniedException("Cuenta de usuario inactiva");
        }
        return user;
    }

    private PracticeAssignment lockOrFindAssignment(Long assignmentId) {
        Optional<PracticeAssignment> locked = practiceAssignmentRepository.findByIdForUpdate(assignmentId);
        if (locked != null && locked.isPresent()) {
            return locked.get();
        }
        return practiceAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
    }

    private void validateEvaluatorAuthorization(User evaluator, PracticeAssignment assignment, EvaluationType evalType) {
        if (assignment == null || evalType == null) {
            throw new BadRequestException("La asignación y el tipo de instrumento son obligatorios.");
        }
        String evaluatorEmail = evaluator.getEmail();
        String role = AssignmentScope.role(evaluator);
        boolean collaboratorType = evalType.isCollaboratorInstrument();
        boolean tutorType = evalType.isTutorInstrument();

        boolean authorized =
                (role.equals("PROFESOR_COLABORADOR") && assignment.isCollaborator(evaluatorEmail) && collaboratorType)
                || (role.equals("TUTOR") && assignment.isTutor(evaluatorEmail) && tutorType)
                || (role.equals("PROFESOR_DE_ASIGNATURA") && assignment.isProfessor(evaluatorEmail) && !collaboratorType && !tutorType);

        if (!authorized) {
            throw new AccessDeniedException("El instrumento no corresponde a su participación en esta práctica");
        }
    }

    private boolean canReadEvaluation(Evaluation evaluation, PracticeAssignment assignment, User caller) {
        if (caller == null || Boolean.FALSE.equals(caller.getActive())) {
            return false;
        }
        String callerEmail = caller.getEmail();
        boolean isAdmin = AssignmentScope.isAdmin(caller);
        boolean isParticipant = assignment != null && assignment.hasParticipant(callerEmail);
        if (!isAdmin && !isParticipant) {
            return false;
        }
        if (Boolean.TRUE.equals(evaluation.getIsFinalized())) {
            return true;
        }
        // Los borradores son estrictamente privados al evaluador propietario autorizado
        if (evaluation.getEvaluator() == null || evaluation.getEvaluator().getEmail() == null) {
            return false;
        }
        if (!evaluation.getEvaluator().getEmail().equalsIgnoreCase(callerEmail)) {
            return false;
        }
        try {
            validateEvaluatorAuthorization(caller, assignment, evaluation.getType());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String resolveCriterionCode(EvaluationCriteriaDTO c) {
        if (c.getCriterionCode() != null && !c.getCriterionCode().isBlank()) {
            return c.getCriterionCode().trim();
        }
        if (c.getCriteriaName() != null && !c.getCriteriaName().isBlank()) {
            return c.getCriteriaName().trim();
        }
        return null;
    }

    private AchievementLevel mapAchievementLevel(int points) {
        if (points >= 6) return AchievementLevel.LOGRO_ALTO;
        if (points >= 4) return AchievementLevel.LOGRO_ADECUADO;
        if (points >= 3) return AchievementLevel.LOGRO_PARCIAL;
        return AchievementLevel.LOGRO_INSUFICIENTE;
    }

    private String buildCanonicalSnapshotJson(
            Evaluation evaluation,
            RubricDefinition rubric,
            List<EvaluationCriteria> criteria,
            LocalDateTime closedAtUtc
    ) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("evaluationId", evaluation.getId());
            root.put("practiceAssignmentId", evaluation.getPracticeAssignment() != null ? evaluation.getPracticeAssignment().getId() : null);
            root.put("type", evaluation.getType() != null ? evaluation.getType().name() : null);
            root.put("instrumentName", rubric != null ? rubric.getName() : defaultInstrumentName(evaluation.getType()));
            root.put("area", rubric != null ? rubric.getArea() : null);
            root.put("visit", rubric != null ? rubric.getVisit() : null);
            root.put("rubricVersion", evaluation.getRubricVersion());
            root.put("revision", evaluation.getRevision());

            Map<String, Object> calculation = new LinkedHashMap<>();
            calculation.put("scale", EvaluationRubricCatalog.OFFICIAL_SCALE);
            calculation.put("minGrade", 2.0);
            calculation.put("maxGrade", 7.0);
            calculation.put("passingGrade", 4.0);
            calculation.put("exigency", 0.60);
            calculation.put("roundingMode", "HALF_UP_1_DECIMAL");
            root.put("calculationConfig", calculation);

            root.put("totalPoints", evaluation.getTotalPoints());
            root.put("maxPoints", evaluation.getMaxPoints());
            root.put("score", evaluation.getScore());
            root.put("qualitativeComments", evaluation.getQualitativeComments());
            root.put("finalizedAt", closedAtUtc.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            Map<String, Object> signer = new LinkedHashMap<>();
            signer.put("id", evaluation.getFinalizedById());
            signer.put("fullName", evaluation.getFinalizedByName());
            signer.put("email", evaluation.getFinalizedByEmail());
            signer.put("role", evaluation.getFinalizedByRole());
            root.put("signer", signer);

            List<Map<String, Object>> criteriaNodes = new ArrayList<>();
            for (EvaluationCriteria c : criteria) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("criterionCode", c.getCriterionCode());
                item.put("dimension", c.getDimension());
                item.put("order", c.getDisplayOrder());
                item.put("text", c.getCriteriaName());
                item.put("criteriaName", c.getCriteriaName());
                item.put("points", c.getPoints());
                item.put("achievementLevel", c.getAchievementLevel() != null ? c.getAchievementLevel().name() : null);
                item.put("justification", c.getJustification());
                criteriaNodes.add(item);
            }
            root.put("criteria", criteriaNodes);

            return mapper().writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Error al construir la instantánea de cierre de la evaluación", e);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }

    private Evaluation enrichEvaluation(Evaluation evaluation, List<EvaluationCriteria> dbCriteria) {
        if (evaluation == null) return null;

        // Si está firmada y posee instantánea, la lectura oficial proviene siempre de la instantánea inmutable.
        if (Boolean.TRUE.equals(evaluation.getIsFinalized())
                && evaluation.getSnapshotJson() != null
                && !evaluation.getSnapshotJson().isBlank()) {
            try {
                JsonNode snap = mapper().readTree(evaluation.getSnapshotJson());
                if (snap.hasNonNull("instrumentName")) {
                    evaluation.setInstrumentName(snap.get("instrumentName").asText());
                }
                if (snap.hasNonNull("area")) {
                    evaluation.setArea(snap.get("area").asText());
                }
                if (snap.hasNonNull("visit")) {
                    evaluation.setVisit(snap.get("visit").asInt());
                }
                if (snap.hasNonNull("rubricVersion")) {
                    evaluation.setRubricVersion(snap.get("rubricVersion").asText());
                }
                if (snap.hasNonNull("totalPoints")) {
                    evaluation.setTotalPoints(snap.get("totalPoints").asInt());
                }
                if (snap.hasNonNull("maxPoints")) {
                    evaluation.setMaxPoints(snap.get("maxPoints").asInt());
                }
                if (snap.hasNonNull("score")) {
                    evaluation.setScore((float) snap.get("score").asDouble());
                }
                List<EvaluationCriteriaDTO> snapshotCriteria = new ArrayList<>();
                JsonNode critArr = snap.get("criteria");
                if (critArr != null && critArr.isArray()) {
                    for (JsonNode n : critArr) {
                        EvaluationCriteriaDTO dto = new EvaluationCriteriaDTO();
                        dto.setCriterionCode(n.hasNonNull("criterionCode") ? n.get("criterionCode").asText() : null);
                        dto.setDimension(n.hasNonNull("dimension") ? n.get("dimension").asText() : null);
                        dto.setOrder(n.hasNonNull("order") ? n.get("order").asInt() : null);
                        String text = n.hasNonNull("text") ? n.get("text").asText()
                                : (n.hasNonNull("criteriaName") ? n.get("criteriaName").asText() : null);
                        dto.setText(text);
                        dto.setCriteriaName(text);
                        dto.setPoints(n.hasNonNull("points") ? (float) n.get("points").asDouble() : null);
                        if (n.hasNonNull("achievementLevel")) {
                            try {
                                dto.setAchievementLevel(AchievementLevel.valueOf(n.get("achievementLevel").asText()));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        dto.setJustification(n.hasNonNull("justification") ? n.get("justification").asText() : null);
                        snapshotCriteria.add(dto);
                    }
                }
                evaluation.setHistoricalManual(false);
                evaluation.setObservedIndicators(snapshotCriteria.size());
                evaluation.setTotalIndicators(snapshotCriteria.size());
                evaluation.setCriteria(snapshotCriteria);
                return evaluation;
            } catch (Exception ignored) {
                // Si el JSON de un mock en pruebas no es válido, continúa con el enriquecimiento estándar
            }
        }

        Optional<RubricDefinition> rubricOpt = catalog().findRubric(evaluation.getType());
        if (rubricOpt.isPresent()) {
            RubricDefinition rubric = rubricOpt.get();
            evaluation.setInstrumentName(rubric.getName());
            evaluation.setArea(rubric.getArea());
            evaluation.setVisit(rubric.getVisit());
            if (evaluation.getRubricVersion() == null) {
                evaluation.setRubricVersion(rubric.getRubricVersion());
            }
            if (evaluation.getMaxPoints() == null) {
                evaluation.setMaxPoints(rubric.getMaxPoints());
            }
            evaluation.setHistoricalManual(false);
            evaluation.setTotalIndicators(rubric.getIndicatorCount());

            Map<String, IndicatorDefinition> officialMap = rubric.indicatorMap();
            List<EvaluationCriteriaDTO> dtoList = new ArrayList<>();
            int sumPoints = 0;
            if (dbCriteria != null) {
                for (EvaluationCriteria c : dbCriteria) {
                    EvaluationCriteriaDTO dto = new EvaluationCriteriaDTO();
                    dto.setId(c.getId());
                    String code = c.getCriterionCode();
                    IndicatorDefinition ind = code != null ? officialMap.get(code) : null;
                    dto.setCriterionCode(code);
                    dto.setDimension(ind != null ? ind.getDimension() : c.getDimension());
                    dto.setOrder(ind != null ? ind.getOrder() : c.getDisplayOrder());
                    String text = ind != null ? ind.getText() : c.getCriteriaName();
                    dto.setText(text);
                    dto.setCriteriaName(text);
                    dto.setAchievementLevel(c.getAchievementLevel());
                    dto.setPoints(c.getPoints());
                    dto.setJustification(c.getJustification());
                    if (c.getPoints() != null) {
                        sumPoints += Math.round(c.getPoints());
                    }
                    dtoList.add(dto);
                }
            }
            dtoList.sort(Comparator.comparing(d -> d.getOrder() != null ? d.getOrder() : Integer.MAX_VALUE));
            if (evaluation.getTotalPoints() == null) {
                evaluation.setTotalPoints(sumPoints);
            }
            evaluation.setObservedIndicators(dtoList.size());
            evaluation.setCriteria(dtoList);
        } else {
            evaluation.setInstrumentName(defaultInstrumentName(evaluation.getType()));
            evaluation.setHistoricalManual(evaluation.getRubricVersion() == null);
            List<EvaluationCriteriaDTO> dtoList = new ArrayList<>();
            if (dbCriteria != null) {
                int idx = 1;
                for (EvaluationCriteria c : dbCriteria) {
                    EvaluationCriteriaDTO dto = new EvaluationCriteriaDTO();
                    dto.setId(c.getId());
                    dto.setCriterionCode(c.getCriterionCode());
                    dto.setDimension(c.getDimension());
                    dto.setOrder(c.getDisplayOrder() != null ? c.getDisplayOrder() : idx);
                    dto.setText(c.getCriteriaName());
                    dto.setCriteriaName(c.getCriteriaName());
                    dto.setAchievementLevel(c.getAchievementLevel());
                    dto.setPoints(c.getPoints());
                    dto.setJustification(c.getJustification());
                    dtoList.add(dto);
                    idx++;
                }
            }
            evaluation.setObservedIndicators(dtoList.size());
            evaluation.setTotalIndicators(dtoList.size());
            evaluation.setCriteria(dtoList);
        }

        if (Boolean.TRUE.equals(evaluation.getIsFinalized())) {
            if (evaluation.getFinalizedAt() == null) {
                evaluation.setFinalizedAt(evaluation.getEvaluationDate() != null ? evaluation.getEvaluationDate() : evaluation.getCreatedAt());
            }
            if (evaluation.getFinalizedByName() == null) {
                if (evaluation.getEvaluator() != null && evaluation.getEvaluator().getFullName() != null) {
                    evaluation.setFinalizedByName(evaluation.getEvaluator().getFullName());
                } else {
                    evaluation.setFinalizedByName(evaluation.getDigitalSignature());
                }
            }
            if (evaluation.getFinalizedByEmail() == null && evaluation.getEvaluator() != null) {
                evaluation.setFinalizedByEmail(evaluation.getEvaluator().getEmail());
            }
            if (evaluation.getFinalizedByRole() == null
                    && evaluation.getEvaluator() != null
                    && evaluation.getEvaluator().getRole() != null) {
                evaluation.setFinalizedByRole(evaluation.getEvaluator().getRole().getName());
            }
        }

        return evaluation;
    }

    private String defaultInstrumentName(EvaluationType type) {
        if (type == null) return "Evaluación de Práctica";
        return switch (type) {
            case PAUTA_OBSERVACION_COLABORADOR -> "Pauta de Observación Colaborador (Histórica)";
            case PAUTA_OBSERVACION_TUTOR -> "Pauta de Observación Tutor (Histórica)";
            case RUBRICA_INFORME -> "Rúbrica de Informe Final";
            case RUBRICA_TANEP -> "Rúbrica TANEP";
            case RUBRICA_PROYECTO_INTERVENCION -> "Rúbrica Proyecto de Intervención";
            case AUTOEVALUACION -> "Autoevaluación";
            case CO_EVALUACION_DUPLA -> "Coevaluación de Dupla";
            default -> type.name();
        };
    }
}
