package com.example.ShardedSagaWallet.entities.saga;

import com.example.ShardedSagaWallet.enums.SagaStatus;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.apache.calcite.model.JsonType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_instance")
@Builder
public class SagaInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Type(JsonType.class)
    @Column(name = "context", columnDefinition = "json")
    private String context;

    @Column(name = "current_step")
    private SagaSteps currentStep;

    public void markAsStarted() {
        this.status = SagaStatus.STARTED;
    }

    public void markAsRunning() {
        this.status = SagaStatus.RUNNING;
    }

    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = SagaStatus.FAILED;
    }

    public void markAsCompensating() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void markAsCompensated() {
        this.status = SagaStatus.COMPENSATED;
    }
}
