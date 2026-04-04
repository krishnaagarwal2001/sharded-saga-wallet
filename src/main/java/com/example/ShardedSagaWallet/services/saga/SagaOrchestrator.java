package com.example.ShardedSagaWallet.services.saga;

import com.example.ShardedSagaWallet.entities.saga.SagaInstance;
import com.example.ShardedSagaWallet.entities.saga.SagaStep;
import com.example.ShardedSagaWallet.enums.SagaStatus;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.enums.StepStatus;
import com.example.ShardedSagaWallet.repositories.SagaInstanceRepository;
import com.example.ShardedSagaWallet.repositories.SagaStepRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SagaOrchestrator implements SagaOrchestratorInterface {
    private final ObjectMapper objectMapper;
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;
    private final SagaStepFactory sagaStepFactory;

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

            boolean success = step.execute(sagaContext);

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
            log.error("Failed to execute step {}", stepName);
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

            boolean success = step.compensate(sagaContext);

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

            log.error("Failed to execute step {}", stepName);
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

        List<SagaStep> completedSteps = sagaStepRepository.findCompletedStepsBySagaInstanceId(sagaInstanceId);

        Boolean allCompensated = true;

        for (SagaStep step : completedSteps) { // TODO :- Steps can be compensated parallel
            boolean compensated = compensateStep(sagaInstanceId, step.getStepName());

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

            // TODO :- either retry or pass to dead letter queue (Resilience Pattern)
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
