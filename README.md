# PeopleSoft Decode & Extract

PeopleSoft Decode & Extract turns text-bearing PeopleSoft exports into ordinary source files that can be searched, compared, reviewed, and stored in version control.

This repository contains two focused desktop and command-line tools:

| Tool | Input | Output |
| --- | --- | --- |
| [Project XML Extractor](apps/xml-extractor) | Application Designer project XML | PeopleCode, SQL, XSLT, HTML, JavaScript, CSS, and other identifiable text |
| [WebUX DAT Extractor](apps/webux-dat-extractor) | Data Mover `.dat` | WebUX JSON, JavaScript, HTML, and SQL definitions |

Both applications provide a Windows GUI for choosing the input and output paths. They can also be called from scripts.

## Status

The extractors are usable and have been tested against real project XML and Data Mover exports. The repository is currently pre-1.0 while packaging, automated tests, and additional PeopleSoft object coverage are refined.

## Building

On Windows, run:

```powershell
.\build-all.ps1
```

The XML extractor requires JDK 25 to build and deliberately targets Java 17 bytecode. The DAT extractor currently builds with Visual Studio Build Tools and targets .NET Framework 4.0. GitHub Actions builds both applications on every change.

Detailed usage and build instructions are in each application's README.

## Downloads

Ready-to-use packages are available from the [latest release](https://github.com/cache117/peoplesoft-decode-extract/releases/latest):

- The XML extractor runs on Windows, macOS, or Linux with Java 17 or later. Windows users can double-click `run-gui.bat`.
- The WebUX DAT extractor runs on Windows with .NET Framework 4.x. Double-click `run.cmd` to launch it.

Both downloads include their own usage instructions. Development builds are also available from the continuous-integration workflow.

## Install on Windows

The downloads are portable applications, so they do not use a traditional installer:

1. Download the appropriate ZIP from the latest release and select **Extract All**. Do not run the application from inside the ZIP.
2. Move the extracted folder to a permanent location, such as `%LOCALAPPDATA%\Programs\PeopleSoft Decode & Extract\XML Extractor` or `%LOCALAPPDATA%\Programs\PeopleSoft Decode & Extract\DAT Extractor`.
3. Create a desktop shortcut to `run-gui.bat` for the XML extractor or `run.cmd` for the DAT extractor.
4. Press **Windows+R**, enter `shell:programs`, and press Enter.
5. Create a **PeopleSoft Decode & Extract** folder there and move the shortcut into it.

The application will then appear under **Start > All apps > PeopleSoft Decode & Extract**. Right-click the Start Menu entry and select **Pin to Start** if desired. Install the shortcut before moving or renaming the extracted application folder, because the shortcut points to that location.

## Project history

This project did not begin from scratch. Its PeopleCode extraction lineage starts with the original [Decode PeopleCode project on SourceForge](https://sourceforge.net/projects/decodepcode/). That work was later adapted and extended in [cache117/decode-pcode](https://github.com/cache117/decode-pcode), the immediate predecessor to this repository.

PeopleSoft Decode & Extract continues that idea with a deliberately narrower focus: turn text-bearing Application Designer project XML and Data Mover DAT exports into ordinary source files. The original SourceForge project and the earlier GitHub adaptation deserve credit for establishing the foundation and direction that led to this tool.

## License

The original code in this repository is available under the [MIT License](LICENSE). Bundled third-party components retain their own licenses; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

PeopleSoft is a trademark of Oracle. This project is an independent utility and is not affiliated with or endorsed by Oracle.
