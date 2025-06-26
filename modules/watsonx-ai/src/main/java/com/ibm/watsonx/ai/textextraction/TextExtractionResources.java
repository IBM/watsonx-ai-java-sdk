/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import java.util.List;

public record TextExtractionResources(int limit, Pagination first, int totalCount, Pagination next,
  List<TextExtractionResources> resources) {

  public record Pagination(String href) {
  }
}
