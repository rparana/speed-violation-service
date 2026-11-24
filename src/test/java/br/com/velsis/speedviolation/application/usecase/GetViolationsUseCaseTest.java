package br.com.velsis.speedviolation.application.usecase;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import br.com.velsis.speedviolation.domain.repository.ViolationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetViolationsUseCaseTest {

    @Mock
    private ViolationRepository repository;

    private GetViolationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetViolationsUseCase(repository);
    }

    @Test
    void shouldDelegateQueryToRepository() {
        ViolationRecord record = new ViolationRecord(
                UUID.randomUUID(),
                "ABC1D23",
                "EQ-001",
                CaptureOrigin.HANDHELD,
                130,
                123,
                100,
                23.0,
                ViolationSeverity.SERIOUS,
                "218-II",
                Instant.parse("2026-11-24T13:59:00Z"),
                Instant.parse("2026-11-24T14:00:00Z")
        );
        when(repository.findByLicensePlate("ABC1D23")).thenReturn(List.of(record));

        List<ViolationRecord> result = useCase.execute("ABC1D23");

        assertThat(result).containsExactly(record);
        verify(repository).findByLicensePlate("ABC1D23");
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryFindsNoViolations() {
        when(repository.findByLicensePlate("XYZ9999")).thenReturn(List.of());

        List<ViolationRecord> result = useCase.execute("XYZ9999");

        assertThat(result).isEmpty();
        verify(repository).findByLicensePlate("XYZ9999");
    }
}
