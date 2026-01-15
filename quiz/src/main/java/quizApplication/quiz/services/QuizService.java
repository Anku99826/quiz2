package quizApplication.quiz.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.repository.QuizRepository;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    public List<Quiz> getAllQuizzes() {
        List<Quiz> quizzes = quizRepository.findAllWithQuestionCount();

        // DEBUG (temporary)
        quizzes.forEach(q ->
            System.out.println("Quiz: " + q.getTitle()
                + " Questions: " + q.getQuestions().size())
        );

        return quizzes;
    }

    public Quiz getQuizById(Long quizId) {
        return quizRepository.findQuizWithQuestions(quizId);
    }
}
