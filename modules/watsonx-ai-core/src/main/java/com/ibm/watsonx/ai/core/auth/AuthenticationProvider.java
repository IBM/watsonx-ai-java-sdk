/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core.auth;

import java.util.concurrent.CompletableFuture;

/**
 * This interface defines a contract for obtaining a token that can be used to authorize or identify a request.
 *
 * @see IAMAuthenticator
 */
public interface AuthenticationProvider {

    /**
     * Returns an access token for authenticating requests.
     *
     * @return the access token as a {@link String}
     */
    String token();

    /**
     * Asynchronously retrieves an access token for authenticating requests.
     * <p>
     * This method is useful when token acquisition involves network calls, I/O operations, or potentially long-running computations. The returned
     * {@link CompletableFuture} will complete with the token when available.
     *
     * @return a {@link CompletableFuture} that will complete with the access token
     */
    CompletableFuture<String> asyncToken();
}
