package quizApplication.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import quizApplication.quiz.entity.Question;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.repository.QuestionRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.services.AdminUserService;
import quizApplication.quiz.services.QuizService;
import quizApplication.quiz.utility.CsvQuestionParser;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	private final AdminUserService service;
	@Autowired
	private QuestionRepository questionRepo;
	@Autowired
	private CsvQuestionParser csvParser;
	@Autowired
    private QuizService quizService; 
	@Autowired
	private QuizRepository quizRepository;

	  public AdminController(AdminUserService service) {
	        this.service = service;
	    }
	
	  //ADMIN MANAGEMENT 
	  @GetMapping("/manage-users")
	    public String adminList(Model model) {
	        model.addAttribute("admins", service.getAllAdmins());
	        return "manageAdmins";
	    }

	  @PostMapping("/manage-users/create")
	    public String createAdmin(
	            @RequestParam String loginId,
	            @RequestParam String password,
	            @RequestParam String role) {

	        service.createAdmin(loginId, password, role);
	        return "redirect:/admin/manage-users";
	    }
	  
	  @PostMapping("/manage-users/reset/{id}")
	    public String resetPassword(
	            @PathVariable Long id,
	            @RequestParam String password) {

	        service.resetPassword(id, password);
	        return "redirect:/admin/manage-users";
	    }
	  
	  @PostMapping("/manage-users/delete/{id}")
	    public String deleteAdmin(
	            @PathVariable Long id,
	            Principal principal) {

	        service.deleteAdmin(id, principal.getName());
	        return "redirect:/admin/manage-users";
	    }
	  
	 //---------------------------------------//
	  
	  
	  
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		
		return "adminDashboard"; 
	}

	@GetMapping("/questions/upload")
	public String uploadPage(Model model) {
	    model.addAttribute("quizzes", quizService.getAllQuizzes());
	    return "admin-upload-questions";
	}

	@PostMapping("/questions/upload")
	public String uploadCsv(
	        @RequestParam("file") MultipartFile file,
	        @RequestParam("quizId") Long quizId,
	        RedirectAttributes redirectAttributes) {

	    try {
	        if (file.isEmpty()) {
	            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file");
	            return "redirect:/admin/questions/upload";
	        }

	        Quiz quiz = quizRepository.findById(quizId)
	                .orElseThrow(() -> new RuntimeException("Quiz not found"));

	        List<Question> questions = csvParser.parse(file);

	        for (Question q : questions) {
	            // 🔥 THIS IS THE FIX
	            q.setQuiz(quiz);
	        }

	        questionRepo.saveAll(questions);

	        redirectAttributes.addFlashAttribute(
	            "success",
	            questions.size() + " questions uploaded successfully"
	        );

	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "CSV upload failed: " + e.getMessage());
	    }

	    return "redirect:/admin/questions/upload";
	}

	//QUIZ 
	
	@GetMapping("/quizzes")
    public String listQuizzes(Model model) {
		
        List<Quiz> quizzes = quizService.getAllQuizzes();
        for (Quiz quiz : quizzes) {
			System.err.println("insinde quiz admin controller");
			System.err.println(quizzes.size());
		}
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("quizCount", quizzes.size());
        return "quizzes"; 
    }

    @GetMapping("/quizzes/{quizId}/questions")
    public String quizQuestions(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizService.getQuizById(quizId);
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", quiz.getQuestions());
        return "quiz-questions"; 
    }
	
    
    //RESULT
    @GetMapping("/results")
    public String viewResult() {
    	return "result";
    }
    
}
