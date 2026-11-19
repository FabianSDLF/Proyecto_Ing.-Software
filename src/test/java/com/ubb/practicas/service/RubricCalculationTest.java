package com.ubb.practicas.service;

import com.ubb.practicas.exception.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RubricCalculationTest {
    @ParameterizedTest
    @CsvSource({"38,133,3.0","36,126,3.0","46,161,3.0","78,133,4.0","79,133,4.0","80,133,4.0","74,126,4.0","75,126,4.0","76,126,4.0","95,161,4.0","96,161,4.0","97,161,4.0","133,133,7.0","126,126,7.0","161,161,7.0","0,133,2.0"})
    void reproducesClientConversionTables(int points,int max,float grade) {
        assertEquals(grade,EvaluationRubricCatalog.calculateOfficialGrade(points,max));
    }
    @Test void rejectDecimalsAndNonFinitePoints() {
        for(float p:new float[]{0,1,7.1f,2.000001f,Float.NaN,Float.POSITIVE_INFINITY}) assertFalse(EvaluationRubricCatalog.isAllowedPointValue(p));
        assertThrows(BadRequestException.class,()->EvaluationRubricCatalog.calculateOfficialGrade(134,133));
    }
}
