package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.dto.OrderReq;
import com.devanshbhosale.channelhub.dto.OrderRes;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.InvoiceRepository;
import com.devanshbhosale.channelhub.repository.OrderItemRepository;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.service.InvoiceService;
import com.devanshbhosale.channelhub.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final OrderItemRepository orderItemRepository;
    private final ResellerRepository resellerRepository;

    public OrderController(OrderService orderService,
                           InvoiceService invoiceService,
                           InvoiceRepository invoiceRepository,
                           OrderItemRepository orderItemRepository,
                           ResellerRepository resellerRepository) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.orderItemRepository = orderItemRepository;
        this.resellerRepository = resellerRepository;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<OrderRes> place(@Valid @RequestBody OrderReq request,
                                          @AuthenticationPrincipal User user) {
        OrderRes order = orderService.place(user.getId(), request.items(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/api/orders")
    public List<OrderRes> list(@AuthenticationPrincipal User user) {
        return orderService.listForReseller(user.getId());
    }

    @GetMapping("/api/orders/{id}")
    public OrderRes get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return orderService.toRes(orderService.ownOrder(id, user.getId()));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public OrderRes cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return orderService.cancel(id, user.getId());
    }

    @GetMapping("/api/orders/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, @AuthenticationPrincipal User user) {
        SalesOrder order = orderService.ownOrder(id, user.getId());
        // Invoices are issued at approval, so a missing row means the order is not approved yet.
        Invoice invoice = invoiceRepository.findByOrderId(order.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Invoice is not available for this order yet"));
        Reseller reseller = resellerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reseller profile not found"));
        byte[] pdf = invoiceService.generate(invoice, order,
            orderItemRepository.findByOrderId(order.getId()), reseller);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + invoice.getInvoiceNumber() + ".pdf")
            .body(pdf);
    }
}
