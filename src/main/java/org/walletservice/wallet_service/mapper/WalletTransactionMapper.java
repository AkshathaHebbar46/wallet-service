package org.walletservice.wallet_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "walletId", ignore = true)
    @Mapping(target = "transactionDate", expression = "java(java.time.LocalDateTime.now())")
    TransactionEntity toEntity(WalletTransactionRequestDTO dto);

    @Mapping(source = "transactionDate", target = "timestamp")
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "availableDailyLimit", ignore = true)
    WalletTransactionResponseDTO toDTO(TransactionEntity entity);
}

