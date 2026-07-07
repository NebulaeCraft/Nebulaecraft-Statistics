package com.sakana.datacap.exit;

public final class ExitRegionIds {
    public static final String ID_PATTERN = "[\\p{L}\\p{N}_\\-:.]+";
    public static final String ID_RULE_MESSAGE =
            "Exit region id may only contain Unicode letters, numbers, _, -, :, and .";

    private ExitRegionIds() {
    }

    public static boolean isValid(String id) {
        return id != null && id.matches(ID_PATTERN);
    }
}
