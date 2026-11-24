package br.com.velsis.speedviolation.infrastructure.web.dto;

import br.com.velsis.speedviolation.domain.model.ViolationDetails;

public record ViolationDetailsResponse(
        String severity,
        String ctbCode
) {
    public static ViolationDetailsResponse fromDomain(ViolationDetails details) {
        if (details == null) {
            return null;
        }
        return new ViolationDetailsResponse(details.severity().name(), details.ctbCode());
    }
}
