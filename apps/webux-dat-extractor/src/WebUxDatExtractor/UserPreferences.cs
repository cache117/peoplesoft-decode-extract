using System;
using System.IO;

namespace GideonTaylor.PeopleSoft.WebUxDatExtractor
{
    internal sealed class UserPreferences
    {
        public string InputPath { get; set; }
        public string OutputPath { get; set; }

        private static string SettingsPath
        {
            get
            {
                string folder = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "GideonTaylor", "WebUxDatExtractor");
                return Path.Combine(folder, "settings.txt");
            }
        }

        public static UserPreferences Load()
        {
            var result = new UserPreferences();
            try
            {
                if (!File.Exists(SettingsPath)) return result;
                string[] lines = File.ReadAllLines(SettingsPath);
                if (lines.Length > 0) result.InputPath = lines[0];
                if (lines.Length > 1) result.OutputPath = lines[1];
            }
            catch { }
            return result;
        }

        public void Save()
        {
            string folder = Path.GetDirectoryName(SettingsPath);
            Directory.CreateDirectory(folder);
            File.WriteAllLines(SettingsPath, new[] { InputPath ?? "", OutputPath ?? "" });
        }
    }
}
