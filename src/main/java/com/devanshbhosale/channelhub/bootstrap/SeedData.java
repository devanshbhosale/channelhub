package com.devanshbhosale.channelhub.bootstrap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.devanshbhosale.channelhub.domain.Inventory;
import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.OrderItem;
import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import com.devanshbhosale.channelhub.repository.InventoryRepository;
import com.devanshbhosale.channelhub.repository.InvoiceRepository;
import com.devanshbhosale.channelhub.repository.OrderItemRepository;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import com.devanshbhosale.channelhub.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * One-time seed for an empty database (users table empty): the two number
 * sequences, two admins, six resellers, a 40-product catalog with warehouse
 * stock, and 24 backdated orders covering every order status with invoices
 * for the paid statuses. Order numbers are literal CH-/INV- strings here, not
 * nextval draws, so the sequences are created positioned just past the seed
 * data and runtime numbering never collides.
 */
@Component
public class SeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedData.class);

    private static final String[] WAREHOUSES = {"BOM-01", "MAA-01", "DEL-01"};

    private static final List<OrderStatus> INVOICED_STATUSES =
            List.of(OrderStatus.APPROVED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    /** Oldest order first, 24 orders: PENDING 4, APPROVED 3, SHIPPED 2, DELIVERED 10, REJECTED 2, CANCELLED 3. */
    private static final OrderStatus[] SEED_STATUS_PLAN = {
            OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.DELIVERED,
            OrderStatus.APPROVED, OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.REJECTED,
            OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.DELIVERED, OrderStatus.PENDING,
            OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.APPROVED,
            OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.PENDING,
            OrderStatus.SHIPPED, OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.PENDING
    };

    /**
     * Reseller index per order, parallel to SEED_STATUS_PLAN. Hand-mapped so
     * every reseller's seeded outstanding (PENDING + APPROVED + SHIPPED) stays
     * under their credit limit. 0 demo, 1 Krishna, 2 Cybertech, 3 Balaji,
     * 4 Vertex, 5 Zenith.
     */
    private static final int[] SEED_RESELLER_PLAN = {
            0, 4, 1, 2, 0, 3, 5, 5, 1, 5, 1, 1, 0, 2, 2, 5, 3, 4, 4, 4, 3, 0, 2, 0
    };

    /** sku, name, brand, category, price (INR). */
    private static final String[][] PRODUCT_CATALOG = {
            {"C9200L-24P-4G", "Catalyst 9200L 24-Port PoE+ 4x1G Switch", "Cisco", "Switching", "385000.00"},
            {"C9300-24T-E", "Catalyst 9300 24-Port Data Switch", "Cisco", "Switching", "620000.00"},
            {"C1000-24FP-4G-L", "Catalyst 1000 24-Port PoE Switch", "Cisco", "Switching", "112000.00"},
            {"ISR4331/K9", "ISR 4331 Router, 3 GE, 1 NIM Slot", "Cisco", "Routing", "210000.00"},
            {"ISR4451-X/K9", "ISR 4451-X Router, 4 GE, 2 NIM", "Cisco", "Routing", "875000.00"},
            {"AIR-AP2802I-B-K9", "Aironet 2802i Dual-Band Access Point", "Cisco", "Wireless", "58000.00"},
            {"AIR-CT3504-K9", "Wireless LAN Controller 3504", "Cisco", "Wireless", "490000.00"},
            {"SG350-10-K9", "SG350 10-Port Gigabit Managed Switch", "Cisco", "Networking", "24500.00"},
            {"CBS350-24T-4G", "Business 350 24-Port Gigabit Switch", "Cisco", "Networking", "21500.00"},
            {"868703-B21", "ProLiant DL360 Gen10 Rack Server", "HP", "Servers", "425000.00"},
            {"P19774-B21", "ProLiant DL380 Gen10 Rack Server", "HP", "Servers", "945000.00"},
            {"6F1X1PA", "EliteBook 840 G10 Notebook PC", "HP", "Laptops", "132000.00"},
            {"5C2E7PA", "250 G10 Laptop, i5 13th Gen", "HP", "Laptops", "44500.00"},
            {"9G2J9PA", "ProDesk 400 G9 Microtower Desktop", "HP", "Desktops", "52000.00"},
            {"6C3E6PA", "Z2 Tower G9 Workstation", "HP", "Desktops", "145000.00"},
            {"R0Q46A", "MSA 2050 SAN Storage Array", "HP", "Storage", "560000.00"},
            {"Q6R86A", "StoreEver LTO-9 Tape Drive", "HP", "Storage", "185000.00"},
            {"3Y0W4AA", "USB-C Docking Station G5", "HP", "Peripherals", "12900.00"},
            {"21E3S05X00", "ThinkPad E14 Gen 5 Laptop, i5", "Lenovo", "Laptops", "68000.00"},
            {"21J5001WIN", "ThinkPad T14 Gen 4 Laptop, i7", "Lenovo", "Laptops", "115000.00"},
            {"21B8000GIN", "V15 G4 Laptop, i3", "Lenovo", "Laptops", "38900.00"},
            {"11TF002XIG", "IdeaCentre AIO 3 Desktop 24\"", "Lenovo", "Desktops", "58500.00"},
            {"30G5002XIN", "ThinkSystem ST250 Tower Server", "Lenovo", "Servers", "295000.00"},
            {"7Y37A05XNA", "ThinkSystem SR630 Rack Server", "Lenovo", "Servers", "540000.00"},
            {"4XB7A17126", "ThinkSystem 2.4TB 10K SAS HDD", "Lenovo", "Storage", "42000.00"},
            {"4XB0K59964", "ThinkSystem 960GB SATA SSD", "Lenovo", "Storage", "28500.00"},
            {"BX8071514400", "Core i5-14400 14-Core Processor", "Intel", "Processors", "19500.00"},
            {"BX8071514600K", "Core i5-14600K Unlocked Processor", "Intel", "Processors", "32000.00"},
            {"BX8071514700", "Core i7-14700 20-Core Processor", "Intel", "Processors", "42500.00"},
            {"BX8071514900K", "Core i9-14900K Unlocked Processor", "Intel", "Processors", "62000.00"},
            {"BX8071512400", "Core i5-12400 12-Core Processor", "Intel", "Processors", "15800.00"},
            {"BX8070112100", "Core i3-12100 Quad-Core Processor", "Intel", "Processors", "9800.00"},
            {"XeonSilver4410Y", "Xeon Silver 4410Y Processor", "Intel", "Processors", "105000.00"},
            {"920-009483", "MX Keys S Wireless Keyboard", "Logitech", "Peripherals", "10995.00"},
            {"910-006897", "MX Master 3S Wireless Mouse", "Logitech", "Peripherals", "9995.00"},
            {"960-001376", "Brio 4K Ultra HD Webcam", "Logitech", "Peripherals", "18500.00"},
            {"981-001026", "Zone Vibe 130 Wireless Headset", "Logitech", "Peripherals", "6295.00"},
            {"920-010086", "Signature K650 Wireless Keyboard", "Logitech", "Peripherals", "3295.00"},
            {"910-007119", "Signature M650 Wireless Mouse", "Logitech", "Peripherals", "2495.00"},
            {"910-006335", "G502 Hero Gaming Mouse", "Logitech", "Peripherals", "4495.00"}
    };

    private final UserRepository userRepository;
    private final ResellerRepository resellerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    @PersistenceContext
    private EntityManager entityManager;

    public SeedData(UserRepository userRepository,
                    ResellerRepository resellerRepository,
                    ProductRepository productRepository,
                    InventoryRepository inventoryRepository,
                    SalesOrderRepository salesOrderRepository,
                    OrderItemRepository orderItemRepository,
                    InvoiceRepository invoiceRepository,
                    PasswordEncoder passwordEncoder,
                    @Value("${app.seed.admin-password}") String adminPassword) {
        this.userRepository = userRepository;
        this.resellerRepository = resellerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        createSequences();
        List<Reseller> resellers = seedUsers();
        List<Product> products = seedProducts();
        seedInventory(products);
        seedOrders(resellers, products);
        log.info("ChannelHub seed complete: {} users, {} products, {} orders, {} invoices",
                userRepository.count(), productRepository.count(),
                salesOrderRepository.count(), invoiceRepository.count());
    }

    private void createSequences() {
        long invoicedCount = Arrays.stream(SEED_STATUS_PLAN).filter(INVOICED_STATUSES::contains).count();
        entityManager.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH "
                + (SEED_STATUS_PLAN.length + 1) + " INCREMENT BY 1").executeUpdate();
        entityManager.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS invoice_number_seq START WITH "
                + (invoicedCount + 1) + " INCREMENT BY 1").executeUpdate();
    }

    private List<Reseller> seedUsers() {
        // One BCrypt hash per distinct password, reused across accounts.
        String adminHash = passwordEncoder.encode(adminPassword);
        String demoAdminHash = passwordEncoder.encode("Demo@Admin2026");
        String demoResellerHash = passwordEncoder.encode("Demo@Reseller2026");
        String resellerHash = passwordEncoder.encode("Reseller@2026");

        userRepository.save(user("admin@channelhub.in", adminHash, "Devansh Bhosale", UserRole.ADMIN, false));
        userRepository.save(user("admin.demo@channelhub.in", demoAdminHash, "Demo Admin", UserRole.ADMIN, true));

        List<Reseller> resellers = new ArrayList<>();
        resellers.add(reseller("reseller.demo@channelhub.in", demoResellerHash, "Demo Reseller",
                "Sunrise Computer Traders", "27AAECS1234F1Z5", "9820000021", "1500000.00", true));
        resellers.add(reseller("krishna@krishnasystems.in", resellerHash, "Krishna Iyer",
                "Krishna Systems & Peripherals", "27AAKCS4321M1ZK", "9833100121", "1200000.00", false));
        resellers.add(reseller("cybertech@cybertechdist.in", resellerHash, "Farhan Qureshi",
                "Cybertech Distributors", "29AABCC5678P1Z2", "9845600231", "800000.00", false));
        resellers.add(reseller("balaji@balajiitsolutions.in", resellerHash, "Balaji Rao",
                "Shree Balaji IT Solutions", "07AAQCS9012L1Z8", "9867100341", "200000.00", false));
        resellers.add(reseller("vertex@vertexinfotech.in", resellerHash, "Vijay Anand",
                "Vertex Infotech", "33AAICV2468H1ZQ", "9873300451", "1500000.00", false));
        resellers.add(reseller("zenith@zenithdatasystems.in", resellerHash, "Zoya Khan",
                "Zenith Data Systems", "27AAUCZ1359K1Z6", "9892200561", "350000.00", false));
        return resellers;
    }

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>();
        for (String[] row : PRODUCT_CATALOG) {
            Product p = new Product();
            p.setSku(row[0]);
            p.setName(row[1]);
            p.setBrand(row[2]);
            p.setCategory(row[3]);
            p.setDescription(row[1] + ". Sealed channel pack with GST invoice and 3-year carry-in warranty.");
            p.setUnitPrice(new BigDecimal(row[4]));
            products.add(productRepository.save(p));
        }
        return products;
    }

    private void seedInventory(List<Product> products) {
        // Three SKUs deliberately seeded below their reorder level.
        Map<String, Integer> lowStockQty = Map.of(
                "ISR4331/K9", 6,
                "BX8071514900K", 5,
                "920-009483", 9);
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int qty = 45 + (i * 17) % 140;
            int reorder = 12;
            boolean lowStock = lowStockQty.containsKey(p.getSku());
            if (lowStock) {
                qty = lowStockQty.get(p.getSku());
                reorder = qty + 6;
            }
            String primary = WAREHOUSES[i % WAREHOUSES.length];
            inventoryRepository.save(inventory(p, primary, qty, reorder));
            // Secondary warehouse stocking for every third product, never for the low-stock rows.
            if (i % 3 == 0 && !lowStock) {
                String secondary = WAREHOUSES[(i + 1) % WAREHOUSES.length];
                inventoryRepository.save(inventory(p, secondary, qty / 2, reorder));
            }
        }
    }

    private Inventory inventory(Product p, String warehouse, int quantity, int reorderLevel) {
        Inventory row = new Inventory();
        row.setProductId(p.getId());
        row.setWarehouse(warehouse);
        row.setQuantity(quantity);
        row.setReorderLevel(reorderLevel);
        return row;
    }

    private void seedOrders(List<Reseller> resellers, List<Product> products) {
        int year = LocalDate.now().getYear();
        int orderSeq = 0;
        int invoiceSeq = 0;
        for (int i = 0; i < SEED_STATUS_PLAN.length; i++) {
            OrderStatus status = SEED_STATUS_PLAN[i];
            Reseller buyer = resellers.get(SEED_RESELLER_PLAN[i]);
            // Strictly increasing with i, spanning the last ~21 days.
            int daysAgo = Math.round(20f * (SEED_STATUS_PLAN.length - 1 - i) / (SEED_STATUS_PLAN.length - 1));
            LocalDateTime createdAt =
                    LocalDateTime.now().minusDays(daysAgo).minusHours(SEED_STATUS_PLAN.length - 1L - i);

            List<OrderItem> items = buildItems(i, products, daysAgo);
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem item : items) {
                total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            total = total.setScale(2, RoundingMode.HALF_UP);

            SalesOrder order = new SalesOrder();
            order.setOrderNumber(String.format("CH-%d-%05d", year, ++orderSeq));
            order.setResellerId(buyer.getId());
            order.setStatus(status);
            order.setTotalAmount(total);
            if (i == 4) {
                order.setNotes("Urgent — client rollout scheduled for next week.");
            } else if (i == 11) {
                order.setNotes("Consolidated order for branch office fit-out.");
            } else if (i == 23) {
                order.setNotes("Please confirm delivery slot before dispatch.");
            }
            if (status == OrderStatus.REJECTED) {
                order.setRejectionReason(i == 7
                        ? "Vendor price approval pending — please reorder after the new price list."
                        : "GST certificate on file has expired; accounts team withheld approval.");
            }
            order.setCreatedAt(createdAt);
            order.setUpdatedAt(createdAt);
            order = salesOrderRepository.save(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                orderItemRepository.save(item);
            }

            if (INVOICED_STATUSES.contains(status)) {
                Invoice invoice = new Invoice();
                invoice.setOrderId(order.getId());
                invoice.setInvoiceNumber(String.format("INV-%d-%05d", year, ++invoiceSeq));
                invoice.setAmount(total.multiply(new BigDecimal("1.18")).setScale(2, RoundingMode.HALF_UP));
                invoice.setIssuedAt(createdAt.plusHours(3));
                invoiceRepository.save(invoice);
            }
        }
    }

    private List<OrderItem> buildItems(int orderIndex, List<Product> products, int daysAgo) {
        List<OrderItem> items = new ArrayList<>();
        int count = 1 + (orderIndex % 3);
        for (int k = 0; k < count; k++) {
            Product p = products.get((orderIndex * 5 + k * 7) % products.size());
            int qty;
            if (p.getUnitPrice().compareTo(new BigDecimal("100000")) > 0) {
                qty = 1;
            } else if (p.getUnitPrice().compareTo(new BigDecimal("20000")) > 0) {
                qty = 1 + (2 * orderIndex + k) % 4;
            } else {
                qty = 2 + (orderIndex + 3 * k) % 9;
            }
            OrderItem item = new OrderItem();
            item.setProductId(p.getId());
            item.setQuantity(qty);
            item.setUnitPrice(snapshotPrice(p, daysAgo));
            items.add(item);
        }
        return items;
    }

    /**
     * Old orders were placed against a lower price list: up to 8 percent below
     * the current catalog price, scaling with age; recent orders match catalog.
     */
    private BigDecimal snapshotPrice(Product p, int daysAgo) {
        if (daysAgo < 8) {
            return p.getUnitPrice();
        }
        BigDecimal discount = new BigDecimal("0.08")
                .multiply(BigDecimal.valueOf(daysAgo))
                .divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP);
        return p.getUnitPrice().multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
    }

    private User user(String email, String passwordHash, String fullName, UserRole role, boolean demo) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setFullName(fullName);
        u.setRole(role);
        u.setDemo(demo);
        return u;
    }

    private Reseller reseller(String email, String passwordHash, String fullName, String companyName,
                              String gstin, String phone, String creditLimit, boolean demo) {
        User u = userRepository.save(user(email, passwordHash, fullName, UserRole.RESELLER, demo));
        Reseller r = new Reseller();
        r.setUserId(u.getId());
        r.setCompanyName(companyName);
        r.setGstin(gstin);
        r.setPhone(phone);
        r.setCreditLimit(new BigDecimal(creditLimit));
        return resellerRepository.save(r);
    }
}
