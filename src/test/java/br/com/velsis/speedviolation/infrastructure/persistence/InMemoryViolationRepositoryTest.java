package br.com.velsis.speedviolation.infrastructure.persistence;

import br.com.velsis.speedviolation.domain.model.CaptureOrigin;
import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.model.ViolationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryViolationRepositoryTest {

    private InMemoryViolationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryViolationRepository();
    }

    @Test
    void shouldSaveAndFindViolationsByNormalizedLicensePlate() {
        ViolationRecord first = createRecord("abc1d23", 120);
        ViolationRecord second = createRecord("ABC1D23", 135);

        ViolationRecord savedFirst = repository.save(first);
        ViolationRecord savedSecond = repository.save(second);

        assertThat(savedFirst).isEqualTo(first);
        assertThat(savedSecond).isEqualTo(second);

        List<ViolationRecord> results = repository.findByLicensePlate("  abc1d23 ");
        assertThat(results).containsExactly(first, second);
    }

    @Test
    void shouldReturnEmptyListWhenLicensePlateHasNoViolations() {
        assertThat(repository.findByLicensePlate("XYZ9999")).isEmpty();
        assertThat(repository.findByLicensePlate(null)).isEmpty();
    }

    @Test
    void shouldHandleConcurrentWritesSafely() throws Exception {
        int threads = 8;
        int recordsPerThread = 50;
        int totalExpected = threads * recordsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            futures.add(executor.submit(() -> {
                startLatch.await();
                for (int i = 0; i < recordsPerThread; i++) {
                    repository.save(createRecord("CONC001", 110 + threadIndex + i));
                }
                return null;
            }));
        }

        startLatch.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        List<ViolationRecord> stored = repository.findByLicensePlate("CONC001");
        assertThat(stored).hasSize(totalExpected);
    }

    private ViolationRecord createRecord(String plate, int measuredSpeed) {
        return new ViolationRecord(
                UUID.randomUUID(),
                plate,
                "EQ-100",
                CaptureOrigin.FIXED,
                measuredSpeed,
                measuredSpeed - 7,
                100,
                10.0,
                ViolationSeverity.MEDIUM,
                ViolationSeverity.MEDIUM.getCtbCode(),
                Instant.parse("2026-11-24T13:00:00Z"),
                Instant.parse("2026-11-24T13:00:01Z")
        );
    }
}
