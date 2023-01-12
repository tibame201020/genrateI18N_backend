package com.custom.genrateI18NTool.model;

import lombok.*;

import java.io.Serializable;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReplaceTarget implements Serializable {
    private String targetStr;
    private String key;
}
