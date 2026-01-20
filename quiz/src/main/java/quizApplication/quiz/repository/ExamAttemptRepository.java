package quizApplication.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import quizApplication.quiz.entity.ExamAttempt;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

	ExamAttempt findByUsername(String username);
	ExamAttempt findByUsernameAndCompletedFalse(String username); 
	
	@Query("SELECT COUNT(DISTINCT e.quizType) FROM ExamAttempt e")
	long totalQuizzes();
	
	@Query("SELECT COUNT(e) FROM ExamAttempt e")
	long totalAttempts();
	
	
	
	@Query("SELECT COALESCE(AVG(e.score),0) FROM ExamAttempt e")
	double averageScore();

	// ================= QUIZ PERFORMANCE =================
	@Query("""
			    SELECT e.quizType,
			           COUNT(e),
			           COALESCE(AVG(e.score),0),
			           MAX(e.score),
			           MIN(e.score)
			    FROM ExamAttempt e
			    GROUP BY e.quizType
			""")
	List<Object[]> quizPerformanceReport();

	// ================= USER PERFORMANCE =================
	@Query("""
			    SELECT e.username,
			           COUNT(e),
			           COALESCE(AVG(e.score),0),
			           MAX(e.submittedAt)
			    FROM ExamAttempt e
			    GROUP BY e.username
			""")
	List<Object[]> userPerformanceReport();

	// ===== FILTERED QUIZ PERFORMANCE =====

	@Query("""
		    SELECT q.title,
		           q.startDate,
		           q.totalMarks,
		           COUNT(e),
		           COALESCE(AVG(e.score), 0),
		           COALESCE(MAX(e.score), 0),
		           COALESCE(MIN(e.score), 0)
		    FROM Quiz q
		    LEFT JOIN ExamAttempt e
		           ON e.quizType = q.title
		    WHERE (:quizType IS NULL
		           OR LOWER(q.title) LIKE LOWER(CONCAT('%', :quizType, '%')))
		    GROUP BY q.id, q.title, q.startDate, q.totalMarks
		    ORDER BY q.startDate DESC
		""")
		List<Object[]> quizPerformanceFiltered(@Param("quizType") String quizType);
		
		// ===== DISTINCT QUIZ TYPES FROM ATTEMPTS =====
		@Query("""
			    SELECT DISTINCT e.quizType
			    FROM ExamAttempt e
			    ORDER BY e.quizType
			""")
			List<String> findAttemptedQuizzes();
		
		
		@Query("""
			    SELECT e.username,
			           COUNT(e),
			           COALESCE(AVG(e.score),0),
			           MAX(e.submittedAt)
			    FROM ExamAttempt e
			    WHERE e.quizType = :quizType
			    GROUP BY e.username
			    ORDER BY MAX(e.submittedAt) DESC
			""")
			List<Object[]> userPerformanceByQuiz(@Param("quizType") String quizType);

	
}
