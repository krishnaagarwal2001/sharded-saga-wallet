package com.example.ShardedSagaWallet.repositories;

import com.example.ShardedSagaWallet.entities.saga.SagaStep;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaInstanceId(Long sagaInstanceId);

    List<SagaStep> findBySagaInstanceIdAndStatus(Long sagaInstanceId, StepStatus status);

    Optional<SagaStep> findBySagaInstanceIdAndStepNameAndStatus(Long sagaInstanceId, SagaSteps stepName,
            StepStatus status);

    @Query("SELECT s FROM SagaStep s WHERE s.sagaInstanceId = :sagaInstanceId AND s.status = StepStatus.COMPLETED")
    List<SagaStep> findCompletedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);

    @Query("SELECT s FROM SagaStep s WHERE s.sagaInstanceId=:sagaInstanceId AND s.status IN (StepStatus.COMPLETED, StepStatus.COMPENSATED)")
    List<SagaStep> findCompletedOrCompensatedStepsBySagaInstanceId(@Param("sagaInstanceId") Long sagaInstanceId);
}
