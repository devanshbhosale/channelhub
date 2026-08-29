package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderNumberFormatTest extends IntegrationTestBase {

    @Test
    void orderNumberUsesYearAndFiveDigitSequence() throws Exception {
        User user = newResellerUser();
        newResellerFor(user.getId(), "500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 10);
        MvcResult result = mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.issue(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "items", List.of(Map.of("productId", product.getId(), "quantity", 1))))))
            .andExpect(status().isCreated())
            .andReturn();
        String orderNumber = json(result).get("orderNumber").asText();
        assertThat(orderNumber).matches("CH-" + Year.now().getValue() + "-\\d{5}");
    }
}
