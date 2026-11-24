package br.com.velsis.speedviolation.infrastructure.web.controller;

import br.com.velsis.speedviolation.application.usecase.EvaluateSpeedUseCase;
import br.com.velsis.speedviolation.application.usecase.GetViolationsUseCase;
import br.com.velsis.speedviolation.domain.model.SpeedReading;
import br.com.velsis.speedviolation.domain.model.ViolationEvaluation;
import br.com.velsis.speedviolation.infrastructure.web.dto.EvaluateSpeedRequest;
import br.com.velsis.speedviolation.infrastructure.web.dto.EvaluateSpeedResponse;
import br.com.velsis.speedviolation.infrastructure.web.dto.ViolationRecordResponse;
import br.com.velsis.speedviolation.infrastructure.web.exception.GlobalExceptionHandler;
import br.com.velsis.speedviolation.infrastructure.web.validator.SpeedReadingRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/violations")
@Tag(name = "Speed Violations", description = "Avaliação de leituras de velocidade e consulta de infrações registradas")
public class SpeedViolationController {

    private static final Logger log = LoggerFactory.getLogger(SpeedViolationController.class);

    private final EvaluateSpeedUseCase evaluateSpeedUseCase;
    private final GetViolationsUseCase getViolationsUseCase;
    private final SpeedReadingRequestValidator validator;

    public SpeedViolationController(
            EvaluateSpeedUseCase evaluateSpeedUseCase,
            GetViolationsUseCase getViolationsUseCase,
            SpeedReadingRequestValidator validator
    ) {
        this.evaluateSpeedUseCase = Objects.requireNonNull(evaluateSpeedUseCase, "evaluateSpeedUseCase must not be null");
        this.getViolationsUseCase = Objects.requireNonNull(getViolationsUseCase, "getViolationsUseCase must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    @PostMapping("/evaluate")
    @Operation(
            summary = "Avaliar leitura de velocidade",
            description = "Aplica a margem de tolerância configurada, calcula eventual excesso e gravidade conforme o CTB e persiste a infração quando caracterizada."
    )
    public ResponseEntity<EvaluateSpeedResponse> evaluate(
            @Parameter(description = "Origem da captura (FIXED, MOBILE ou HANDHELD)", required = true, example = "FIXED")
            @RequestHeader(value = "x-origin", required = false) String origin,
            @RequestBody(required = false) EvaluateSpeedRequest request
    ) {
        storeRequestContext(request);
        SpeedReading reading = validator.validateAndBuild(origin, request);
        ViolationEvaluation evaluation = evaluateSpeedUseCase.execute(reading);
        log.info(
                "Processed speed reading: licensePlate={}, equipmentId={}, consideredSpeed={}, hasViolation={}",
                evaluation.licensePlate(),
                evaluation.equipmentId(),
                evaluation.consideredSpeed(),
                evaluation.hasViolation()
        );
        return ResponseEntity.ok(EvaluateSpeedResponse.fromDomain(evaluation));
    }

    @GetMapping
    @Operation(
            summary = "Consultar infrações por placa",
            description = "Retorna todas as infrações registradas em memória para a placa informada (formato antigo ou Mercosul)."
    )
    public ResponseEntity<List<ViolationRecordResponse>> findByLicensePlate(
            @Parameter(description = "Placa do veículo (ex: ABC1234 ou ABC1D23)", required = true, example = "ABC1D23")
            @RequestParam(value = "licensePlate", required = false) String licensePlate
    ) {
        storeLicensePlateContext(licensePlate);
        String normalizedPlate = validator.validateLicensePlate(licensePlate);
        List<ViolationRecordResponse> response = getViolationsUseCase.execute(normalizedPlate)
                .stream()
                .map(ViolationRecordResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(response);
    }

    private void storeRequestContext(EvaluateSpeedRequest request) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null && request != null) {
            attrs.setAttribute(GlobalExceptionHandler.ATTR_LICENSE_PLATE, request.licensePlate(), RequestAttributes.SCOPE_REQUEST);
            attrs.setAttribute(GlobalExceptionHandler.ATTR_EQUIPMENT_ID, request.equipmentId(), RequestAttributes.SCOPE_REQUEST);
            attrs.setAttribute(GlobalExceptionHandler.ATTR_CAPTURE_TIMESTAMP, request.captureTimestamp(), RequestAttributes.SCOPE_REQUEST);
        }
    }

    private void storeLicensePlateContext(String licensePlate) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(GlobalExceptionHandler.ATTR_LICENSE_PLATE, licensePlate, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
