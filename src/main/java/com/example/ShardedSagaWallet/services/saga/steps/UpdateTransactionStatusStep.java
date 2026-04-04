package com.example.ShardedSagaWallet.services.saga.steps;

import com.example.ShardedSagaWallet.entities.Transaction;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.enums.TransactionStatus;
import com.example.ShardedSagaWallet.repositories.TransactionRepository;
import com.example.ShardedSagaWallet.services.saga.SagaContext;
import com.example.ShardedSagaWallet.services.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateTransactionStatusStep implements SagaStep {
    private final TransactionRepository transactionRepository;

    @Override
    public boolean execute(SagaContext sagaContext) {
        Long transactionId = sagaContext.getLong("transactionId");

        log.info("Updating transaction status for transaction {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        sagaContext.put("originalTransactionStatus", transaction.getStatus());

        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        log.info("Transaction status updated for transaction {}", transactionId);

        sagaContext.put("transactionStatusAfterUpdate", transaction.getStatus());

        log.info("Update transaction status step executed successfully");

        return true;
    }

    @Override
    public boolean compensate(SagaContext sagaContext) {
        Long transactionId = sagaContext.getLong("transactionId");

        log.info("Compensating transaction status for transaction {}", transactionId);

        TransactionStatus originalTransactionStatus = TransactionStatus.valueOf(sagaContext.getString("originalTransactionStatus"));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        sagaContext.put("originalTransactionStatusBeforeCompensation", transaction.getStatus());

        transaction.setStatus(originalTransactionStatus);
        transactionRepository.save(transaction);

        log.info("Transaction status compensated for transaction {}", transactionId);

        sagaContext.put("transactionStatusAfterCompensation", transaction.getStatus());

        log.info("Compensating transaction status step executed successfully");

        return true;
    }

    @Override
    public SagaSteps getName() {
        return SagaSteps.UPDATE_TRANSACTION_STATUS_STEP;
    }
}
