using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Windows.Forms;

namespace GideonTaylor.PeopleSoft.WebUxDatExtractor
{
    public sealed class MainForm : Form
    {
        private readonly TextBox input = new TextBox();
        private readonly TextBox output = new TextBox();
        private readonly Button extract = new Button();
        private readonly Label status = new Label();
        private string automaticallySuggestedOutput;

        public MainForm()
        {
            Text = "PeopleSoft WebUX DAT Extractor";
            ClientSize = new Size(760, 250);
            MinimumSize = new Size(650, 285);
            StartPosition = FormStartPosition.CenterScreen;
            Font = new Font("Segoe UI", 9F);

            var heading = new Label { Text = "Extract WebUX files from a PeopleSoft Data Mover export", AutoSize = true, Font = new Font(Font, FontStyle.Bold), Location = new Point(18, 18) };
            var help = new Label { Text = "Uses IS_OJ_OBJ_DTL to identify JSON, JavaScript, and HTML stored in IS_OJ_CFG_DTL.", AutoSize = true, Location = new Point(18, 45) };
            var inputLabel = new Label { Text = "Data Mover file (.dat)", AutoSize = true, Location = new Point(18, 82) };
            input.SetBounds(18, 103, 620, 25); input.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            var inputBrowse = new Button { Text = "Browse…" }; inputBrowse.SetBounds(646, 101, 92, 28); inputBrowse.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            var outputLabel = new Label { Text = "Output folder", AutoSize = true, Location = new Point(18, 139) };
            output.SetBounds(18, 160, 620, 25); output.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            var outputBrowse = new Button { Text = "Browse…" }; outputBrowse.SetBounds(646, 158, 92, 28); outputBrowse.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            extract.Text = "Extract"; extract.SetBounds(646, 204, 92, 30); extract.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            status.Text = "Ready."; status.SetBounds(18, 207, 610, 38); status.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;

            Controls.AddRange(new Control[] { heading, help, inputLabel, input, inputBrowse, outputLabel, output, outputBrowse, extract, status });
            inputBrowse.Click += BrowseInput;
            outputBrowse.Click += BrowseOutput;
            extract.Click += ExtractFiles;
            input.TextChanged += InputChanged;

            UserPreferences preferences = UserPreferences.Load();
            input.Text = preferences.InputPath ?? "";
            output.Text = preferences.OutputPath ?? "";
            automaticallySuggestedOutput = SuggestedOutput(input.Text);
        }

        private void BrowseInput(object sender, EventArgs e)
        {
            using (var dialog = new OpenFileDialog())
            {
                dialog.Title = "Choose PeopleSoft Data Mover DAT file";
                dialog.Filter = "Data Mover files (*.dat)|*.dat|All files (*.*)|*.*";
                dialog.CheckFileExists = true;
                if (File.Exists(input.Text)) { dialog.InitialDirectory = Path.GetDirectoryName(input.Text); dialog.FileName = Path.GetFileName(input.Text); }
                if (dialog.ShowDialog(this) == DialogResult.OK) input.Text = dialog.FileName;
            }
        }

        private void BrowseOutput(object sender, EventArgs e)
        {
            using (var dialog = new FolderBrowserDialog())
            {
                dialog.Description = "Choose where extracted WebUX files should be saved";
                dialog.SelectedPath = Directory.Exists(output.Text) ? output.Text : (File.Exists(input.Text) ? Path.GetDirectoryName(input.Text) : "");
                if (dialog.ShowDialog(this) == DialogResult.OK) { output.Text = dialog.SelectedPath; automaticallySuggestedOutput = null; }
            }
        }

        private void InputChanged(object sender, EventArgs e)
        {
            string suggestion = SuggestedOutput(input.Text);
            if (string.IsNullOrWhiteSpace(output.Text) || SamePath(output.Text, automaticallySuggestedOutput))
            {
                output.Text = suggestion;
                automaticallySuggestedOutput = suggestion;
            }
        }

        private void ExtractFiles(object sender, EventArgs e)
        {
            extract.Enabled = false; status.Text = "Reading and extracting…";
            try
            {
                string inputPath = input.Text; string outputPath = output.Text;
                ExtractionResult result = new WebUxExtractor().Extract(inputPath, outputPath);
                new UserPreferences { InputPath = inputPath, OutputPath = outputPath }.Save();
                status.Text = string.Format("Done: {0} files — {1} JSON, {2} JavaScript, {3} HTML, {4} SQL. Skipped {5} duplicate rows.",
                    result.FileCount, result.JsonFiles, result.JavaScriptFiles, result.HtmlFiles, result.SqlFiles, result.DuplicateRowsSkipped);
                if (MessageBox.Show(this, status.Text + "\r\n\r\nOpen the output folder?", "Extraction complete", MessageBoxButtons.YesNo, MessageBoxIcon.Information) == DialogResult.Yes)
                    Process.Start("explorer.exe", outputPath);
            }
            catch (Exception ex) { status.Text = "Extraction failed."; MessageBox.Show(this, ex.Message, "Extraction failed", MessageBoxButtons.OK, MessageBoxIcon.Error); }
            finally { extract.Enabled = true; }
        }

        private static string SuggestedOutput(string path)
        {
            if (string.IsNullOrWhiteSpace(path)) return "";
            try { return Path.Combine(Path.GetDirectoryName(path), Path.GetFileNameWithoutExtension(path) + "-extracted"); }
            catch { return ""; }
        }

        private static bool SamePath(string left, string right)
        {
            if (string.IsNullOrWhiteSpace(left) || string.IsNullOrWhiteSpace(right)) return false;
            try { return string.Equals(Path.GetFullPath(left).TrimEnd('\\'), Path.GetFullPath(right).TrimEnd('\\'), StringComparison.OrdinalIgnoreCase); }
            catch { return string.Equals(left, right, StringComparison.OrdinalIgnoreCase); }
        }
    }
}
