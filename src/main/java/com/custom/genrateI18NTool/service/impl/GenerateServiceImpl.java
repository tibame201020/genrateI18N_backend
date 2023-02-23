package com.custom.genrateI18NTool.service.impl;

import com.custom.genrateI18NTool.model.ReplaceTarget;
import com.custom.genrateI18NTool.model.TransFile;
import com.custom.genrateI18NTool.model.TransResult;
import com.custom.genrateI18NTool.service.GenerateService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        //key加工
        replaceTargets = processingReplaceTarget(replaceTargets, transFile.getDefaultLocalePath(), transFile.getPath());

        String fileContent = getTransFileContent(transFile.getPath(), transFile.getCharset());
        String fileName = new File(transFile.getPath()).getName();

        StringBuilder localDefaultStr = new StringBuilder(LOCALE_TITLE.getValue(fileName));
        StringBuilder localeEnStr = new StringBuilder(LOCALE_TITLE.getValue(fileName));

        List<ReplaceTarget> replaceTargetList =
                Arrays.stream(replaceTargets)
                        .sorted((o1, o2) -> o2.getTargetStr().length() - o1.getTargetStr().length())
                        .collect(Collectors.toList());

        if (replaceTargetList.isEmpty()) {
            return null;
        }
        for (ReplaceTarget replaceTarget : replaceTargetList) {
            String targetStr = replaceTarget.getTargetStr();
            String key = S_TEXTFIELD.getValue(replaceTarget.getKey());
            String preContent = fileContent;
            fileContent = fileContent.replace(targetStr, key);
            if (preContent.equals(fileContent)) {
                continue;
            }
            if (checkLocaleKeyExist(transFile.getDefaultLocalePath(), replaceTarget.getKey())) {
                continue;
            }

            localDefaultStr = buildLocaleStr(localDefaultStr, replaceTarget.getKey(), targetStr, false);
            localeEnStr = buildLocaleStr(localeEnStr, replaceTarget.getKey(), targetStr, true);
        }

        transResult.setFileContent(fileContent);
        transResult.setDefaultLocalePathContent(localDefaultStr.toString());
        transResult.setEnLocalePathContent(localeEnStr.toString());
        if (localDefaultStr.toString().replaceAll(LOCALE_TITLE.getValue(fileName), "").trim().equals("")) {
            transResult.setDefaultLocalePathContent("");
            transResult.setEnLocalePathContent("");
        }

        return transResult;
    }

    private ReplaceTarget[] processingReplaceTarget(ReplaceTarget[] replaceTargets, String defaultLocalePath, String jspPath) {
        List<ReplaceTarget> processingReplaceTargets = new ArrayList<>();
        String localeContent = getTransFileContent(defaultLocalePath, "UTF-8");
        List<ReplaceTarget> localeReplaceTargets = transLocale(localeContent);
        long tmpCount = 0;

        for (ReplaceTarget replaceTarget : replaceTargets) {
            if ((replaceTarget.getKey().indexOf("..") == replaceTarget.getKey().lastIndexOf("..")) && replaceTarget.getKey().contains("msg")) {
                String system = replaceTarget.getKey().split("\\.\\.")[0];
                String jspName = new File(jspPath).getName().substring(0, new File(jspPath).getName().indexOf(".jsp"));
                String msg = replaceTarget.getKey().split("\\.\\.")[1];
                String templateKey = system + "." + jspName + "." + msg;
                long templateKeyCount = localeReplaceTargets.stream().filter(replaceTargetInlocale -> replaceTargetInlocale.getKey().contains(templateKey)).count();
                tmpCount ++;
                templateKeyCount = templateKeyCount + tmpCount;

                String newKey = templateKey + templateKeyCount;
                replaceTarget.setKey(newKey);
                processingReplaceTargets.add(replaceTarget);
            } else {
                processingReplaceTargets.add(replaceTarget);
            }
        }
        return processingReplaceTargets.stream().toArray(ReplaceTarget[]::new);
    }

    private boolean checkLocaleKeyExist(String defaultLocalePath, String key) {
        String localeContent = getTransFileContent(defaultLocalePath, "UTF-8");
        if (StringUtils.isBlank(localeContent)) {
            return false;
        }
        return localeContent.contains(key+"=");
    }

    @Override
    public TransFile preview(TransFile transFile) {
        if (new File(transFile.getPath()).isFile()) {
            transFile.setContent(getTransFileContent(transFile.getPath(), transFile.getCharset()));
            return transFile;
        }
        return null;
    }

    @Override
    public boolean commitChange(TransResult transResult) {
        boolean result;
        result = writeFile(transResult.getDefaultLocalePath(), transResult.getDefaultLocalePathContent(), true);
        if (!result) {
            return false;
        }
        result = writeFile(transResult.getEnLocalePath(), transResult.getEnLocalePathContent(), true);

        refactorJsp(new File(transResult.getFilePath()), new File(transResult.getDefaultLocalePath()));

        try {
            refactorAllJsp(transResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private void refactorAllJsp(TransResult transResult) throws Exception {
        File file = new File(transResult.getFilePath());
        if (!file.getParent().contains("jsp")) {
            return;
        }
        String jspDirPath = getJspDirPath(file.getParent());
        File jspDir = new File(jspDirPath);
        findAllFilesInFolder(jspDir, new File(transResult.getDefaultLocalePath()));
    }

    private void findAllFilesInFolder(File folder, File localeFile) throws Exception {
        for (File file : folder.listFiles()) {
            if (!file.isDirectory()) {
                refactorJsp(file, localeFile);
            } else {
                findAllFilesInFolder(file, localeFile);
            }
        }
    }

    private void refactorJsp(File file, File localeFile) {
        String fe = FilenameUtils.getExtension(file.getName());
        if (!fe.equals("jsp")) {
            return;
        }
        String jspContent = getTransFileContent(file.getAbsolutePath(), "UTF-8");
        if (!jspContent.contains("taglib prefix=\"s\"")) {
            System.out.println(file.getAbsolutePath());
            System.out.println("don't have struts tag plz check");
            return;
        }
        String localeContent = getTransFileContent(localeFile.getAbsolutePath(), "UTF-8");

        List<ReplaceTarget> replaceTargets = transLocale(localeContent);

        for (ReplaceTarget replaceTarget : replaceTargets) {
            String targetStr = replaceTarget.getTargetStr();
            String key = S_TEXTFIELD.getValue(replaceTarget.getKey());
            try {
                List<String> fileContents = FileUtils.readLines(file, StandardCharsets.UTF_8);
                boolean isNeedWrite = false;

                for (int i = 0; i < fileContents.size(); i++) {
                    String fileLine = fileContents.get(i);

                    if (fileLine.contains(targetStr)) {

                        if (isBrokenStr(fileLine, targetStr) || isInStrutsTag(fileLine, targetStr) || isAnnotation(fileLine)) {
                            continue;
                        }

                        String newLine = fileLine.replaceFirst(targetStr, key);
                        while (newLine.contains(targetStr) && !(isBrokenStr(newLine, targetStr) || isInStrutsTag(newLine, targetStr) || isAnnotation(fileLine))) {
                            String originLine = newLine;
                            newLine = newLine.replaceFirst(targetStr, key);
                            if (originLine.trim().equals(newLine.trim())) {
                                String preStr = newLine.substring(0, newLine.indexOf(targetStr));
                                String lastStr = newLine.substring(newLine.indexOf(targetStr) + targetStr.length());
                                newLine = preStr + key + lastStr;
                            }
                        }
                        fileContents.set(i, newLine);
                        isNeedWrite = true;
                    }
                }

                if (isNeedWrite) {
                    FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), fileContents);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isAnnotation(String contentLine) {
        boolean isAnnotation = false;

        String[] annotationArray = new String[] { "<!--" , "-->", "//-->", "//", "<%", "%>" };
        for (String annotation : annotationArray) {
            if (contentLine.contains(annotation)) {
                isAnnotation = true;
                break;
            }
        }
        return isAnnotation;
    }

    private boolean isInStrutsTag(String contentLine, String targetStr) {
        boolean isInStrutsTag = false;
        Pattern pattern = Pattern.compile("<s:.*((?!<>).)*[\\\"\\']>");
        Matcher matcher = pattern.matcher(contentLine);

        if (matcher.find()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                String groupStr = matcher.group(i);
                if (groupStr.contains(targetStr)) {
                    isInStrutsTag = true;
                    break;
                }
            }
        }

        return isInStrutsTag;
    }

   private boolean isBrokenStr(String contentLine, String targetStr) {
        int targetIndex = contentLine.indexOf(targetStr);

        String preStr;
        try {
            preStr = String.valueOf(contentLine.charAt(targetIndex - 1));
        } catch (Exception e) {
            preStr = "";
        }
        boolean preIsChinese = isChinese(preStr);
        if (preIsChinese) {
            return true;
        }

        String afterStr;
        try {
            afterStr = String.valueOf(contentLine.charAt(targetIndex + targetStr.length()));
        } catch (Exception e) {
            afterStr = "";
        }
        boolean afterIsChinese = isChinese(afterStr);

       if (afterIsChinese) {
           return true;
       }

        return false;
    }

    private boolean isChinese(String str) {
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    private List<ReplaceTarget> transLocale(String localeContent) {
        List<ReplaceTarget> replaceTargets = new ArrayList<>();

        String[] localeContents = localeContent.split("#");
        for (String str: localeContents) {
            if (!str.contains("=")) {
                continue;
            }
            String replaceTargetStr = str.substring(0, str.indexOf("\n")).trim();
            String replaceKey = str.substring(str.indexOf("\n"), str.indexOf("=")).trim();

            ReplaceTarget replaceTarget = new ReplaceTarget();
            replaceTarget.setTargetStr(replaceTargetStr);
            replaceTarget.setKey(replaceKey);
            replaceTargets.add(replaceTarget);
        }

        return replaceTargets.stream().sorted((o1, o2) -> o2.getTargetStr().length() - o1.getTargetStr().length()).collect(Collectors.toList());
    }

    private String getJspDirPath(String path) {
        File file = new File(path);
        if (file.getParent().endsWith("jsp")) {
            return file.getParent();
        }

        return getJspDirPath(file.getParent());
    }

    @Override
    public List<String> getFileLists(TransResult transResult) {
        if (new File(transResult.getFilePath()).isDirectory()) {
            return getFileLists(transResult.getFilePath());
        }
        return null;
    }

    private List<String> getFileLists(String filePath) {
        try {
            List<String> fileLists = new ArrayList<>();
            File file = new File(filePath);
            File[] files = file.listFiles();
            for (File childFile : files) {
                fileLists.add(childFile.getAbsolutePath());
            }
            return fileLists;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean writeFile(String filePath, String content, boolean append) {
        try {
            FileUtils.writeStringToFile(getFile(filePath), content, StandardCharsets.UTF_8, append);
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

    private String transToUnicode(String str) {
        return StringEscapeUtils.escapeJava(str);
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
            return null;
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
