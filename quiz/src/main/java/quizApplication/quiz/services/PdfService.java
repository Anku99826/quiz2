package quizApplication.quiz.services;



import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import quizApplication.quiz.entity.ExamAttempt;

import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    public void generateTopUsersPdf(List<ExamAttempt> users, OutputStream os) throws Exception {

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, os);

        document.open();

        // Title
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Top Users Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph date = new Paragraph("Generated On: " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);

        document.add(Chunk.NEWLINE);

        // Table with 4 columns
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3, 2, 2});

        // Table Header
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        addCell(table, "Rank", headerFont);
        addCell(table, "Username", headerFont);
        addCell(table, "Score", headerFont);
        addCell(table, "Time Taken", headerFont);

        // Table Rows
        Font rowFont = new Font(Font.HELVETICA, 12);
        int rank = 1;
        for (ExamAttempt attempt : users) {
            long seconds = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
            String formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60);

            addCell(table, String.valueOf(rank++), rowFont);
            addCell(table, attempt.getUsername(), rowFont);
            addCell(table, String.valueOf(attempt.getScore()), rowFont);
            addCell(table, formattedTime, rowFont);
        }

        document.add(table);
        document.close();
    }

    // Helper method to add cells
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
