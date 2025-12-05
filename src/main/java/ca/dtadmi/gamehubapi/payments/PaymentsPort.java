package ca.dtadmi.gamehubapi.payments;

import java.util.Map;

/**
 * Capability port for payments/subscriptions. Concrete adapters (Stripe, Internal, Null)
 * implement these methods. Keep the surface small and map vendor-specific payloads
 * inside adapters.
 */
public interface PaymentsPort {
    /**
     * Create a checkout and return a URL to redirect the user to (or null if unsupported).
     */
    String createCheckout(CheckoutRequest req) throws Exception;

    /**
     * Handle an incoming webhook/event. Returns true if processed.
     */
    boolean handleWebhook(String signatureHeader, byte[] payload) throws Exception;

    /**
     * Health check to validate credentials/connectivity.
     */
    Map<String, Object> health();

    record CheckoutRequest(String priceId, String successUrl, String cancelUrl, String customerId) {
    }
}
