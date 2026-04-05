package com.example.ShardedSagaWallet.services.saga.steps;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.services.WalletService;
import com.example.ShardedSagaWallet.services.saga.SagaContext;
import com.example.ShardedSagaWallet.services.saga.SagaStepInterface;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitSourceWalletStep implements SagaStepInterface {

    private final WalletService walletService;

    @Override
    @Transactional
    public boolean execute(SagaContext sagaContext) {
        Long fromWalletId = sagaContext.getLong("fromWalletId");
        BigDecimal amount = sagaContext.getBigDecimal("amount");

        log.info("Debiting source wallet {} with amount {}", fromWalletId, amount);

        Wallet wallet = walletService.debit(fromWalletId, amount);

        sagaContext.put("originalSourceWalletBalance", wallet.getPreviousBalance());
        sagaContext.put("sourceWalletBalanceAfterDebit", wallet.getBalance());

        log.info("Debit source wallet step executed successfully");
        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext sagaContext) {
        Long fromWalletId = sagaContext.getLong("fromWalletId");
        BigDecimal amount = sagaContext.getBigDecimal("amount");

        log.info("Compensating source wallet {} with amount {}", fromWalletId, amount);

        Wallet wallet = walletService.credit(fromWalletId, amount);

        sagaContext.put("sourceWalletBalanceAfterCreditCompensation", wallet.getBalance());

        log.info("Compensation executed successfully");
        return true;
    }

    @Override
    public SagaSteps getName() {
        return SagaSteps.DEBIT_SOURCE_WALLET_STEP;
    }
}