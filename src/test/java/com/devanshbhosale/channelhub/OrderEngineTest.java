package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import com.devanshbhosale.channelhub.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderEngineTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    private record Fixture(Reseller reseller, String token) {
    }

    @Test
    void creditBoundaryExactlyEqualsLimitPasses() throws Exception {
        Fixture fixture = newFixture("100000.00");
        Product product = newProduct("50000.00");
        stock(product.getId(), "BOM-01", 10);
        MvcResult result = place(fixture, List.of(Map.of("productId", product.getId(), "quantity", 2)));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(result).get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void creditExceededReturns422WithDetails() throws Exception {
        Fixture fixture = newFixture("100000.00");
        Product product = newProduct("10000.00");
        stock(product.getId(), "BOM-01", 50);
        placeOk(fixture, product, 6); // 60000 outstanding
        MvcResult result = place(fixture, List.of(Map.of("productId", product.getId(), "quantity", 5)));
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        var body = json(result);
        assertThat(body.get("error").asText()).isEqualTo("Credit limit exceeded");
        assertThat(body.get("creditLimit").asDouble()).isEqualTo(100000.00);
        assertThat(body.get("outstanding").asDouble()).isEqualTo(60000.00);
        assertThat(body.get("orderTotal").asDouble()).isEqualTo(50000.00);
    }

    @Test
    void insufficientStockReturns409WithPerProductDetails() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product first = newProduct("1000.00");
        Product second = newProduct("2000.00");
        stock(first.getId(), "BOM-01", 3);
        stock(second.getId(), "MAA-01", 2);
        MvcResult result = place(fixture, List.of(
            Map.of("productId", first.getId(), "quantity", 5),
            Map.of("productId", second.getId(), "quantity", 4)));
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        var details = json(result).get("details");
        assertThat(details.size()).isEqualTo(2);
        assertThat(details.get(0).get("sku").asText()).isEqualTo(first.getSku());
        assertThat(details.get(0).get("requested").asInt()).isEqualTo(5);
        assertThat(details.get(0).get("available").asInt()).isEqualTo(3);
        assertThat(details.get(1).get("sku").asText()).isEqualTo(second.getSku());
        assertThat(details.get(1).get("requested").asInt()).isEqualTo(4);
        assertThat(details.get(1).get("available").asInt()).isEqualTo(2);
    }

    @Test
    void stockFillsFullestWarehouseFirst() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 2);
        stock(product.getId(), "MAA-01", 10);
        stock(product.getId(), "DEL-01", 5);
        placeOk(fixture, product, 12);
        assertThat(inventoryRepository.findByProductIdAndWarehouse(product.getId(), "BOM-01").orElseThrow().getQuantity())
            .isEqualTo(2);
        assertThat(inventoryRepository.findByProductIdAndWarehouse(product.getId(), "MAA-01").orElseThrow().getQuantity())
            .isEqualTo(0);
        assertThat(inventoryRepository.findByProductIdAndWarehouse(product.getId(), "DEL-01").orElseThrow().getQuantity())
            .isEqualTo(3);
    }

    @Test
    void orderItemKeepsPriceSnapshotWhenCatalogPriceChanges() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 10);
        long orderId = placeOk(fixture, product, 2);
        product.setUnitPrice(new BigDecimal("9999.00"));
        productRepository.save(product);
        mockMvc.perform(get("/api/orders/" + orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].sku").value(product.getSku()))
            .andExpect(jsonPath("$.items[0].unitPrice").value(1000.00));
    }

    @Test
    void resellerCancelsOwnPendingOrder() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 10);
        long orderId = placeOk(fixture, product, 1);
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void resellerCannotCancelApprovedOrder() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 10);
        long orderId = placeOk(fixture, product, 1);
        orderService.transition(orderId, OrderStatus.APPROVED, null);
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token())))
            .andExpect(status().isConflict());
    }

    @Test
    void approveShipDeliverFreesCreditAndIssuesInvoice() throws Exception {
        Fixture fixture = newFixture("100000.00");
        Product product = newProduct("10000.00");
        stock(product.getId(), "BOM-01", 50);
        long first = placeOk(fixture, product, 8); // 80000 outstanding
        MvcResult blocked = place(fixture, List.of(Map.of("productId", product.getId(), "quantity", 3)));
        assertThat(blocked.getResponse().getStatus()).isEqualTo(422);
        mockMvc.perform(patch("/api/admin/orders/" + first + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken(false)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.invoiceNumber").isNotEmpty());
        orderService.transition(first, OrderStatus.SHIPPED, null);
        orderService.transition(first, OrderStatus.DELIVERED, null);
        MvcResult freed = place(fixture, List.of(Map.of("productId", product.getId(), "quantity", 3)));
        assertThat(freed.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void rejectRequiresReason() throws Exception {
        Fixture fixture = newFixture("500000.00");
        Product product = newProduct("1000.00");
        stock(product.getId(), "BOM-01", 10);
        long orderId = placeOk(fixture, product, 1);
        String admin = bearer(adminToken(false));
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\",\"reason\":\"   \"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\",\"reason\":\"Pricing mismatch\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("Pricing mismatch"));
    }

    private Fixture newFixture(String creditLimit) {
        User user = newResellerUser();
        Reseller reseller = newResellerFor(user.getId(), creditLimit);
        return new Fixture(reseller, jwtService.issue(user));
    }

    private String adminToken(boolean demo) {
        User admin = newUser(uniqueEmail("adm"), "Password@123", UserRole.ADMIN, demo);
        return jwtService.issue(admin);
    }

    private MvcResult place(Fixture fixture, List<Map<String, Object>> items) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(fixture.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("items", items))))
            .andReturn();
    }

    private long placeOk(Fixture fixture, Product product, int quantity) throws Exception {
        MvcResult result = place(fixture, List.of(Map.of("productId", product.getId(), "quantity", quantity)));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return json(result).get("id").asLong();
    }
}
