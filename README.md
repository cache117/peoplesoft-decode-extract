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

## License

The original code in this repository is available under the [MIT License](LICENSE). Bundled third-party components retain their own licenses; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

PeopleSoft is a trademark of Oracle. This project is an independent utility and is not affiliated with or endorsed by Oracle.
