package quizApplication.quiz.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import quizApplication.quiz.entity.ExamAttempt;
import quizApplication.quiz.entity.Question;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuestionRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.services.AdminUserService;
import quizApplication.quiz.services.QuizService;
import quizApplication.quiz.utility.CsvQuestionParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/admin")
@SessionAttributes({ "previewQuestions", "quiz" })
@PreAuthorize("hasRole('ADMIN')")

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
	@Autowired
	private ExamAttemptRepository examAttemptRepo;

	public AdminController(AdminUserService service) {
		this.service = service;
	}

	// ADMIN MANAGEMENT
	@GetMapping("/manage-users")
	public String adminList(Model model) {
		model.addAttribute("admins", service.getAllAdmins());
		return "manageAdmins";
	}

	@PostMapping("/manage-users/create")
	public String createAdmin(@RequestParam String loginId, @RequestParam String password, @RequestParam String role) {

		service.createAdmin(loginId, password, role);
		return "redirect:/admin/manage-users";
	}

	@PostMapping("/manage-users/reset/{id}")
	public String resetPassword(@PathVariable Long id, @RequestParam String password) {

		service.resetPassword(id, password);
		return "redirect:/admin/manage-users";
	}

	@PostMapping("/manage-users/delete/{id}")
	public String deleteAdmin(@PathVariable Long id, Principal principal) {

		service.deleteAdmin(id, principal.getName());
		return "redirect:/admin/manage-users";
	}

	// ---------------------------------------//

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
	public String uploadCsv(@RequestParam("file") MultipartFile file, @RequestParam("quizId") Long quizId, Model model,
			RedirectAttributes redirectAttributes) {

		try {
			if (file.isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "Please upload a CSV file");
				return "redirect:/admin/questions/upload";
			}

			Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));

			List<Question> questions = csvParser.parse(file);

			// Attach quiz but DO NOT SAVE
			questions.forEach(q -> q.setQuiz(quiz));

			model.addAttribute("previewQuestions", questions);
			model.addAttribute("quiz", quiz);

			return "admin-question-preview"; // preview page

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "CSV upload failed: " + e.getMessage());
			return "redirect:/admin/questions/upload";
		}
	}

	@PostMapping("/questions/upload/confirm")
	public String confirmUpload(@SessionAttribute("previewQuestions") List<Question> questions,
			SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {

		questionRepo.saveAll(questions);

		sessionStatus.setComplete(); // clear session

		redirectAttributes.addFlashAttribute("success", questions.size() + " questions uploaded successfully");

		return "redirect:/admin/questions/upload";
	}

	@PostMapping("/questions/upload/remove/{index}")
	public String removeQuestion(@PathVariable int index,
			@SessionAttribute("previewQuestions") List<Question> questions, @SessionAttribute("quiz") Quiz quiz,
			Model model) {

		if (index >= 0 && index < questions.size()) {
			questions.remove(index);
		}
		model.addAttribute("quiz", quiz);
		return "admin-question-preview";
	}

	// QUIZ

	@PostMapping("/quiz/create")
	public String createQuiz(@RequestParam String title, @RequestParam int timeLimit, @RequestParam double negativeMark,
			@RequestParam int totalMarks, RedirectAttributes redirectAttributes) {

		Quiz quiz = new Quiz();
		quiz.setTitle(title);
		quiz.setTimeLimit(timeLimit);
		quiz.setNegativeMark(negativeMark);
		quiz.setTotalMarks(totalMarks);
		quiz.setActive(true);

		quizRepository.save(quiz);

		redirectAttributes.addFlashAttribute("success", "Quiz created successfully. Now upload questions.");

		return "redirect:/admin/questions/upload";
	}

	@GetMapping("/quizzes")
	public String listQuizzes(Model model) {

		List<Quiz> quizzes = quizService.getAllQuizzes();

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

	@PostMapping("/quiz/{id}/toggle")
	@ResponseBody
	public String toggleQuizStatus(@PathVariable Long id) {
		Quiz quiz = quizRepository.findById(id).orElseThrow(() -> new RuntimeException("Quiz not found"));

		quiz.setActive(!quiz.isActive());
		quizRepository.save(quiz);

		return quiz.isActive() ? "ACTIVE" : "INACTIVE";
	}

	// REPORTS
	
	@GetMapping("/reports")
	public String viewReports(Model model) {
		model.addAttribute("totalAttempts", examAttemptRepo.totalAttempts());

		model.addAttribute("averageScore", examAttemptRepo.averageScore());

		return "report";
	}
	
	@GetMapping("/reports/attempt/{id}")
	public String attemptDetails(@PathVariable Long id, Model model) throws Exception {

	    ExamAttempt attempt =
	            examAttemptRepo.findById(id).orElseThrow();

	    ObjectMapper mapper = new ObjectMapper();


	    Map<String, String> rawAnswers =
	            mapper.readValue(
	                attempt.getAnswersJson(),
	                new TypeReference<Map<String, String>>() {}
	            );

	    Map<Long, String> answers = new HashMap<>();
	    for (Map.Entry<String, String> e : rawAnswers.entrySet()) {
	        answers.put(Long.valueOf(e.getKey()), e.getValue());
	    }
	    for (Long key : answers.keySet()) {
	        System.err.println("ANSWER KEY: " + key + " VALUE: " + answers.get(key));
			
		}
	    List<Question> questions =
	            questionRepo.findAllById(answers.keySet());
	    
	    System.err.println("INSIDE ATTEMPT DETAILS: "+ questions.size());
	    
	    model.addAttribute("attempt", attempt);
	    model.addAttribute("questions", questions);
	    model.addAttribute("answers", answers);

	    return "report-attempt-details";
	}


	
	@GetMapping("/reports/quiz-performance")
	public String quizWiseReport(@RequestParam(required = false) String quizType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			Model model) {
		LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
		LocalDateTime to = (toDate != null) ? toDate.atTime(23, 59, 59) : null;

		model.addAttribute("reports", examAttemptRepo.quizPerformanceFiltered(quizType, from, to));

		model.addAttribute("selectedQuiz", quizType);

		return "report-quiz-performance";
	}

	@GetMapping("/reports/user-performance")
	public String userPerformance(Model model) {

		List<Object[]> reports = examAttemptRepo.userPerformanceReport();

		model.addAttribute("reports", reports);

		return "report-user-performance";
	}
	
	
	@GetMapping("/reports/user/{username}")
	public String userAttempts(@PathVariable String username, Model model) {
	    model.addAttribute("attempts",
	        examAttemptRepo.findByUsername(username));
	    return "report-user-attempts";
	}
	
	
	@GetMapping("/report/export/quiz-performance")
	public void exportQuizPerformance(  @RequestParam(required=false) String quizType,
	        @RequestParam(required=false) @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate,
	        @RequestParam(required=false) @DateTimeFormat(iso = ISO.DATE) LocalDate toDate,
	        HttpServletResponse response) throws IOException {

	    response.setContentType("text/csv");
	    response.setHeader("Content-Disposition",
	            "attachment; filename=quiz-performance.csv");

	    PrintWriter writer = response.getWriter();
	    writer.println("Quiz,Attempts,Avg,Max,Min");

	    for (Object[] r : examAttemptRepo.quizPerformanceReport()) {
	        writer.println(r[0] + "," + r[1] + "," + r[2] + "," + r[3] + "," + r[4]);
	    }
	}

}
