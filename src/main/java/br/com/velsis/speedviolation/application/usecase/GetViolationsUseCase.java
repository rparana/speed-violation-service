package br.com.velsis.speedviolation.application.usecase;

import br.com.velsis.speedviolation.domain.model.ViolationRecord;
import br.com.velsis.speedviolation.domain.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class GetViolationsUseCase {

    private final ViolationRepository repository;

    public GetViolationsUseCase(ViolationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public List<ViolationRecord> execute(String licensePlate) {
        return repository.findByLicensePlate(licensePlate);
    }
}
