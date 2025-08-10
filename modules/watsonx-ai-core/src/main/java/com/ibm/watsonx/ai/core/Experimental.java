/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated type as experimental, indicating that it is in a beta or preview state.
 * <p>
 * Experimental elements are subject to change or removal in future versions. They may be incomplete, unstable, or incompatible with future releases,
 * and should be used with caution in production code.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Experimental {}
