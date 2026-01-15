package quizApplication.quiz.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;

import quizApplication.quiz.entity.Question;

@Component
public class CsvQuestionParser {

    public List<Question> parse(MultipartFile file) throws Exception {

        List<Question> questions = new ArrayList<>();

        try (CSVReader reader =
                 new CSVReader(new InputStreamReader(file.getInputStream()))) {

            String[] data;
            boolean firstLine = true;

            while ((data = reader.readNext()) != null) {

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (data.length < 10) {
                    throw new RuntimeException("Invalid CSV row: " + String.join(",", data));
                }

                Question q = new Question();
                q.setQuizType(data[0].trim());
                q.setSection(data[1].trim());
                q.setQuestionText(data[2].trim());
                q.setOptionA(data[3].trim());
                q.setOptionB(data[4].trim());
                q.setOptionC(data[5].trim());
                q.setOptionD(data[6].trim());
                q.setCorrectAnswer(data[7].trim());
                q.setMarks(Integer.parseInt(data[8].trim()));
                q.setNegativeMarks(Double.parseDouble(data[9].trim()));
                
                questions.add(q);
            }
        }

        return questions;
    }
}