package quizApplication.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String quizType;
    @Column(nullable = false)
    private Integer score;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private Boolean completed;   // ⭐ FOR RESUME LOGIC

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String answersJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String statusJson;

    private Integer currentQuestion;
}
