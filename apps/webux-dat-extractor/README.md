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
src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe --input export.dat --output export-extracted
```

## Build

Run `build.cmd`, or open `WebUxDatExtractor.sln` in Visual Studio. The projects target .NET Framework 4.0 so they build with the tools currently installed on this machine and run on later .NET Framework 4.x installations.

## DMSLib dependency

The DAT parser is a pinned, vendored copy of [Gideon-Taylor/DMSLib](https://github.com/Gideon-Taylor/DMSLib). `DMSLib.Reader.csproj` source-links the upstream reader files and intentionally omits the SQLite converter used by DMS Viewer. The upstream sources are not edited; `HashCodeCompat.cs` supplies one newer framework API when targeting .NET Framework 4.0.

See `vendor/DMSLib-UPSTREAM.md` and the included upstream license for version and attribution.
