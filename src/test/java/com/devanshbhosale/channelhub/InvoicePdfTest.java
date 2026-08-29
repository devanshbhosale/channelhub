package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.dto.OrderReq;
import com.devanshbhosale.channelhub.dto.OrderRes;
import com.devanshbhosale.channelhub.service.InvoiceService;
import com.devanshbhosale.channelhub.service.OrderService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void invoicePdfContainsRupeeSymbolAndInvoiceNumber() throws Exception {
        User user = newResellerUser();
        Reseller reseller = newResellerFor(user.getId(), "500000.00");
        Product product = newProduct("5000.00");
        stock(product.getId(), "BOM-01", 10);
        OrderRes order = orderService.place(user.getId(),
            List.of(new OrderReq.OrderItemReq(product.getId(), 2)), null);
        orderService.transition(order.id(), OrderStatus.APPROVED, null);
        Invoice invoice = invoiceRepository.findByOrderId(order.id()).orElseThrow();

        byte[] pdf = invoiceService.generate(invoice,
            salesOrderRepository.findById(order.id()).orElseThrow(),
            orderItemRepository.findByOrderId(order.id()),
            reseller);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("₹");
            assertThat(text).contains(invoice.getInvoiceNumber());
            assertThat(text).contains(reseller.getCompanyName());
            assertThat(text).contains("This is a computer-generated invoice");
        }
    }
}
