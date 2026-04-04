package com.example.ShardedSagaWallet.services.saga.steps;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.enums.SagaSteps;
import com.example.ShardedSagaWallet.repositories.WalletRepository;
import com.example.ShardedSagaWallet.services.saga.SagaContext;
import com.example.ShardedSagaWallet.services.saga.SagaStep;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebitSourceWalletStep implements SagaStep {

    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public boolean execute(SagaContext sagaContext) {
        Long fromWalletId = sagaContext.getLong("fromWalletId");
        BigDecimal amount = sagaContext.getBigDecimal("amount");

        log.info("Debiting source wallet {} with amount {}", fromWalletId, amount);

        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        log.info("Wallet fetched with balance {}", wallet.getBalance());
        sagaContext.put("originalSourceWalletBalance", wallet.getBalance());

        wallet.debit(amount);
        walletRepository.save(wallet);

        log.info("Wallet saved with balance {}", wallet.getBalance());
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

        Wallet wallet = walletRepository.findByIdWithLock(fromWalletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        log.info("Wallet fetched with balance {}", wallet.getBalance());
        sagaContext.put("sourceWalletBalanceBeforeCreditCompensation", wallet.getBalance());


        wallet.credit(amount);
        walletRepository.save(wallet);

        log.info("Wallet saved with balance {}", wallet.getBalance());
        sagaContext.put("sourceWalletBalanceAfterCreditCompensation", wallet.getBalance());

        log.info("Compensating source wallet step executed successfully");

        return true;
    }

    @Override
    public SagaSteps getName() {
        return SagaSteps.DEBIT_SOURCE_WALLET_STEP;
    }
}
