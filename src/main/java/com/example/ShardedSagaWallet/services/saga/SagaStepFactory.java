package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.enums.SagaSteps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SagaStepFactory {
    private final Map<SagaSteps, SagaStepInterface> sagaStepMap;

    public SagaStepInterface getSagaStep(SagaSteps sagaStep) {
        return sagaStepMap.get(sagaStep);
    }
}
