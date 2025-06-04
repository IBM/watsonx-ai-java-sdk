package com.ibm.watsonx.auth;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import com.ibm.watsonx.core.auth.AuthenticationProvider;
import com.ibm.watsonx.core.http.AsyncHttpInterceptor;
import com.ibm.watsonx.core.http.SyncHttpInterceptor;

/**
 * Interceptor that adds a Bearer token to outgoing requests.
 */
public class BearerInterceptor implements SyncHttpInterceptor, AsyncHttpInterceptor {

    private final AuthenticationProvider authenticator;

    /**
     * Constructs a new BearerInterceptor with the given authenticator.
     *
     * @param authenticator the authenticator used to retrieve bearer tokens
     */
    public BearerInterceptor(AuthenticationProvider authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> intercept(HttpRequest request, BodyHandler<T> bodyHandler,
        int index, AsyncChain chain) {
        return authenticator.getTokenAsync()
            .thenCompose(token -> chain.proceed(requestWithBearer(request, token), bodyHandler));
    }

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, BodyHandler<T> bodyHandler, int index, Chain chain) throws IOException, InterruptedException {
        var token = authenticator.getToken();
        return chain.proceed(requestWithBearer(request, token), bodyHandler);
    }

    // Creates a copy of the given request with the Authorization header set to use the Bearer token.
    private HttpRequest requestWithBearer(HttpRequest request, String token) {
        return HttpRequest.newBuilder(request, (key, value) -> true)
            .header("Authorization", "Bearer %s".formatted(token))
            .build();
    }
}
