package com.gideontaylor.peoplesoft.extractor.core;

import java.nio.file.Path;

record TextAsset(Path relativePath, String content, Kind kind) {
    enum Kind { PEOPLECODE, SQL, CONTENT }
}
