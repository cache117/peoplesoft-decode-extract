package com.gideontaylor.peoplesoft.extractor.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ExtractionResult(Path input, Path output, List<Path> files, Map<String, Integer> instanceCounts,
        int peopleCodeCount, int sqlCount, int contentCount) {
    public String summary() {
        return "Extracted " + peopleCodeCount + " PeopleCode, " + sqlCount + " SQL/XSLT, and "
                + contentCount + " web/text asset(s) to " + output.toAbsolutePath();
    }
}
