package com.eleks.common.auth;


import com.eleks.common.dto.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private ObjectMapper objectMapper;

    public AuthenticationEntryPointImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorDto errorDto = ErrorDto.builder()
                .messages(Collections.singletonList("Unauthorized"))
                .timestamp(LocalDateTime.now())
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .build();
        objectMapper.writeValue(response.getOutputStream(), errorDto);
    }
}