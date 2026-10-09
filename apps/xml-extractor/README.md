# PeopleSoft XML Extractor

A focused desktop and command-line application that extracts text-based source artifacts from a PeopleSoft Application Designer project XML export.

It has no database, Git, SVN, or PeopleCode bytecode-decoding dependencies.

## Extracted artifacts

- PeopleCode (`.pcode`)
- SQL definitions (`.sql`)
- XSLT definitions (`.xsl`)
- HTML content (`.html`)
- JavaScript content (`.js`)
- CSS stylesheets (`.css`)
- Text content whose format can be identified from its name, declared format, or contents

Duplicate stylesheet payloads represented by both CRM and SSM instances are written once. Binary image content is intentionally skipped.

Web assets use a flat layout such as `HTML/DEFINITION.html`, `HTML/DEFINITION.js`, and `StyleSheet/DEFINITION.css`. Non-English or alternate content variants receive a filename suffix to prevent data loss.

PeopleSoft HTML definitions are emitted only as HTML or JavaScript. CSS embedded in an HTML definition remains part of the `.html` file; only stylesheet definitions are emitted as `.css`.

Every run also creates `extraction-manifest.json`, including extracted files and XML instance classes that did not contain recognized source text.

On Windows, the GUI uses the native Explorer file and folder dialogs. Other platforms fall back to the Java chooser.

The suggested output folder follows the selected XML until you choose or type a custom output location. That preference is remembered between launches.

## Run

Double-click `run-gui.bat`, or run:

```text
java -jar peoplesoft-xml-extractor.jar
```

Command line:

```text
java -jar peoplesoft-xml-extractor.jar --input project.xml --output output-folder
```

## Install on the Windows Start Menu

1. Select **Extract All** on the downloaded ZIP. Do not run the application from inside the ZIP.
2. Move the extracted folder to a permanent location, such as `%LOCALAPPDATA%\Programs\PeopleSoft Decode & Extract\XML Extractor`.
3. Right-click `run-gui.bat` and select **Show more options > Send to > Desktop (create shortcut)**.
4. Press **Windows+R**, enter `shell:programs`, and press Enter.
5. Create a **PeopleSoft Decode & Extract** folder there and move the new shortcut into it.

The XML extractor will then appear under **Start > All apps > PeopleSoft Decode & Extract**. You can right-click it there and select **Pin to Start**. Do not move or rename the extracted application folder afterward unless you also recreate the shortcut.

## Build

Run `build.bat` or `build.ps1`. A current JDK is required; JDK 25 LTS is recommended. The source deliberately remains compatible with Java 17 so the unbundled JAR works on a wider range of existing machines.
