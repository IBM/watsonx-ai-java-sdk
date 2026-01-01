/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

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
     * Returns the endpoint for ML services (e.g., model inference, training).
     */
    public String mlEndpoint() {
        return mlEndpoint;
    }

    /**
     * Returns the endpoint for WX services (e.g., prompts, notebooks, tools).
     */
    public String wxEndpoint() {
        return wxEndpoint;
    }
}
