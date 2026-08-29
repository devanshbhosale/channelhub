package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductsApiTest extends IntegrationTestBase {

    @Test
    void productsRequireAuthenticationAndSupportPaging() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isUnauthorized());

        User user = newResellerUser();
        Product product = newProduct("1500.00");
        stock(product.getId(), "BOM-01", 5);
        mockMvc.perform(get("/api/products?page=0&size=5&q=" + product.getSku())
                .header(HttpHeaders.AUTHORIZATION, bearer(jwtService.issue(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].sku").value(product.getSku()));
    }
}
