package quizApplication.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String quizType;      
    private String section;       

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String correctAnswer; // A/B/C/D
    @Column(nullable = false)
    private Integer marks;        // 1

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;// 1-5
    
    @Transient
    private boolean valid = true;

    @Transient
    private String errorMessage;

}
