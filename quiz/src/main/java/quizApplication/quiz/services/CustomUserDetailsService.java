package quizApplication.quiz.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import quizApplication.quiz.entity.User;
import quizApplication.quiz.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

		User user = userRepository.findByLoginId(loginId);

		if (user == null) {
			throw new UsernameNotFoundException("Admin not found");
		}

		return org.springframework.security.core.userdetails.User.withUsername(user.getLoginId())
				.password(user.getPassword()).roles(user.getRole().replace("ROLE_", "")).build();
	}
}
