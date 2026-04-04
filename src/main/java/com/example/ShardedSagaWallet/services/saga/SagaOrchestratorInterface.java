package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.entities.saga.SagaInstance;
import com.example.ShardedSagaWallet.enums.SagaSteps;

public interface SagaOrchestratorInterface {
    Long startSaga(SagaContext context);

    boolean executeStep(Long sagaInstanceId, SagaSteps stepName);

    boolean compensateStep(Long sagaInstanceId, SagaSteps stepName);

    SagaInstance getSagaInstance(Long sagaInstanceId);

    void compensateSaga(Long sagaInstanceId);

    void failSaga(Long sagaInstanceId);

    void completeSaga(Long sagaInstanceId);
}
