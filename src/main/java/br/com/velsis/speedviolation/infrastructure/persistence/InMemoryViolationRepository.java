package br.com.velsis.speedviolation.infrastructure.persistence;

import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.repository.ViolationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryViolationRepository implements ViolationRepository {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ViolationRecord>> storage = new ConcurrentHashMap<>();

    @Override
    public ViolationRecord save(ViolationRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        String normalizedPlate = normalizePlate(record.licensePlate());
        storage.computeIfAbsent(normalizedPlate, key -> new CopyOnWriteArrayList<>())
                .add(record);
        return record;
    }

    @Override
    public List<ViolationRecord> findByLicensePlate(String licensePlate) {
        if (licensePlate == null) {
            return List.of();
        }
        CopyOnWriteArrayList<ViolationRecord> records = storage.get(normalizePlate(licensePlate));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return List.copyOf(records);
    }

    private String normalizePlate(String licensePlate) {
        Objects.requireNonNull(licensePlate, "licensePlate must not be null");
        return licensePlate.trim().toUpperCase(Locale.ROOT);
    }
}
