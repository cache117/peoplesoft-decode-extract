using System;
using System.IO;
using System.Windows.Forms;

namespace GideonTaylor.PeopleSoft.WebUxDatExtractor
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            if (args.Length > 0)
            {
                try
                {
                    string input = null; string output = null;
                    for (int i = 0; i < args.Length; i++)
                    {
                        if ((args[i] == "--input" || args[i] == "-i") && i + 1 < args.Length) input = args[++i];
                        else if ((args[i] == "--output" || args[i] == "-o") && i + 1 < args.Length) output = args[++i];
                    }
                    if (input == null || output == null)
                    {
                        Console.Error.WriteLine("Usage: WebUxDatExtractor.exe --input file.dat --output folder");
                        return 2;
                    }
                    ExtractionResult result = new WebUxExtractor().Extract(input, output);
                    Console.WriteLine("Extracted {0} files ({1} JSON, {2} JavaScript, {3} HTML, {4} SQL).",
                        result.FileCount, result.JsonFiles, result.JavaScriptFiles, result.HtmlFiles, result.SqlFiles);
                    Console.WriteLine("Manifest: " + result.ManifestPath);
                    return 0;
                }
                catch (Exception ex) { Console.Error.WriteLine(ex.Message); return 1; }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
            return 0;
        }
    }
}
