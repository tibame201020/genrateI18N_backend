package com.custom.genrateI18NTool.service.impl;

import com.custom.genrateI18NTool.model.ReplaceTarget;
import com.custom.genrateI18NTool.model.TransFile;
import com.custom.genrateI18NTool.model.TransResult;
import com.custom.genrateI18NTool.service.GenerateService;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.custom.genrateI18NTool.constant.Tag.*;

@Service
public class GenerateServiceImpl implements GenerateService {

    @Override
    public TransResult trans(TransFile transFile) {
        TransResult transResult = new TransResult();
        transResult.setFilePath(transFile.getPath());
        transResult.setDefaultLocalePath(transFile.getDefaultLocalePath());
        transResult.setEnLocalePath(transFile.getEnLocalePath());

        ReplaceTarget[] replaceTargets = transFile.getReplaceTargets();
        String fileContent = getTransFileContent(transFile.getPath(), transFile.getCharset());
        String fileName = new File(transFile.getPath()).getName();

        StringBuilder localDefaultStr = new StringBuilder(LOCALE_TITLE.getValue(fileName));
        StringBuilder localeEnStr = new StringBuilder(LOCALE_TITLE.getValue(fileName));

        for (ReplaceTarget replaceTarget : replaceTargets) {
            String targetStr = replaceTarget.getTargetStr();
            String key = S_TEXTFIELD.getValue(replaceTarget.getKey());
            fileContent = fileContent.replace(targetStr, key);
            localDefaultStr = buildLocaleStr(localDefaultStr, replaceTarget.getKey(), targetStr, false);
            localeEnStr = buildLocaleStr(localeEnStr, replaceTarget.getKey(), targetStr, true);
        }

        transResult.setFileContent(fileContent);
        transResult.setDefaultLocalePathContent(localDefaultStr.toString());
        transResult.setEnLocalePathContent(localeEnStr.toString());

        return transResult;
    }

    @Override
    public String preview(TransFile transFile) {
        return getTransFileContent(transFile.getPath(), transFile.getCharset());
    }

    @Override
    public boolean commitChange(TransResult transResult) {
        boolean result;
        result = writeFile(transResult.getFilePath(), transResult.getFileContent(), false);
        if (!result) {
            return false;
        }
        result = writeFile(transResult.getDefaultLocalePath(), transResult.getDefaultLocalePathContent(), true);
        if (!result) {
            return false;
        }
        result = writeFile(transResult.getEnLocalePath(), transResult.getEnLocalePathContent(), true);

        return result;
    }

    private boolean writeFile(String filePath, String content, boolean append) {
        try {
            FileUtils.write(getFile(filePath), content, StandardCharsets.UTF_8, append);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private StringBuilder buildLocaleStr(StringBuilder stringBuilder, String key, String value, boolean isLocaleEn) {
        stringBuilder.append(LOCALE_ORIGIN_STR.getValue(value));
        if (!isLocaleEn) {
            value = transToUnicode(value);
        }

        return stringBuilder.append(key).append("=").append(value).append("\n");
    }

    private String transToUnicode(String value) {
        StringBuilder unicode = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            unicode.append("\\u").append(Integer.toHexString(c));
        }

        return unicode.toString();
    }

    private File getFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                FileUtils.createParentDirectories(file);
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    private String getTransFileContent(String filepath, String charsetStr) {
        TransFile transFile = new TransFile();
        transFile.setPath(filepath);
        Charset charset = getCharset(charsetStr);
        try {
            File file = new File(filepath);
            return FileUtils.readFileToString(file, charset);
        } catch (Exception e) {
            throw new RuntimeException("read original file failed");
        }
    }

    private Charset getCharset(String charset) {
        try {
            return Charset.forName(charset);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
