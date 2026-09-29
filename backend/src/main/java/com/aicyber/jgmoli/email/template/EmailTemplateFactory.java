package com.aicyber.jgmoli.email.template;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
import com.aicyber.jgmoli.checkout.dto.OrderLineResponse;
import com.aicyber.jgmoli.checkout.dto.OrderResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceLineResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class EmailTemplateFactory {
    private static final Locale AUSTRALIA = Locale.forLanguageTag("en-AU");

    public EmailContent verification(String name, String actionUrl) {
        String subject = "Verify your JG MOLI email";
        String text = """
                Hi %s,

                Confirm this email belongs to you:
                %s

                This link expires in 24 hours. If you did not create a JG MOLI account, you can ignore this email.

                JG MOLI
                Leave the noise. Enter your world.
                """.formatted(displayName(name), actionUrl);
        return new EmailContent(subject, text, frame(
                "YOUR JG MOLI",
                "One step closer.",
                "Confirm your email to keep your account, recommendations and future orders connected.",
                "VERIFY EMAIL",
                actionUrl,
                "This link expires in 24 hours. If you did not create this account, no action is needed."
        ));
    }

    public EmailContent passwordReset(String name, String actionUrl) {
        String subject = "Reset your JG MOLI password";
        String text = """
                Hi %s,

                Reset your JG MOLI password:
                %s

                This link expires in 30 minutes. If you did not request a reset, you can ignore this email.
                """.formatted(displayName(name), actionUrl);
        return new EmailContent(subject, text, frame(
                "ACCOUNT ACCESS",
                "Reset. Return. Play.",
                "Choose a new password and get back to your world.",
                "RESET PASSWORD",
                actionUrl,
                "This link expires in 30 minutes. If you did not request it, your password has not changed."
        ));
    }

    public EmailContent orderCreated(OrderResponse order, String accountUrl) {
        String subject = "Order " + order.orderReference() + " is ready for payment";
        String itemText = order.items().stream()
                .map(item -> "%d × %s — %s".formatted(item.quantity(), item.productName(), money(item.lineTotalCents(), order.currency())))
                .reduce((left, right) -> left + "\n" + right).orElse("");
        String text = """
                Hi %s,

                We saved order %s.

                %s

                Total including GST: %s

                Return to JG MOLI to review and complete payment:
                %s
                """.formatted(displayName(order.delivery().recipientName()), order.orderReference(), itemText,
                money(order.totalCents(), order.currency()), accountUrl);
        String rows = order.items().stream().map(item -> orderRow(item, order.currency()))
                .reduce((left, right) -> left + right).orElse("");
        return new EmailContent(subject, text, commerceFrame(
                "ORDER SAVED",
                "Your gear is lined up.",
                "Order " + escape(order.orderReference()),
                rows,
                "TOTAL INCLUDING GST",
                money(order.totalCents(), order.currency()),
                "REVIEW ORDER",
                accountUrl,
                "Payment has not been taken yet. Your final order is confirmed after successful payment."
        ));
    }

    public EmailContent invoiceIssued(InvoiceResponse invoice, String accountUrl) {
        String subject = "Tax invoice " + invoice.invoiceNumber() + " — JG MOLI";
        String itemText = invoice.lines().stream()
                .map(line -> "%d × %s — %s".formatted(line.quantity(), line.description(),
                        money(line.lineTotalIncGstCents(), invoice.currency())))
                .reduce((left, right) -> left + "\n" + right).orElse("");
        String text = """
                Hi %s,

                Payment confirmed for order %s.
                Tax invoice: %s

                %s

                Amount paid: %s
                GST included: %s

                Sign in to JG MOLI to view your order and invoice:
                %s

                %s | ABN %s
                """.formatted(displayName(invoice.buyerName()), invoice.orderReference(), invoice.invoiceNumber(), itemText,
                money(invoice.amountPaidCents(), invoice.currency()), money(invoice.gstCents(), invoice.currency()),
                accountUrl, invoice.sellerLegalName(), invoice.sellerAbn());
        String rows = invoice.lines().stream().map(line -> invoiceRow(line, invoice.currency()))
                .reduce((left, right) -> left + right).orElse("");
        return new EmailContent(subject, text, commerceFrame(
                "PAYMENT CONFIRMED",
                "Your world is on its way.",
                "Tax invoice " + escape(invoice.invoiceNumber()),
                rows,
                "AMOUNT PAID",
                money(invoice.amountPaidCents(), invoice.currency()),
                "VIEW ORDER & INVOICE",
                accountUrl,
                escape(invoice.sellerLegalName()) + " · ABN " + escape(invoice.sellerAbn())
        ));
    }

    public EmailContent fulfilmentUpdated(AdminDtos.OrderDetail order, String accountUrl) {
        String status = order.summary().fulfillmentStatus();
        String headline = switch (status) {
            case "SHIPPED" -> "Your gear is moving.";
            case "DELIVERED" -> "Your world has arrived.";
            case "CANCELLED" -> "Your order was cancelled.";
            default -> "Your order has been updated.";
        };
        String detail = "SHIPPED".equals(status)
                ? "Carrier: %s · Tracking: %s".formatted(order.carrier(), order.trackingNumber())
                : "Order " + order.summary().orderReference();
        String subject = switch (status) {
            case "SHIPPED" -> "Order " + order.summary().orderReference() + " has shipped";
            case "DELIVERED" -> "Order " + order.summary().orderReference() + " was delivered";
            case "CANCELLED" -> "Order " + order.summary().orderReference() + " was cancelled";
            default -> "Order " + order.summary().orderReference() + " was updated";
        };
        String text = "%s\n\n%s\n\n%s".formatted(headline, detail, accountUrl);
        return new EmailContent(subject, text, frame(
                "ORDER " + status,
                headline,
                detail,
                "VIEW ORDER",
                accountUrl,
                "Order " + order.summary().orderReference()
        ));
    }

    private String orderRow(OrderLineResponse item, String currency) {
        return itemRow(item.quantity(), item.brand() + " " + item.productName(), item.lineTotalCents(), currency);
    }

    private String invoiceRow(InvoiceLineResponse item, String currency) {
        return itemRow(item.quantity(), item.description(), item.lineTotalIncGstCents(), currency);
    }

    private String itemRow(int quantity, String description, long amount, String currency) {
        return """
                <tr>
                  <td style="padding:15px 0;border-bottom:1px solid #d7d6d1;color:#181b1c;font-size:14px;line-height:1.5;">%d × %s</td>
                  <td style="padding:15px 0;border-bottom:1px solid #d7d6d1;color:#181b1c;font-size:14px;text-align:right;white-space:nowrap;">%s</td>
                </tr>
                """.formatted(quantity, escape(description), money(amount, currency));
    }

    private String frame(String eyebrow, String headline, String body, String button, String url, String note) {
        return shell("""
                <p style="margin:0 0 24px;color:#5b999b;font-size:12px;font-weight:700;letter-spacing:3px;">%s</p>
                <h1 style="margin:0 0 22px;color:#f7f6f2;font-size:38px;line-height:1.05;font-weight:500;letter-spacing:-1.5px;">%s</h1>
                <p style="margin:0 0 32px;color:#b8bcbb;font-size:16px;line-height:1.7;">%s</p>
                %s
                <p style="margin:28px 0 0;color:#777d7c;font-size:12px;line-height:1.6;">%s</p>
                """.formatted(escape(eyebrow), escape(headline), escape(body), button(button, url), escape(note)));
    }

    private String commerceFrame(String eyebrow, String headline, String reference, String rows,
                                 String totalLabel, String total, String button, String url, String note) {
        return shell("""
                <p style="margin:0 0 20px;color:#5b999b;font-size:12px;font-weight:700;letter-spacing:3px;">%s</p>
                <h1 style="margin:0 0 12px;color:#f7f6f2;font-size:36px;line-height:1.08;font-weight:500;letter-spacing:-1.5px;">%s</h1>
                <p style="margin:0 0 28px;color:#929796;font-size:13px;letter-spacing:1px;">%s</p>
                <table role="presentation" style="width:100%%;border-collapse:collapse;background:#f4f3ef;padding:0 20px;">
                  <tbody>%s</tbody>
                  <tfoot><tr>
                    <td style="padding:20px 0 4px;color:#68706f;font-size:11px;font-weight:700;letter-spacing:1.6px;">%s</td>
                    <td style="padding:20px 0 4px;color:#181b1c;font-size:22px;font-weight:700;text-align:right;">%s</td>
                  </tr></tfoot>
                </table>
                <div style="margin-top:30px;">%s</div>
                <p style="margin:26px 0 0;color:#777d7c;font-size:12px;line-height:1.6;">%s</p>
                """.formatted(escape(eyebrow), escape(headline), escape(reference), rows,
                escape(totalLabel), escape(total), button(button, url), note));
    }

    private String shell(String content) {
        return """
                <!doctype html><html><body style="margin:0;background:#ecebe6;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" style="width:100%%;border-collapse:collapse;"><tr><td style="padding:36px 16px;">
                    <table role="presentation" style="width:100%%;max-width:620px;margin:0 auto;border-collapse:collapse;background:#101617;">
                      <tr><td style="padding:25px 36px;border-bottom:1px solid #293031;color:#f7f6f2;font-size:15px;font-weight:700;letter-spacing:4px;">JG <span style="color:#5b999b;">MOLI</span></td></tr>
                      <tr><td style="padding:42px 36px 38px;">%s</td></tr>
                      <tr><td style="padding:20px 36px;border-top:1px solid #293031;color:#656d6c;font-size:10px;letter-spacing:1.5px;">LEAVE THE NOISE. ENTER YOUR WORLD.</td></tr>
                    </table>
                  </td></tr></table>
                </body></html>
                """.formatted(content);
    }

    private String button(String label, String url) {
        return """
                <a href="%s" style="display:inline-block;background:#f7f6f2;color:#111516;text-decoration:none;padding:16px 22px;font-size:12px;font-weight:700;letter-spacing:2px;">%s →</a>
                """.formatted(escape(url), escape(label));
    }

    private String money(long cents, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(AUSTRALIA);
        format.setCurrency(java.util.Currency.getInstance(currency));
        return format.format(BigDecimal.valueOf(cents, 2));
    }

    private String displayName(String name) {
        return name == null || name.isBlank() ? "there" : name.trim();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
