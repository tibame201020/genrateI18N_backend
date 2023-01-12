package com.custom.genrateI18NTool.constant;

public enum Tag {

    S_TEXTFIELD("<s:textfield key=\"%s\" />"),
    LOCALE_TITLE("\n### %s ###\n"),
    LOCALE_ORIGIN_STR("#%s\n");

    String value;

    Tag(String value) {
        this.value = value;
    }

    public String getValue(String str) {
        return String.format(value, str);
    }


}
