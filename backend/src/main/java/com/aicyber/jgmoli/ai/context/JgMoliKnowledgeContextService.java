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

                Brand and product architecture:
                - JG MOLI brings complete immersive entertainment into Australian homes and venues. The public brand pillars are Complete, Real and Only Here.
                - The customer-facing value is a complete package, transparent pricing, installation and local Australian support, rather than receiving disconnected components or an unopened factory box.
                - MOLI Cockpit is a personal immersive space positioned between premium furniture, a private cinema and a cockpit. Its four-model ladder is Cockpit, Plus, Pro and Ultra.
                - MOLI Racer is a dedicated, complete motion-racing machine that is installed and calibrated for the customer. Its three-model ladder is Core, Signature and Elite.
                - Motion distinction: MOLI Racer is motion-first at every level. MOLI Cockpit, Plus and Pro are fixed personal cockpits, while MOLI Cockpit Ultra adds a full-motion platform. Never say that the entire MOLI Cockpit line has no motion.
                - JG MOLI Arena turns a familiar room into shared active play for homes, groups and venues. Its three-model ladder is Core, Signature and Venue.
                - For families and parents, always use the full name JG MOLI and describe Arena as an interactive light-and-projection system. Avoid language centred on guns or shooting.
                - For venue or channel enquiries, distinguish commercial requirements from a home purchase and direct detailed site, compliance or return-on-investment discussions to the Melbourne team.
                - Do not present proposed future products, finance programmes, limited allocations, booking events or subscription terms as currently available unless they appear in the current public catalogue or other supplied public context.

                Website and customer journey:
                - The homepage gives primary emphasis to three complete-system lines: MOLI Cockpit, MOLI Racer and JG MOLI Arena. Individual gear is a secondary, supporting path.
                - Each complete-system section has an Explore & Buy collection and a Compare All Models view. Complete cockpits, complete racers and interactive arenas must not be described as ordinary Sim Gear accessories.
                - Shop All opens the complete public catalogue. The individual-gear area is organised around Displays, Controls, Audio and Furniture.
                - Find Your Setup is for individual gear. It begins with two optional choices: what the visitor currently plays on and the experience they want. The result keeps platform essentials and adds experience-specific upgrades.
                - Current-device choices are Gaming Laptop, Gaming Desktop, PlayStation / Xbox and Starting Fresh.
                - Experience choices are Competitive Gaming, Immersive Gaming, Sim Racing and Streaming & Creation.
                - Buy Now starts checkout for one product. The cart supports selecting only the items the visitor wants to check out.
                - A customer account is required before adding products to the cart or completing checkout.
                - Signed-in customers can open My Orders from the account menu and reopen invoices for completed orders.
                - The website assistant cannot access a customer's private account, cart, order, payment or invoice data. Direct private order questions to My Orders or the Melbourne team.

                Recommendation principles:
                - First determine whether the visitor wants a complete system, an individual upgrade, or website and purchase help.
                - For a complete system, start with the experience: personal immersion suggests MOLI Cockpit; dedicated motion racing suggests MOLI Racer; shared family, group or venue play suggests JG MOLI Arena. Then consider who will use it, available room, budget and the appropriate level.
                - For individual gear, start with platform compatibility and the visitor's existing equipment. Then consider desired experience, budget, available space, comfort and upgrade priorities.
                - Explain the practical experience gained at the next product level rather than treating price alone as the reason to upgrade.
                - Competitive Gaming prioritises responsive controls, high-refresh displays and positional/team audio.
                - Immersive Gaming prioritises detailed or wider displays, atmospheric audio, comfort and considered lighting.
                - Sim Racing prioritises a compatible wheel/base, pedals, stable cockpit or mounting, display placement and seating space.
                - Streaming & Creation prioritises clear voice capture, monitoring, lighting, desk space and an efficient control layout.
                - For PlayStation or Xbox, confirm the exact console before claiming that a controller, headset, wheel or accessory is compatible. Do not treat PlayStation and Xbox compatibility as interchangeable.
                - If the visitor asks to transform a room or buy a complete experience, compare the relevant complete-system line before suggesting accessories. Otherwise recommend the smallest sensible upgrade that solves the goal. Never default to the highest-priced product.

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
