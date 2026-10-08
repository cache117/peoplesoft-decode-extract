using DMSLib;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Web.Script.Serialization;

namespace GideonTaylor.PeopleSoft.WebUxDatExtractor
{
    public sealed class WebUxExtractor
    {
        private const string ConfigTable = "IS_OJ_CFG_DTL";
        private const string ObjectTable = "IS_OJ_OBJ_DTL";
        private const string SqlTable = "IS_CO_SQL_DEFN";

        public ExtractionResult Extract(string datPath, string outputRoot)
        {
            if (string.IsNullOrWhiteSpace(datPath)) throw new ArgumentException("Choose a DAT file.", "datPath");
            if (!File.Exists(datPath)) throw new FileNotFoundException("DAT file not found.", datPath);
            if (!string.Equals(Path.GetExtension(datPath), ".dat", StringComparison.OrdinalIgnoreCase))
                throw new ArgumentException("The input must be a .dat file.", "datPath");
            if (string.IsNullOrWhiteSpace(outputRoot)) throw new ArgumentException("Choose an output folder.", "outputRoot");

            Directory.CreateDirectory(outputRoot);
            DMSFile file = DMSReader.Read(datPath);
            List<Mapping> mappings = ReadMappings(file);
            Dictionary<string, List<Mapping>> byConfig = mappings
                .GroupBy(m => m.ConfigurationId, StringComparer.OrdinalIgnoreCase)
                .ToDictionary(g => g.Key, g => g.ToList(), StringComparer.OrdinalIgnoreCase);

            var result = new ExtractionResult();
            var contentKeys = new HashSet<string>(StringComparer.Ordinal);
            var reservedPaths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

            foreach (DMSTable table in file.Tables.Where(t => Same(t.Name, ConfigTable)))
            {
                int idIndex = ColumnIndex(table, "IS_OJ_CFG_ID");
                int dateIndex = ColumnIndex(table, "EFFDT");
                int statusIndex = ColumnIndex(table, "EFF_STATUS");
                int textIndex = ColumnIndex(table, "IS_OJ_CFG_TEXT");

                foreach (DMSRow row in table.Rows)
                {
                    string id = Clean(row.GetStringValue(idIndex));
                    string content = row.GetStringValue(textIndex).TrimEnd('\0');
                    List<Mapping> matches;
                    if (!byConfig.TryGetValue(id, out matches))
                    {
                        result.UnmappedRows++;
                        continue;
                    }

                    foreach (IGrouping<string, Mapping> roleMappings in matches.GroupBy(m => m.Role, StringComparer.OrdinalIgnoreCase))
                    {
                        Mapping mapping = roleMappings.First();
                        string duplicateKey = mapping.Role + "\n" + id + "\n" + content;
                        if (!contentKeys.Add(duplicateKey))
                        {
                            result.DuplicateRowsSkipped++;
                            continue;
                        }

                        string folder = FolderFor(mapping.Role);
                        string extension = ExtensionFor(mapping.Role);
                        string relative = Path.Combine(folder, SafeFileName(id) + extension);
                        relative = MakeUnique(relative, reservedPaths);
                        string destination = Path.Combine(outputRoot, relative);
                        Directory.CreateDirectory(Path.GetDirectoryName(destination));
                        File.WriteAllText(destination, content, new UTF8Encoding(false));

                        if (mapping.Role == "JSON") result.JsonFiles++;
                        else if (mapping.Role == "JavaScript") result.JavaScriptFiles++;
                        else result.HtmlFiles++;

                        result.Entries.Add(new ManifestEntry {
                            ConfigurationId = id,
                            ObjectIds = roleMappings.Select(m => m.ObjectId).Distinct(StringComparer.OrdinalIgnoreCase).OrderBy(v => v).ToList(),
                            Role = mapping.Role,
                            AppClasses = roleMappings.Select(m => m.AppClass).Where(v => v.Length > 0).Distinct(StringComparer.OrdinalIgnoreCase).OrderBy(v => v).ToList(),
                            EffectiveDate = Clean(row.GetStringValue(dateIndex)),
                            EffectiveStatus = Clean(row.GetStringValue(statusIndex)), OutputFile = relative.Replace('\\', '/'),
                            SourceWhereClause = table.WhereClause
                        });
                    }
                }
            }

            ExtractSql(file, outputRoot, result, contentKeys, reservedPaths);

            var manifest = new ManifestDocument {
                SourceFile = Path.GetFullPath(datPath), GeneratedAtUtc = DateTime.UtcNow.ToString("o"),
                JsonFiles = result.JsonFiles, JavaScriptFiles = result.JavaScriptFiles, HtmlFiles = result.HtmlFiles,
                SqlFiles = result.SqlFiles,
                DuplicateRowsSkipped = result.DuplicateRowsSkipped, UnmappedRows = result.UnmappedRows,
                Files = result.Entries
            };
            result.ManifestPath = Path.Combine(outputRoot, "extraction-manifest.json");
            var serializer = new JavaScriptSerializer { MaxJsonLength = int.MaxValue };
            File.WriteAllText(result.ManifestPath, PrettyJson(serializer.Serialize(manifest)), new UTF8Encoding(false));
            return result;
        }

