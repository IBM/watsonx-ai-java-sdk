/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.gateway.catalog;

import static java.util.Objects.isNull;
import java.util.List;

/**
 * Response returned by the list-models endpoint of the IBM watsonx.ai Model Gateway.
 *
 * @param object the object type, always {@code "list"}
 * @param data the list of models
 */
public record ModelGatewayListModelsResponse(String object, List<ModelGatewayModel> data) {

    public ModelGatewayListModelsResponse {
        data = isNull(data) ? null : List.copyOf(data);
    }
}
