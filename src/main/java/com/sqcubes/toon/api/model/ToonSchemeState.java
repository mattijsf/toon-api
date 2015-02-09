package com.sqcubes.toon.api.model;

public enum ToonSchemeState {
    COMFORT(0),
    HOME(1),
    SLEEP(2),
    AWAY(3);

    private final int schemeStateCode;

    private ToonSchemeState(int schemeStateCode) {
        this.schemeStateCode = schemeStateCode;
    }

    public int schemeStateCode() {
        return schemeStateCode;
    }

    public static ToonSchemeState fromSchemeStateCode(int schemeStateCode) {
        for (ToonSchemeState state : ToonSchemeState.values()) {
            if (state.schemeStateCode() == schemeStateCode) {
                return state;
            }
        }
        return ToonSchemeState.HOME;
    }
}
