package com.devanshbhosale.channelhub.service;

import com.devanshbhosale.channelhub.domain.Invoice;
import com.devanshbhosale.channelhub.domain.OrderItem;
import com.devanshbhosale.channelhub.domain.Product;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.ProductRepository;
import com.devanshbhosale.channelhub.util.MoneyFormat;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GST-style tax invoice rendered with OpenPDF. Noto Sans is embedded (IDENTITY_H)
 * so the rupee glyph renders; the font ships on the classpath at fonts/.
 */
@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final BigDecimal CGST_RATE = new BigDecimal("0.09");
    private static final Color INK = new Color(26, 29, 33);
    private static final Color MUTED = new Color(110, 110, 110);
    private static final Color HEADER_BG = new Color(243, 242, 238);

    /** Category -> HSN/SAC defaults. */
    private static final Map<String, String> HSN_BY_CATEGORY = Map.ofEntries(
        Map.entry("Networking", "8517"),
        Map.entry("Switching", "8517"),
        Map.entry("Routing", "8517"),
        Map.entry("Wireless", "8517"),
        Map.entry("Computers", "8471"),
        Map.entry("Servers", "8471"),
        Map.entry("Laptops", "8471"),
        Map.entry("Desktops", "8471"),
        Map.entry("Peripherals", "8473"),
        Map.entry("Processors", "8542"),
        Map.entry("Storage", "8542"));

    private final ProductRepository productRepository;

    public InvoiceService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public byte[] generate(Invoice invoice, SalesOrder order, List<OrderItem> items, Reseller reseller) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 48);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            BaseFont baseFont = BaseFont.createFont("fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font title = new Font(baseFont, 15, Font.NORMAL, INK);
            Font muted = new Font(baseFont, 9, Font.NORMAL, MUTED);
            Font normal = new Font(baseFont, 9, Font.NORMAL, INK);
            Font emphasis = new Font(baseFont, 10, Font.NORMAL, INK);
            Font tableHeader = new Font(baseFont, 8, Font.NORMAL, INK);

            Paragraph header = new Paragraph();
            header.add(new Chunk("ChannelHub", title));
            header.add(new Chunk("   —   TAX INVOICE", muted));
            document.add(header);

            Paragraph meta = spaced(new Paragraph("Invoice No: " + nullSafe(invoice.getInvoiceNumber())
                + "        Date: " + (invoice.getIssuedAt() == null ? "" : DATE_FORMAT.format(invoice.getIssuedAt()))
                + "        Order: " + nullSafe(order.getOrderNumber()), normal), 12);
            document.add(meta);

            document.add(spaced(new Paragraph("Bill To", muted), 4));
            document.add(spaced(new Paragraph(nullSafe(reseller.getCompanyName()), normal), 2));
            document.add(spaced(new Paragraph("GSTIN: " + nullSafe(reseller.getGstin())
                + "        Phone: " + nullSafe(reseller.getPhone()), normal), 12));

            Map<Long, Product> products = productRepository.findAllById(
                    items.stream().map(OrderItem::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

            PdfPTable table = new PdfPTable(new float[]{5, 17, 32, 11, 7, 13, 15});
            table.setWidthPercentage(100);
            table.setSpacingBefore(4);
            for (String column : List.of("Sr", "SKU", "Description", "HSN/SAC", "Qty", "Rate", "Amount")) {
                PdfPCell cell = cell(new Phrase(column, tableHeader), Element.ALIGN_LEFT);
                cell.setBackgroundColor(HEADER_BG);
                table.addCell(cell);
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            int sr = 1;
            for (OrderItem item : items.stream().sorted(Comparator.comparing(OrderItem::getId)).toList()) {
                Product product = products.get(item.getProductId());
                String category = product == null ? "" : nullSafe(product.getCategory());
                BigDecimal lineAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineAmount);
                table.addCell(cell(new Phrase(String.valueOf(sr++), normal), Element.ALIGN_CENTER));
                table.addCell(cell(new Phrase(product == null ? "" : nullSafe(product.getSku()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(product == null ? "" : nullSafe(product.getName()), normal), Element.ALIGN_LEFT));
                table.addCell(cell(new Phrase(HSN_BY_CATEGORY.getOrDefault(category, "8517"), normal), Element.ALIGN_CENTER));
                table.addCell(cell(new Phrase(String.valueOf(item.getQuantity()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(MoneyFormat.inrPlain(item.getUnitPrice()), normal), Element.ALIGN_RIGHT));
                table.addCell(cell(new Phrase(MoneyFormat.inr(lineAmount), normal), Element.ALIGN_RIGHT));
            }
            document.add(table);

            BigDecimal cgst = subtotal.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sgst = cgst;
            BigDecimal grandTotal = subtotal.add(cgst).add(sgst);

            PdfPTable totals = new PdfPTable(new float[]{65, 35});
            totals.setWidthPercentage(100);
            totals.setSpacingBefore(10);
            addTotalRow(totals, "Subtotal", MoneyFormat.inrPlain(subtotal), normal);
            addTotalRow(totals, "CGST @ 9%", MoneyFormat.inrPlain(cgst), normal);
            addTotalRow(totals, "SGST @ 9%", MoneyFormat.inrPlain(sgst), normal);
            addTotalRow(totals, "Grand Total", MoneyFormat.inr(invoice.getAmount() == null ? grandTotal : invoice.getAmount()), emphasis);
            document.add(totals);

            document.add(spaced(new Paragraph("This is a computer-generated invoice", muted), 0));

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate invoice");
        }
    }

    private PdfPCell cell(Phrase phrase, int horizontalAlignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setPadding(5);
        cell.setHorizontalAlignment(horizontalAlignment);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(4);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Paragraph spaced(Paragraph paragraph, float spaceAfter) {
        paragraph.setSpacingAfter(spaceAfter);
        return paragraph;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
