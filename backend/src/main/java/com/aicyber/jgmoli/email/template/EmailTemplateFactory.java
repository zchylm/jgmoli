package com.aicyber.jgmoli.email.template;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
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
        String subject = "Confirm your JG MOLI email";
        String text = """
                Hi %s,

                Confirm this email belongs to you:
                %s

                This link expires in 24 hours. If you did not create a JG MOLI account, you can ignore this email.

                JG MOLI Support
                """.formatted(displayName(name), actionUrl);
        return new EmailContent(subject, text, messageFrame(
                "ACCOUNT",
                "Confirm your email.",
                "Hi " + displayName(name) + ",",
                "One click keeps your account, recommendations and future orders connected.",
                "CONFIRM EMAIL",
                actionUrl,
                "This secure link expires in 24 hours. If you did not create this account, no action is needed."
        ));
    }

    public EmailContent passwordReset(String name, String actionUrl) {
        String subject = "Reset your JG MOLI password";
        String text = """
                Hi %s,

                Use this secure link to reset your JG MOLI password:
                %s

                This link expires in 30 minutes. If you did not request it, your password has not changed.

                JG MOLI Support
                """.formatted(displayName(name), actionUrl);
        return new EmailContent(subject, text, messageFrame(
                "ACCOUNT ACCESS",
                "Reset your password.",
                "Hi " + displayName(name) + ",",
                "Choose a new password, then return to your setup.",
                "RESET PASSWORD",
                actionUrl,
                "This secure link expires in 30 minutes. If you did not request it, your password has not changed."
        ));
    }

    public EmailContent invoiceIssued(InvoiceResponse invoice, String accountUrl) {
        String subject = "Order confirmed " + invoice.orderReference() + " · Tax invoice attached";
        String itemText = invoice.lines().stream()
                .map(line -> "%d × %s — %s".formatted(line.quantity(), line.description(),
                        money(line.lineTotalIncGstCents(), invoice.currency())))
                .reduce((left, right) -> left + "\n" + right).orElse("");
        String text = """
                Hi %s,

                Your payment is confirmed for order %s.
                Tax invoice: %s

                %s

                Amount paid: %s
                GST included: %s

                Your PDF tax invoice is attached. You can also view the order in your JG MOLI account:
                %s

                %s | ABN %s
                """.formatted(displayName(invoice.buyerName()), invoice.orderReference(), invoice.invoiceNumber(), itemText,
                money(invoice.amountPaidCents(), invoice.currency()), money(invoice.gstCents(), invoice.currency()),
                accountUrl, invoice.sellerLegalName(), invoice.sellerAbn());
        String rows = invoice.lines().stream().map(line -> invoiceRow(line, invoice.currency()))
                .reduce((left, right) -> left + right).orElse("");
        String content = """
                <p style="margin:0 0 18px;color:#5b9697;font-size:11px;font-weight:700;letter-spacing:2.6px;">PAYMENT CONFIRMED</p>
                <h1 class="email-headline" style="margin:0;color:#131718;font-size:38px;line-height:1.08;font-weight:500;letter-spacing:-1.4px;">Your setup is confirmed.</h1>
                <p style="margin:20px 0 32px;color:#626968;font-size:15px;line-height:1.7;">Hi %s, your order is now confirmed. Your formal tax invoice is attached as a PDF.</p>
                <table role="presentation" style="width:100%%;table-layout:fixed;border-collapse:collapse;border:1px solid #d9d9d3;">
                  <tr class="email-detail">
                    <td style="padding:17px 20px;border-bottom:1px solid #e2e1dc;color:#727877;font-size:10px;font-weight:700;letter-spacing:1.5px;">ORDER</td>
                    <td style="padding:17px 20px;border-bottom:1px solid #e2e1dc;color:#171b1c;font-size:13px;font-weight:700;text-align:right;word-break:break-word;">%s</td>
                  </tr>
                  <tr class="email-detail">
                    <td style="padding:17px 20px;color:#727877;font-size:10px;font-weight:700;letter-spacing:1.5px;">TAX INVOICE</td>
                    <td style="padding:17px 20px;color:#171b1c;font-size:13px;font-weight:700;text-align:right;word-break:break-word;">%s</td>
                  </tr>
                </table>
                <table role="presentation" style="width:100%%;table-layout:fixed;margin-top:22px;border-collapse:collapse;background:#f1f0eb;">
                  <tbody>%s</tbody>
                </table>
                <table role="presentation" style="width:100%%;table-layout:fixed;border-collapse:collapse;background:#121819;">
                  <tr class="email-total">
                    <td style="padding:22px 20px;color:#aeb5b3;font-size:10px;font-weight:700;letter-spacing:1.7px;">AMOUNT PAID · GST INCLUDED</td>
                    <td style="padding:22px 20px;color:#ffffff;font-size:25px;font-weight:700;text-align:right;">%s</td>
                  </tr>
                </table>
                <div style="margin-top:30px;">%s</div>
                <p style="margin:24px 0 0;color:#747a79;font-size:12px;line-height:1.65;">The attached PDF is your tax invoice. Keep it for your records; your order details also remain available in My Orders.</p>
                <p style="margin:18px 0 0;color:#8a8f8e;font-size:11px;line-height:1.6;">%s · ABN %s</p>
                """.formatted(escape(displayName(invoice.buyerName())), escape(invoice.orderReference()),
                escape(invoice.invoiceNumber()), rows, money(invoice.amountPaidCents(), invoice.currency()),
                button("VIEW ORDER", accountUrl), escape(invoice.sellerLegalName()), escape(invoice.sellerAbn()));
        return new EmailContent(subject, text, shell(
                "Payment confirmed for order " + escape(invoice.orderReference()), content));
    }

    public EmailContent fulfilmentUpdated(AdminDtos.OrderDetail order, String accountUrl) {
        String status = order.summary().fulfillmentStatus();
        String headline = switch (status) {
            case "SHIPPED" -> "Your gear is moving.";
            case "DELIVERED" -> "Your setup has arrived.";
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
        return new EmailContent(subject, text, messageFrame(
                "ORDER " + status,
                headline,
                "Order " + order.summary().orderReference(),
                detail,
                "VIEW ORDER",
                accountUrl,
                "You can see the latest order details in My Orders."
        ));
    }

    private String invoiceRow(InvoiceLineResponse item, String currency) {
        return """
                <tr class="email-item">
                  <td style="width:68%%;padding:17px 20px;border-bottom:1px solid #d9d8d2;color:#1a1e1f;font-size:13px;line-height:1.45;word-break:break-word;">%d × %s<br><span style="color:#7a807f;font-size:10px;letter-spacing:.5px;">%s</span></td>
                  <td style="width:32%%;padding:17px 20px;border-bottom:1px solid #d9d8d2;color:#1a1e1f;font-size:13px;font-weight:700;text-align:right;">%s</td>
                </tr>
                """.formatted(item.quantity(), escape(item.description()), escape(item.sku()),
                money(item.lineTotalIncGstCents(), currency));
    }

    private String messageFrame(String eyebrow, String headline, String greeting, String body,
                                String button, String url, String note) {
        String content = """
                <p style="margin:0 0 18px;color:#5b9697;font-size:11px;font-weight:700;letter-spacing:2.6px;">%s</p>
                <h1 class="email-headline" style="margin:0;color:#131718;font-size:38px;line-height:1.08;font-weight:500;letter-spacing:-1.4px;">%s</h1>
                <p style="margin:26px 0 8px;color:#202526;font-size:15px;line-height:1.7;">%s</p>
                <p style="margin:0 0 30px;color:#626968;font-size:15px;line-height:1.7;">%s</p>
                %s
                <p style="margin:26px 0 0;padding-top:22px;border-top:1px solid #ddddd7;color:#777d7c;font-size:11px;line-height:1.65;">%s</p>
                """.formatted(escape(eyebrow), escape(headline), escape(greeting), escape(body),
                button(button, url), escape(note));
        return shell(escape(headline), content);
    }

    private String shell(String preheader, String content) {
        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                      @media only screen and (max-width: 520px) {
                        .email-body { padding: 34px 24px 32px !important; }
                        .email-headline { font-size: 31px !important; }
                        .email-header { padding: 21px 24px !important; }
                        .email-detail td, .email-item td, .email-total td {
                          display: block !important;
                          width: auto !important;
                          text-align: left !important;
                          word-break: break-word !important;
                        }
                        .email-detail td:first-child { padding-bottom: 4px !important; border-bottom: 0 !important; }
                        .email-detail td:last-child { padding-top: 4px !important; }
                        .email-item td:first-child { padding-bottom: 4px !important; border-bottom: 0 !important; }
                        .email-item td:last-child { padding-top: 4px !important; }
                        .email-total td:first-child { padding-bottom: 4px !important; }
                        .email-total td:last-child { padding-top: 4px !important; }
                      }
                    </style>
                  </head>
                  <body style="margin:0;padding:0;background:#efeee9;font-family:Arial,Helvetica,sans-serif;-webkit-text-size-adjust:100%%;">
                    <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%s</div>
                    <table role="presentation" style="width:100%%;border-collapse:collapse;background:#efeee9;">
                      <tr><td style="padding:32px 12px;">
                        <table role="presentation" style="width:100%%;max-width:640px;table-layout:fixed;margin:0 auto;border-collapse:collapse;background:#fbfaf7;border:1px solid #d8d8d2;">
                          <tr><td class="email-header" colspan="2" style="padding:24px 36px;background:#121819;color:#ffffff;font-size:14px;font-weight:700;letter-spacing:4px;">JG <span style="color:#70a9aa;">MOLI</span></td></tr>
                          <tr><td class="email-body" colspan="2" style="padding:46px 44px 42px;">%s</td></tr>
                          <tr>
                            <td colspan="2" style="padding:21px 36px;border-top:1px solid #deded8;color:#858a89;font-size:9px;letter-spacing:1.5px;">
                              LEAVE THE NOISE. ENTER YOUR WORLD.
                            </td>
                          </tr>
                        </table>
                      </td></tr>
                    </table>
                  </body>
                </html>
                """.formatted(preheader, content);
    }

    private String button(String label, String url) {
        return """
                <a href="%s" style="display:inline-block;background:#121819;color:#ffffff;text-decoration:none;padding:16px 22px;font-size:11px;font-weight:700;letter-spacing:1.8px;">%s&nbsp;&nbsp;→</a>
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
