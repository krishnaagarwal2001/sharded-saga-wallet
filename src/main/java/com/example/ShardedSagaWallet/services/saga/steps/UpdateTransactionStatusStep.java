package com.example.ShardedSagaWallet.services.saga.steps;

import com.example.ShardedSagaWallet.entities.Transaction;
import com.example.ShardedSagaWallet.entities.saga.SagaInstance;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.enums.TransactionStatus;
import com.example.ShardedSagaWallet.repositories.SagaInstanceRepository;
import com.example.ShardedSagaWallet.repositories.TransactionRepository;
import com.example.ShardedSagaWallet.services.saga.SagaContext;
import com.example.ShardedSagaWallet.services.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateTransactionStatusStep implements SagaStepInterface {
        private final TransactionRepository transactionRepository;
        private final SagaInstanceRepository sagaInstanceRepository;

        private final ObjectMapper objectMapper;

        @Override
        @Transactional
        public boolean execute(SagaContext sagaContext, Long sagaInstanceId) {
                Long transactionId = sagaContext.getLong("transactionId");

                log.info("Updating transaction status for transaction {}", transactionId);

                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transaction not found"));

                SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                                .orElseThrow(() -> new RuntimeException("SagaInstance not found: " + sagaInstanceId));

                sagaContext.put("originalTransactionStatus", transaction.getStatus());

                transaction.setStatus(TransactionStatus.SUCCESS);
                // transactionRepository.save(transaction); Don't need this as hibernate managed entity

                log.info("Transaction status updated for transaction {}", transactionId);

                sagaContext.put("transactionStatusAfterUpdate", transaction.getStatus());
                sagaInstance.setContext(objectMapper.writeValueAsString(sagaContext));

                log.info("Update transaction status step executed successfully");

                return true;
        }

        @Override
        @Transactional
        public boolean compensate(SagaContext sagaContext, Long sagaInstanceId) {
                Long transactionId = sagaContext.getLong("transactionId");

                log.info("Compensating transaction status for transaction {}", transactionId);

                TransactionStatus originalTransactionStatus = TransactionStatus
                                .valueOf(sagaContext.getString("originalTransactionStatus"));

                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transaction not found"));

                SagaInstance sagaInstance = sagaInstanceRepository.findById(sagaInstanceId)
                                .orElseThrow(() -> new RuntimeException("SagaInstance not found: " + sagaInstanceId));

                sagaContext.put("originalTransactionStatusBeforeCompensation", transaction.getStatus());

                transaction.setStatus(originalTransactionStatus);
                // transactionRepository.save(transaction); Don't need this as hibernate managed entity

                log.info("Transaction status compensated for transaction {}", transactionId);

                sagaContext.put("transactionStatusAfterCompensation", transaction.getStatus());
                sagaInstance.setContext(objectMapper.writeValueAsString(sagaContext));

                log.info("Compensating transaction status step executed successfully");

                return true;
        }

        @Override
        public SagaSteps getName() {
                return SagaSteps.UPDATE_TRANSACTION_STATUS_STEP;
        }
}
