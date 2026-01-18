package quizApplication.quiz.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import quizApplication.quiz.entity.ExamAttempt;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
	
	 ExamAttempt findByUsername(String username);
	 // ================= SUMMARY =================
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
	        SELECT e.quizType,
	               COUNT(e),
	               COALESCE(AVG(e.score),0),
	               MAX(e.score),
	               MIN(e.score)
	        FROM ExamAttempt e
	        WHERE (:quizType IS NULL OR e.quizType = :quizType)
	          AND (:fromDate IS NULL OR e.submittedAt >= :fromDate)
	          AND (:toDate IS NULL OR e.submittedAt <= :toDate)
	        GROUP BY e.quizType
	    """)
	    List<Object[]> quizPerformanceFiltered(
	            @Param("quizType") String quizType,
	            @Param("fromDate") LocalDateTime fromDate,
	            @Param("toDate") LocalDateTime toDate
	    );


}
