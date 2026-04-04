package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.enums.SagaSteps;

public interface SagaStep {

    boolean execute (SagaContext sagaContext);

    boolean compensate (SagaContext sagaContext);

    SagaSteps getName();
}
