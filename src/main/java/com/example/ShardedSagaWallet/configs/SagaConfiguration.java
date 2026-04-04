package com.example.ShardedSagaWallet.configs;

import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.services.saga.SagaStepInterface;
import com.example.ShardedSagaWallet.services.saga.steps.CreditDestinationWalletStep;
import com.example.ShardedSagaWallet.services.saga.steps.DebitSourceWalletStep;
import com.example.ShardedSagaWallet.services.saga.steps.UpdateTransactionStatusStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SagaConfiguration {

    @Bean
    public Map<SagaSteps, SagaStepInterface> sagaStepMap(
            DebitSourceWalletStep debitSourceWalletStep,
            CreditDestinationWalletStep creditDestinationWalletStep,
            UpdateTransactionStatusStep updateTransactionStatusStep) {
        Map<SagaSteps, SagaStepInterface> map = new HashMap<>();
        map.put(SagaSteps.DEBIT_SOURCE_WALLET_STEP, debitSourceWalletStep);
        map.put(SagaSteps.CREDIT_DESTINATION_WALLET_STEP, creditDestinationWalletStep);
        map.put(SagaSteps.UPDATE_TRANSACTION_STATUS_STEP, updateTransactionStatusStep);
        return map;
    }
}
