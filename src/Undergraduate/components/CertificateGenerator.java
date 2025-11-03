package Undergraduate.components;

import org.openpdf.text.*;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates a certificate PDF using OpenPDF 3.0.0 in LANDSCAPE mode.
 * Loads background from classpath: /resources/certificates/template.png
 */
public class CertificateGenerator {

    public static void generate(String studentName, String courseName, String outputPath) {
        try {
            // Ensure destination folder exists
            File outFile = new File(outputPath);
            if (outFile.getParentFile() != null) outFile.getParentFile().mkdirs();

            // -------------------------------------------------------------
            // 🧭 Landscape A4 page
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outFile));
            document.open();

            // -------------------------------------------------------------
            // Load background template from classpath
            URL resourceURL = CertificateGenerator.class.getResource("/resources/certificates/template.png");
            if (resourceURL == null) {
                throw new IllegalStateException(
                        "⚠️ template.png not found at /resources/certificates/template.png");
            }

            Image bg = Image.getInstance(resourceURL);
            bg.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth()); // swap width/height for landscape
            bg.setAbsolutePosition(0, 0);
            writer.getDirectContentUnder().addImage(bg);

            // -------------------------------------------------------------
            // Fonts (using java.awt.Color)
            Font nameFont   = FontFactory.getFont(FontFactory.HELVETICA, 36, Font.BOLD,   new Color(0,0,0));
            Font courseFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.NORMAL, new Color(64,64,64));
            Font dateFont   = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.ITALIC, new Color(80,80,80));

            float centerX = PageSize.A4.getHeight() / 2; // because rotated

            // -------------------------------------------------------------
            // Text overlay (adjust to your template layout)
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    new Phrase(studentName, nameFont),
                    centerX,
                    310,   // y‑position for landscape alignment
                    0
            );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    new Phrase("has successfully completed the course: " + courseName, courseFont),
                    centerX,
                    270,
                    0
            );

            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    new Phrase("Issued on " + dateStr, dateFont),
                    centerX,
                    230,
                    0
            );

            // -------------------------------------------------------------
            document.close();
            System.out.println("✅ Certificate (landscape) saved → " + outFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Certificate generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Quick local test
    public static void main(String[] args) {
        generate(
                "Heshan Dilshara",
                "Object Oriented Programming (OOP)",
                "certificates/test_certificate_landscape.pdf"
        );
    }
}