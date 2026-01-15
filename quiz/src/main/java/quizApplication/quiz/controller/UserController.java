package quizApplication.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import quizApplication.quiz.entity.ExamAttempt;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuizRepository;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private QuizRepository quizRepository;
    @Autowired
    private ExamAttemptRepository examAttemptRepo;
	
    @GetMapping("/dashboard")
    public String userDashboard(Principal principal, RedirectAttributes redirectAttributes, Model model) {

        if (principal == null) {
            return "redirect:/login/user";
        }

        String username = principal.getName();
        
        
        ExamAttempt existingAttempt = examAttemptRepo.findByUsername(username);
        if (existingAttempt != null) {
            redirectAttributes.addFlashAttribute(
                "info", "You have already attempted the quiz."
            );
            return "redirect:/exam/summary";
        }
        else {
        	
        	List<Quiz> allQuizTypes =    quizRepository.findAll();
        	
        	model.addAttribute("quizTypes",allQuizTypes);
            
            return "userDashboard";
        }
        
    }


}
