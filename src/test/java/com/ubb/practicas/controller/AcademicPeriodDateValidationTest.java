package com.ubb.practicas.controller;

import com.ubb.practicas.model.AcademicPeriod;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.repository.AcademicPeriodRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicPeriodDateValidationTest {
    private AcademicPeriodRepository periods;
    private AcademicPeriodController controller;

    @BeforeEach
    void setUp() {
        periods = mock(AcademicPeriodRepository.class);
        controller = new AcademicPeriodController(periods, mock(PracticeAssignmentRepository.class));
    }

    private AcademicPeriod period(String start, String end) {
        AcademicPeriod period = new AcademicPeriod();
        period.setName("2027-1");
        period.setStatus(PeriodStatus.ACTIVO);
        period.setStartDate(start == null ? null : LocalDate.parse(start));
        period.setEndDate(end == null ? null : LocalDate.parse(end));
        return period;
    }

    @Test
    void rejectsReversedDatesBeforeCreatingOrClosingTheCurrentPeriod() {
        AcademicPeriod invalid = period("2027-07-01", "2027-03-01");
        assertThrows(IllegalArgumentException.class, () -> controller.crearPeriodo(invalid));
        verifyNoInteractions(periods);
    }

    @Test
    void rejectsReversedDatesBeforeChangingAnExistingPeriod() {
        AcademicPeriod invalid = period("2027-07-01", "2027-03-01");
        assertThrows(IllegalArgumentException.class, () -> controller.actualizarPeriodo(10L, invalid));
        verifyNoInteractions(periods);
    }

    @ParameterizedTest
    @CsvSource({"2027-03-01,2027-07-01", "2027-03-01,2027-03-01", ",2027-07-01", "2027-03-01,", ","})
    void acceptsOrderedEqualOrOptionalDates(String start, String end) {
        AcademicPeriod valid = period(start, end);
        when(periods.findByStatus(PeriodStatus.ACTIVO)).thenReturn(List.of());
        when(periods.save(valid)).thenReturn(valid);
        assertSame(valid, controller.crearPeriodo(valid).getBody());
        verify(periods).save(valid);
    }
}
