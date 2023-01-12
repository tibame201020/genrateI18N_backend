package com.custom.genrateI18NTool.model;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransResult implements Serializable {
    private String filePath;
    private String fileContent;

    private String defaultLocalePath;
    private String defaultLocalePathContent;

    private String enLocalePath;
    private String enLocalePathContent;
}
