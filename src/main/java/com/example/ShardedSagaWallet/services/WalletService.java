package com.example.ShardedSagaWallet.services;

import com.example.ShardedSagaWallet.entities.Wallet;
import com.example.ShardedSagaWallet.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    public Wallet getWalletById(Long walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public void debit(Long walletId, BigDecimal amount) {
        log.info("Debiting {} from wallet {}", amount, walletId);

        Wallet wallet = getWalletById(walletId);

        wallet.debit(amount);
        walletRepository.save(wallet);

        log.info("Debit successful for wallet {}", walletId);
    }

    @Transactional
    public void credit(Long walletId, BigDecimal amount) {
        log.info("Crediting {} from wallet {}", amount, walletId);

        Wallet wallet = getWalletById(walletId);

        wallet.credit(amount);
        walletRepository.save(wallet);

        log.info("Credit successful for wallet {}", walletId);
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
