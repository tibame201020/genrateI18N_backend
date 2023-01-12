package com.custom.genrateI18NTool.service;

import com.custom.genrateI18NTool.model.TransFile;
import com.custom.genrateI18NTool.model.TransResult;

import java.util.List;

public interface GenerateService {
    TransResult trans(TransFile transFile);

    TransFile preview(TransFile transFile);

    boolean commitChange(TransResult transResult);

    List<String> getFileLists(TransResult transResult);
}
