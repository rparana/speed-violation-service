package br.com.velsis.speedviolation.domain.repository;

import br.com.velsis.speedviolation.domain.model.ViolationRecord;

import java.util.List;

public interface ViolationRepository {

    ViolationRecord save(ViolationRecord record);

    List<ViolationRecord> findByLicensePlate(String licensePlate);
}
