package com.eleks.userservice.security;

import com.eleks.common.dto.ErrorDto;
import com.eleks.common.security.JwtTokenService;
import com.eleks.common.security.model.JwtUserDataClaim;
import com.eleks.userservice.dto.login.JwtResponse;
import com.eleks.userservice.dto.login.LoginRequest;
import com.eleks.userservice.dto.user.UserRequestDto;
import com.eleks.userservice.dto.user.UserResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static com.eleks.common.config.SecurityConstants.AUTH_HEADER;
import static com.eleks.common.config.SecurityConstants.BEARER_TOKEN_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
public class SecurityTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Sql(scripts = "classpath:scripts/add_test_user.sql")
    void login_ValidCredentialsWithoutAuthToken_ShouldReturnOkAndJwtToken() throws Exception {
        LoginRequest request = new LoginRequest("mcPaul", "Password12");
        String response = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        JwtResponse token = objectMapper.readValue(response, JwtResponse.class);

        assertNotNull(token);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:scripts/clear_all_users.sql")
    void saveUser_ValidUserWithoutAuthToken_ShouldReturnCreatedAndSavedUser() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("SecondPaul")
                .password("CryptPass")
                .firstName("Paul")
                .lastName("McViol")
                .dateOfBirth(LocalDate.now())
                .email("p.viol@eleks.com")
                .receiveNotifications(true)
                .build();

        String response = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        UserResponseDto responseUser = objectMapper.readValue(response, UserResponseDto.class);

        assertNotNull(responseUser.getId());
    }

    @Test
    public void getUser_WithoutAuthHeader_ShouldReturnUnAuthorizedError() throws Exception {
        String responseBody = mockMvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse().getContentAsString();

        ErrorDto error = objectMapper.readValue(responseBody, ErrorDto.class);
        assertEquals(error.getStatusCode(), HttpStatus.UNAUTHORIZED.value());
        assertEquals("Unauthorized", error.getMessages().get(0));
        assertNotNull(error.getTimestamp());
    }

    @Test
    public void getUser_WithExpiredJWT_ShouldReturnUnAuthorizedError() throws Exception {
        String subject = objectMapper.writeValueAsString(new JwtUserDataClaim("mcPaul", 1L));
        String expiredJwt = jwtTokenService.doGenerateToken(subject, 0);

        String responseBody = mockMvc.perform(get("/users/1")
                .header(AUTH_HEADER, BEARER_TOKEN_PREFIX + expiredJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse().getContentAsString();

        ErrorDto error = objectMapper.readValue(responseBody, ErrorDto.class);
        assertEquals(error.getStatusCode(), HttpStatus.UNAUTHORIZED.value());
        assertEquals("Unauthorized", error.getMessages().get(0));
        assertNotNull(error.getTimestamp());
    }

    @Test
    public void getUser_WithInvalidJWT_ShouldReturnUnAuthorizedError() throws Exception {
        String responseBody = mockMvc.perform(get("/users/1")
                .header(AUTH_HEADER, BEARER_TOKEN_PREFIX + "random string"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse().getContentAsString();

        ErrorDto error = objectMapper.readValue(responseBody, ErrorDto.class);
        assertEquals(error.getStatusCode(), HttpStatus.UNAUTHORIZED.value());
        assertEquals("Unauthorized", error.getMessages().get(0));
        assertNotNull(error.getTimestamp());
    }

    @Test
    @Sql(scripts = "classpath:scripts/add_test_user.sql")
    public void getUser_WithValidJWT_ShouldReturnOkAndUser() throws Exception {
        String jwt = jwtTokenService.generateToken(new JwtUserDataClaim("mcPaul", 2L));

        String responseBody = mockMvc.perform(get("/users/2")
                .header(AUTH_HEADER, BEARER_TOKEN_PREFIX + jwt))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse().getContentAsString();

        UserResponseDto user = objectMapper.readValue(responseBody, UserResponseDto.class);
        assertEquals(2L, user.getId());
    }
}
