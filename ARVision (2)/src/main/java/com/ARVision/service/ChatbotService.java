package com.ARVision.service;

import com.ARVision.entity.Product;
import com.ARVision.repository.OrderRepository;
import com.ARVision.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    // ── Main entry point ───────────────────────────────────────
    public String chat(String userMessage) {
        String msg = userMessage.toLowerCase().trim();

        // 1. Try smart rule-based answer first (works without Gemini)
        String ruleAnswer = ruleBasedAnswer(msg, userMessage);
        if (ruleAnswer != null)
            return ruleAnswer;

        // 2. Fall back to Gemini AI for complex questions
        String systemContext = buildSystemContext(userMessage);
        return callGemini(systemContext, userMessage);
    }

    // ══════════════════════════════════════════════════════════
    // SMART RULE-BASED RESPONSES (no API key needed)
    // ══════════════════════════════════════════════════════════
    private String ruleBasedAnswer(String msg, String original) {

        // ── Greetings ──
        if (matches(msg, "hello", "hi", "hey", "good morning", "good evening", "good afternoon", "howdy")) {
            return "👋 Hi there! Welcome to **ARVision** — where you shop in Augmented Reality! " +
                    "I'm ARBot, your shopping assistant.\n\n" +
                    "I can help you with:\n" +
                    "• 🛍️ Browsing products\n" +
                    "• 📦 Tracking your order\n" +
                    "• 🕶️ Understanding AR try-on\n" +
                    "• 💳 Payment & shipping info\n\n" +
                    "What can I help you with today?";
        }

        // ── Buying / Checkout process ──
        if (matches(msg, "how to buy", "how do i buy", "how to purchase", "how do i purchase",
                "buying process", "purchase process", "checkout", "how to order", "how do i order",
                "place order", "buying", "i want to buy", "i am buying", "i'm buying",
                "process of buying", "purchasing")) {
            return "🛒 **How to Buy on ARVision** — it's easy!\n\n" +
                    "**Step 1 — Browse Products**\n" +
                    "Go to the Shop page and browse or search for products. Look for the 🕶️ **AR** badge to try them in AR!\n\n"
                    +
                    "**Step 2 — AR Try-On (optional)**\n" +
                    "Click a product and tap **\"Try in AR\"** to preview it in your room using your camera.\n\n" +
                    "**Step 3 — Add to Cart**\n" +
                    "Click **\"Add to Cart\"** on any product you like.\n\n" +
                    "**Step 4 — Sign In**\n" +
                    "Create an account or sign in to continue.\n\n" +
                    "**Step 5 — Checkout**\n" +
                    "Go to your 🛒 Cart → Click **\"Checkout\"** → Enter your delivery address.\n\n" +
                    "**Step 6 — Payment**\n" +
                    "Pay securely with your credit/debit card via **Stripe**.\n\n" +
                    "**Step 7 — Track Order**\n" +
                    "Check **\"My Orders\"** for your order status and estimated delivery! 📦\n\n" +
                    "Need help with any specific step?";
        }

        // ── Multiple items / quantity ──
        if (matches(msg, "two", "2", "multiple", "quantity", "several", "more than one",
                "t-shirt", "tshirt", "shirt", "pair", "bundle")) {
            return "🛒 **Buying Multiple Items**\n\n" +
                    "You can easily buy multiple items or quantities!\n\n" +
                    "1. Browse to the product (e.g., T-shirt)\n" +
                    "2. Select your **size/color** if available\n" +
                    "3. Click **\"Add to Cart\"** — set quantity to **2** (or add it twice)\n" +
                    "4. Continue shopping or go to **Cart → Checkout**\n" +
                    "5. Enter your delivery address & pay via Stripe\n\n" +
                    "💡 Your order total will include both items together.\n\n" +
                    "Is there anything specific about our products you'd like to know?";
        }

        // ── Shipping ──
        if (matches(msg, "shipping", "delivery", "deliver", "how long", "when will", "how many days",
                "ship", "arrive", "arrival", "dispatch")) {
            return "📦 **Shipping Information**\n\n" +
                    "• **Delivery Time:** 5 business days after your order is confirmed\n" +
                    "• **Tracking:** You can track your order status under **\"My Orders\"**\n" +
                    "• **Order Statuses:** PENDING → PROCESSING → SHIPPED → DELIVERED\n" +
                    "• **Shipping Fee:** Included in the product price (free shipping!)\n\n" +
                    "Once your order is shipped, you'll see the status update to **SHIPPED** in your order history. 🚚";
        }

        // ── Return & Refund ──
        if (matches(msg, "return", "refund", "cancel", "money back", "exchange", "replace",
                "wrong item", "damaged", "broken", "not satisfied")) {
            return "↩️ **Return & Refund Policy**\n\n" +
                    "• **Cancellation:** You can cancel orders in **PENDING** or **PROCESSING** status\n" +
                    "• **Refund:** Refunds are available before your order is shipped\n" +
                    "• **SHIPPED/DELIVERED** orders cannot be refunded\n\n" +
                    "**How to cancel:**\n" +
                    "1. Go to **My Orders**\n" +
                    "2. Find your order\n" +
                    "3. Click **\"Cancel Order\"** (available only for PENDING/PROCESSING)\n\n" +
                    "For damaged items or other issues, contact our support team. 📧";
        }

        // ── Payment ──
        if (matches(msg, "payment", "pay", "credit card", "debit card", "stripe", "visa",
                "mastercard", "how to pay", "payment method", "secure")) {
            return "💳 **Payment Information**\n\n" +
                    "We use **Stripe** for 100% secure payments.\n\n" +
                    "• ✅ Visa & Mastercard credit/debit cards\n" +
                    "• ✅ All major international cards\n" +
                    "• ✅ Payments are encrypted and secure\n" +
                    "• ✅ No card details stored on our servers\n\n" +
                    "Your payment is processed only after you confirm checkout. 🔒";
        }

        // ── AR Try-On ──
        if (matches(msg, "ar", "augmented reality", "try on", "try-on", "3d", "how does ar",
                "ar model", "virtual try", "see in room", "preview")) {
            return "🕶️ **AR Try-On Feature**\n\n" +
                    "ARVision lets you preview products in **your real room** before buying!\n\n" +
                    "**How it works:**\n" +
                    "1. Browse products with the 🕶️ **AR badge**\n" +
                    "2. Open the product page and click **\"View in AR\"**\n" +
                    "3. Point your camera at a flat surface\n" +
                    "4. The 3D model appears in your space!\n" +
                    "5. Walk around it, see different angles\n\n" +
                    "**Supported formats:** GLB & USDZ (works on Android & iOS)\n\n" +
                    "It's like having the product in your room before you pay! 🪄";
        }

        // ── Order tracking ──
        if (matches(msg, "order status", "track order", "where is my order", "track my order",
                "my order", "order tracking", "check order")) {
            Optional<String> orderNum = extractOrderNumber(original);
            if (orderNum.isPresent()) {
                return lookupOrder(orderNum.get());
            }
            return "📦 **Track Your Order**\n\n" +
                    "To check your order status:\n\n" +
                    "1. Go to **My Orders** in the top navigation\n" +
                    "2. You'll see all your orders with current status\n\n" +
                    "**Order statuses:**\n" +
                    "• 🟡 **PENDING** — Order received, being verified\n" +
                    "• 🔵 **PROCESSING** — Being prepared for shipping\n" +
                    "• 🚚 **SHIPPED** — On the way to you!\n" +
                    "• ✅ **DELIVERED** — Delivered!\n" +
                    "• ❌ **CANCELLED** — Order was cancelled\n\n" +
                    "You can also share your order number (e.g., ORD-ABC1234) here and I'll look it up for you!";
        }

        // ── Product listing ──
        if (matches(msg, "what products", "do you have", "show products", "product list",
                "what do you sell", "catalog", "available products", "what can i buy")) {
            List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                return "🛍️ Our product catalog is currently being updated! New items will be available very soon.\n\n"
                        +
                        "In the meantime, you can:\n" +
                        "• Set up your account\n" +
                        "• Explore how AR try-on works\n" +
                        "• Check back shortly for new arrivals!\n\n" +
                        "Is there anything else I can help you with?";
            }
            StringBuilder sb = new StringBuilder("🛍️ **Our Current Products:**\n\n");
            for (Product p : products) {
                sb.append(String.format("• **%s** — $%.2f", p.getName(),
                        p.getPrice() != null ? p.getPrice() : 0f));
                if (p.getArModel() != null)
                    sb.append(" 🕶️ AR");
                if (p.getStockQuantity() != null && p.getStockQuantity() > 0)
                    sb.append(" | In Stock");
                sb.append("\n");
            }
            sb.append("\nTap any product on the shop page to see details and try AR! 🎉");
            return sb.toString();
        }

        // ── Account / Sign up ──
        if (matches(msg, "account", "sign up", "register", "login", "sign in",
                "create account", "forgot password")) {
            return "👤 **Account Help**\n\n" +
                    "• **Sign Up:** Click **\"Sign Up\"** in the top right corner\n" +
                    "• **Sign In:** Click **\"Sign In\"** and enter your email & password\n" +
                    "• **Forgot Password:** Contact our support team for a reset\n\n" +
                    "You need an account to place orders and track them. Registration is free and takes under 1 minute! ⚡";
        }

        // ── Contact / Support ──
        if (matches(msg, "contact", "support", "help", "email", "phone", "customer service")) {
            return "📞 **Contact & Support**\n\n" +
                    "Our team is here to help!\n\n" +
                    "• 💬 **Chat:** You're already chatting with me (ARBot)!\n" +
                    "• 📧 **Email:** support@arvision.store\n" +
                    "• ⏰ **Hours:** Monday–Friday, 9 AM–6 PM (local time)\n\n" +
                    "I can answer most questions instantly — just ask! 😊";
        }

        // ── Thanks ──
        if (matches(msg, "thank", "thanks", "thank you", "ty", "great", "awesome", "perfect",
                "helpful", "thx")) {
            return "😊 You're welcome! Happy shopping at **ARVision**! 🛍️\n\n" +
                    "Feel free to ask if you need anything else — I'm always here to help! 🤖✨";
        }

        // No rule matched → try Gemini
        return null;
    }

    /** Returns true if the message contains any keyword as a whole-word match */
    private boolean matches(String msg, String... keywords) {
        for (String kw : keywords) {
            // Use word boundary for single words; plain contains for multi-word phrases
            if (kw.contains(" ")) {
                if (msg.contains(kw))
                    return true;
            } else {
                // \b ensures "hi" doesn't match inside "shipping" or "t-shirts"
                if (msg.matches(".*\\b" + java.util.regex.Pattern.quote(kw) + "\\b.*"))
                    return true;
            }
        }
        return false;
    }

    /** Live order lookup by order number */
    private String lookupOrder(String orderNumber) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getOrderNumber() != null &&
                        o.getOrderNumber().equalsIgnoreCase(orderNumber))
                .findFirst()
                .map(order -> {
                    String items = order.getOrderItems().stream()
                            .map(i -> i.getProduct().getName() + " x" + i.getQuantity())
                            .collect(Collectors.joining(", "));
                    return String.format(
                            "📦 **Order %s**\n\n" +
                                    "• Status: **%s**\n" +
                                    "• Total: **$%.2f**\n" +
                                    "• Items: %s\n" +
                                    "• Est. Delivery: %s\n\n" +
                                    "Is there anything else you'd like to know?",
                            order.getOrderNumber(),
                            order.getStatus(),
                            order.getTotalAmount() != null ? order.getTotalAmount() : 0f,
                            items,
                            order.getEstimatedDelivery() != null
                                    ? order.getEstimatedDelivery().toLocalDate().toString()
                                    : "TBD");
                })
                .orElse("❌ I couldn't find an order with number **" + orderNumber + "**.\n\n" +
                        "Please double-check the order number (format: ORD-XXXXXXXX) or visit **My Orders** to see all your orders.");
    }

    // ══════════════════════════════════════════════════════════
    // GEMINI CONTEXT BUILDER (for non-FAQ questions)
    // ══════════════════════════════════════════════════════════
    private String buildSystemContext(String userMessage) {
        StringBuilder context = new StringBuilder();
        context.append("""
                You are ARBot, a friendly AI assistant for ARVision — an AR-powered e-commerce store.

                === BUSINESS INFO ===
                - Store: ARVision | AR try-on for glasses, eyewear, accessories
                - Shipping: 5 business days
                - Payment: Stripe (credit/debit card)
                - Refund: PENDING/PROCESSING orders only
                - Order flow: PENDING → PROCESSING → SHIPPED → DELIVERED

                """);

        String msg = userMessage.toLowerCase();
        if (isProductQuery(msg)) {
            context.append("=== PRODUCTS ===\n");
            List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                context.append("(Catalog is currently empty)\n");
            } else {
                products.forEach(p -> context.append(String.format(
                        "- %s | $%.2f | %s | AR: %s\n",
                        p.getName(),
                        p.getPrice() != null ? p.getPrice() : 0f,
                        p.getCategory() != null ? p.getCategory() : "General",
                        p.getArModel() != null ? "Yes" : "No")));
            }
            context.append("\n");
        }

        extractOrderNumber(userMessage)
                .ifPresent(num -> context.append("=== ORDER LOOKUP ===\n").append(lookupOrder(num)).append("\n"));

        context.append("Be concise, warm, and helpful. Format prices as $XX.XX.\n");
        return context.toString();
    }

    private boolean isProductQuery(String msg) {
        return msg.contains("product") || msg.contains("item") || msg.contains("buy")
                || msg.contains("price") || msg.contains("stock") || msg.contains("catalog")
                || msg.contains("glass") || msg.contains("sunglass") || msg.contains("eyewear");
    }

    private Optional<String> extractOrderNumber(String message) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("ORD-[A-Za-z0-9]{6,10}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(message);
        return m.find() ? Optional.of(m.group().toUpperCase()) : Optional.empty();
    }

    // ══════════════════════════════════════════════════════════
    // GEMINI API CALL
    // ══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private String callGemini(String systemContext, String userMessage) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemContext))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userMessage)))),
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 500));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + geminiApiKey, new HttpEntity<>(body, headers), Map.class);
            var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            return parts.get(0).get("text").toString();
        } catch (Exception e) {
            // Gemini unavailable — give a helpful generic reply
            return "🤖 I'm here to help! I can answer questions about:\n\n" +
                    "• 🛒 How to buy & checkout\n" +
                    "• 📦 Shipping & delivery (5 days)\n" +
                    "• ↩️ Returns & refunds\n" +
                    "• 🕶️ AR try-on feature\n" +
                    "• 💳 Payment methods\n" +
                    "• 📋 Order tracking\n\n" +
                    "Just ask me any of these topics! 😊";
        }
    }
}
