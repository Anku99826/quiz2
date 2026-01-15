package quizApplication.quiz.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import quizApplication.quiz.entity.User;
import quizApplication.quiz.repository.UserRepository;

@Service
public class AdminUserService {

	private final UserRepository repo;
	private final PasswordEncoder encoder;

	public AdminUserService(UserRepository repo, PasswordEncoder encoder) {
		this.repo = repo;
		this.encoder = encoder;
	}

	public void createAdmin(String loginId, String rawPassword, String role) {
		if (repo.findByLoginId(loginId) != null) {
			throw new RuntimeException("Login ID already exists");
		}

		User user = new User();
		user.setLoginId(loginId);
		user.setPassword(encoder.encode(rawPassword));
		user.setRole(role);

		repo.save(user);
	}

	public List<User> getAllAdmins() {
		return repo.findAll().stream().filter(u -> u.getRole() != null && u.getRole().startsWith("ROLE_")).toList();
	}

	public void resetPassword(Long id, String newPassword) {
		User user = repo.findById(id).orElseThrow();
		user.setPassword(encoder.encode(newPassword));
		repo.save(user);
	}

	public void deleteAdmin(Long id, String currentLoginId) {
		User user = repo.findById(id).orElseThrow();

		if (user.getLoginId().equals(currentLoginId)) {
			throw new RuntimeException("You cannot delete your own account");
		}

		repo.delete(user);
	}
}
