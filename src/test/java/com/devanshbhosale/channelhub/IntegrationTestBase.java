package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.Inventory;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import com.devanshbhosale.channelhub.repository.InventoryRepository;
import com.devanshbhosale.channelhub.repository.InvoiceRepository;
import com.devanshbhosale.channelhub.repository.OrderItemRepository;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import com.devanshbhosale.channelhub.repository.UserRepository;
import com.devanshbhosale.channelhub.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Shared fixtures for the API tests. Every test builds its own data with unique
 * emails / SKUs so nothing depends on seed data or on execution order.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected ResellerRepository resellerRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected InventoryRepository inventoryRepository;
    @Autowired protected SalesOrderRepository salesOrderRepository;
    @Autowired protected OrderItemRepository orderItemRepository;
    @Autowired protected InvoiceRepository invoiceRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.local";
    }

    protected User newUser(String email, String password, UserRole role, boolean demo) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName("Test User");
        user.setRole(role);
        user.setDemo(demo);
        return userRepository.save(user);
    }

    protected User newResellerUser() {
        return newUser(uniqueEmail("reseller"), "Password@123", UserRole.RESELLER, false);
    }

    protected Reseller newResellerFor(Long userId, String creditLimit) {
        Reseller reseller = new Reseller();
        reseller.setUserId(userId);
        reseller.setCompanyName("Test Traders Pvt Ltd");
        reseller.setGstin("27ABCDE1234F1Z5");
        reseller.setPhone("9820000001");
        reseller.setCreditLimit(new BigDecimal(creditLimit));
        return resellerRepository.save(reseller);
    }

    protected Product newProduct(String price) {
        Product product = new Product();
        product.setSku("TST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        product.setName("Test Product");
        product.setBrand("TestBrand");
        product.setCategory("Networking");
        product.setDescription("Test fixture product");
        product.setUnitPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.save(product);
    }

    protected Inventory stock(Long productId, String warehouse, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(quantity);
        inventory.setReorderLevel(2);
        return inventoryRepository.save(inventory);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected JsonNode json(MvcResult result) throws IOException {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
