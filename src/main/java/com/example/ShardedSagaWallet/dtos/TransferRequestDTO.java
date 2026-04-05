package com.example.ShardedSagaWallet.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {
    @NotNull(message = "From wallet Id is required")
    private Long fromWalletId;

    @NotNull(message = "From user Id is required")
    private Long fromUserId;

    @NotNull(message = "To wallet Id is required")
    private Long toWalletId;

    @NotNull(message = "To user Id is required")
    private Long toUserId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Builder.Default
    private String description="";
}

