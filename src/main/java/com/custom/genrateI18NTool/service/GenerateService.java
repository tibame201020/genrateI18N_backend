package com.custom.genrateI18NTool.service;

import com.custom.genrateI18NTool.model.TransFile;
import com.custom.genrateI18NTool.model.TransResult;

public interface GenerateService {
    TransResult trans(TransFile transFile);

    String preview(TransFile transFile);

    boolean commitChange(TransResult transResult);
}
