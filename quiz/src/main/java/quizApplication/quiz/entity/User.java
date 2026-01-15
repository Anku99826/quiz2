package quizApplication.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	 @Column(unique = true, nullable = false)
	private String loginId;
	@Column(nullable = false)
	private String password;
	@Column(nullable = false)
	private String role;

	@OneToOne
	@JoinColumn(name = "user_rollNumber", referencedColumnName = "userid")
	private UserDetails userDetail;
}
