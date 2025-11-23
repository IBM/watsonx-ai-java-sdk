/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.chat.model.schema;

/**
 * Enumeration of supported semantic formats for JSON Schema string values.
 * <p>
 * These formats provide additional validation hints and are aligned with common JSON Schema specifications and RFC standards. They describe the
 * expected semantic meaning of a string beyond its basic type.
 *
 * <ul>
 * <li>{@link #DATE_TIME} – an RFC 3339 date-time string</li>
 * <li>{@link #DATE} – a full-date string in RFC 3339 format</li>
 * <li>{@link #TIME} – a full-time string in RFC 3339 format</li>
 * <li>{@link #DURATION} – a duration formatted according to ISO 8601</li>
 * <li>{@link #EMAIL} – a valid email address</li>
 * <li>{@link #HOSTNAME} – a valid Internet hostname</li>
 * <li>{@link #IPV4} – a valid IPv4 address</li>
 * <li>{@link #IPV6} – a valid IPv6 address</li>
 * <li>{@link #UUID} – a valid Universally Unique Identifier</li>
 * </ul>
 *
 * <p>
 * Each enum constant corresponds to the exact string value that will be emitted in the generated JSON Schema.
 */
public enum Format {

    /**
     * Represents a string formatted as an RFC 3339 date-time. Example: {@code "2025-01-30T12:34:56Z"}.
     */
    DATE_TIME("date-time"),

    /**
     * Represents a string formatted as an RFC 3339 full-date. Example: {@code "2025-01-30"}.
     */
    DATE("date"),

    /**
     * Represents a string formatted as an RFC 3339 full-time. Example: {@code "12:34:56Z"}.
     */
    TIME("time"),

    /**
     * Represents a duration formatted according to ISO 8601. Example: {@code "P3DT4H30M"}.
     */
    DURATION("duration"),

    /**
     * Represents a string containing a valid email address.
     */
    EMAIL("email"),

    /**
     * Represents a valid Internet hostname.
     */
    HOSTNAME("hostname"),

    /**
     * Represents a valid IPv4 address.
     */
    IPV4("ipv4"),

    /**
     * Represents a valid IPv6 address.
     */
    IPV6("ipv6"),

    /**
     * Represents a valid UUID.
     */
    UUID("uuid");

    private final String value;

    /**
     * Creates a new {@link Format} with the given JSON Schema format value.
     *
     * @param value the string value associated with the format
     */
    Format(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON Schema string value associated with this format.
     *
     * @return the format’s string representation
     */
    public String getValue() {
        return value;
    }
}