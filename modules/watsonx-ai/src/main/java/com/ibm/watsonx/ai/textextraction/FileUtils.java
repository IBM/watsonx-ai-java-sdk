/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textextraction;

import static java.util.Objects.requireNonNull;
import com.ibm.watsonx.ai.textextraction.TextExtractionParameters.Type;

public class FileUtils {

  public static String addExtension(String filename, Type extension) {
    requireNonNull(filename, "filename cannot be null");
    requireNonNull(extension, "extension cannot be null");

    if (filename.endsWith(extension.value()))
      return filename;

    var index = filename.lastIndexOf(".");
    return index > 0 ? filename.substring(0, index).concat(extension.value())
      : filename.concat(".").concat(extension.value());
  }
}
