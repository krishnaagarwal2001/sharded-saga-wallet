package com.example.ShardedSagaWallet.services;

import com.example.ShardedSagaWallet.entities.Transaction;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.services.saga.SagaContext;
import com.example.ShardedSagaWallet.services.saga.SagaOrchestratorInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferSagaService {
    private final SagaOrchestratorInterface sagaOrchestrator;
    private final TransactionService transactionService;

    @Transactional
    private Long createSagaData(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description) {

        Transaction transaction = transactionService.createTransaction(fromWalletId, toWalletId, amount, description);

        SagaContext sagaContext = SagaContext
                .builder()
                .data(Map.ofEntries(
                        Map.entry("transactionId", transaction.getId()),
                        Map.entry("fromWalletId", fromWalletId),
                        Map.entry("toWalletId", toWalletId),
                        Map.entry("amount", amount),
                        Map.entry("description", description)))
                .build();

        Long sagaInstanceId = sagaOrchestrator.startSaga(sagaContext);

        log.info("Saga instance created with id {}", sagaInstanceId);

        transactionService.updateTransactionWithSagaInstanceId(transaction.getId(), sagaInstanceId);

        return sagaInstanceId;
    }

    public Long initiateTransfer(
            Long fromWalletId,
            Long toWalletId,
            BigDecimal amount,
            String description) {

        Long sagaInstanceId = createSagaData(fromWalletId, toWalletId, amount, description);
        executeTransferSaga(sagaInstanceId);
        return sagaInstanceId;
    }

    private void executeTransferSaga(Long sagaInstanceId) {
        log.info("Executing transfer saga with id {}", sagaInstanceId);

        try {

            for (SagaSteps step : SagaSteps.values()) {
                boolean success = sagaOrchestrator.executeStep(sagaInstanceId, step);
                if (!success) {
                    log.error("Failed to execute step {}", step.toString());
                    sagaOrchestrator.failSaga(sagaInstanceId);
                    return;
                }
            }

            sagaOrchestrator.completeSaga(sagaInstanceId);
            log.info("Transfer saga completed with id {}", sagaInstanceId);
        } catch (Exception e) {
            log.error("Failed to execute transfer saga with id {}", sagaInstanceId, e);
            sagaOrchestrator.failSaga(sagaInstanceId);

        }
    }
}
