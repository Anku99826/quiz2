package quizApplication.quiz.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {

	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne
	private ExamAttempt attempt;
	@ManyToOne
	private Question question;
	
	private String selectedOption;
	private boolean markedForReview;
	
}
