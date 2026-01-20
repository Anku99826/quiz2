package quizApplication.quiz.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import quizApplication.quiz.entity.ExamAttempt;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.entity.UserDetails;
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.repository.UserDetailsRepository;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private QuizRepository quizRepository;
    @Autowired
    private ExamAttemptRepository examAttemptRepo;
    @Autowired
    private UserDetailsRepository userDetailsRepository;
	
    @GetMapping("/dashboard")
    public String userDashboard(Principal principal, RedirectAttributes redirectAttributes, Model model) {

        if (principal == null) {
            return "redirect:/login/user";
        }

        String username = principal.getName();
        UserDetails user = userDetailsRepository.findByUserid(username);
        String fullName = Optional.ofNullable(user)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .filter(name -> !name.contains("null")) // guards against null first/last name
                .orElse(username);

        
        ExamAttempt existingAttempt = examAttemptRepo.findByUsername(username);
        if (existingAttempt != null) {
            redirectAttributes.addFlashAttribute(
                "info", "You have already attempted the quiz."
            );
            return "redirect:/exam/summary";
        }
        else {
        	
        	List<Quiz> allQuizTypes =    quizRepository.findAll();
        	
        	model.addAttribute("fullName", fullName);
        	model.addAttribute("quizTypes",allQuizTypes);
            
            return "userDashboard";
        }
        
    }


}
