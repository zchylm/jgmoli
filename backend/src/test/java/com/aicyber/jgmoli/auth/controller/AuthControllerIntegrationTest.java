package com.aicyber.jgmoli.auth.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registerReturnsAnAuthenticatedCustomerAndProtectsCurrentUser() throws Exception {
        String email = uniqueEmail();
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "Jordan")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.customerReference").value(org.hamcrest.Matchers.matchesPattern("JGM-CUS-[A-F0-9]{12}")))
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.user.displayName").value("Jordan"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andReturn();

        String token = JsonPath.read(registration.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.displayName").value("Jordan"));
    }

    @Test
    void verificationAndPasswordResetLinksCompleteTheAccountFlow() throws Exception {
        String email = uniqueEmail();
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "Morgan")))
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = JsonPath.read(registration.getResponse().getContentAsString(), "$.accessToken");

        String verificationToken = tokenFromQueuedEmail(email, "EMAIL_VERIFICATION", "verifyEmail");
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(verificationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your email is verified. Welcome to JG MOLI."));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));

        mockMvc.perform(post("/api/auth/password/reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account matches that email, a reset link is on its way."));
        String resetToken = tokenFromQueuedEmail(email, "PASSWORD_RESET", "resetPassword");
        mockMvc.perform(post("/api/auth/password/reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"password\":\"a-new-password-2026\"}".formatted(resetToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been updated. Log in to continue."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"play-better-2026\"}".formatted(email)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"a-new-password-2026\"}".formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void loginUsesAGenericErrorAndDuplicateRegistrationStaysActionable() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "Avery")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), "Avery")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.message").value("An account with this email already exists. Log in to continue."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email or password is incorrect."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"play-better-2026"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void rejectsInvalidRegistrationAndMissingAuthenticationWithoutLeakingDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid email address."));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Log in to continue."));
    }

    private String uniqueEmail() {
        return "player-" + UUID.randomUUID() + "@example.com";
    }

    private String registerBody(String email, String displayName) {
        return """
                {"email":"%s","password":"play-better-2026","displayName":"%s"}
                """.formatted(email, displayName);
    }

    private String tokenFromQueuedEmail(String email, String messageType, String parameter) {
        String html = jdbcTemplate.queryForObject("""
                SELECT html_body FROM transactional_email_outbox
                WHERE recipient_email = ? AND message_type = ?
                ORDER BY queued_at DESC LIMIT 1
                """, String.class, email.toLowerCase(), messageType);
        Matcher matcher = Pattern.compile(parameter + "=([A-Za-z0-9_-]+)").matcher(html);
        if (!matcher.find()) throw new AssertionError("Email did not include " + parameter + " token");
        return matcher.group(1);
    }
}
