using System.Collections.Generic;

namespace GideonTaylor.PeopleSoft.WebUxDatExtractor
{
    public sealed class ExtractionResult
    {
        public int JsonFiles { get; set; }
        public int JavaScriptFiles { get; set; }
        public int HtmlFiles { get; set; }
        public int SqlFiles { get; set; }
        public int DuplicateRowsSkipped { get; set; }
        public int UnmappedRows { get; set; }
        public string ManifestPath { get; set; }
        public List<ManifestEntry> Entries { get; set; }

        public ExtractionResult() { Entries = new List<ManifestEntry>(); }
        public int FileCount { get { return JsonFiles + JavaScriptFiles + HtmlFiles + SqlFiles; } }
    }

    public sealed class ManifestDocument
    {
        public string SourceFile { get; set; }
        public string GeneratedAtUtc { get; set; }
        public int JsonFiles { get; set; }
        public int JavaScriptFiles { get; set; }
        public int HtmlFiles { get; set; }
        public int SqlFiles { get; set; }
        public int DuplicateRowsSkipped { get; set; }
        public int UnmappedRows { get; set; }
        public List<ManifestEntry> Files { get; set; }
    }

    public sealed class ManifestEntry
    {
        public string ConfigurationId { get; set; }
        public string SqlId { get; set; }
        public List<string> ObjectIds { get; set; }
        public string Role { get; set; }
        public List<string> AppClasses { get; set; }
        public string EffectiveDate { get; set; }
        public string EffectiveStatus { get; set; }
        public string Description { get; set; }
        public string Product { get; set; }
        public string OutputFile { get; set; }
        public string SourceWhereClause { get; set; }
    }
}
