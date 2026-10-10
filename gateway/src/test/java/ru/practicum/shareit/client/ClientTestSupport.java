package ru.practicum.shareit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

abstract class ClientTestSupport {
    protected static final String BASE = "http://localhost:9090";
    protected static final String HEADER = "X-Sharer-User-Id";
    protected MockRestServiceServer server;
    protected RestTemplate rest;
    protected ObjectMapper objectMapper;
    private HttpClient httpClient;

    @BeforeEach
    void setUpTransport() {
        ClientConfiguration configuration = new ClientConfiguration();
        httpClient = configuration.shareItHttpClient(Duration.ofSeconds(2));
        rest = configuration.shareItRestTemplate(new RestTemplateBuilder(), httpClient,
                BASE, Duration.ofSeconds(3));
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        rest.getMessageConverters().forEach(converter -> {
            if (converter instanceof MappingJackson2HttpMessageConverter json) {
                json.setObjectMapper(objectMapper);
            }
        });
        server = MockRestServiceServer.bindTo(rest).build();
    }

    @AfterEach
    void verifyTransport() {
        try {
            server.verify();
        } finally {
            httpClient.close();
        }
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
