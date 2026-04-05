package com.example.ShardedSagaWallet.services;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    public Wallet createWallet(Long userId) {
        log.info("Creating wallet for user {}", userId);
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .previousBalance(BigDecimal.ZERO)
                .build();
        wallet = walletRepository.save(wallet);
        log.info("Wallet created with id {}", wallet.getId());
        return wallet;
    }

    public Wallet getWalletById(Long walletId) {
        log.info("Getting wallet with id {}", walletId);
        return walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    /*
         It is a private method, so no need for @Transactional
         (Spring AOP does not apply to private methods).

         This method must be called from within an active @Transactional context,
         otherwise pessimistic locking and dirty checking will not work as expected.
     */
    private Wallet getWalletWithLock(Long walletId, Long userId) {
        log.info("Getting wallet (lock) with walletId {} and userId {}", walletId, userId);
        return walletRepository.findByIdAndUserIdWithLock(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    public Wallet getWalletByUserId(Long userId) {
        log.info("Getting wallet by user id {}", userId);
        return walletRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    /*
        `@Transactional(propagation = Propagation.REQUIRES_NEW)` ensures that the method always
        executes in a **new, independent transaction**. If an existing transaction is present,
        it is **suspended**, and a new one is started.

        This is used in the `credit` and `debit` methods of `WalletService`
        because they are invoked from Saga steps as well.

        Each Saga step should run in its **own transaction**. Without `REQUIRES_NEW`,
        these methods would join the existing transaction, and if a later step fails,
        the entire transaction would be rolled back — including previously successful steps.

        By using `REQUIRES_NEW`, each operation (like debit or credit) is **committed independently**,
        ensuring that completed steps are not rolled back automatically and can be compensated if
        needed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Wallet debit(Long walletId, Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Debit Amount must be greater than zero");
        }

        Wallet wallet = getWalletWithLock(walletId, userId);

        log.info("Wallet {} fetched with balance {}", walletId, wallet.getBalance());

        wallet.debit(amount); // validation inside entity

        log.info("Wallet {} debited. Balance: {} -> {}",
                walletId, wallet.getPreviousBalance(), wallet.getBalance());

        return wallet;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Wallet credit(Long walletId, Long userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Credit Amount must be greater than zero");
        }

        Wallet wallet = getWalletWithLock(walletId, userId);

        log.info("Wallet {} fetched with balance {}", walletId, wallet.getBalance());

        wallet.credit(amount);

        log.info("Wallet {} credited. Balance: {} -> {}",
                walletId, wallet.getPreviousBalance(), wallet.getBalance());

        return wallet;
    }

    @Transactional
    public BigDecimal getWalletBalance(Long walletId) {
        log.info("Getting balance for wallet {}", walletId);

        Wallet wallet = getWalletById(walletId);
        BigDecimal balance = wallet.getBalance();

        log.info("Balance for wallet {} is {}", walletId, balance);

        return balance;
    }

}
