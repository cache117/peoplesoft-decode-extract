package com.gideontaylor.peoplesoft.extractor.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class ProjectExtractor {
    private static final Pattern TRAILING_SPACES = Pattern.compile("\\s+$");
    private final Set<String> identities = new LinkedHashSet<>();
    private final List<Path> writtenFiles = new ArrayList<>();
    private final Map<String, Integer> instanceCounts = new LinkedHashMap<>();
    private int peopleCodeCount;
    private int sqlCount;
    private int contentCount;

    public ExtractionResult extract(Path input, Path output, Consumer<String> log) throws Exception {
        if (!Files.isRegularFile(input)) throw new IOException("Project XML does not exist: " + input);
        Files.createDirectories(output);
        identities.clear();
        writtenFiles.clear();
        instanceCounts.clear();
        peopleCodeCount = sqlCount = contentCount = 0;

        log.accept("Reading " + input.toAbsolutePath());
        DocumentBuilder builder = newDocumentBuilder();
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            StringBuilder instance = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (instance == null && line.contains("<instance")) instance = new StringBuilder();
                if (instance != null) {
                    instance.append(line).append('\n');
                    if (line.contains("</instance>")) {
                        processInstance(builder.parse(new InputSource(new StringReader(instance.toString()))), output, log);
                        instance = null;
                    }
                }
            }
            if (instance != null) throw new IOException("The final <instance> in the project XML is incomplete.");
        }

        writeManifest(input, output);
        return new ExtractionResult(input, output, List.copyOf(writtenFiles), Map.copyOf(instanceCounts),
                peopleCodeCount, sqlCount, contentCount);
    }

    private void processInstance(Document document, Path output, Consumer<String> log) throws IOException {
        Element root = document.getDocumentElement();
        String className = root.getAttribute("class");
        instanceCounts.merge(className.isBlank() ? "(unknown)" : className, 1, Integer::sum);

        String peopleCode = leafPayload(root, "peoplecode_text");
        if (peopleCode != null) write(assetForPeopleCode(root, peopleCode), output, log);

        String sql = leafPayload(root, "lpszSqlText");
        if (sql != null) write(assetForSql(root, sql), output, log);

        if ("CRM".equals(className)) {
            String content = leafPayload(root, "hContStrData");
            if (content != null) write(assetForContent(root, content), output, log);
        } else if ("SSM".equals(className)) {
            String css = leafPayload(root, "hExtStyleSheetStr");
            if (css != null) write(assetForStyleSheet(root, css), output, log);
        }
    }

    private void write(TextAsset asset, Path output, Consumer<String> log) throws IOException {
        if (asset == null || !identities.add(asset.identity())) return;
        Path target = output.resolve(asset.relativePath()).normalize();
        if (!target.startsWith(output.toAbsolutePath().normalize()) && output.isAbsolute()) {
            throw new IOException("Unsafe output path: " + target);
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, normalizeLines(asset.content()), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        writtenFiles.add(output.relativize(target));
        switch (asset.kind()) {
            case PEOPLECODE -> peopleCodeCount++;
            case SQL -> sqlCount++;
            case CONTENT -> contentCount++;
        }
        log.accept("Wrote " + target.toAbsolutePath());
    }

    private static TextAsset assetForPeopleCode(Element root, String content) {
        int[] ids = new int[7];
        String[] values = new String[7];
        for (int i = 0; i < 7; i++) {
            ids[i] = intValue(root, "eObjectID_" + i, 0);
            values[i] = text(root, "szObjectValue_" + i);
        }
        int objectType = peopleCodeType(ids);
        String category = peopleCodeCategory(objectType);
        int last = lastValue(values);
        if (last < 0) return null;
        int actualLast = objectType == 46 ? last - 2 : last - 1;
        actualLast = Math.max(0, actualLast);
        Path path = Path.of(category);
        for (int i = 0; i < actualLast; i++) {
            if (i > 0 && objectType == 43) break;
            if ("GBL".equals(values[i]) && objectType == 48) continue;
            path = path.resolve(safe(values[i]));
        }
        String fileName = String.join(" ", Arrays.copyOfRange(values, actualLast, last + 1))
                .replace(" OnExecute", "");
        path = path.resolve(safe(fileName) + ".pcode");
        return new TextAsset("pc:" + path, path, content, TextAsset.Kind.PEOPLECODE);
    }

    private static TextAsset assetForSql(Element root, String content) {
        String name = defaultValue(text(root, "szSqlId"), "unnamed-sql").trim();
        int type = intValue(root, "szSqlType", 2);
        String folder = switch (type) {
            case 0 -> "SQL_0";
            case 1 -> "SQL_AE";
            case 6 -> "XSLT";
            case 2 -> "SQL";
            default -> "SQL" + type;
        };
        String extension = type == 6 ? "xsl" : "sql";
        Path path = Path.of(folder, safe(name), safe(name) + "." + extension);
        return new TextAsset("sql:" + path, path, content, TextAsset.Kind.SQL);
    }

    private static TextAsset assetForContent(Element root, String content) {
        String name = defaultValue(text(root, "szContName"), "unnamed-content").trim();
        int type = intValue(root, "eContType", 4);
        int alt = intValue(root, "nAltContNum", 1);
        String language = defaultValue(text(root, "szLanguageCd"), "ENG").trim();
        String extension = inferFormat(name, text(root, "szContFmt"), type, content);
        String folder = type == 9 ? "StyleSheet" : "HTML";
        Path path = Path.of(folder, safe(name) + variantSuffix(language, alt) + "." + extension);
        return new TextAsset("content:" + name + ':' + language + ':' + alt + ':' + extension,
                path, content, TextAsset.Kind.CONTENT);
    }

    private static TextAsset assetForStyleSheet(Element root, String content) {
        String name = defaultValue(text(root, "szStyleSheetName"), "unnamed-stylesheet").trim();
        String language = defaultValue(text(root, "szLanguageCd"), "ENG").trim();
        Path path = Path.of("StyleSheet", safe(name) + variantSuffix(language, 1) + ".css");
        return new TextAsset("content:" + name + ':' + language + ":1:css", path, content, TextAsset.Kind.CONTENT);
    }

    private static String variantSuffix(String language, int alternate) {
        StringBuilder suffix = new StringBuilder();
        if (!"ENG".equalsIgnoreCase(language)) suffix.append('.').append(safe(language));
        if (alternate != 1) suffix.append('.').append(alternate);
        return suffix.toString();
    }

    private void writeManifest(Path input, Path output) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"source\": \"").append(json(input.toAbsolutePath().toString())).append("\",\n");
        json.append("  \"summary\": { \"peopleCode\": ").append(peopleCodeCount)
                .append(", \"sqlOrXslt\": ").append(sqlCount).append(", \"webOrText\": ").append(contentCount).append(" },\n");
        json.append("  \"instanceClasses\": {");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : instanceCounts.entrySet()) {
            if (!first) json.append(',');
            json.append("\n    \"").append(json(entry.getKey())).append("\": ").append(entry.getValue());
            first = false;
        }
        if (!first) json.append('\n');
        json.append("  },\n  \"files\": [");
        for (int i = 0; i < writtenFiles.size(); i++) {
            if (i > 0) json.append(',');
            json.append("\n    \"").append(json(writtenFiles.get(i).toString().replace('\\', '/'))).append("\"");
        }
        if (!writtenFiles.isEmpty()) json.append('\n');
        json.append("  ]\n}\n");
        Files.writeString(output.resolve("extraction-manifest.json"), json, StandardCharsets.UTF_8);
    }

    private static DocumentBuilder newDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder();
    }

    private static String leafPayload(Element root, String tag) {
        NodeList nodes = root.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (hasElementChild(node)) continue;
            String value = node.getTextContent();
            String trimmed = value == null ? "" : value.trim();
            if (!trimmed.isEmpty() && !"HANDLE".equals(trimmed) && !"POINTER".equals(trimmed)) return value;
        }
        return null;
    }

    private static boolean hasElementChild(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) if (children.item(i).getNodeType() == Node.ELEMENT_NODE) return true;
        return false;
    }

    private static String text(Element root, String tag) {
        NodeList nodes = root.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private static int intValue(Element root, String tag, int fallback) {
        try { return Integer.parseInt(defaultValue(text(root, tag), Integer.toString(fallback)).trim()); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static int peopleCodeType(int[] ids) {
        if (ids[0] == 1 && ids[1] == 2 && ids[2] == 12 && ids[3] == 0) return 8;
        if (ids[0] == 3 && ids[1] == 4 && ids[2] == 5 && ids[3] == 12) return 9;
        if (ids[0] == 60 && ids[1] == 12 && ids[2] == 0 && ids[3] == 0) return 39;
        if (ids[0] == 60 && ids[1] == 87 && ids[2] == 12 && ids[3] == 0) return 40;
        if (ids[0] == 66 && ids[1] == 77 && ((ids[2] == 78 && ids[3] == 12) || (ids[2] == 39 && ids[3] == 20))) return 43;
        if (ids[0] == 74) return 42;
        if (ids[0] == 9 && ids[1] == 12 && ids[2] == 0 && ids[3] == 0) return 44;
        if (ids[0] == 10 && ids[1] == 39 && ids[2] == 12 && ids[3] == 0) return 46;
        if (ids[0] == 10 && ids[1] == 39 && ids[2] == 1 && ids[3] == 12) return 47;
        if (ids[0] == 10 && ids[1] == 39 && ids[2] == 1 && ids[3] == 2) return 48;
        if (ids[0] == 104) return 58;
        return -1;
    }

    private static String peopleCodeCategory(int type) {
        return switch (type) {
            case 8 -> "Record PeopleCode"; case 9 -> "Menu PeopleCode"; case 39 -> "Message PeopleCode";
            case 40 -> "Subscription PeopleCode"; case 42 -> "Component Interface PeopleCode";
            case 43 -> "Application Engine PeopleCode"; case 44 -> "Page PeopleCode";
            case 46 -> "Component PeopleCode"; case 47 -> "Component Record PeopleCode";
            case 48 -> "Component Record Field PeopleCode"; case 58 -> "Application Package PeopleCode";
            default -> "Unknown PeopleCode";
        };
    }

    private static String inferFormat(String name, String declared, int type, String content) {
        String format = defaultValue(declared, "").trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        if (type == 9) return "css";
        if (type == 4) return inferHtmlDefinitionFormat(name, format, content);
        if (format.contains("javascript")) return "js";
        if (format.endsWith("/html")) return "html";
        if (format.endsWith("/css")) return "css";
        if (format.endsWith("/xml")) return "xml";
        if (!format.isEmpty()) return format.replaceAll("[^a-z0-9]+", "");
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String ext : List.of("js", "css", "html", "htm", "xml", "xsl", "xslt", "json", "sql"))
            if (lowerName.endsWith("." + ext) || lowerName.endsWith("_" + ext) || lowerName.endsWith("-" + ext)) return ext;
        String lower = content.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("<?xml") || lower.startsWith("<xsl:")) return lower.contains("<xsl:") ? "xsl" : "xml";
        if (looksLikeHtml(lower)) return "html";
        if (looksLikeJavaScript(lower)) return "js";
        if (lower.contains("{") && lower.contains("}") && lower.contains(":")) return "css";
        if (lower.startsWith("<")) return "html";
        return type == 4 ? "html" : "txt";
    }

    private static String inferHtmlDefinitionFormat(String name, String declaredFormat, String content) {
        if (declaredFormat.contains("javascript") || "js".equals(declaredFormat)
                || declaredFormat.endsWith("/js")) return "js";

        String lowerName = name.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".js") || lowerName.endsWith("_js") || lowerName.endsWith("-js")) return "js";

        String lowerContent = content.trim().toLowerCase(Locale.ROOT);
        String markupCandidate = stripLeadingBlockComments(lowerContent);
        if (looksLikeHtml(markupCandidate) || markupCandidate.startsWith("<")) return "html";
        return looksLikeJavaScript(lowerContent) ? "js" : "html";
    }

    private static String stripLeadingBlockComments(String content) {
        String result = content.stripLeading();
        while (result.startsWith("/*")) {
            int end = result.indexOf("*/", 2);
            if (end < 0) break;
            result = result.substring(end + 2).stripLeading();
        }
        return result;
    }

    private static boolean looksLikeHtml(String content) {
        return content.startsWith("<!doctype html") || content.startsWith("<html") || content.startsWith("<head")
                || content.startsWith("<body") || content.startsWith("<script") || content.startsWith("<style")
                || content.startsWith("<div") || content.startsWith("<span") || content.startsWith("<table")
                || content.startsWith("<form") || content.startsWith("<template") || content.startsWith("<!--");
    }

    private static boolean looksLikeJavaScript(String content) {
        int signals = 0;
        if (content.contains("function ") || content.contains("function(")) signals++;
        if (content.contains("const ") || content.contains("let ") || content.contains("var ")) signals++;
        if (content.contains("window.") || content.contains("document.")) signals++;
        if (content.contains("addeventlistener(") || content.contains("queryselector(")) signals++;
        if (content.contains("=>")) signals++;
        if (content.contains("import ") || content.contains("export ")) signals++;
        if (content.contains("jquery(") || content.contains("$(")) signals++;
        if (content.startsWith("\"use strict\"") || content.startsWith("'use strict'")) signals++;
        return signals >= 1;
    }

    private static int lastValue(String[] values) {
        for (int i = values.length - 1; i >= 0; i--) if (values[i] != null && !values[i].trim().isEmpty()) return i;
        return -1;
    }

    private static String safe(String value) {
        String result = defaultValue(value, "unnamed").trim().replace("/", "_").replace("?", "_")
                .replace("*", "_").replace("<", "_lt_").replace(">", "_gt_").replace("\\", "__")
                .replace(":", "_").replace('"', '_').replace('|', '_');
        while (result.endsWith(".")) result = result.substring(0, result.length() - 1);
        return result.isBlank() ? "unnamed" : result;
    }

    private static String normalizeLines(String input) {
        StringBuilder out = new StringBuilder();
        input.lines().forEach(line -> out.append(TRAILING_SPACES.matcher(line).replaceFirst("")).append(System.lineSeparator()));
        return out.toString();
    }

    private static String defaultValue(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
}
