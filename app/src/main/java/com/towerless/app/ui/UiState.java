package com.towerless.app.ui;

/** Presentation preferences shared by screen renderers. */
public final class UiState {
    public enum Mode { BEGINNER, ADVANCED }

    private Mode mode = Mode.BEGINNER;
    private String languageTag = "hi";

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) {
        if (mode != null) this.mode = mode;
    }

    public String getLanguageTag() { return languageTag; }
    public void setLanguageTag(String languageTag) {
        if (languageTag != null && !languageTag.trim().isEmpty()) {
            this.languageTag = languageTag;
        }
    }
}
