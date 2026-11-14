/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.deployment.DeploymentResource.DeploymentEntity;
import com.ibm.watsonx.ai.deployment.DeploymentResource.OnlineDeployment;

class DeploymentResourceTest {

    @SuppressWarnings("unchecked")
    @Test
    void should_snapshot_nested_custom_and_online_deployment_parameters() {
        var values = new ArrayList<>(List.of("original"));
        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("values", values);
        var online = new OnlineDeployment(parameters);
        var entity = new DeploymentEntity(online, parameters, null, null, null, null, null, null, null, null, null);

        values.add("changed");
        parameters.clear();

        for (var snapshot : List.of(online.parameters(), entity.custom())) {
            assertEquals(Map.of("values", List.of("original")), snapshot);
            assertThrows(UnsupportedOperationException.class, () -> ((List<String>) snapshot.get("values")).add("changed"));
        }
    }
}
