package com.ibm.watsonx.core.auth;

import java.util.concurrent.CompletableFuture;

/**
 * This interface defines a contract for obtaining a token that can be used to authorize or identify a request.
 */
public interface AuthenticationProvider {

    /**
     * Returns a token.
     *
     * @return Token.
     */
    String getToken();

    /**
     * Asynchronously retrieves a token.
     *
     * @return a {@link CompletableFuture} that will complete with the token
     */
    CompletableFuture<String> getTokenAsync();
}
