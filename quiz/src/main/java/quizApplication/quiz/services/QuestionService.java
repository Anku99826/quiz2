package quizApplication.quiz.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import quizApplication.quiz.entity.Question;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.repository.QuestionRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.utility.CsvQuestionParser;

@Service
public class QuestionService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CsvQuestionParser csvQuestionParser;

    @Transactional
    public void uploadQuestions(MultipartFile file, Long quizId) throws Exception {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<Question> questions = csvQuestionParser.parse(file);

        for (Question q : questions) {
            
            q.setQuiz(quiz);
        }

        questionRepository.saveAll(questions);
    }
}
