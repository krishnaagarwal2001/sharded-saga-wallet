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
public class CreditDestinationWalletStep implements SagaStepInterface {

    private final WalletService walletService;

    @Override
    @Transactional
    public boolean execute(SagaContext sagaContext) {
        Long toWalletId = sagaContext.getLong("toWalletId");
        BigDecimal amount = sagaContext.getBigDecimal("amount");

        log.info("Crediting destination wallet {} with amount {}", toWalletId, amount);

        Wallet wallet = walletService.credit(toWalletId, amount);

        sagaContext.put("originalToWalletBalance", wallet.getPreviousBalance());
        sagaContext.put("toWalletBalanceAfterCredit", wallet.getBalance());

        log.info("Credit destination wallet step executed successfully");
        return true;
    }

    @Override
    @Transactional
    public boolean compensate(SagaContext sagaContext) {
        Long toWalletId = sagaContext.getLong("toWalletId");
        BigDecimal amount = sagaContext.getBigDecimal("amount");

        log.info("Compensation credit of destination wallet {} with amount {}", toWalletId, amount);

        Wallet wallet = walletService.debit(toWalletId, amount);

        sagaContext.put("toWalletBalanceAfterCreditCompensation", wallet.getBalance());

        log.info("Credit compensation of destination wallet step executed successfully");
        return true;
    }

    @Override
    public SagaSteps getName() {
        return SagaSteps.CREDIT_DESTINATION_WALLET_STEP;
    }
}
