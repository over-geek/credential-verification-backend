package com.icps.credential_verification.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.icps.credential_verification.dto.CertificatePdfDto;
import com.icps.credential_verification.exception.ResourceNotFoundException;
import com.icps.credential_verification.model.Credential;
import com.icps.credential_verification.repository.CredentialRepository;
import com.icps.credential_verification.service.CertificatePdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class CertificatePdfServiceImpl implements CertificatePdfService {

    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
    private static final PDFont HEADING_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BODY_BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final CredentialRepository credentialRepository;

    public CertificatePdfServiceImpl(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    public CertificatePdfDto generateCertificate(UUID credentialId) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found."));
        ensureQrToken(credential);

        try {
            byte[] content = renderCertificate(credential);
            return new CertificatePdfDto(buildFilename(credential), content);
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("Unable to generate certificate PDF.", exception);
        }
    }

    private byte[] renderCertificate(Credential credential) throws IOException, WriterException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            BufferedImage qrImage = createQrImage("cv://verify/" + credential.getQrToken());
            PDImageXObject qrCode = LosslessFactory.createFromImage(document, qrImage);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawBackground(content);
                drawHeader(content);
                drawRecipient(content, credential);
                drawDetails(content, credential);
                drawQrSection(content, qrCode, credential);
                drawFooter(content, credential);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private BufferedImage createQrImage(String value) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(value, BarcodeFormat.QR_CODE, 220, 220);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void ensureQrToken(Credential credential) {
        if (credential.getQrToken() != null && !credential.getQrToken().isBlank()) {
            return;
        }

        credential.setQrToken(generateUniqueQrToken());
        credentialRepository.save(credential);
    }

    private String generateUniqueQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (credentialRepository.existsByQrToken(token));

        return token;
    }

    private void drawBackground(PDPageContentStream content) throws IOException {
        content.setNonStrokingColor(new Color(250, 251, 252));
        content.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        content.fill();

        content.setStrokingColor(new Color(18, 102, 90));
        content.setLineWidth(3);
        content.addRect(42, 42, PAGE_WIDTH - 84, PAGE_HEIGHT - 84);
        content.stroke();

        content.setStrokingColor(new Color(203, 161, 53));
        content.setLineWidth(1.2f);
        content.addRect(54, 54, PAGE_WIDTH - 108, PAGE_HEIGHT - 108);
        content.stroke();
    }

    private void drawHeader(PDPageContentStream content) throws IOException {
        drawCenteredText(content, HEADING_FONT, 14, "ICPS UNIVERSITY", PAGE_HEIGHT - 110);
        drawCenteredText(content, TITLE_FONT, 34, "Certificate of Completion", PAGE_HEIGHT - 155);
        drawCenteredText(content, BODY_FONT, 12, "This certificate is proudly presented to", PAGE_HEIGHT - 195);
    }

    private void drawRecipient(PDPageContentStream content, Credential credential) throws IOException {
        String studentName = credential.getFirstName() + " " + credential.getLastName();
        drawCenteredText(content, TITLE_FONT, 30, studentName, PAGE_HEIGHT - 245);
        drawCenteredText(content, BODY_FONT, 12, "for successfully completing", PAGE_HEIGHT - 285);
        drawCenteredText(content, HEADING_FONT, 20, credential.getCourse(), PAGE_HEIGHT - 320);
    }

    private void drawDetails(PDPageContentStream content, Credential credential) throws IOException {
        float x = 128;
        float y = PAGE_HEIGHT - 380;
        drawLabelValue(content, "University", credential.getUniversity(), x, y);
        drawLabelValue(content, "Duration", credential.getDuration(), x, y - 34);
        drawLabelValue(content, "Class", credential.getCredentialClass(), x, y - 68);
    }

    private void drawQrSection(PDPageContentStream content, PDImageXObject qrCode, Credential credential) throws IOException {
        float qrSize = 115;
        float qrX = PAGE_WIDTH - 190;
        float qrY = 120;

        content.setNonStrokingColor(Color.WHITE);
        content.addRect(qrX - 10, qrY - 10, qrSize + 20, qrSize + 20);
        content.fill();
        content.drawImage(qrCode, qrX, qrY, qrSize, qrSize);

        drawText(content, BODY_BOLD_FONT, 10, "Verification QR", qrX - 4, qrY - 25);
    }

    private void drawFooter(PDPageContentStream content, Credential credential) throws IOException {
        drawText(content, BODY_FONT, 9, "Verify this credential by scanning the QR code or tapping the linked NFC chip.", 92, 82);
    }

    private void drawLabelValue(PDPageContentStream content, String label, String value, float x, float y) throws IOException {
        drawText(content, BODY_BOLD_FONT, 11, label.toUpperCase(), x, y);
        drawText(content, BODY_FONT, 15, value, x + 115, y);
    }

    private void drawCenteredText(PDPageContentStream content, PDFont font, int fontSize, String text, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        drawText(content, font, fontSize, text, (PAGE_WIDTH - textWidth) / 2, y);
    }

    private void drawText(PDPageContentStream content, PDFont font, int fontSize, String text, float x, float y) throws IOException {
        content.beginText();
        content.setNonStrokingColor(new Color(24, 32, 43));
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(safePdfText(text));
        content.endText();
    }

    private String safePdfText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String buildFilename(Credential credential) {
        return "certificate-" + credential.getId() + ".pdf";
    }
}
