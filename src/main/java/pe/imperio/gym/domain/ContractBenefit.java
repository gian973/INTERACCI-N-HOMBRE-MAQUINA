package pe.imperio.gym.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_benefits")
public class ContractBenefit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long getId() {
        return id;
    }

    @Column(name = "payment_id")
    public Long paymentId;

    public Long getPaymentId() {
        return paymentId;
    }

    @Column(name = "label")
    public String label;

    public String getLabel() {
        return label;
    }
}
