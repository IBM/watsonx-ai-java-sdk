/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import java.util.Arrays;
import java.util.Optional;

/**
 * Supported IBM Cloud regions.
 */
public enum CloudRegion {

    DALLAS("https://us-south.ml.cloud.ibm.com", "https://api.dataplatform.cloud.ibm.com/wx"),
    FRANKFURT("https://eu-de.ml.cloud.ibm.com", "https://api.eu-de.dataplatform.cloud.ibm.com/wx"),
    LONDON("https://eu-gb.ml.cloud.ibm.com", "https://api.eu-gb.dataplatform.cloud.ibm.com/wx"),
    TOKYO("https://jp-tok.ml.cloud.ibm.com", "https://api.jp-tok.dataplatform.cloud.ibm.com/wx"),
    SYDNEY("https://au-syd.ml.cloud.ibm.com", "https://api.au-syd.dai.cloud.ibm.com/wx"),
    TORONTO("https://ca-tor.ml.cloud.ibm.com", "https://api.ca-tor.dai.cloud.ibm.com/wx"),
    MUMBAI("https://ap-south-1.aws.wxai.ibm.com", "https://api.ap-south-1.aws.data.ibm.com/wx");

    private final String mlEndpoint;
    private final String wxEndpoint;

    CloudRegion(String mlEndpoint, String wxEndpoint) {
        this.mlEndpoint = mlEndpoint;
        this.wxEndpoint = wxEndpoint;
    }

    /**
     * Returns the endpoint for ML services.
     */
    public String mlEndpoint() {
        return mlEndpoint;
    }

    /**
     * Returns the endpoint for WX services.
     */
    public String wxEndpoint() {
        return wxEndpoint;
    }

    /**
     * Returns the {@link CloudRegion} whose ML endpoint matches the given URL, or {@link Optional#empty()} if none matches.
     *
     * @param url the ML endpoint URL to look up
     * @return an {@link Optional} containing the matching region, or empty if not found
     */
    public static Optional<CloudRegion> fromMlEndpoint(String url) {
        return url == null
            ? Optional.empty()
            : Arrays.stream(values()).filter(r -> r.mlEndpoint.equals(url)).findFirst();
    }

    /**
     * Returns the {@link CloudRegion} whose WX endpoint matches the given URL, or {@link Optional#empty()} if none matches.
     *
     * @param url the WX endpoint URL to look up
     * @return an {@link Optional} containing the matching region, or empty if not found
     */
    public static Optional<CloudRegion> fromWxEndpoint(String url) {
        return url == null
            ? Optional.empty()
            : Arrays.stream(values()).filter(r -> r.wxEndpoint.equals(url)).findFirst();
    }
}
