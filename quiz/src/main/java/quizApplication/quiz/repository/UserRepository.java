package quizApplication.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import quizApplication.quiz.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByLoginId(String loginId);
    User findByRole(String role);
    List<User> findAllByRole(String role);

}
