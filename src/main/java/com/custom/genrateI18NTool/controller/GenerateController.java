package com.custom.genrateI18NTool.controller;

import com.custom.genrateI18NTool.model.TransFile;
import com.custom.genrateI18NTool.model.TransResult;
import com.custom.genrateI18NTool.service.GenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GenerateController {
    @Autowired
    private GenerateService generateService;

    @RequestMapping("preview")
    public TransFile preview(@RequestBody TransFile transFile) {
        return generateService.preview(transFile);
    }

    @RequestMapping("trans")
    public TransResult trans(@RequestBody TransFile transFile) {
        return generateService.trans(transFile);
    }

    @RequestMapping("commitChange")
    public boolean commitChange(@RequestBody TransResult transResult) {
        return generateService.commitChange(transResult);
    }

    @RequestMapping("getFileLists")
    public List<String> getFileLists(@RequestBody TransResult transResult) {
        return generateService.getFileLists(transResult);
    }
}
