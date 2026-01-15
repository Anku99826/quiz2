package quizApplication.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import quizApplication.quiz.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>{

	List<Question> findByQuizType(String quizType);

}


