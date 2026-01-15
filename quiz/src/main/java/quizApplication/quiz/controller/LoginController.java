package quizApplication.quiz.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import quizApplication.quiz.entity.User;
import quizApplication.quiz.repository.UserRepository;


@Controller
@RequestMapping("/login")
public class LoginController {
	@Value("${ad.auth.url}")
	private String authURL;

	@Value("${ad.auth.token}")
	private String authToken;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/admin")
	private String getAdminLoginPage() {
		return "adminLogin";
	}

	@PostMapping("/admin")
	public String adminLogin(
	        @RequestParam String username,
	        @RequestParam String password,
	        HttpServletRequest request,
	        Model model) {
		System.err.println("INISDE ADMIN LOGIN POST METHOD");
	    User admin = userRepository.findByLoginId(username);

	    if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
	        model.addAttribute("error", "Invalid admin credentials");
	        return "adminLogin";
	    }

	    UsernamePasswordAuthenticationToken auth =
	            new UsernamePasswordAuthenticationToken(
	                    admin.getLoginId(),
	                    null,
	                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
	            );

	    saveSecurityContext(auth, request);

	    return "redirect:/admin/dashboard";
	}


	@GetMapping("/user")
	private String getLoginPage() {
		return "userLoginPage";
	}

	@PostMapping("/user")
	private String postUserLoginRequest(
	        @RequestParam String username,
	        @RequestParam String password,
	        Model model,
	        HttpServletRequest request) {

	    try {

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        headers.set("Authorization", authToken);

	        String jsonBody =
	                "{ \"user_id\": \"" + username + "\", \"password\": \"" + password + "\" }";

	        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

	        ResponseEntity<Map> response =
	                restTemplate.exchange(authURL, HttpMethod.POST, entity, Map.class);

	        if (response.getStatusCode() == HttpStatus.OK) {

	            UsernamePasswordAuthenticationToken auth =
	                    new UsernamePasswordAuthenticationToken(
	                            username,
	                            null,
	                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
	                    );

	            SecurityContext context = SecurityContextHolder.createEmptyContext();
	            context.setAuthentication(auth);

	            SecurityContextHolder.setContext(context);

	            request.getSession(true)
	                   .setAttribute(
	                       HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
	                       context
	                   );

	            return "redirect:/user/dashboard";
	        }

	    } catch (Exception e) {
	        model.addAttribute("error", "Invalid username or password");
	    }

	    return "userLoginPage";
	}
	
	
	@PostMapping("/user2")
	private String loginForTestUser(
	        @RequestParam String username,
	        @RequestParam String password,
	        Model model,
	        HttpServletRequest request) {
		System.err.println("INSIDE TEST LOGIN METHOD");
		
	    // ===== TEST LOGIN ONLY =====
	    if ("test".equals(username) && "test".equals(password)) {

	        UsernamePasswordAuthenticationToken auth =
	                new UsernamePasswordAuthenticationToken(
	                        username,
	                        null,
	                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
	                );

	        SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);

            SecurityContextHolder.setContext(context);

            request.getSession(true)
                   .setAttribute(
                       HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                       context
                   );


	        return "redirect:/user/dashboard";
	    }

	    model.addAttribute("error", "Invalid username or password");
	    return "userLoginPage";
	}

	
	
	
	private void saveSecurityContext(
	        UsernamePasswordAuthenticationToken auth,
	        HttpServletRequest request) {

	    SecurityContext context = SecurityContextHolder.createEmptyContext();
	    context.setAuthentication(auth);

	    SecurityContextHolder.setContext(context);

	    request.getSession(true).setAttribute(
	        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
	        context
	    );
	}
	
	
}
