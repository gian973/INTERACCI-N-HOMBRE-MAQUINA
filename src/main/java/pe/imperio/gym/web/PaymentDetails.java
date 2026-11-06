package pe.imperio.gym.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("paymentDetails")
public record PaymentDetails(
    @Value("${app.payments.wallet-phone}") String walletPhone,
    @Value("${app.payments.holder}") String holder,
    @Value("${app.payments.bank}") String bank,
    @Value("${app.payments.cci}") String cci,
    @Value("${app.payments.demo}") boolean demo
) {}
