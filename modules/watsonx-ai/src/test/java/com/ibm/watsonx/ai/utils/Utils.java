/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.utils;

public class Utils {

    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        NoSuchFieldException lastException = null;

        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                lastException = e;
                clazz = clazz.getSuperclass();
            }
        }
        throw lastException;
    }

    public static boolean isVirtual() {
        try {
            var method = Thread.class.getMethod("isVirtual");
            return (boolean) method.invoke(Thread.currentThread());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
