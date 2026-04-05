package com.example.ShardedSagaWallet.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditWalletRequestDTO {
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "User Id is required")
    private Long userId;

    @NotNull(message = "Wallet Id is required")
    private Long walletId;
}
