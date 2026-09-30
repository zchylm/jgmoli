package com.aicyber.jgmoli.invoice.service;

import com.aicyber.jgmoli.invoice.dto.InvoiceAddressResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceLineResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class TaxInvoicePdfRenderer {
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont MEDIUM = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);

    public byte[] render(InvoiceResponse invoice) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDDocumentInformation information = new PDDocumentInformation();
            information.setTitle("Tax Invoice " + invoice.invoiceNumber());
            information.setAuthor(invoice.sellerTradingName());
            information.setSubject("Tax invoice for order " + invoice.orderReference());
            document.setDocumentInformation(information);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                drawInvoice(canvas, invoice);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Tax invoice PDF could not be generated", exception);
        }
    }

    private void drawInvoice(PDPageContentStream canvas, InvoiceResponse invoice) throws IOException {
        float pageWidth = PDRectangle.A4.getWidth();
        float left = 46;
        float right = pageWidth - 46;

        fillColor(canvas, 16, 21, 22);
        canvas.addRect(0, 716, pageWidth, 126);
        canvas.fill();
        text(canvas, MEDIUM, 12, left, 790, "JG MOLI", 245, 247, 244);
        text(canvas, REGULAR, 8, left, 771, "LEAVE THE NOISE. ENTER YOUR WORLD.", 123, 163, 160);
        rightText(canvas, MEDIUM, 26, right, 785, "TAX INVOICE", 245, 247, 244);
        rightText(canvas, REGULAR, 9, right, 761, invoice.invoiceNumber(), 188, 195, 193);

        label(canvas, left, 683, "ISSUED");
        text(canvas, MEDIUM, 10, left, 666, DATE.format(invoice.issuedAt()), 28, 31, 32);
        label(canvas, 188, 683, "ORDER");
        text(canvas, MEDIUM, 10, 188, 666, invoice.orderReference(), 28, 31, 32);
        label(canvas, 350, 683, "PAYMENT");
        text(canvas, MEDIUM, 10, 350, 666, invoice.paymentReference(), 28, 31, 32);
        rightText(canvas, MEDIUM, 9, right, 675, "PAID", 77, 139, 136);
        rule(canvas, left, right, 644);

        label(canvas, left, 616, "FROM");
        text(canvas, MEDIUM, 11, left, 596, invoice.sellerTradingName(), 28, 31, 32);
        text(canvas, REGULAR, 8.5f, left, 580, invoice.sellerLegalName(), 92, 98, 99);
        text(canvas, REGULAR, 8.5f, left, 566, "ABN " + invoice.sellerAbn(), 92, 98, 99);
        text(canvas, REGULAR, 8.5f, left, 552, invoice.sellerAddress(), 92, 98, 99);
        text(canvas, REGULAR, 8.5f, left, 538, invoice.sellerEmail() + "  |  " + invoice.sellerPhone(), 92, 98, 99);

        float billLeft = 330;
        label(canvas, billLeft, 616, "BILL TO");
        text(canvas, MEDIUM, 11, billLeft, 596, invoice.buyerName(), 28, 31, 32);
        text(canvas, REGULAR, 8.5f, billLeft, 580, invoice.buyerEmail(), 92, 98, 99);
        int addressY = 566;
        for (String addressLine : addressLines(invoice.buyerAddress())) {
            text(canvas, REGULAR, 8.5f, billLeft, addressY, addressLine, 92, 98, 99);
            addressY -= 14;
        }

        float tableTop = 490;
        fillColor(canvas, 232, 231, 226);
        canvas.addRect(left, tableTop, right - left, 26);
        canvas.fill();
        label(canvas, left + 8, tableTop + 9, "ITEM");
        label(canvas, 342, tableTop + 9, "QTY");
        label(canvas, 389, tableTop + 9, "UNIT EX GST");
        label(canvas, 463, tableTop + 9, "GST");
        rightLabel(canvas, right - 8, tableTop + 9, "TOTAL");

        float y = tableTop - 21;
        for (InvoiceLineResponse line : invoice.lines()) {
            text(canvas, MEDIUM, 7.8f, left + 8, y,
                    ellipsize(line.description() + " / " + line.sku(), 55), 32, 35, 35);
            text(canvas, REGULAR, 8.3f, 350, y, Integer.toString(line.quantity()), 55, 59, 60);
            rightText(canvas, REGULAR, 8.3f, 449, y, money(line.unitPriceExGstCents()), 55, 59, 60);
            rightText(canvas, REGULAR, 8.3f, 500, y, money(line.gstCents()), 55, 59, 60);
            rightText(canvas, MEDIUM, 8.3f, right - 8, y, money(line.lineTotalIncGstCents()), 32, 35, 35);
            rule(canvas, left, right, y - 9);
            y -= 18;
        }

        float totalsTop = Math.max(116, y - 8);
        float totalsLeft = 365;
        total(canvas, totalsLeft, right, totalsTop, "Subtotal ex GST", money(invoice.subtotalExGstCents()), false);
        total(canvas, totalsLeft, right, totalsTop - 21, "GST", money(invoice.gstCents()), false);
        total(canvas, totalsLeft, right, totalsTop - 42, "Delivery", money(invoice.deliveryCents()), false);
        rule(canvas, totalsLeft, right, totalsTop - 52);
        total(canvas, totalsLeft, right, totalsTop - 73, "Amount paid", money(invoice.amountPaidCents()), true);

        text(canvas, REGULAR, 7.5f, left, 54,
                "Prices are in " + invoice.currency() + ". GST is included where shown.", 109, 114, 114);
        text(canvas, REGULAR, 7.5f, left, 40,
                "Keep this document for your records. Thank you for choosing JG MOLI.", 109, 114, 114);
        rightText(canvas, REGULAR, 7.5f, right, 40, invoice.invoiceNumber(), 109, 114, 114);
    }

    private String[] addressLines(InvoiceAddressResponse address) {
        String locality = String.join(" ", address.suburb(), address.state(), address.postcode()).trim();
        String country = "AU".equals(address.countryCode()) ? "Australia" : address.countryCode();
        if (address.addressLine2() == null || address.addressLine2().isBlank()) {
            return new String[]{address.addressLine1(), locality, country};
        }
        return new String[]{address.addressLine1(), address.addressLine2(), locality, country};
    }

    private void total(PDPageContentStream canvas, float left, float right, float y,
                       String title, String value, boolean emphasis) throws IOException {
        PDFont font = emphasis ? MEDIUM : REGULAR;
        float size = emphasis ? 11 : 8.5f;
        text(canvas, font, size, left, y, title, 46, 50, 51);
        rightText(canvas, font, size, right, y, value, 28, 31, 32);
    }

    private void label(PDPageContentStream canvas, float x, float y, String value) throws IOException {
        text(canvas, MEDIUM, 6.8f, x, y, value, 91, 147, 144);
    }

    private void rightLabel(PDPageContentStream canvas, float right, float y, String value) throws IOException {
        rightText(canvas, MEDIUM, 6.8f, right, y, value, 91, 147, 144);
    }

    private void rule(PDPageContentStream canvas, float left, float right, float y) throws IOException {
        canvas.setStrokingColor(210 / 255f, 211 / 255f, 207 / 255f);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(left, y);
        canvas.lineTo(right, y);
        canvas.stroke();
    }

    private void rightText(PDPageContentStream canvas, PDFont font, float size, float right, float y,
                           String value, int red, int green, int blue) throws IOException {
        String safe = safeText(value);
        float width = font.getStringWidth(safe) / 1000 * size;
        text(canvas, font, size, right - width, y, safe, red, green, blue);
    }

    private void text(PDPageContentStream canvas, PDFont font, float size, float x, float y,
                      String value, int red, int green, int blue) throws IOException {
        canvas.beginText();
        canvas.setFont(font, size);
        fillColor(canvas, red, green, blue);
        canvas.newLineAtOffset(x, y);
        canvas.showText(safeText(value));
        canvas.endText();
    }

    private String safeText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private void fillColor(PDPageContentStream canvas, int red, int green, int blue) throws IOException {
        canvas.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
    }

    private String ellipsize(String value, int maximum) {
        String safe = safeText(value);
        return safe.length() <= maximum ? safe : safe.substring(0, maximum - 3) + "...";
    }

    private String money(long cents) {
        return String.format(Locale.ENGLISH, "$%,.2f", cents / 100.0);
    }
}
