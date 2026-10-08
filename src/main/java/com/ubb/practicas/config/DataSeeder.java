package com.ubb.practicas.config;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeder de datos iniciales del sistema PITAGORAS (UBB).
 * Inicializa automáticamente los roles, usuarios con hashes BCrypt, periodos académicos,
 * centros de práctica escolar, asignaturas por ciclos, asignaciones, duplas colaborativas,
 * planificaciones didácticas en diferentes estados, documentos de portafolio, evaluaciones,
 * notificaciones y asistencias de chatbot.
 */
@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    public CommandLineRunner cargarDatosIniciales(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PracticeCenterRepository centerRepository,
            SubjectRepository subjectRepository,
            PracticeAssignmentRepository assignmentRepository,
            AcademicPeriodRepository periodRepository,
            StudentPairRepository studentPairRepository,
            PortfolioDocumentRepository documentRepository,
            LessonPlanRepository lessonPlanRepository,
            EvaluationRepository evaluationRepository,
            EvaluationCriteriaRepository criteriaRepository,
            NotificationRepository notificationRepository,
            ChatbotLogRepository chatbotLogRepository,
            ChatbotMessageRepository chatbotMessageRepository) {
        return args -> {
            // 1. Crear Roles si no existen
            if (roleRepository.count() == 0) {
                Role admin = new Role(null, "Administrador");
                Role coord = new Role(null, "Coordinador de practicas");
                Role estudiante = new Role(null, "Estudiante");
                Role profesor = new Role(null, "Profesor de Asignatura");
                Role colaborador = new Role(null, "Profesor Colaborador");
                Role tutor = new Role(null, "Tutor");
                roleRepository.saveAll(List.of(admin, coord, estudiante, profesor, colaborador, tutor));
                System.out.println("SEMILLAS: Roles creados exitosamente.");
            }

            Role rAdmin = roleRepository.findByName("Administrador");
            Role rCoord = roleRepository.findByName("Coordinador de practicas");
            Role rEstudiante = roleRepository.findByName("Estudiante");
            Role rProfesor = roleRepository.findByName("Profesor de Asignatura");
            Role rColaborador = roleRepository.findByName("Profesor Colaborador");
            Role rTutor = roleRepository.findByName("Tutor");

            // 2. Crear Usuarios completos si faltan
            if (userRepository.findByEmail("admin@ubb.cl") == null) {
                userRepository.save(createUser("Patricio Morales Castro", "admin@ubb.cl", "+56941238901", "admin123", rAdmin, passwordEncoder, true));
            }
            if (userRepository.findByEmail("coordinador@ubb.cl") == null) {
                userRepository.save(createUser("Dra. María Elena Valenzuela", "coordinador@ubb.cl", "+56941238902", "coord123", rCoord, passwordEncoder, true));
            }
            // Profesores de Asignatura UBB
            if (userRepository.findByEmail("profesor.inicial@ubb.cl") == null) {
                userRepository.save(createUser("Mg. Roberto San Martín", "profesor.inicial@ubb.cl", "+56941238903", "profe123", rProfesor, passwordEncoder, true));
            }
            if (userRepository.findByEmail("profesor.intermedio@ubb.cl") == null) {
                userRepository.save(createUser("Dra. Carmen Gloria Soto", "profesor.intermedio@ubb.cl", "+56941238904", "profe123", rProfesor, passwordEncoder, true));
            }
            if (userRepository.findByEmail("profesor.avanzado@ubb.cl") == null) {
                userRepository.save(createUser("Dr. Gonzalo Henríquez", "profesor.avanzado@ubb.cl", "+56941238905", "profe123", rProfesor, passwordEncoder, true));
            }
            if (userRepository.findByEmail("profesor@ubb.cl") == null) {
                userRepository.save(createUser("Profesor Asignatura Prueba", "profesor@ubb.cl", "+56941238906", "profesor123", rProfesor, passwordEncoder, true));
            }
            // Tutores UBB
            if (userRepository.findByEmail("tutor@ubb.cl") == null) {
                userRepository.save(createUser("Mg. Claudia Arriagada Lagos", "tutor@ubb.cl", "+56941238907", "tutor123", rTutor, passwordEncoder, true));
            }
            if (userRepository.findByEmail("tutor.chillan@ubb.cl") == null) {
                userRepository.save(createUser("Mg. Francisco Saavedra Muñoz", "tutor.chillan@ubb.cl", "+56941238908", "tutor123", rTutor, passwordEncoder, true));
            }
            if (userRepository.findByEmail("tutor.talcahuano@ubb.cl") == null) {
                userRepository.save(createUser("Mg. Lorena Figueroa Pino", "tutor.talcahuano@ubb.cl", "+56941238909", "tutor123", rTutor, passwordEncoder, true));
            }
            // Profesores Colaboradores en Colegios
            if (userRepository.findByEmail("colaborador@ubb.cl") == null) {
                userRepository.save(createUser("Carlos Riquelme Sandoval", "colaborador@ubb.cl", "+56941238910", "colab123", rColaborador, passwordEncoder, true));
            }
            if (userRepository.findByEmail("marcela.fuentes@liceo.cl") == null) {
                userRepository.save(createUser("Marcela Fuentes Alarcón", "marcela.fuentes@liceo.cl", "+56941238911", "colab123", rColaborador, passwordEncoder, true));
            }
            if (userRepository.findByEmail("rodrigo.paredes@colegio.cl") == null) {
                userRepository.save(createUser("Rodrigo Paredes Bastías", "rodrigo.paredes@colegio.cl", "+56941238912", "colab123", rColaborador, passwordEncoder, true));
            }
            if (userRepository.findByEmail("viviana.carrasco@colegio.cl") == null) {
                userRepository.save(createUser("Viviana Carrasco Vera", "viviana.carrasco@colegio.cl", "+56941238913", "colab123", rColaborador, passwordEncoder, true));
            }
            // Estudiantes
            if (userRepository.findByEmail("estudiante@ubb.cl") == null) {
                userRepository.save(createUser("Matías Esteban Muñoz Pino", "estudiante@ubb.cl", "+56941238914", "estudiante123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante1@ubb.cl") == null) {
                userRepository.save(createUser("Valentina Ignacia Castro Rivas", "estudiante1@ubb.cl", "+56941238915", "estudiante1123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante2@ubb.cl") == null) {
                userRepository.save(createUser("Diego Alonso Santander Navarrete", "estudiante2@ubb.cl", "+56941238916", "estudiante2123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante3@ubb.cl") == null) {
                userRepository.save(createUser("Fernanda Paz Sepúlveda Jara", "estudiante3@ubb.cl", "+56941238917", "estudiante3123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante4@ubb.cl") == null) {
                userRepository.save(createUser("Benjamín Andrés Vera Morales", "estudiante4@ubb.cl", "+56941238918", "estudiante123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante5@ubb.cl") == null) {
                userRepository.save(createUser("Camila Javiera Toledo Lagos", "estudiante5@ubb.cl", "+56941238919", "estudiante123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante6@ubb.cl") == null) {
                userRepository.save(createUser("Felipe Ignacio Carrasco Silva", "estudiante6@ubb.cl", "+56941238920", "estudiante123", rEstudiante, passwordEncoder, true));
            }
            if (userRepository.findByEmail("estudiante.inactivo@ubb.cl") == null) {
                userRepository.save(createUser("Tomás Alexis Romero Venegas", "estudiante.inactivo@ubb.cl", "+56941238921", "estudiante123", rEstudiante, passwordEncoder, false));
            }

            // 3. Periodos Académicos
            if (periodRepository.count() == 0) {
                AcademicPeriod p1 = new AcademicPeriod(null, "Segundo Semestre 2025", 2025, 2, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 12, 20), PeriodStatus.ARCHIVADO);
                AcademicPeriod p2 = new AcademicPeriod(null, "Primer Semestre 2026", 2026, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31), PeriodStatus.CERRADO);
                AcademicPeriod p3 = new AcademicPeriod(null, "Segundo Semestre 2026", 2026, 2, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20), PeriodStatus.ACTIVO);
                periodRepository.saveAll(List.of(p1, p2, p3));
            }
            AcademicPeriod periodoActivo = periodRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PeriodStatus.ACTIVO)
                    .findFirst()
                    .orElse(periodRepository.findAll().get(0));

            // 4. Centros de Práctica
            if (centerRepository.count() <= 1) {
                if (centerRepository.count() == 0) {
                    PracticeCenter c1 = new PracticeCenter(null, "Liceo Bicentenario de Excelencia San Nicolás", SchoolDependency.PUBLICO_SLEP, "Av. Bernardo O'Higgins 345", "San Nicolás", "San Nicolás", "+56422551020", "Víctor Reyes Reyes", "director@bicentenariosannicolas.cl");
                    centerRepository.save(c1);
                }
                PracticeCenter c2 = new PracticeCenter(null, "Colegio Concepción San Pedro", SchoolDependency.PRIVADO, "Los Robles 123, Villa San Pedro", "San Pedro de la Paz", "San Pedro de la Paz", "+56412798000", "Juan Pablo Ortiz", "rectoria@colegioconcepcionsanpedro.cl");
                PracticeCenter c3 = new PracticeCenter(null, "Liceo de Niñas Marta Brunet Cáraves", SchoolDependency.PUBLICO_SLEP, "Claudio Arrau 655", "Chillán", "Chillán", "+56422223450", "Ana María Rosas", "contacto@liceomartabrunet.cl");
                PracticeCenter c4 = new PracticeCenter(null, "Colegio Salesiano de Concepción", SchoolDependency.SUBVENCIONADO, "Av. San Juan Bosco 140", "Concepción", "Concepción", "+56412727100", "Pbro. Claudio Cartes", "rectoria@salesianoconcepcion.cl");
                PracticeCenter c5 = new PracticeCenter(null, "Liceo Polivalente de San Nicolás", SchoolDependency.PUBLICO_SLEP, "Balmaceda 180", "San Nicolás", "San Nicolás", "+56422551100", "Nelson Silva Morales", "contacto@polivalentesannicolas.cl");
                PracticeCenter c6 = new PracticeCenter(null, "CEIA Centro Educacional de Adultos Talcahuano", SchoolDependency.ADULTOS, "Av. Cristóbal Colón 250", "Talcahuano", "Talcahuano", "+56412543210", "Rosa Hermosilla Pérez", "direccion@ceiatlc.cl");
                centerRepository.saveAll(List.of(c2, c3, c4, c5, c6));
            }

            // 5. Asignaturas de Práctica
            User profInicial = userRepository.findByEmail("profesor.inicial@ubb.cl") != null ? userRepository.findByEmail("profesor.inicial@ubb.cl") : userRepository.findByEmail("profesor@ubb.cl");
            User profIntermedio = userRepository.findByEmail("profesor.intermedio@ubb.cl") != null ? userRepository.findByEmail("profesor.intermedio@ubb.cl") : profInicial;
            User profAvanzado = userRepository.findByEmail("profesor.avanzado@ubb.cl") != null ? userRepository.findByEmail("profesor.avanzado@ubb.cl") : profInicial;

            if (subjectRepository.count() <= 2) {
                Subject s1 = new Subject(null, "Práctica Inicial: Identidad y Contexto Educativo", PracticeCycle.INICIAL, "PEM-110", profInicial);
                Subject s2 = new Subject(null, "Práctica Inicial: Observación y Ayudantía en Aula", PracticeCycle.INICIAL, "PEM-120", profInicial);
                Subject s3 = new Subject(null, "Práctica Pedagógica Intermedia: Diseño y Transposición Didáctica", PracticeCycle.INTERMEDIO, "PEM-210", profIntermedio);
                Subject s4 = new Subject(null, "Práctica Pedagógica Intermedia: Gestión y Evaluación de Aula", PracticeCycle.INTERMEDIO, "PEM-220", profIntermedio);
                Subject s5 = new Subject(null, "Práctica Profesional y Síntesis Curricular", PracticeCycle.AVANZADO, "PEM-310", profAvanzado);
                subjectRepository.saveAll(List.of(s1, s2, s3, s4, s5));
            }

            // 6. Asignaciones de Práctica
            if (assignmentRepository.count() <= 4) {
                User uEst1 = userRepository.findByEmail("estudiante@ubb.cl");
                User uEst2 = userRepository.findByEmail("estudiante1@ubb.cl");
                User uEst3 = userRepository.findByEmail("estudiante2@ubb.cl");
                User uEst4 = userRepository.findByEmail("estudiante3@ubb.cl");
                User uEst5 = userRepository.findByEmail("estudiante4@ubb.cl");
                User uEst6 = userRepository.findByEmail("estudiante5@ubb.cl");

                User uColab1 = userRepository.findByEmail("marcela.fuentes@liceo.cl") != null ? userRepository.findByEmail("marcela.fuentes@liceo.cl") : userRepository.findByEmail("colaborador@ubb.cl");
                User uColab2 = userRepository.findByEmail("colaborador@ubb.cl");
                User uColab3 = userRepository.findByEmail("viviana.carrasco@colegio.cl") != null ? userRepository.findByEmail("viviana.carrasco@colegio.cl") : uColab2;
                User uColab4 = userRepository.findByEmail("rodrigo.paredes@colegio.cl") != null ? userRepository.findByEmail("rodrigo.paredes@colegio.cl") : uColab2;

                User uTutor1 = userRepository.findByEmail("tutor@ubb.cl");
                User uTutor2 = userRepository.findByEmail("tutor.chillan@ubb.cl") != null ? userRepository.findByEmail("tutor.chillan@ubb.cl") : uTutor1;
                User uTutor3 = userRepository.findByEmail("tutor.talcahuano@ubb.cl") != null ? userRepository.findByEmail("tutor.talcahuano@ubb.cl") : uTutor1;

                Subject subIni = subjectRepository.findAll().stream().filter(s -> s.getCycle() == PracticeCycle.INICIAL).findFirst().orElse(null);
                Subject subInt = subjectRepository.findAll().stream().filter(s -> s.getCycle() == PracticeCycle.INTERMEDIO).findFirst().orElse(null);
                Subject subAvn = subjectRepository.findAll().stream().filter(s -> s.getCycle() == PracticeCycle.AVANZADO).findFirst().orElse(subInt);

                List<PracticeCenter> centros = centerRepository.findAll();
                PracticeCenter centSanPedro = centros.stream().filter(c -> c.getName().contains("San Pedro")).findFirst().orElse(centros.get(0));
                PracticeCenter centSanNicolas = centros.stream().filter(c -> c.getName().contains("San Nicolás")).findFirst().orElse(centros.get(0));
                PracticeCenter centSalesiano = centros.stream().filter(c -> c.getName().contains("Salesiano")).findFirst().orElse(centros.get(0));
                PracticeCenter centMarta = centros.stream().filter(c -> c.getName().contains("Marta Brunet")).findFirst().orElse(centros.get(0));

                if (uEst2 != null && uEst3 != null && subInt != null) {
                    PracticeAssignment asig2 = new PracticeAssignment(null, uEst2, subInt, centSanPedro, uColab1, uTutor1, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                    PracticeAssignment asig3 = new PracticeAssignment(null, uEst3, subInt, centSanPedro, uColab1, uTutor1, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                    asig2 = assignmentRepository.save(asig2);
                    asig3 = assignmentRepository.save(asig3);

                    // 7. Dupla 1
                    if (studentPairRepository.count() == 0) {
                        StudentPair pair1 = new StudentPair(null, asig2, asig3, periodoActivo, LocalDateTime.now());
                        studentPairRepository.save(pair1);
                    }

                    if (uEst4 != null && uEst5 != null) {
                        PracticeAssignment asig4 = new PracticeAssignment(null, uEst4, subInt, centSanNicolas, uColab2, uTutor2, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                        PracticeAssignment asig5 = new PracticeAssignment(null, uEst5, subInt, centSanNicolas, uColab2, uTutor2, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                        asig4 = assignmentRepository.save(asig4);
                        asig5 = assignmentRepository.save(asig5);

                        StudentPair pair2 = new StudentPair(null, asig4, asig5, periodoActivo, LocalDateTime.now());
                        studentPairRepository.save(pair2);
                    }

                    if (uEst1 != null && subIni != null) {
                        PracticeAssignment asig1 = new PracticeAssignment(null, uEst1, subIni, centMarta, uColab4, uTutor1, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                        assignmentRepository.save(asig1);
                    }

                    if (uEst6 != null && subAvn != null) {
                        PracticeAssignment asig6 = new PracticeAssignment(null, uEst6, subAvn, centSalesiano, uColab3, uTutor3, periodoActivo, AssignmentStatus.ACTIVA, LocalDateTime.now());
                        assignmentRepository.save(asig6);
                    }
                }
            }

            // 8. Documentos y Planificaciones
            if (lessonPlanRepository.count() == 0) {
                PracticeAssignment asigEstudiante1 = assignmentRepository.findAll().stream()
                        .filter(a -> a.getStudent() != null && "estudiante1@ubb.cl".equals(a.getStudent().getEmail()))
                        .findFirst().orElse(null);

                if (asigEstudiante1 != null) {
                    PortfolioDocument doc1 = new PortfolioDocument(null, asigEstudiante1, DocumentType.PLANIFICACION,
                            "Planificacion_Geometria_8Basico_Pitagoras.pdf",
                            "/api/portafolio/descargar/6a49598f-c0ed-48d2-a6d1-107baf60298a.pdf",
                            "6a49598f-c0ed-48d2-a6d1-107baf60298a.pdf",
                            "application/pdf", 245120L, LocalDateTime.now().minusDays(5));
                    doc1 = documentRepository.save(doc1);

                    PortfolioDocument docGuia = new PortfolioDocument(null, asigEstudiante1, DocumentType.GUIA_TRABAJO,
                            "Guia_Ejercicios_Teorema_Pitagoras_8B.pdf",
                            "/api/portafolio/descargar/1bb5e2bc-5b7f-454b-8788-bb47c67f2ab6.pdf",
                            "1bb5e2bc-5b7f-454b-8788-bb47c67f2ab6.pdf",
                            "application/pdf", 182300L, LocalDateTime.now().minusDays(5));
                    documentRepository.save(docGuia);

                    String jsonPitagoras = "{\"inicio\": {\"minutos\": 15, \"descripcion\": \"Activación de conocimientos previos sobre áreas de cuadrados y triángulos rectángulos. Pregunta desafiante: ¿Cómo calcular la longitud de una rampa sin medirla directamente?\", \"recursos\": [\"Proyector\", \"GeoGebra interactivo\", \"Pizarra\"]}, \"desarrollo\": {\"minutos\": 60, \"descripcion\": \"Demostración geométrica del Teorema de Pitágoras mediante descomposición de áreas. Trabajo colaborativo en parejas con guía de aprendizaje con problemas de contexto real.\", \"recursos\": [\"Guía impresa\", \"Cartulina milimetrada\", \"Tijeras\", \"Calculadoras\"]}, \"cierre\": {\"minutos\": 15, \"descripcion\": \"Ticket de salida individual y metacognición grupal sobre aplicaciones en arquitectura y diseño.\", \"recursos\": [\"Tickets de salida\"]}, \"adaptaciones_pie\": \"Material concreto tangible y apoyo personalizado de profesor co-docente.\"}";

                    LessonPlan plan1 = new LessonPlan(null, asigEstudiante1, doc1, PlanStatus.APROBADO, "8° Básico",
                            MathAxis.GEOMETRIA,
                            "OA 12: Explicar de manera concreta, pictórica y simbólica la validez del teorema de Pitágoras y aplicarlo a la resolución de problemas geométricos y de la vida cotidiana.",
                            "1. Comprender la relación métrica fundamental entre catetos e hipotenusa.\n2. Resolver situaciones contextualizadas de cálculo de distancias usando el teorema.\n3. Modelar figuras geométricas compuestas.",
                            jsonPitagoras,
                            "Excelente transposición didáctica. Las actividades de inicio enganchan adecuadamente y la conexión con GeoGebra favorece la comprensión visual del teorema. Planificación plenamente aprobada para ejecución.",
                            1, null, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(4));
                    lessonPlanRepository.save(plan1);

                    // Planificación 2 (En revisión docente)
                    PracticeAssignment asigEstudiante3 = assignmentRepository.findAll().stream()
                            .filter(a -> a.getStudent() != null && "estudiante3@ubb.cl".equals(a.getStudent().getEmail()))
                            .findFirst().orElse(asigEstudiante1);

                    String jsonAlgebra = "{\"inicio\": {\"minutos\": 15, \"descripcion\": \"Problema contextualizado de tarifas de planes móviles.\"}, \"desarrollo\": {\"minutos\": 60, \"descripcion\": \"Modelamiento en pizarra del método gráfico y resolución de sistemas 2x2.\"}, \"cierre\": {\"minutos\": 15, \"descripcion\": \"Síntesis de criterios algebraicos de clasificación de soluciones.\"}}";
                    LessonPlan plan2 = new LessonPlan(null, asigEstudiante3, null, PlanStatus.EN_REVISION_DOCENTE, "1° Medio",
                            MathAxis.ALGEBRA,
                            "OA 04: Resolver sistemas de ecuaciones lineales (2x2) relacionados con problemas de la vida diaria.",
                            "1. Representar gráficamente ecuaciones ax + by = c.\n2. Identificar el punto de intersección de dos rectas como la solución única.",
                            jsonAlgebra,
                            "Pendiente de validación presencial con el profesor colaborador.",
                            1, null, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2));
                    lessonPlanRepository.save(plan2);

                    // 9. Evaluaciones y Criterios
                    if (evaluationRepository.count() == 0) {
                        User tutorClaudia = asigEstudiante1.getTutor();
                        Evaluation eval1 = new Evaluation(null, asigEstudiante1, tutorClaudia, EvaluationType.PAUTA_OBSERVACION_TUTOR,
                                6.8f, "Excelente manejo del aula y dominio disciplinar del Teorema de Pitágoras. El clima de aprendizaje fue muy propicio para la participación activa.",
                                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3));
                        eval1 = evaluationRepository.save(eval1);

                        EvaluationCriteria c1 = new EvaluationCriteria(null, eval1, "Dominio disciplinar y transposición didáctica", AchievementLevel.LOGRO_ALTO, 1.0f, "Explica con rigurosidad y analogías pertinentes.");
                        EvaluationCriteria c2 = new EvaluationCriteria(null, eval1, "Estructura de los 3 momentos de la clase", AchievementLevel.LOGRO_ALTO, 1.0f, "Tiempos bien distribuidos y claros.");
                        EvaluationCriteria c3 = new EvaluationCriteria(null, eval1, "Evaluación formativa y atención a la diversidad", AchievementLevel.LOGRO_ADECUADO, 0.8f, "Buen monitoreo con material accesible.");
                        criteriaRepository.saveAll(List.of(c1, c2, c3));
                    }

                    // 10. Notificaciones
                    if (notificationRepository.count() == 0) {
                        Notification notif1 = new Notification(null, asigEstudiante1.getStudent(), NotificationType.PLANIFICACION_APROBADA,
                                "Tu planificación 'Teorema de Pitágoras 8° Básico' ha sido aprobada por la Dra. Carmen Gloria Soto.",
                                false, LocalDateTime.now().minusDays(4), plan1.getId(), "PLANIFICACION");
                        Notification notif2 = new Notification(null, asigEstudiante1.getStudent(), NotificationType.EVALUACION_PUBLICADA,
                                "Tu tutora Mg. Claudia Arriagada ha publicado la pauta de observación de tu clase.",
                                false, LocalDateTime.now().minusDays(3), 1L, "EVALUACION");
                        notificationRepository.saveAll(List.of(notif1, notif2));
                    }

                    // 11. Chatbot Log y Mensajes IA
                    if (chatbotLogRepository.count() == 0) {
                        ChatbotLog chatLog = new ChatbotLog(null, plan1,
                                "El objetivo de aprendizaje está formulado con precisión taxonómica. Se sugiere fortalecer la justificación del momento de cierre para consolidar la distinción conceptual.",
                                true, 1, LocalDateTime.now().minusDays(5));
                        chatLog = chatbotLogRepository.save(chatLog);

                        ChatbotMessage m1 = new ChatbotMessage(null, chatLog, MessageRole.ESTUDIANTE,
                                "¿Cómo puedo justificar mejor la necesidad de la desviación estándar frente a la varianza en 2° medio?",
                                1, LocalDateTime.now().minusDays(5));
                        ChatbotMessage m2 = new ChatbotMessage(null, chatLog, MessageRole.ASISTENTE_IA,
                                "Puedes explicar a los estudiantes que la varianza queda expresada en unidades cuadradas, mientras que la desviación estándar devuelve la medida a la unidad original de los datos, facilitando su interpretación directa.",
                                2, LocalDateTime.now().minusDays(5));
                        chatbotMessageRepository.saveAll(List.of(m1, m2));
                    }
                }
            }

            System.out.println("=================================================================");
            System.out.println(">>> SEMILLAS DE DATOS COMPLETAS CARGADAS EXITOSAMENTE EN PITAGORAS");
            System.out.println(">>> Roles: " + roleRepository.count() + " | Usuarios: " + userRepository.count() + " | Periodos: " + periodRepository.count());
            System.out.println(">>> Centros: " + centerRepository.count() + " | Asignaturas: " + subjectRepository.count() + " | Asignaciones: " + assignmentRepository.count());
            System.out.println(">>> Duplas: " + studentPairRepository.count() + " | Planificaciones: " + lessonPlanRepository.count() + " | Evaluaciones: " + evaluationRepository.count());
            System.out.println("=================================================================");
        };
    }

    private User createUser(String fullName, String email, String phone, String rawPassword, Role role, PasswordEncoder passwordEncoder, boolean active) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setActive(active);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
