package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfiguration {
    @Bean
    public HttpClient shareItHttpClient(@Value("${shareit-server.connect-timeout:2s}") Duration timeout) {
        return HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Bean
    public RestTemplate shareItRestTemplate(RestTemplateBuilder builder, HttpClient shareItHttpClient,
                                            @Value("${shareit-server.url:http://localhost:9090}") String baseUrl,
                                            @Value("${shareit-server.read-timeout:10s}") Duration timeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(shareItHttpClient);
        factory.setReadTimeout(timeout);
        DefaultUriBuilderFactory uris = new DefaultUriBuilderFactory(baseUrl);
        uris.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);
        return builder.requestFactory(() -> factory).uriTemplateHandler(uris).build();
    }
}
