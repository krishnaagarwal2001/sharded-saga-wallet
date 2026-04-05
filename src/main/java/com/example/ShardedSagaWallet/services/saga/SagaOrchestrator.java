package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.entities.saga.SagaInstance;
import com.example.ShardedSagaWallet.entities.saga.SagaStep;
import com.example.ShardedSagaWallet.enums.SagaStatus;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.enums.StepStatus;
import com.example.ShardedSagaWallet.repositories.SagaInstanceRepository;
import com.example.ShardedSagaWallet.repositories.SagaStepRepository;

import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestrator implements SagaOrchestratorInterface {
    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

    private final Retry sagaCompensationRetry;

    @Override
    @Transactional
    public Long startSaga(SagaContext context) {
        try {
            String contextJsonString = objectMapper.writeValueAsString(context);

            SagaInstance sagaInstance = SagaInstance.builder()
                    .context(contextJsonString)
                    .status(SagaStatus.STARTED)
                    .build();

            sagaInstanceRepository.save(sagaInstance);

            log.info("Started saga with id {}", sagaInstance.getId());

            return sagaInstance.getId();
        } catch (Exception e) {
            log.error("Error starting saga", e);
            throw new RuntimeException("Error starting saga", e);
        }
    }

    @Override
    @Transactional
    public boolean executeStep(Long sagaInstanceId, SagaSteps stepName) {

        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);

        if (step == null) {
            log.error("Saga step not found for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        // we are finding in db in case of retries
        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.PENDING).orElse(
                        SagaStep.builder().sagaInstanceId(sagaInstanceId).stepName(stepName).status(StepStatus.PENDING)
                                .build());

        if (sagaStepDB.getId() == null) {
            sagaStepDB = sagaStepRepository.save(sagaStepDB);
        }

        try {
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

            sagaStepDB.markAsRunning(); // update the status as running
            sagaStepRepository.save(sagaStepDB);

            boolean success = step.execute(sagaContext, sagaInstanceId);

            if (success) {
                sagaStepDB.markAsCompleted(); // update the status as completed
                sagaStepRepository.save(sagaStepDB);

                sagaInstance.setCurrentStep(stepName);
                sagaInstance.markAsRunning();
                sagaInstanceRepository.save(sagaInstance);

                log.info("Step {} executed successfully", stepName);
                return true;

            } else {
                sagaStepDB.markAsFailed(); // update the status as failed
                sagaStepRepository.save(sagaStepDB);

                log.error("Step {} failed", stepName);
                return false;
            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);
            log.error("Failed to execute step {}", stepName, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean compensateStep(Long sagaInstanceId, SagaSteps stepName) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

        SagaStepInterface step = sagaStepFactory.getSagaStep(stepName);

        if (step == null) {
            log.error("Saga step not found for step name {}", stepName);
            throw new RuntimeException("Saga step not found");
        }

        // we are finding in db in case of retries
        SagaStep sagaStepDB = sagaStepRepository
                .findBySagaInstanceIdAndStepNameAndStatus(sagaInstanceId, stepName, StepStatus.COMPLETED).orElse(
                        null // no such step found in the db
                );

        if (sagaStepDB.getId() == null) {
            log.info("Step {} not found in the db for saga instance {}, so it is already compensated or not executed",
                    stepName, sagaInstanceId);
            return true;
        }

        try {
            SagaContext sagaContext = objectMapper.readValue(sagaInstance.getContext(), SagaContext.class);

            sagaStepDB.markAsCompensating();
            sagaStepRepository.save(sagaStepDB);

            boolean success = step.compensate(sagaContext, sagaInstanceId);

            if (success) {
                sagaStepDB.markAsCompensated();
                sagaStepRepository.save(sagaStepDB);
                log.info("Step {} compensated successfully", stepName);
                return true;
            } else {
                sagaStepDB.markAsFailed();
                sagaStepRepository.save(sagaStepDB);

                log.error("Step {} failed", stepName);
                return false;

            }

        } catch (Exception e) {
            sagaStepDB.markAsFailed();
            sagaStepRepository.save(sagaStepDB);

            log.error("Failed to compensate step {}", stepName, e);
            return false;
        }

    }

    private boolean compensateStepWithRetry(Long sagaInstanceId, SagaSteps stepName) {

        // Decorate the operation (compensateStep) with Resilience4j retry.
        // Retry.decorateCallable takes a Retry instance and a Callable<T> (here
        // Boolean)
        // and returns a "retry-aware" Callable that will automatically retry on failure
        // according to the Retry config.
        java.util.concurrent.Callable<Boolean> decoratedCallable = Retry.decorateCallable(sagaCompensationRetry,
                () -> compensateStep(sagaInstanceId, stepName));

        try {
            // Execute the decorated Callable.
            // If the operation fails, Resilience4j retry logic automatically retries
            // based on the configuration (maxAttempts, waitDuration, retryExceptions,
            // etc.).
            return decoratedCallable.call(); // call() throws Exception

        } catch (Exception e) {
            // Handle failures after all retries have been exhausted.
            // Here, we log the error and could also send the step to a DLQ (Dead Letter
            // Queue)
            // or trigger an alert for manual intervention.
            log.error("Compensation failed after retries for step {} of saga {}", stepName, sagaInstanceId, e);

            // Return false to indicate that the compensation step ultimately failed
            // despite retry attempts.
            return false;
        }
    }

    @Override
    public SagaInstance getSagaInstance(Long sagaInstanceId) {
        return sagaInstanceRepository.findById(sagaInstanceId)
                .orElseThrow(() -> new RuntimeException("Saga instance not found"));
    }

    @Override
    @Transactional
    public void compensateSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);

        sagaInstance.markAsCompensating();
        sagaInstanceRepository.save(sagaInstance);

        Boolean allCompensated = true;

        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);

        // Reverse the list to compensate steps in reverse order of completion
        Collections.reverse(completedSteps);

        for (SagaStep step : completedSteps) { // TODO :- Steps can be compensated parallel
            boolean compensated = compensateStepWithRetry(sagaInstanceId, step.getStepName());

            if (!compensated) {
                allCompensated = false;
            }
        }

        if (allCompensated) {
            sagaInstance.markAsCompensated();
            sagaInstanceRepository.save(sagaInstance);

            log.info("Saga {} compensated successfully", sagaInstanceId);
        } else {
            log.error("Saga {} compensation failed", sagaInstanceId);

            // TODO :- pass to dead letter queue (Resilience Pattern)
        }

    }

    @Override
    @Transactional
    public void failSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);
        sagaInstance.markAsFailed();
        sagaInstanceRepository.save(sagaInstance);

        compensateSaga(sagaInstanceId);

        log.info("Saga {} failed", sagaInstanceId);
    }

    @Override
    @Transactional
    public void completeSaga(Long sagaInstanceId) {
        SagaInstance sagaInstance = getSagaInstance(sagaInstanceId);
        sagaInstance.markAsCompleted();
        sagaInstanceRepository.save(sagaInstance);
    }
}
