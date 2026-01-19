package quizApplication.quiz.utility;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import quizApplication.quiz.entity.Question;

@Component
public class CsvQuestionParser {

    public List<Question> parse(MultipartFile file) throws Exception {

        List<Question> questions = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')     // standard CSV
                .withQuoteChar('"')     // IMPORTANT: handles commas inside quotes
                .build();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(parser)
                .build()) {

            String[] data;
            int rowNumber = 0;

            while ((data = reader.readNext()) != null) {
                rowNumber++;

                // Skip header
                if (rowNumber == 1) continue;

                if (data.length != 9) {
                    throw new RuntimeException(
                        "Invalid CSV format at row " + rowNumber +
                        ". Expected 9 columns but found " + data.length
                    );
                }
                Question q = new Question();

                try {
                    q.setQuizType(data[0].trim());
                    q.setSection(data[1].trim());
                    q.setQuestionText(data[2].trim());
                    q.setOptionA(data[3].trim());
                    q.setOptionB(data[4].trim());
                    q.setOptionC(data[5].trim());
                    q.setOptionD(data[6].trim());

                    String correct = data[7].trim();
                    if (!List.of("A","B","C","D").contains(correct)) {
                        throw new RuntimeException("Correct answer must be A/B/C/D");
                    }
                    q.setCorrectAnswer(correct);

                    q.setMarks(Integer.parseInt(data[8].trim()));
                   

                } catch (Exception e) {
                    q.setValid(false);
                    q.setErrorMessage(e.getMessage());
                }

                questions.add(q);

            }
        }

        return questions;
    }
}
