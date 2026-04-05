package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.enums.SagaSteps;

public interface SagaStepInterface {

    boolean execute(SagaContext sagaContext, Long sagaInstanceId);

    boolean compensate(SagaContext sagaContext, Long sagaInstanceId);

    SagaSteps getName();
}