        private static void ExtractSql(DMSFile file, string outputRoot, ExtractionResult result,
            HashSet<string> contentKeys, HashSet<string> reservedPaths)
        {
            foreach (DMSTable table in file.Tables.Where(t => Same(t.Name, SqlTable)))
            {
                int idIndex = ColumnIndex(table, "IS_CO_SQL_ID");
                int dateIndex = ColumnIndex(table, "EFFDT");
                int statusIndex = ColumnIndex(table, "EFF_STATUS");
                int descriptionIndex = OptionalColumnIndex(table, "DESCR");
                int productIndex = OptionalColumnIndex(table, "IS_CO_PRODUCT");
                int textIndex = ColumnIndex(table, "IS_CO_SQL_TEXT");

                foreach (DMSRow row in table.Rows)
                {
                    string id = Clean(row.GetStringValue(idIndex));
                    string content = row.GetStringValue(textIndex).TrimEnd('\0');
                    string duplicateKey = "SQL\n" + id + "\n" + content;
                    if (!contentKeys.Add(duplicateKey))
                    {
                        result.DuplicateRowsSkipped++;
                        continue;
                    }

                    string relative = MakeUnique(Path.Combine("SQL", SafeFileName(id) + ".sql"), reservedPaths);
                    string destination = Path.Combine(outputRoot, relative);
                    Directory.CreateDirectory(Path.GetDirectoryName(destination));
                    File.WriteAllText(destination, content, new UTF8Encoding(false));
                    result.SqlFiles++;

                    result.Entries.Add(new ManifestEntry {
                        SqlId = id,
                        ObjectIds = new List<string>(),
                        Role = "SQL",
                        AppClasses = new List<string>(),
                        EffectiveDate = Clean(row.GetStringValue(dateIndex)),
                        EffectiveStatus = Clean(row.GetStringValue(statusIndex)),
                        Description = descriptionIndex < 0 ? null : Clean(row.GetStringValue(descriptionIndex)),
                        Product = productIndex < 0 ? null : Clean(row.GetStringValue(productIndex)),
                        OutputFile = relative.Replace('\\', '/'),
                        SourceWhereClause = table.WhereClause
                    });
                }
            }
        }

        private static List<Mapping> ReadMappings(DMSFile file)
        {
            var mappings = new List<Mapping>();
            var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (DMSTable table in file.Tables.Where(t => Same(t.Name, ObjectTable)))
            {
                int objectIndex = ColumnIndex(table, "IS_OJ_OBJ_ID");
                int appClassIndex = ColumnIndex(table, "IS_CO_AP_PKGPTHCLS");
                AddRoleMappings(table, mappings, seen, objectIndex, appClassIndex, "IS_OJ_CFG_OPTIONS", "JSON");
                AddRoleMappings(table, mappings, seen, objectIndex, appClassIndex, "IS_OJ_CFG_MODEL", "JavaScript");
                AddRoleMappings(table, mappings, seen, objectIndex, appClassIndex, "IS_OJ_CFG_VIEW", "HTML");
            }
            return mappings;
        }

        private static void AddRoleMappings(DMSTable table, List<Mapping> mappings, HashSet<string> seen,
            int objectIndex, int appClassIndex, string column, string role)
        {
            int configIndex = ColumnIndex(table, column);
            foreach (DMSRow row in table.Rows)
            {
                string config = Clean(row.GetStringValue(configIndex));
                if (config.Length == 0) continue;
                string objectId = Clean(row.GetStringValue(objectIndex));
                string key = role + "|" + config + "|" + objectId;
                if (seen.Add(key)) mappings.Add(new Mapping {
                    ConfigurationId = config, ObjectId = objectId, Role = role,
                    AppClass = Clean(row.GetStringValue(appClassIndex))
                });
            }
        }

        private static int ColumnIndex(DMSTable table, string name)
        {
            for (int i = 0; i < table.Columns.Count; i++) if (Same(table.Columns[i].Name, name)) return i;
            throw new FormatException("Table " + table.Name + " does not contain expected column " + name + ".");
        }

        private static int OptionalColumnIndex(DMSTable table, string name)
        {
            for (int i = 0; i < table.Columns.Count; i++) if (Same(table.Columns[i].Name, name)) return i;
            return -1;
        }

        private static string MakeUnique(string relative, HashSet<string> reserved)
        {
            if (reserved.Add(relative)) return relative;
            string directory = Path.GetDirectoryName(relative);
            string stem = Path.GetFileNameWithoutExtension(relative);
            string extension = Path.GetExtension(relative);
            int number = 2;
            string candidate;
            do { candidate = Path.Combine(directory, stem + "." + number++ + extension); } while (!reserved.Add(candidate));
            return candidate;
        }

        private static string SafeFileName(string value)
        {
            foreach (char c in Path.GetInvalidFileNameChars()) value = value.Replace(c, '_');
            return string.IsNullOrWhiteSpace(value) ? "unnamed" : value;
        }

        private static string Clean(string value) { return (value ?? "").Trim('\0', ' ', '\r', '\n', '\t'); }
        private static bool Same(string left, string right) { return string.Equals(left, right, StringComparison.OrdinalIgnoreCase); }
        private static string FolderFor(string role) { return role == "JavaScript" ? "JavaScript" : role; }
        private static string ExtensionFor(string role) { return role == "JSON" ? ".json" : role == "JavaScript" ? ".js" : ".html"; }

        private static string PrettyJson(string json)
        {
            var output = new StringBuilder(); int depth = 0; bool quoted = false; bool escaped = false;
            foreach (char c in json)
            {
                if (quoted) { output.Append(c); if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; continue; }
                if (c == '"') { quoted = true; output.Append(c); }
                else if (c == '{' || c == '[') { output.Append(c).AppendLine(); depth++; output.Append(new string(' ', depth * 2)); }
                else if (c == '}' || c == ']') { output.AppendLine(); depth--; output.Append(new string(' ', depth * 2)).Append(c); }
                else if (c == ',') { output.Append(c).AppendLine().Append(new string(' ', depth * 2)); }
                else if (c == ':') output.Append(": ");
                else if (!char.IsWhiteSpace(c)) output.Append(c);
            }
            return output.ToString();
        }

        private sealed class Mapping { public string ConfigurationId; public string ObjectId; public string Role; public string AppClass; }
    }
}
