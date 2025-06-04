package com.ibm.watsonx.core.http;

import java.net.http.HttpClient;

/**
 * The abstract base class for all HTTP clients used in the application.
 *
 * @see SyncHttpClient
 * @see AsyncHttpClient
 */
public abstract class BaseHttpClient {

   final HttpClient delegate;

   /**
    * Constructs a new instance of BaseHttpClient with the provided HttpClient delegate.
    *
    * @param httpClient {@link HttpClient} instance.
    */
   public BaseHttpClient(HttpClient httpClient) {
      this.delegate = httpClient;
   }
}
