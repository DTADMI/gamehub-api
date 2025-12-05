package ca.dtadmi.gamehubapi.payments;

import java.util.HashMap;
import java.util.Map;

/**
 * No-op adapter used when payments are disabled or in local/dev environments.
 */
public class NullPaymentsAdapter implements PaymentsPort {
    @Override
    public String createCheckout(CheckoutRequest req) {
        return null; // Nothing to do
    }

    @Override
    public boolean handleWebhook(String signatureHeader, byte[] payload) {
        return true; // Ignore
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "ok");
        m.put("adapter", "null");
        return m;
    }
}
