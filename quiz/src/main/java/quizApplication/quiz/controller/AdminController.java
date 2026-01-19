package quizApplication.quiz.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

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
	public String uploadCsv(
	        @RequestParam("file") MultipartFile file,
	        @RequestParam("quizId") Long quizId,
	        Model model,
	        RedirectAttributes redirectAttributes) {

	    try {
	        if (file.isEmpty()) {
	            redirectAttributes.addFlashAttribute("error", "Please upload a CSV file");
	            return "redirect:/admin/questions/upload";
	        }

	        // Fetch selected quiz
	        Quiz quiz = quizRepository.findById(quizId)
	                .orElseThrow(() -> new RuntimeException("Quiz not found"));

	        List<Question> questions = csvParser.parse(file);

	        if (questions.isEmpty()) {
	            redirectAttributes.addFlashAttribute("error", "CSV file is empty");
	            return "redirect:/admin/questions/upload";
	        }

	        // ✅ VALIDATE quizType in CSV matches selected quiz
	        String selectedQuizType = quiz.getTitle().trim();
	        for (int i = 0; i < questions.size(); i++) {
	            Question q = questions.get(i);
	            if (!selectedQuizType.equalsIgnoreCase(q.getQuizType().trim())) {
	                redirectAttributes.addFlashAttribute(
	                    "error",
	                    "Quiz Type mismatch at row " + (i + 1) +
	                    "! Selected Quiz: '" + selectedQuizType +
	                    "', CSV contains: '" + q.getQuizType() + "'"
	                );
	                return "redirect:/admin/questions/upload";
	            }
	        }

	        // Attach quiz AFTER validation
	        questions.forEach(q -> q.setQuiz(quiz));

	        // Send to preview page
	        model.addAttribute("previewQuestions", questions);
	        model.addAttribute("quiz", quiz);

	        return "admin-question-preview";

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

	// download sample
	@GetMapping("/questions/template")
	public void downloadCsvTemplate(HttpServletResponse response) throws IOException {

		response.setContentType("text/csv");
		response.setHeader("Content-Disposition", "attachment; filename=quiz_questions_template.csv");

		PrintWriter writer = response.getWriter();

		writer.println("quizType,section,questionText,optionA,optionB,optionC,optionD,correctAnswer,marks");
		writer.println("Quiz 1,QUANT,\"What is 2,000 + 3,000?\",\"3,500\",\"4,000\",\"5,000\",\"5,500\",C,1");

		writer.flush();
		writer.close();
	}

	// QUIZ

	@PostMapping("/quiz/create")
	public String createQuiz(@RequestParam String title, @RequestParam int timeLimit, @RequestParam int totalMarks,
			RedirectAttributes redirectAttributes) {

		Quiz quiz = new Quiz();
		quiz.setTitle(title);
		quiz.setTimeLimit(timeLimit);

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
		 long totalQuizzes = quizService.countAllQuizzes();
		 
		 
		
		model.addAttribute("totalAttempts", examAttemptRepo.totalAttempts());
		model.addAttribute("totalQuizzes", totalQuizzes);
		model.addAttribute("averageScore", examAttemptRepo.averageScore());
		

		return "report";
	}

	@GetMapping("/reports/attempt/{id}")
	public String attemptDetails(@PathVariable Long id, Model model) throws Exception {

		ExamAttempt attempt = examAttemptRepo.findById(id).orElseThrow();

		ObjectMapper mapper = new ObjectMapper();

		Map<String, String> rawAnswers = mapper.readValue(attempt.getAnswersJson(),
				new TypeReference<Map<String, String>>() {
				});

		Map<Long, String> answers = new HashMap<>();
		for (Map.Entry<String, String> e : rawAnswers.entrySet()) {
			answers.put(Long.valueOf(e.getKey()), e.getValue());
		}

		List<Question> questions = questionRepo.findAllById(answers.keySet());

		System.err.println(attempt.getAnswersJson());

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
		model.addAttribute("attempts", examAttemptRepo.findByUsername(username));
		return "report-user-attempts";
	}

	@GetMapping("/report/export/quiz-performance")
	public void exportQuizPerformance(@RequestParam(required = false) String quizType,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate toDate,
			HttpServletResponse response) throws IOException {

		response.setContentType("text/csv");
		response.setHeader("Content-Disposition", "attachment; filename=quiz-performance.csv");

		PrintWriter writer = response.getWriter();
		writer.println("Quiz,Attempts,Avg,Max,Min");

		for (Object[] r : examAttemptRepo.quizPerformanceReport()) {
			writer.println(r[0] + "," + r[1] + "," + r[2] + "," + r[3] + "," + r[4]);
		}
	}
	@GetMapping("/reports/attempt/{id}/pdf")
	public void downloadAttemptPdf(@PathVariable Long id,
	                               HttpServletResponse response) throws Exception {

	    // 1️⃣ Fetch attempt
	    ExamAttempt attempt = examAttemptRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Attempt not found"));

	    // 2️⃣ Parse answers JSON (same logic as UI)
	    ObjectMapper mapper = new ObjectMapper();

	    Map<String, String> rawAnswers = mapper.readValue(
	            attempt.getAnswersJson(),
	            new TypeReference<Map<String, String>>() {}
	    );

	    Map<Long, String> answers = new HashMap<>();
	    for (Map.Entry<String, String> e : rawAnswers.entrySet()) {
	        answers.put(Long.valueOf(e.getKey()), e.getValue());
	    }

	    // 3️⃣ Fetch only attempted questions
	    List<Question> questions = questionRepo.findAllById(answers.keySet());

	    // 4️⃣ HTTP response config
	    response.setContentType("application/pdf");
	    response.setHeader("Content-Disposition",
	            "attachment; filename=attempt-" + id + ".pdf");

	    // 5️⃣ PDF setup
	    Document document = new Document(PageSize.A4);
	    PdfWriter.getInstance(document, response.getOutputStream());
	    document.open();

	    // 6️⃣ Fonts
	    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
	    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
	    Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

	    // 7️⃣ Title
	    document.add(new Paragraph("Exam Attempt Report", titleFont));
	    document.add(Chunk.NEWLINE);

	    DateTimeFormatter formatter =
	            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	    // 8️⃣ Attempt meta
	    document.add(new Paragraph("User: " + attempt.getUsername(), normalFont));
	    document.add(new Paragraph("Quiz: " + attempt.getQuizType(), normalFont));
	    document.add(new Paragraph("Score: " + attempt.getScore(), normalFont));
	    document.add(new Paragraph(
	            "Submitted At: " + attempt.getSubmittedAt().format(formatter),
	            normalFont));

	    document.add(Chunk.NEWLINE);

	    // 9️⃣ Questions
	    int qNo = 1;

	    for (Question q : questions) {

	        String userAnswer = answers.get(q.getId());
	        boolean correct =
	                userAnswer != null && userAnswer.equals(q.getCorrectAnswer());

	        document.add(new Paragraph(
	                "Q" + qNo++ + ". " + q.getQuestionText(),
	                headerFont));

	        document.add(new Paragraph("A. " + q.getOptionA(), normalFont));
	        document.add(new Paragraph("B. " + q.getOptionB(), normalFont));
	        document.add(new Paragraph("C. " + q.getOptionC(), normalFont));
	        document.add(new Paragraph("D. " + q.getOptionD(), normalFont));

	        document.add(new Paragraph(
	                "Your Answer: " +
	                        (userAnswer != null ? userAnswer : "Not Answered"),
	                normalFont));

	        document.add(new Paragraph(
	                "Correct Answer: " + q.getCorrectAnswer(),
	                normalFont));

	        document.add(new Paragraph(
	                "Result: " + (correct ? "Correct" : "Wrong"),
	                normalFont));

	        document.add(Chunk.NEWLINE);
	    }

	    document.close();
	}


}

