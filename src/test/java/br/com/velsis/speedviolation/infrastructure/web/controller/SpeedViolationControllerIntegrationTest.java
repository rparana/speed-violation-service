package br.com.velsis.speedviolation.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class SpeedViolationControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldEvaluateViolationPersistAndReturnInGetQuery() throws Exception {
    String plate = "INT1A23";
    String timestamp = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
    String payload = """
        {
          "licensePlate": "%s",
          "measuredSpeed": 127,
          "speedLimit": 100,
          "equipmentId": "EQ-INT-01",
          "captureTimestamp": "%s"
        }
        """.formatted(plate, timestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "FIXED")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.licensePlate").value(plate))
        .andExpect(jsonPath("$.equipmentId").value("EQ-INT-01"))
        .andExpect(jsonPath("$.measuredSpeed").value(127))
        .andExpect(jsonPath("$.consideredSpeed").value(120))
        .andExpect(jsonPath("$.speedLimit").value(100))
        .andExpect(jsonPath("$.excessPercentage").value(20.0))
        .andExpect(jsonPath("$.hasViolation").value(true))
        .andExpect(jsonPath("$.violation.severity").value("MEDIUM"))
        .andExpect(jsonPath("$.violation.ctbCode").value("218-I"))
        .andExpect(jsonPath("$.processedAt").isNotEmpty());

    mockMvc.perform(get("/api/v1/violations")
        .param("licensePlate", plate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").isNotEmpty())
        .andExpect(jsonPath("$[0].licensePlate").value(plate))
        .andExpect(jsonPath("$[0].origin").value("FIXED"))
        .andExpect(jsonPath("$[0].severity").value("MEDIUM"))
        .andExpect(jsonPath("$[0].ctbCode").value("218-I"));
  }

  @Test
  void shouldEvaluateWithoutViolationAndNotPersist() throws Exception {
    String plate = "INT2B34";
    String timestamp = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
    String payload = """
        {
          "licensePlate": "%s",
          "measuredSpeed": 67,
          "speedLimit": 60,
          "equipmentId": "EQ-INT-02",
          "captureTimestamp": "%s"
        }
        """.formatted(plate, timestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "MOBILE")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.licensePlate").value(plate))
        .andExpect(jsonPath("$.consideredSpeed").value(60))
        .andExpect(jsonPath("$.excessPercentage").value(0.0))
        .andExpect(jsonPath("$.hasViolation").value(false))
        .andExpect(jsonPath("$.violation").isEmpty());

    mockMvc.perform(get("/api/v1/violations")
        .param("licensePlate", plate))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void shouldReturnEmptyListForPlateWithoutViolations() throws Exception {
    mockMvc.perform(get("/api/v1/violations")
        .param("licensePlate", "NOP9Z99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void shouldReturn400WhenLicensePlateIsInvalid() throws Exception {
    String timestamp = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
    String payload = """
        {
          "licensePlate": "INVALID",
          "measuredSpeed": 120,
          "speedLimit": 100,
          "equipmentId": "EQ-INT-03",
          "captureTimestamp": "%s"
        }
        """.formatted(timestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "FIXED")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_LICENSE_PLATE"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void shouldReturn400WhenOriginHeaderIsMissingOrLowercase() throws Exception {
    String timestamp = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
    String payload = """
        {
          "licensePlate": "ABC1D23",
          "measuredSpeed": 120,
          "speedLimit": 100,
          "equipmentId": "EQ-INT-04",
          "captureTimestamp": "%s"
        }
        """.formatted(timestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ORIGIN"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "fixed")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ORIGIN"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void shouldReturn400WhenCaptureTimestampIsInTheFuture() throws Exception {
    String futureTimestamp = Instant.now().plus(1, ChronoUnit.HOURS).toString();
    String payload = """
        {
          "licensePlate": "ABC1D23",
          "measuredSpeed": 120,
          "speedLimit": 100,
          "equipmentId": "EQ-INT-05",
          "captureTimestamp": "%s"
        }
        """.formatted(futureTimestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "HANDHELD")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("FUTURE_CAPTURE_TIMESTAMP"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void shouldReturn400WhenJsonIsMalformed() throws Exception {
    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "FIXED")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"licensePlate\": \"ABC1D23\", \"measuredSpeed\": \"not-a-number\" "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message")
            .value("Malformed request payload or invalid parameter type"))
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
  }

  @Test
  void shouldExposeActuatorHealthAndPrometheusMetrics() throws Exception {
    String timestamp = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
    String payload = """
        {
          "licensePlate": "MET1A23",
          "measuredSpeed": 135,
          "speedLimit": 100,
          "equipmentId": "EQ-MET-01",
          "captureTimestamp": "%s"
        }
        """.formatted(timestamp);

    mockMvc.perform(post("/api/v1/violations/evaluate")
        .header("x-origin", "FIXED")
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isOk());

    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("speed_evaluations_total")))
        .andExpect(content().string(containsString("speed_violations_total")));
  }
}
