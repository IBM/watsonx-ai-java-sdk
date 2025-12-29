/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textprocessing;

/**
 * Enum representing the Cloud Object Storage (COS) service URLs for different IBM Cloud regions.
 */
public enum CosUrl {
    US_SOUTH("https://s3.us-south.cloud-object-storage.appdomain.cloud"),
    US_EAST("https://s3.us-east.cloud-object-storage.appdomain.cloud"),
    EU_GB("https://s3.eu-gb.cloud-object-storage.appdomain.cloud"),
    EU_DE("https://s3.eu-de.cloud-object-storage.appdomain.cloud"),
    AU_SYD("https://s3.au-syd.cloud-object-storage.appdomain.cloud"),
    JP_TOK("https://s3.jp-tok.cloud-object-storage.appdomain.cloud"),
    JP_OSA("https://s3.jp-osa.cloud-object-storage.appdomain.cloud"),
    CA_TOR("https://s3.ca-tor.cloud-object-storage.appdomain.cloud"),
    BR_SAO("https://s3.br-sao.cloud-object-storage.appdomain.cloud"),
    EU_ES("https://s3.eu-es.cloud-object-storage.appdomain.cloud"),
    CA_MON("https://s3.ca-mon.cloud-object-storage.appdomain.cloud");

    private final String url;

    CosUrl(String url) {
        this.url = url;
    }

    public String value() {
        return url;
    }
}
