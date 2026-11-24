package br.com.velsis.speedviolation.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI speedViolationOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Speed Violation Service API")
                                                .version("1.0.0")
                                                .description("API para apuração de infrações de excesso de velocidade conforme regras do CTB e consulta de registros por placa."))
                                .servers(List.of(new Server()
                                                .url("/")
                                                .description("Servidor da aplicação")));
        }
}
