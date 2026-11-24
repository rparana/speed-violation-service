package br.com.velsis.speedviolation.application.config;

import br.com.velsis.speedviolation.domain.service.SpeedViolationEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DomainBeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SpeedViolationEvaluator speedViolationEvaluator(SpeedToleranceProperties properties, Clock clock) {
        return new SpeedViolationEvaluator(properties.toDomainPolicy(), clock);
    }
}
