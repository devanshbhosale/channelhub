package com.devanshbhosale.channelhub;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestBase {

    @Test
    void registerCreatesResellerWithToken() throws Exception {
        String email = uniqueEmail("reg");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody(email))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.fullName").value("Test Reseller"))
            .andExpect(jsonPath("$.user.role").value("RESELLER"));
    }

    @Test
    void duplicateEmailRegistrationReturns409() throws Exception {
        String email = uniqueEmail("dup");
        register(email);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody(email))))
            .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTokenWithDemoFlag() throws Exception {
        String email = uniqueEmail("login");
        register(email);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Password@123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.isDemo").value(false));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String email = uniqueEmail("badlogin");
        register(email);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPass@123"))))
            .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> registerBody(String email) {
        return Map.of("email", email,
            "password", "Password@123",
            "fullName", "Test Reseller",
            "companyName", "Auth Test Traders",
            "gstin", "27ABCDE1234F1Z5",
            "phone", "9820000001");
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody(email))))
            .andExpect(status().isCreated());
    }
}
