package org.walletservice.wallet_service.validation.validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WalletInternalValidationService {

    private final WebClient webClient;
    private final String internalAuthToken;

    public WalletInternalValidationService(
            WebClient.Builder webClientBuilder,
            @Value("${wallet-service.base-url}") String walletServiceBaseUrl,
            @Value("${internal.auth.token}") String internalAuthToken) {
        this.webClient = webClientBuilder.baseUrl(walletServiceBaseUrl).build();
        this.internalAuthToken = internalAuthToken;
    }

    public void validateReceiverWallet(Long toWalletId) {
        webClient.get()
                .uri("/internal/wallet/{walletId}/validate", toWalletId)
                .header("Internal-Token", internalAuthToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // consider using timeout or exception handling here
    }
}
