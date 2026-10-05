package ru.practicum.shareit.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class ControllerTestSupport {
    protected static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper objectMapper;

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected void assertBody(MvcResult result, Object expected) throws Exception {
        assertEquals(objectMapper.readTree(json(expected)),
                objectMapper.readTree(result.getResponse().getContentAsByteArray()));
    }
}
