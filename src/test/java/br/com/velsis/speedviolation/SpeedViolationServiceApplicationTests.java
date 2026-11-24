package br.com.velsis.speedviolation;

import br.com.velsis.speedviolation.application.config.SpeedToleranceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SpeedViolationServiceApplicationTests {

	@Autowired
	private SpeedToleranceProperties toleranceProperties;

	@Test
	void contextLoads() {
		var policy = toleranceProperties.toDomainPolicy();
		assertThat(policy.fixedMarginKmh()).isEqualTo(7);
		assertThat(policy.percentageMargin()).isEqualTo(7.0);
		assertThat(policy.speedLimitThreshold()).isEqualTo(100);
	}

}
