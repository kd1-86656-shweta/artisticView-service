package com.artisticview.security.filter;

import com.artisticview.common.api.ApiError;
import com.artisticview.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Writes the standard error envelope from filters, where @RestControllerAdvice does not apply. */
@Component
@RequiredArgsConstructor
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(ApiError.of(code, message)));
    }
}
