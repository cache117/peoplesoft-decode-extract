package com.gideontaylor.peoplesoft.extractor;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.gideontaylor.peoplesoft.extractor.core.ExtractionResult;
import com.gideontaylor.peoplesoft.extractor.core.ProjectExtractor;
import com.gideontaylor.peoplesoft.extractor.gui.ExtractorFrame;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            SwingUtilities.invokeLater(() -> {
                useSystemLookAndFeel();
                new ExtractorFrame().setVisible(true);
            });
            return;
        }

        try {
            Path input = null;
            Path output = null;
            for (int i = 0; i < args.length; i++) {
                if ("--input".equals(args[i]) && i + 1 < args.length) input = Path.of(args[++i]);
                else if ("--output".equals(args[i]) && i + 1 < args.length) output = Path.of(args[++i]);
            }
            if (input == null && args.length >= 2) {
                input = Path.of(args[0]);
                output = Path.of(args[1]);
            }
            if (input == null || output == null) {
                System.err.println("Usage: java -jar peoplesoft-xml-extractor.jar --input project.xml --output output-folder");
                System.exit(2);
            }

            ExtractionResult result = new ProjectExtractor().extract(input, output, System.out::println);
            System.out.println(result.summary());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            // The cross-platform look-and-feel remains available as a safe fallback.
        }
    }
}
