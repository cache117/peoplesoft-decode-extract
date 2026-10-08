package com.gideontaylor.peoplesoft.extractor.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.gideontaylor.peoplesoft.extractor.core.ExtractionResult;
import com.gideontaylor.peoplesoft.extractor.core.ProjectExtractor;

public final class ExtractorFrame extends JFrame {
    private static final Preferences PREFS = Preferences.userNodeForPackage(ExtractorFrame.class);
    private final JTextField inputField = new JTextField(PREFS.get("input", ""), 48);
    private final JTextField outputField = new JTextField(PREFS.get("output", ""), 48);
    private final JTextArea log = new JTextArea();
    private final JButton extractButton = new JButton("Extract project");
    private boolean updatingOutputField;
    private boolean outputFollowsInput;

    public ExtractorFrame() {
        super("PeopleSoft XML Extractor");
        outputFollowsInput = PREFS.getBoolean("outputFollowsInput", outputMatchesSuggestedInput());
        outputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { outputWasEdited(); }
            @Override public void removeUpdate(DocumentEvent event) { outputWasEdited(); }
            @Override public void changedUpdate(DocumentEvent event) { outputWasEdited(); }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 520));
        setLocationByPlatform(true);
        add(buildContent());
        pack();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel choices = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridx = 0;
        choices.add(new JLabel("Project XML:"), c);
        c.gridx = 1;
        c.weightx = 1;
        choices.add(inputField, c);
        JButton inputButton = new JButton("Browse…");
        inputButton.addActionListener(event -> chooseInput());
        c.gridx = 2;
        c.weightx = 0;
        choices.add(inputButton, c);

        c.gridy = 1;
        c.gridx = 0;
        choices.add(new JLabel("Output root:"), c);
        c.gridx = 1;
        c.weightx = 1;
        choices.add(outputField, c);
        JButton outputButton = new JButton("Browse…");
        outputButton.addActionListener(event -> chooseOutput());
        c.gridx = 2;
        c.weightx = 0;
        choices.add(outputButton, c);
        content.add(choices, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(false);
        log.setText("Choose an Application Designer project XML file and an output folder.\n");
        content.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        extractButton.addActionListener(event -> extract());
        actions.add(extractButton);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    private void chooseInput() {
        if (isWindows()) {
            try {
                File file = WindowsNativeDialogs.chooseXmlFile(this, "Choose PeopleSoft project XML",
                        startingDirectory(inputField.getText()));
                if (file != null) applyInputSelection(file);
                return;
            } catch (RuntimeException | UnsatisfiedLinkError ex) {
                log.append("Native Windows picker unavailable; using Java fallback.\n");
            }
        }
        JFileChooser chooser = new JFileChooser(startingDirectory(inputField.getText()));
        chooser.setDialogTitle("Choose PeopleSoft project XML");
        chooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            applyInputSelection(chooser.getSelectedFile());
        }
    }

    private void applyInputSelection(File file) {
        boolean updateOutput = outputField.getText().isBlank() || outputFollowsInput || outputMatchesSuggestedInput();
        inputField.setText(file.getAbsolutePath());
        if (updateOutput) setOutputField(suggestedOutput(file), true);
    }

    private void chooseOutput() {
        if (isWindows()) {
            try {
                File folder = WindowsNativeDialogs.chooseFolder(this, "Choose output root",
                        startingDirectory(outputField.getText()));
                if (folder != null) setOutputField(folder.getAbsolutePath(), false);
                return;
            } catch (RuntimeException | UnsatisfiedLinkError ex) {
                log.append("Native Windows folder picker unavailable; using Java fallback.\n");
            }
        }
        JFileChooser chooser = new JFileChooser(startingDirectory(outputField.getText()));
        chooser.setDialogTitle("Choose output root");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            setOutputField(chooser.getSelectedFile().getAbsolutePath(), false);
        }
    }

    private void setOutputField(String value, boolean followsInput) {
        updatingOutputField = true;
        try {
            outputField.setText(value);
        } finally {
            updatingOutputField = false;
        }
        outputFollowsInput = followsInput;
    }

    private void outputWasEdited() {
        if (!updatingOutputField) outputFollowsInput = false;
    }

    private boolean outputMatchesSuggestedInput() {
        if (inputField.getText().isBlank() || outputField.getText().isBlank()) return false;
        try {
            return samePath(outputField.getText(), suggestedOutput(new File(inputField.getText())));
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private static String suggestedOutput(File input) {
        String name = input.getName().replaceFirst("(?i)\\.xml$", "");
        File parent = input.getAbsoluteFile().getParentFile();
        return new File(parent, name + "-extracted").getAbsolutePath();
    }

    private static boolean samePath(String first, String second) {
        String normalizedFirst = Path.of(first).toAbsolutePath().normalize().toString();
        String normalizedSecond = Path.of(second).toAbsolutePath().normalize().toString();
        return isWindows() ? normalizedFirst.equalsIgnoreCase(normalizedSecond)
                : normalizedFirst.equals(normalizedSecond);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }

    private File startingDirectory(String value) {
        if (value == null || value.isBlank()) return null;
        File file = new File(value);
        return file.isDirectory() ? file : file.getParentFile();
    }

    private void extract() {
        if (inputField.getText().isBlank() || outputField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Choose both the project XML and output root.", "Missing location",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path input = Path.of(inputField.getText().trim());
        Path output = Path.of(outputField.getText().trim());
        PREFS.put("input", input.toString());
        PREFS.put("output", output.toString());
        PREFS.putBoolean("outputFollowsInput", outputFollowsInput);
        extractButton.setEnabled(false);
        log.setText("");

        SwingWorker<ExtractionResult, String> worker = new SwingWorker<>() {
            @Override
            protected ExtractionResult doInBackground() throws Exception {
                return new ProjectExtractor().extract(input, output, this::publish);
            }

            @Override
            protected void process(List<String> messages) {
                messages.forEach(message -> log.append(message + System.lineSeparator()));
                log.setCaretPosition(log.getDocument().getLength());
            }

            @Override
            protected void done() {
                extractButton.setEnabled(true);
                try {
                    ExtractionResult result = get();
                    log.append(System.lineSeparator() + result.summary() + System.lineSeparator());
                    JOptionPane.showMessageDialog(ExtractorFrame.this, result.summary(), "Extraction complete",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    log.append(System.lineSeparator() + "ERROR: " + cause + System.lineSeparator());
                    JOptionPane.showMessageDialog(ExtractorFrame.this, cause.getMessage(), "Extraction failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
