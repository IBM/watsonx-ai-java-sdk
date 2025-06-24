/*
 * Copyright IBM Corp. 2025 - 2025
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.textgeneration;

import java.util.List;

/**
 * Container class for different moderation configurations.
 * <p>
 * Supports various moderation types such as Hate and Profanity (HAP), Personally Identifiable Information (PII), and Granite Guardian.
 * <p>
 * <b>Example usage:</b>
 *
 * <pre>{@code
 * Moderation.builder()
 *   .hap(Hap.of(TextModeration.of(0.85f), TextModeration.of(0.9f), true))
 *   .pii(Pii.of(true, false, false))
 *   .graniteGuardian(GraniteGuardian.of(TextModeration.of(0.95f), true))
 *   .inputRanges(List.of(InputRanges.of(0, 100)))
 *   .build();
 * }</pre>
 *
 */
public class Moderation {

  /**
   * Properties controlling the moderation of text with threshold.
   *
   * @param enabled whether the moderation is enabled
   * @param threshold threshold score for triggering moderation
   */
  public record TextModeration(boolean enabled, float threshold) {

    public static TextModeration of(float threshold) {
      return new TextModeration(true, threshold);
    }
  }

  /**
   * Properties for masking behavior during moderation. If this object exists (even empty), masking will be applied.
   *
   * @param removeEntityValue if true, the detected entity value will be removed from the output
   */
  public record MaskProperties(boolean removeEntityValue) {
  }

  /**
   * Properties specific to Hate and Profanity (HAP) moderation.
   *
   * @param input moderation properties applied to the input text
   * @param output moderation properties applied to the output text
   * @param mask masking properties applied when moderation detects content
   */
  public record Hap(TextModeration input, TextModeration output, MaskProperties mask) {

    public static Hap of(TextModeration input, TextModeration output, boolean mask) {
      return new Hap(input, output, new MaskProperties(mask));
    }
  }

  /**
   * Moderation properties without threshold, used for PII filtering.
   *
   * @param enabled whether the moderation is enabled
   */
  public record TextModerationWithoutThreshold(boolean enabled) {
  }

  /**
   * Properties specific to Personally Identifiable Information (PII) moderation.
   *
   * @param input moderation properties applied to the input text
   * @param output moderation properties applied to the output text
   * @param mask masking properties applied when PII is detected
   */
  public record Pii(TextModerationWithoutThreshold input, TextModerationWithoutThreshold output,
    MaskProperties mask) {

    public static Pii of(boolean input, boolean output, boolean mask) {
      return new Pii(new TextModerationWithoutThreshold(input), new TextModerationWithoutThreshold(output),
        new MaskProperties(mask));
    }
  }

  /**
   * Properties specific to the Granite Guardian moderation detector. This detector is in beta and may change in future versions.
   *
   * @param input moderation properties applied to the input text
   * @param mask masking properties applied when Granite Guardian detects content
   */
  public record GraniteGuardian(TextModeration input, MaskProperties mask) {

    public static GraniteGuardian of(TextModeration input, boolean mask) {
      return new GraniteGuardian(input, new MaskProperties(mask));
    }
  }

  /**
   * Represents a range within the input text to which moderation is applied. The end index is exclusive.
   *
   * @param start the start index of the range (inclusive), must be >= 0
   * @param end the end index of the range (exclusive), must be >= 0
   */
  public record InputRanges(Integer start, Integer end) {

    public static InputRanges of(Integer start, Integer end) {
      return new InputRanges(start, end);
    }
  }

  private final Hap hap;
  private final Pii pii;
  private final GraniteGuardian graniteGuardian;
  private final List<InputRanges> inputRanges;

  public Moderation(Builder builder) {
    hap = builder.hap;
    pii = builder.pii;
    graniteGuardian = builder.graniteGuardian;
    inputRanges = builder.inputRanges;
  }

  public Hap getHap() {
    return hap;
  }

  public Pii getPii() {
    return pii;
  }

  public GraniteGuardian getGraniteGuardian() {
    return graniteGuardian;
  }

  public List<InputRanges> getInputRanges() {
    return inputRanges;
  }

  /**
   * Returns a new {@link Builder} instance.
   * <p>
   * <b>Example usage:</b>
   *
   * <pre>{@code
   * Moderation.builder()
   *   .hap(Hap.of(TextModeration.of(0.85f), TextModeration.of(0.9f), true))
   *   .pii(Pii.of(true, false, false))
   *   .graniteGuardian(GraniteGuardian.of(TextModeration.of(0.95f), true))
   *   .inputRanges(List.of(InputRanges.of(0, 100)))
   *   .build();
   * }</pre>
   *
   * @return {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder class for constructing {@link Moderation} instances with configurable parameters.
   */
  public static class Builder {
    private Hap hap;
    private Pii pii;
    private GraniteGuardian graniteGuardian;
    private List<InputRanges> inputRanges;

    /**
     * Sets the Hate and Profanity (HAP) moderation configuration.
     *
     * @param hap the HAP moderation configuration
     */
    public Builder hap(Hap hap) {
      this.hap = hap;
      return this;
    }

    /**
     * Sets the Personally Identifiable Information (PII) moderation configuration.
     *
     * @param pii the PII moderation configuration
     */
    public Builder pii(Pii pii) {
      this.pii = pii;
      return this;
    }

    /**
     * Sets the Granite Guardian moderation configuration.
     *
     * @param graniteGuardian the Granite Guardian moderation configuration
     */
    public Builder graniteGuardian(GraniteGuardian graniteGuardian) {
      this.graniteGuardian = graniteGuardian;
      return this;
    }

    /**
     * Sets the list of input ranges to which moderation should be applied. Only the specified ranges of the input text will be evaluated.
     *
     * @param inputRanges the list of {@link InputRanges}
     */
    public Builder inputRanges(List<InputRanges> inputRanges) {
      this.inputRanges = inputRanges;
      return this;
    }

    /**
     * Builds a {@link Moderation} instance using the configured parameters.
     *
     * @return a new instance of {@link Moderation}
     */
    public Moderation build() {
      return new Moderation(this);
    }
  }
}
