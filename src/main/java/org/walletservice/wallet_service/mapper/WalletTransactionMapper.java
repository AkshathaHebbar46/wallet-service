package org.walletservice.wallet_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.walletservice.wallet_service.dto.request.WalletTransactionRequestDTO;
import org.walletservice.wallet_service.dto.response.WalletTransactionResponseDTO;
import org.walletservice.wallet_service.entity.transaction.TransactionEntity;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    @Mapping(target = "walletId", ignore = true)
    @Mapping(target = "transactionDate", ignore = true)
    TransactionEntity toEntity(WalletTransactionRequestDTO dto);

    @Mapping(source = "entity.transactionId", target = "transactionId")
    @Mapping(source = "entity.amount", target = "amount")
    @Mapping(source = "entity.type", target = "type")
    @Mapping(source = "entity.transactionDate", target = "timestamp")
    @Mapping(source = "entity.description", target = "description")
    WalletTransactionResponseDTO toDTO(
            TransactionEntity entity,
            double balance,
            double availableDailyLimit
    );
}
