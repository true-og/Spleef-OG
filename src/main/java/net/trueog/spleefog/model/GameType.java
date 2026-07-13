package net.trueog.spleefog.model;

import java.util.Locale;

public enum GameType {

    CLASSIC, BOW;

    public static GameType parse(String value) {

        if (value == null) {

            return CLASSIC;

        }

        String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("BOW_SPLEEF") || normalized.equals("BOWSPLEEF")) {

            return BOW;

        }

        return valueOf(normalized);

    }

}
