package quizApplication.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import quizApplication.quiz.entity.Quiz;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

	@Query("""
			    SELECT q
			    FROM Quiz q
			    LEFT JOIN q.questions ques
			    GROUP BY q
			""")
	List<Quiz> findAllWithQuestionCount();

	@Query("""
			    SELECT q
			    FROM Quiz q
			    LEFT JOIN FETCH q.questions
			    WHERE q.id = :quizId
			""")
	Quiz findQuizWithQuestions(Long quizId);
}
