package com.gideontaylor.peoplesoft.extractor.core;

import java.nio.file.Path;

record TextAsset(String identity, Path relativePath, String content, Kind kind) {
    enum Kind { PEOPLECODE, SQL, CONTENT }
}
