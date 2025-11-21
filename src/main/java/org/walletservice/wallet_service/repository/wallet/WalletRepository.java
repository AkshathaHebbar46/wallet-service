package org.walletservice.wallet_service.repository.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.walletservice.wallet_service.entity.wallet.WalletEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // Find all wallets belonging to a specific user
    List<WalletEntity> findByUserId(Long userId);   //check for index
    boolean existsByUserId(Long userId);

    // Find wallets created before a certain date
    List<WalletEntity> findByCreatedAtBefore(java.time.LocalDateTime date);

    @Query("SELECT w FROM WalletEntity w WHERE w.balance > (SELECT AVG(w2.balance) FROM WalletEntity w2)")
    List<WalletEntity> findWalletsAboveAverageBalance();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    Optional<WalletEntity> findByIdForUpdate(@Param("id") Long id);
}
