package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.entities.saga.SagaInstance;

public interface SagaOrchestratorInterface {
    Long startSaga(SagaContext context);

    boolean executeStep(Long sagaInstanceId, String stepName);

    boolean compensateStep(Long sagaInstanceId, String stepName);

    SagaInstance getSagaInstance(Long sagaInstanceId);

    void compensateSaga(Long sagaInstanceId);

    void failSaga(Long sagaInstanceId);

    void completeSaga(Long sagaInstanceId);
}
