package com.custom.genrateI18NTool.model;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransFile implements Serializable {
    private String path;
    private String charset;
    private String content;
    private String defaultLocalePath;
    private String enLocalePath;
    private ReplaceTarget[] replaceTargets;
}
