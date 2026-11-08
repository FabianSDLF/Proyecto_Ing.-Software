"""
PITAGORAS - Suite de Pruebas Automatizadas para el Microservicio de Chatbot
Verifica la resiliencia, validación de esquemas y lógica de evaluación pedagógica.
"""

import sys
import unittest
from main import (
    generar_chat_mock,
    generar_evaluacion_mock,
    LessonPlanEvaluationRequest,
    LessonPlanEvaluationResponse,
    ChatRequest,
    health_check,
    evaluar_planificacion_endpoint,
    chat_endpoint
)

class TestChatbotService(unittest.TestCase):

    def test_health_check(self):
        """Verifica que el healthcheck responda correctamente"""
        res = health_check()
        self.assertEqual(res["status"], "healthy")
        self.assertEqual(res["service"], "pitagoras-chatbot")
        self.assertIn("mode", res)
        print("  [OK] Healthcheck retorna estado saludable")

    def test_mock_chat_pedagogico(self):
        """Verifica respuestas pedagógicas simuladas según palabras clave"""
        r_inicio = generar_chat_mock("¿Cómo hacer el inicio de mi clase?")
        self.assertIn("Inicio", r_inicio)
        self.assertIn("conocimientos previos", r_inicio.lower())

        r_cierre = generar_chat_mock("¿Qué sugerencias tienes para el cierre y metacognición?")
        self.assertIn("Cierre", r_cierre)
        self.assertIn("ticket de salida", r_cierre.lower())

        r_oa = generar_chat_mock("¿Cómo redactar un objetivo con taxonomía de Bloom?")
        self.assertIn("Objetivo", r_oa)
        self.assertIn("Verbo", r_oa)

        r_frac = generar_chat_mock("3. Quiero enseñar operatoria con fracciones en 6° básico, ¿qué conocimientos previos deben tener consolidados los alumnos?")
        self.assertIn("fracciones", r_frac.lower())
        self.assertIn("6° Básico", r_frac)
        self.assertIn("MCM", r_frac)

        r_prob = generar_chat_mock("¿Cuál es el enfoque didáctico sugerido por el MINEDUC para introducir la Probabilidad en educación básica?")
        self.assertIn("MINEDUC", r_prob)
        self.assertIn("frecuencial", r_prob.lower())

        r_ent = generar_chat_mock('Necesito un "gancho" motivacional para enseñar números enteros en 7° básico. ¿Qué me sugieres?')
        self.assertIn("7° Básico", r_ent)
        self.assertIn("Enteros", r_ent)

        r_dua = generar_chat_mock("¿Cómo debo operacionalizar los 3 principios del DUA en matemática?")
        self.assertIn("DUA", r_dua)
        self.assertIn("Representación", r_dua)

        r_autonomia = generar_chat_mock("Hazme una planificación completa desde cero")
        self.assertIn("no puedo hacer ni redactar la planificación por ti", r_autonomia)
        print("  [OK] Respuestas pedagógicas del chat contextualizadas correctamente (Fracciones, Probabilidad, Enteros, DUA, Rúbrica y Regla de Autonomía V3)")

    def test_evaluacion_planificacion_completa(self):
        """Verifica la evaluación heurística de una planificación bien redactada"""
        req = LessonPlanEvaluationRequest(
            schoolGrade="2° Medio",
            mathAxis="ALGEBRA",
            baseLearningObjective="OA 02: Mostrar que comprenden las relaciones entre potencias, raíces enésimas y logaritmos.",
            specificObjectives="Calcular logaritmos utilizando la definición formal y sus propiedades operacionales.",
            objetivoActitudinal="Demostrar interés y esfuerzo en la resolución de problemas abstractos.",
            duracion="90 minutos",
            recursos="Guía de ejercicios, pizarra, calculadora científica",
            evaluacion="Ticket de salida formativo con 2 ejercicios de aplicación",
            inicio="Se activa conocimientos previos sobre potencias de base 10 mediante un desafío en la pizarra. Se presenta el objetivo.",
            desarrollo="El docente modela el concepto de logaritmo como exponente. Los estudiantes resuelven ejercicios en parejas guiados por el docente.",
            cierre="Los estudiantes completan un ticket de salida individual y responden qué relación encontraron entre potencias y logaritmos."
        )

        res = generar_evaluacion_mock(req)
        self.assertIsInstance(res, LessonPlanEvaluationResponse)
        self.assertTrue(res.aiApproved, "Una planificación detallada debe resultar aprobada")
        self.assertGreaterEqual(res.score, 4.0, "La nota debe ser de aprobación (>= 4.0)")
        self.assertTrue(len(res.strengths) >= 2, "Debe identificar fortalezas pedagógicas")
        self.assertIsNotNone(res.momentAnalysis.inicio)
        self.assertIsNotNone(res.momentAnalysis.desarrollo)
        self.assertIsNotNone(res.momentAnalysis.cierre)
        print(f"  [OK] Planificación completa aprobada con nota estimada: {res.score} / 7.0")

    def test_evaluacion_planificacion_deficiente(self):
        """Verifica que una planificación incompleta o escueta reciba nota baja y sugerencias de mejora"""
        req_pobre = LessonPlanEvaluationRequest(
            schoolGrade="1° Medio",
            mathAxis="NUMEROS",
            baseLearningObjective="Numeros",
            specificObjectives="",
            inicio="Hola",
            desarrollo="Hacer guia",
            cierre="Chao"
        )

        res = generar_evaluacion_mock(req_pobre)
        self.assertFalse(res.aiApproved, "Una planificación con momentos vacíos o de una palabra debe reprobar")
        self.assertLess(res.score, 4.0, "La nota debe reflejar deficiencias (< 4.0)")
        self.assertTrue(len(res.improvements) >= 2, "Debe generar sugerencias de mejora")
        print(f"  [OK] Planificación deficiente detectada y reprobada (Nota: {res.score} / 7.0)")

    def test_pydantic_schema_integrity(self):
        """Verifica la integridad de los esquemas Pydantic requeridos por la API"""
        schema = LessonPlanEvaluationResponse.model_json_schema()
        self.assertIn("aiApproved", schema["properties"])
        self.assertIn("score", schema["properties"])
        self.assertIn("feedback", schema["properties"])
        self.assertIn("momentAnalysis", schema["properties"])
        print("  [OK] Esquema JSON de LessonPlanEvaluationResponse es válido y completo")

if __name__ == "__main__":
    print("\n" + "=" * 60)
    print("EJECUTANDO PRUEBAS UNITARIAS DEL CHATBOT PEDAGÓGICO")
    print("=" * 60)
    suite = unittest.TestLoader().loadTestsFromTestCase(TestChatbotService)
    runner = unittest.TextTestRunner(verbosity=0)
    result = runner.run(suite)
    if result.wasSuccessful():
        print("=" * 60)
        print("TODAS LAS PRUEBAS DEL CHATBOT PASARON EXITOSAMENTE (5/5)")
        print("=" * 60 + "\n")
        sys.exit(0)
    else:
        sys.exit(1)
