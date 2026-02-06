/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.core;

/**
 * Enum representing supported languages with their corresponding ISO 639 language codes.
 */
public enum Language {
    AFRIKAANS("af"),
    ALBANIAN("sq"),
    AYMARA("ay"),
    BASQUE("eu"),
    BELARUSIAN("be"),
    BISLAMA("bi"),
    BULGARIAN("bg"),
    CATALAN("ca"),
    CREE("cr"),
    DANISH("da"),
    DUTCH("nl"),
    ENGLISH("en"),
    ESTONIAN("et"),
    FIJIAN("fj"),
    FILIPINO("fil"),
    FINNISH("fi"),
    FRENCH("fr"),
    GALICIAN("gl"),
    GERMAN("de"),
    GREEK("el"),
    HAITIAN_CREOLE("ht"),
    HEBREW("he"),
    HINDI("hi"),
    INDONESIAN("id"),
    IRISH("ga"),
    ITALIAN("it"),
    JAPANESE("ja"),
    JAVANESE("jv"),
    KALAALLISUT("kl"),
    KINYARWANDA("rw"),
    KONGO("kg"),
    KOREAN("ko"),
    KUANYAMA("kj"),
    LATIN("la"),
    MALAGASY("mg"),
    MANX("gv"),
    MARATHI("mr"),
    MACEDONIAN("mk"),
    NDONGA("ng"),
    NEPALI("ne"),
    NORTH_NDEBELE("nd"),
    NORWEGIAN("no"),
    OCCITAN("oc"),
    OJIBWA("oj"),
    POLISH("pl"),
    PORTUGUESE("pt"),
    QUECHUA("qu"),
    ROMANSH("rm"),
    RUNDI("rn"),
    RUSSIAN("ru"),
    SANGO("sg"),
    SANSKRIT("sa"),
    SERBIAN("sr"),
    SHONA("sn"),
    SPANISH("es"),
    SUNDANESE("su"),
    SWAHILI("sw"),
    SWATI("ss"),
    SWEDISH("sv"),
    TAMIL("ta"),
    TELUGU("te"),
    TSONGA("ts"),
    TSWANA("tn"),
    UKRAINIAN("uk"),
    UZBEK("uz"),
    XHOSA("xh"),
    ZULU("zu");

    private final String isoCode;

    Language(String isoCode) {
        this.isoCode = isoCode;
    }

    public String isoCode() {
        return isoCode;
    }

}
