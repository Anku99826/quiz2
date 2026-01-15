package quizApplication.quiz.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDetails {
	@Id
	private String userid;
	private String firstName;
	private String lastName;
	private String email;
	private String department;
	private String designation;
	private String scale;
	private String location;
	
}
