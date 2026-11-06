package pe.imperio.gym.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Component("culqiGateway")
public class CulqiGateway {
    private static final String CHARGES_URL = "https://api.culqi.com/v2/charges";

    private final String publicKey;
    private final String secretKey;
    private final RestClient client;

    public CulqiGateway(
            @Value("${app.culqi.public-key:}") String publicKey,
            @Value("${app.culqi.secret-key:}") String secretKey) {
        this.publicKey = publicKey.strip();
        this.secretKey = secretKey.strip();
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean isEnabled() {
        return (publicKey.startsWith("pk_test_") && secretKey.startsWith("sk_test_"))
                || (publicKey.startsWith("pk_live_") && secretKey.startsWith("sk_live_"));
    }

    public String publicKey() {
        return isEnabled() ? publicKey : "";
    }

    public void validateToken(String token) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("Culqi no está configurado. Contacta con el administrador.");
        }
        String expectedTokenPrefix = publicKey.startsWith("pk_test_") ? "tkn_test_" : "tkn_live_";
        if (token == null || !token.startsWith(expectedTokenPrefix) || token.length() > 200) {
            throw new IllegalArgumentException("No se recibió un token de pago válido de Culqi.");
        }
    }

    public String charge(String token, BigDecimal amount, String description, String email) {
        validateToken(token);
        long amountInCents;
        try {
            amountInCents = amount.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El importe del plan no tiene un formato válido.");
        }

        var request = Map.of(
                "amount", amountInCents,
                "currency_code", "PEN",
                "description", description,
                "email", email,
                "source_id", token);
        try {
            JsonNode response = client.post()
                    .uri(CHARGES_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("outcome").hasNonNull("type")) {
                throw new PaymentException("Culqi devolvió una respuesta incompleta.", false);
            }
            String outcome = response.path("outcome").path("type").asText();
            if (!outcome.equals("venta_exitosa")) {
                throw new PaymentException("Culqi no confirmó el cobro.", true);
            }
            String chargeId = response.path("id").asText();
            String expectedChargePrefix = publicKey.startsWith("pk_test_") ? "chr_test_" : "chr_live_";
            if (!chargeId.startsWith(expectedChargePrefix) || chargeId.length() > 80
                    || response.path("amount").asLong(-1) != amountInCents
                    || !response.path("currency_code").asText().equals("PEN")) {
                throw new PaymentException("Culqi devolvió una confirmación incompleta.", false);
            }
            return chargeId;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            boolean definitive = status >= 400 && status < 500 && status != 408 && status != 409 && status != 429;
            throw new PaymentException("Culqi no pudo completar el cobro.", definitive);
        } catch (RestClientException e) {
            throw new PaymentException("No fue posible verificar la respuesta de Culqi.", false);
        }
    }

    public static class PaymentException extends RuntimeException {
        private final boolean definitive;

        public PaymentException(String message, boolean definitive) {
            super(message);
            this.definitive = definitive;
        }

        public boolean isDefinitive() {
            return definitive;
        }
    }
}
