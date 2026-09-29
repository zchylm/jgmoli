package com.aicyber.jgmoli.ai.context;

import com.aicyber.jgmoli.catalog.dto.CatalogProductResponse;
import com.aicyber.jgmoli.catalog.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class JgMoliKnowledgeContextService implements KnowledgeContextProvider {

    private final CatalogRepository catalogRepository;
    private final String legalName;
    private final String tradingName;
    private final String abn;
    private final String address;
    private final String email;
    private final String phone;

    public JgMoliKnowledgeContextService(
            CatalogRepository catalogRepository,
            @Value("${jgmoli.invoice.seller-legal-name}") String legalName,
            @Value("${jgmoli.invoice.seller-trading-name}") String tradingName,
            @Value("${jgmoli.invoice.seller-abn}") String abn,
            @Value("${jgmoli.invoice.seller-address}") String address,
            @Value("${jgmoli.invoice.seller-email}") String email,
            @Value("${jgmoli.invoice.seller-phone}") String phone
    ) {
        this.catalogRepository = catalogRepository;
        this.legalName = legalName;
        this.tradingName = tradingName;
        this.abn = abn;
        this.address = address;
        this.email = email;
        this.phone = phone;
    }

    @Override
    public String currentContext() {
        List<CatalogProductResponse> products = catalogRepository.findActiveProducts(null, null);
        StringBuilder context = new StringBuilder("""
                <JG_MOLI_PUBLIC_CONTEXT>
                This is public, customer-facing JG MOLI information supplied at request time.
                Product prices are AUD and state whether GST is included. A public catalogue listing does not confirm an exact stock quantity, dispatch date or platform compatibility beyond the confirmed description below.

                Company:
                """)
                .append("- Trading name: ").append(tradingName).append('\n')
                .append("- Legal entity: ").append(legalName).append('\n')
                .append("- ABN: ").append(abn).append('\n')
                .append("- Address: ").append(address).append('\n')
                .append("- Email: ").append(email).append('\n')
                .append("- Phone: ").append(phone).append('\n')
                .append("""

                Website and customer journey:
                - Shop All Gear opens the complete public product catalogue. Visitors can narrow by Displays, Controls, Audio, Sim Gear and Furniture, then by product subtype.
                - Get My Recommendation begins with two optional choices: what the visitor currently plays on and the experience they want. The result keeps platform essentials and adds experience-specific upgrades.
                - Current-device choices are Gaming Laptop, Gaming Desktop, PlayStation / Xbox and Starting Fresh.
                - Experience choices are Competitive Gaming, Immersive Gaming, Sim Racing and Streaming & Creation.
                - Buy Now starts checkout for one product. The cart supports selecting only the items the visitor wants to check out.
                - A customer account is required before adding products to the cart or completing checkout.
                - Signed-in customers can open My Orders from the account menu and reopen invoices for completed orders.
                - The website assistant cannot access a customer's private account, cart, order, payment or invoice data. Direct private order questions to My Orders or the Melbourne team.

                Recommendation principles:
                - Start with platform compatibility and the visitor's existing equipment. Then consider desired experience, budget, available space, comfort and upgrade priorities.
                - Competitive Gaming prioritises responsive controls, high-refresh displays and positional/team audio.
                - Immersive Gaming prioritises detailed or wider displays, atmospheric audio, comfort and considered lighting.
                - Sim Racing prioritises a compatible wheel/base, pedals, stable cockpit or mounting, display placement and seating space.
                - Streaming & Creation prioritises clear voice capture, monitoring, lighting, desk space and an efficient control layout.
                - For PlayStation or Xbox, confirm the exact console before claiming that a controller, headset, wheel or accessory is compatible. Do not treat PlayStation and Xbox compatibility as interchangeable.
                - Recommend the smallest sensible upgrade that solves the visitor's goal. Do not automatically suggest a complete setup or the highest-priced product.

                Current public catalogue:
                """);

        if (products.isEmpty()) {
            context.append("- No products are currently present in the public catalogue.\n");
        } else {
            for (CatalogProductResponse product : products) appendProduct(context, product);
        }
        return context.append("</JG_MOLI_PUBLIC_CONTEXT>").toString();
    }

    private void appendProduct(StringBuilder context, CatalogProductResponse product) {
        context.append("\nProduct: ").append(product.brand()).append(' ').append(product.name()).append('\n')
                .append("- Category: ").append(product.categoryName()).append(" / ").append(product.subtype()).append('\n')
                .append("- Description: ").append(product.description()).append('\n')
                .append("- Price: ").append(String.format(Locale.ROOT, "$%,.2f AUD", product.priceCents() / 100.0))
                .append(product.priceIncludesGst() ? " including GST" : " excluding GST").append('\n');
    }
}
