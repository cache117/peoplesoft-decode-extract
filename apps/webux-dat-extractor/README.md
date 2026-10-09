# PeopleSoft WebUX DAT Extractor

A small Windows application that extracts text-based WebUX configuration from a PeopleSoft Data Mover `.dat` file.

It reads `IS_OJ_OBJ_DTL` as the authoritative mapping and writes content from `IS_OJ_CFG_DTL` as:

- `JSON/<configuration-id>.json` for `IS_OJ_CFG_OPTIONS`
- `JavaScript/<configuration-id>.js` for `IS_OJ_CFG_MODEL`
- `HTML/<configuration-id>.html` for `IS_OJ_CFG_VIEW`
- `SQL/<sql-id>.sql` from `IS_CO_SQL_DEFN.IS_CO_SQL_TEXT`
- `extraction-manifest.json` with source and mapping details

Exact duplicates caused by repeated Data Mover export blocks are written once. Different content that resolves to the same name is preserved with `.2`, `.3`, and so on.

## Run it

Double-click `run.cmd`. The app uses standard Windows file and folder dialogs and remembers the last paths. When the input changes, an automatically suggested output path changes with it; a folder chosen manually is left alone.

Command-line use is also supported:

```text
WebUxDatExtractor.exe --input export.dat --output export-extracted
```

When running from a source checkout instead of a downloaded release, the executable is under `src\WebUxDatExtractor\bin\Release` after building.

## Install on the Windows Start Menu

1. Select **Extract All** on the downloaded ZIP. Do not run the application from inside the ZIP.
2. Move the extracted folder to a permanent location, such as `%LOCALAPPDATA%\Programs\PeopleSoft Decode & Extract\DAT Extractor`.
3. Right-click `run.cmd` and select **Show more options > Send to > Desktop (create shortcut)**.
4. Press **Windows+R**, enter `shell:programs`, and press Enter.
5. Create a **PeopleSoft Decode & Extract** folder there and move the new shortcut into it.

The DAT extractor will then appear under **Start > All apps > PeopleSoft Decode & Extract**. You can right-click it there and select **Pin to Start**. Do not move or rename the extracted application folder afterward unless you also recreate the shortcut.

## Build

Run `build.cmd`, or open `WebUxDatExtractor.sln` in Visual Studio. The projects target .NET Framework 4.0 so they build with the tools currently installed on this machine and run on later .NET Framework 4.x installations.

## DMSLib dependency

The DAT parser is a pinned, vendored copy of [Gideon-Taylor/DMSLib](https://github.com/Gideon-Taylor/DMSLib). `DMSLib.Reader.csproj` source-links the upstream reader files and intentionally omits the SQLite converter used by DMS Viewer. The upstream sources are not edited; `HashCodeCompat.cs` supplies one newer framework API when targeting .NET Framework 4.0.

See `vendor/DMSLib-UPSTREAM.md` and the included upstream license for version and attribution.
