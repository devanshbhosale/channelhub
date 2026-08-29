package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo admin (isDemo=true) can read the admin area but every write is blocked
 * by DemoAdminReadOnlyFilter with 403.
 */
class DemoAdminAccessTest extends IntegrationTestBase {

    @Test
    void demoAdminCanListOrders() throws Exception {
        User demoAdmin = newUser(uniqueEmail("demo-admin"), "Password@123", UserRole.ADMIN, true);
        mockMvc.perform(get("/api/admin/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.issue(demoAdmin))))
            .andExpect(status().isOk());
    }

    @Test
    void demoAdminCannotCreateProducts() throws Exception {
        User demoAdmin = newUser(uniqueEmail("demo-admin-w"), "Password@123", UserRole.ADMIN, true);
        mockMvc.perform(post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.issue(demoAdmin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productBody())))
            .andExpect(status().isForbidden());
    }

    @Test
    void realAdminCanCreateProducts() throws Exception {
        User admin = newUser(uniqueEmail("real-admin"), "Password@123", UserRole.ADMIN, false);
        mockMvc.perform(post("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.issue(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productBody())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").isNotEmpty())
            .andExpect(jsonPath("$.active").value(true));
    }

    private Map<String, Object> productBody() {
        return Map.of("sku", "ADM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "name", "Admin Test Product",
            "brand", "TestBrand",
            "category", "Switching",
            "description", "Created by admin test",
            "unitPrice", 12500,
            "active", true);
    }
}
