package org.walletservice.wallet_service.service.wallet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WalletInternalValidationService {

    private final WebClient webClient;
    private final String internalAuthToken;

    public WalletInternalValidationService(WebClient.Builder webClientBuilder,
                                           @Value("${internal.auth.token}") String internalAuthToken) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
        this.internalAuthToken = internalAuthToken;
    }

    public void validateReceiverWallet(Long toWalletId) {
        webClient.get()
                .uri("/internal/wallet/{walletId}/validate", toWalletId)
                .header("Internal-Token", internalAuthToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

