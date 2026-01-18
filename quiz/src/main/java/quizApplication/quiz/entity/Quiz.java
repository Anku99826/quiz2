package quizApplication.quiz.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.List;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column( nullable = false, unique = true)
    private String quizCode;
    private String title;
    
    // Exam duration in minutes
    private int timeLimit;

    // Negative mark per wrong answer
    private double negativeMark;

    // Total marks for the quiz
    private int totalMarks;

    // Whether quiz is visible to users
    private boolean active = true;

    // One quiz → many questions
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY)
    private List<Question> questions;
}
