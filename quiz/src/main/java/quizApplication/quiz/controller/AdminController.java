package quizApplication.quiz.controller;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;
import quizApplication.quiz.entity.ExamAttempt;
import quizApplication.quiz.entity.Question;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.entity.UserDetails;
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuestionRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.repository.UserDetailsRepository;
import quizApplication.quiz.services.AdminUserService;
import quizApplication.quiz.services.PdfService;
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
	@Autowired
	private PdfService pdfService;
	@Autowired
	private UserDetailsRepository userDetailsRepository;
	
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

	// download sample template
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
		model.addAttribute("totalAttempts", examAttemptRepo.totalAttempts());
		model.addAttribute("totalQuizzes", examAttemptRepo.totalQuizzes());
		model.addAttribute("averageScore", examAttemptRepo.averageScore());

		return "report";
	}

	@GetMapping("/reports/attempt/{id}")
	public String attemptDetails(@PathVariable Long id, Model model) throws Exception {

	    ExamAttempt attempt = examAttemptRepo.findById(id).orElseThrow();

	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, String> rawAnswers =
	            mapper.readValue(attempt.getAnswersJson(),
	                    new TypeReference<Map<String, String>>() {});

	    Map<Long, String> answers = new HashMap<>();
	    for (Map.Entry<String, String> e : rawAnswers.entrySet()) {
	        answers.put(Long.valueOf(e.getKey()), e.getValue());
	    }

	    List<Question> questions = questionRepo.findAllById(answers.keySet());

	    // ✅ Fetch user details
	    UserDetails userDetails =
	            userDetailsRepository.findByUserid(attempt.getUsername());

	    model.addAttribute("attempt", attempt);
	    model.addAttribute("questions", questions);
	    model.addAttribute("answers", answers);
	    model.addAttribute("userDetails", userDetails); // ✅

	    return "report-attempt-details";
	}

	@GetMapping("/reports/quiz-performance")
	public String quizWiseReport(@RequestParam(required = false) String quizType, Model model) {

	    model.addAttribute("reports", examAttemptRepo.quizPerformanceFiltered(quizType)
	    );

	    model.addAttribute("selectedQuiz", quizType);
	    return "report-quiz-performance";
	}

	@GetMapping("/reports/user-performance")
	public String userPerformanceQuizList(Model model) {

	    model.addAttribute(
	        "quizzes",
	        examAttemptRepo.findAttemptedQuizzes()
	    );

	    return "report-user-performance-quizzes";
	}
	
	
	@GetMapping("/reports/user-performance/{quizType}")
	public String userPerformanceByQuiz(
	        @PathVariable String quizType,
	        Model model) {

	    // Get real data from repo
	    List<Object[]> reports = examAttemptRepo.userPerformanceByQuiz(quizType);

		/*
		 * // --- TEMP: Add sample users if less than 100 for demo --- int currentCount
		 * = reports.size(); LocalDateTime now = LocalDateTime.now();
		 * 
		 * for (int i = currentCount + 1; i <= 100; i++) { String username = "user" + i;
		 * long totalAttempts = (long) (Math.random() * 10 + 1); long score = (long)
		 * (Math.random() * 100); LocalDateTime lastAttempt = now.minusDays((long)
		 * (Math.random() * 30));
		 * 
		 * reports.add(new Object[]{username, totalAttempts, score, lastAttempt}); } //
		 * ----------------------------------------------------------
		 */
	    model.addAttribute("reports", reports);
	    model.addAttribute("selectedQuiz", quizType);

	    return "report-user-performance";
	}


	@GetMapping("/reports/user/{username}")
	public String userAttempts(@PathVariable String username, Model model) {
		model.addAttribute("attempts", examAttemptRepo.findByUsername(username));
		return "report-user-attempts";
	}

	//export as CSV
	@GetMapping("/report/export/quiz-performance")
	public void exportQuizPerformance(
	        @RequestParam(required = false) String quizType,
	        HttpServletResponse response) throws IOException {

	    response.setContentType("text/csv");
	    response.setHeader(
	            "Content-Disposition",
	            "attachment; filename=quiz-performance.csv"
	    );

	    PrintWriter writer = response.getWriter();

	    // CSV Header (matches new report)
	    writer.println("Quiz,Start Date,Total Marks, Time Limit(min),Total Attempts,Avg Marks,Max Marks,Min Marks");

	    List<Object[]> reports = examAttemptRepo.quizPerformanceFiltered(quizType);

	    for (Object[] r : reports) {
	        writer.println(
	                r[0] + "," +                       // Quiz Title
	                r[1] + "," +                       // Start Date
	                r[2] + "," +                       // Total Marks
	                r[3] + "," +                       // Time Limit
	                r[4] + "," +                       // Attempts
	                String.format("%.2f", r[5]) + "," +// Avg
	                r[6] + "," +                       // Max
	                r[7]                               // Min
	        );
	    }

	    writer.flush();
	}

	
	//export quiz performance report as PDF
	@GetMapping("/report/export/quiz-performance/pdf")
	public void exportQuizPerformancePdf(
	        @RequestParam(required = false) String quizType,
	        HttpServletResponse response) throws Exception {

	    response.setContentType("application/pdf");
	    response.setHeader(
	            "Content-Disposition",
	            "attachment; filename=quiz-performance.pdf"
	    );

	    List<Object[]> reports = examAttemptRepo.quizPerformanceFiltered(quizType);

	    Document document = new Document(PageSize.A4.rotate());
	    PdfWriter.getInstance(document, response.getOutputStream());

	    document.open();

	    // Title
	    Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
	    Paragraph title = new Paragraph("Quiz Performance Report", titleFont);
	    title.setAlignment(Element.ALIGN_CENTER);
	    title.setSpacingAfter(20);
	    document.add(title);

	    // Table (same columns as CSV)
	    PdfPTable table = new PdfPTable(7);
	    table.setWidthPercentage(100);
	    table.setSpacingBefore(10);
	    table.setWidths(new float[]{3, 2, 2, 2, 2, 2, 2});

	    Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
	    Font dataFont = new Font(Font.HELVETICA, 10);

	    addHeader(table, headerFont,
	            "Quiz",
	            "Start Date",
	            "Total Marks",
	            "Total Attempts",
	            "Avg Marks",
	            "Max Marks",
	            "Min Marks"
	    );

	    for (Object[] r : reports) {
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[0]), dataFont)));
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[1]), dataFont)));
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[2]), dataFont)));
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[3]), dataFont)));
	        Number avg = (Number) r[4];
	        table.addCell(new PdfPCell(new Phrase(String.format("%.2f", avg.doubleValue()), dataFont)));
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[5]), dataFont)));
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(r[6]), dataFont)));
	    }

	    document.add(table);
	    document.close();
	}

	//Helper method for PDF export
	private void addHeader(PdfPTable table, Font font, String... headers) {
	    for (String h : headers) {
	        PdfPCell cell = new PdfPCell(new Phrase(h, font));
	        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
	        cell.setBackgroundColor(Color.LIGHT_GRAY);
	        table.addCell(cell);
	    }
	}

	//export user performance report as PDF
	@GetMapping("/report/export/user-performance/pdf")
	public void exportUserPerformancePdf(
	        @RequestParam(required = false) String quizType,
	        HttpServletResponse response) throws Exception {

	    response.setContentType("application/pdf");
	    response.setHeader(
	            "Content-Disposition",
	            "attachment; filename=user-performance.pdf"
	    );

	    // Fetch real user performance data
	    List<Object[]> reports = examAttemptRepo.userPerformanceByQuiz(quizType);

		/*
		 * // Optional: add sample users for testing if needed int currentCount =
		 * reports.size(); LocalDateTime now = LocalDateTime.now(); for (int i =
		 * currentCount + 1; i <= 100; i++) { String username = "user" + i; long
		 * totalAttempts = 1; // Usually 1 attempt int score = (int) (Math.random() *
		 * 100); LocalDateTime lastAttempt = now.minusDays((long) (Math.random() * 30));
		 * String quizTitle = quizType; // same quiz reports.add(new Object[]{username,
		 * totalAttempts, score, lastAttempt, quizTitle}); }
		 */

	    // Initialize PDF
	    Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
	    PdfWriter.getInstance(document, response.getOutputStream());
	    document.open();

	    // --- Heading (like frontend) ---
	    Font headingFont = new Font(Font.HELVETICA, 16, Font.BOLD);
	    Paragraph heading = new Paragraph("User Performance", headingFont);
	    heading.setAlignment(Element.ALIGN_LEFT);

	    // Quiz name in muted smaller font
	    Font quizFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);
	    Paragraph quizName = new Paragraph("(" + quizType + ")", quizFont);
	    quizName.setAlignment(Element.ALIGN_LEFT);
	    quizName.setSpacingAfter(15);

	    document.add(heading);
	    document.add(quizName);

	    // --- Table ---
	    PdfPTable table = new PdfPTable(6);
	    table.setWidthPercentage(100);
	    table.setWidths(new float[]{1, 3, 2, 2, 3, 3});

	    Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
	    Font dataFont = new Font(Font.HELVETICA, 10);

	    // Header row
	    String[] headers = {"#", "Username", "Total Attempts", "Score", "Last Attempt", "Quiz Title"};
	    for (String h : headers) {
	        PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
	        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
	        cell.setBackgroundColor(Color.LIGHT_GRAY);
	        table.addCell(cell);
	    }

	 // Data rows
	    int count = 1;
	    for (Object[] u : reports) {
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(count++), dataFont))); // #

	        // Username: Check if u[0] is null
	        String username = (u[0] != null) ? String.valueOf(u[0]) : "NA";
	        table.addCell(new PdfPCell(new Phrase(username, dataFont)));

	        // Total Attempts: u[1] should ideally always be present based on context
	        table.addCell(new PdfPCell(new Phrase(String.valueOf(u[1]), dataFont)));

	        // Score: Check if u[2] is null
	        String score = (u[2] != null) ? String.valueOf(u[2]) : "NA";
	        table.addCell(new PdfPCell(new Phrase(score, dataFont)));

	        // Last Attempt: Check if u[3] is null
	        if (u[3] != null) {
	            LocalDateTime lastAttempt = (LocalDateTime) u[3];
	            table.addCell(new PdfPCell(
	                new Phrase(lastAttempt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), dataFont)
	            ));
	        } else {
	            table.addCell(new PdfPCell(new Phrase("NA", dataFont)));
	        }

	        // Quiz Title: use u[4] if exists and is not null, else fallback to quizType, else fallback to "NA"
	        String quizTitle;
	        if (u.length > 4 && u[4] != null) {
	            quizTitle = String.valueOf(u[4]);
	        } else if (quizType != null) { // Assume quizType is defined elsewhere
	            quizTitle = quizType;
	        } else {
	            quizTitle = "NA";
	        }
	        table.addCell(new PdfPCell(new Phrase(quizTitle, dataFont)));
	    }

	    document.add(table);
	    document.close();
	}

//export user performance report as CSV
	@GetMapping("/report/export/user-performance")
	public void exportUserPerformanceCsv(
	        @RequestParam(required = false) String quizType,
	        HttpServletResponse response) throws IOException {

	    // Set CSV content type
	    response.setContentType("text/csv");
	    response.setHeader(
	            "Content-Disposition",
	            "attachment; filename=user-performance.csv"
	    );

	    PrintWriter writer = response.getWriter();

	    // CSV Header
	    writer.println("Index,Username,Total Attempts,Score,Last Attempt,Quiz Title");

	    // Fetch real data
	    List<Object[]> reports = examAttemptRepo.userPerformanceByQuiz(quizType);

	    // Optional: add sample users if less than 100
	    int currentCount = reports.size();
	    LocalDateTime now = LocalDateTime.now();
	    for (int i = currentCount + 1; i <= 100; i++) {
	        String username = "user" + i;
	        long totalAttempts = 1;
	        long score = (long) (Math.random() * 100);
	        LocalDateTime lastAttempt = now.minusDays((long) (Math.random() * 30));
	        String quizTitle = quizType;
	        reports.add(new Object[]{username, totalAttempts, score, lastAttempt, quizTitle});
	    }

	    // Write CSV rows
	    int index = 1;
	    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	    for (Object[] u : reports) {
	        String quizTitle;
	        if (u.length > 4 && u[4] != null) {
	            quizTitle = String.valueOf(u[4]);
	        } else {
	            quizTitle = quizType;
	        }

	        String lastAttemptStr = "";
	        if (u[3] instanceof LocalDateTime) {
	            lastAttemptStr = ((LocalDateTime) u[3]).format(dtf);
	        }

	        writer.println(
	                index++ + "," +
	                u[0] + "," +              // Username
	                u[1] + "," +              // Total Attempts
	                u[2] + "," +              // Score
	                lastAttemptStr + "," +    // Last Attempt
	                quizTitle                 // Quiz Title
	        );
	    }

	    writer.flush();
	}

	
	
	// get TOP N users
	
	@GetMapping("/report/export/user-performance/top/{topN}")
	public void exportTopUsersPdf(@PathVariable int topN,
	                              @RequestParam String quizType,
	                              HttpServletResponse response) throws Exception {

	    List<ExamAttempt> attempts = examAttemptRepo.findByQuizTypeAndCompletedTrue(quizType);

	    // Aggregate by user: max score, lowest time
	    Map<String, ExamAttempt> topAttempts = new HashMap<>();
	    for (ExamAttempt a : attempts) {
	        String user = a.getUsername();
	        long timeTaken = Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds();
	        if (!topAttempts.containsKey(user)) topAttempts.put(user, a);
	        else {
	            ExamAttempt existing = topAttempts.get(user);
	            long existingTime = Duration.between(existing.getStartedAt(), existing.getSubmittedAt()).getSeconds();
	            if (a.getScore() > existing.getScore() || (a.getScore().equals(existing.getScore()) && timeTaken < existingTime)) {
	                topAttempts.put(user, a);
	            }
	        }
	    }

	    // Sort and limit
	    List<ExamAttempt> topUsers = topAttempts.values().stream()
	            .sorted(Comparator.comparing(ExamAttempt::getScore).reversed()
	                    .thenComparing(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt())))
	            .limit(topN)
	            .toList();

	    // Send PDF
	    response.setContentType("application/pdf");
	    response.setHeader("Content-Disposition", "attachment; filename=top_" + topN + "_users.pdf");
	    pdfService.generateTopUsersPdf(topUsers, response.getOutputStream());
	}

	
	@GetMapping("/reports/attempt/{id}/pdf")
	public void downloadAttemptPdf(@PathVariable Long id,
	                               HttpServletResponse response) throws Exception {

	    // 1️⃣ Fetch attempt
	    ExamAttempt attempt = examAttemptRepo.findById(id)
	            .orElseThrow(() -> new RuntimeException("Attempt not found"));

	    // 2️⃣ Fetch user details
	    UserDetails userDetails =
	            userDetailsRepository.findByUserid(attempt.getUsername());

	    // 3️⃣ Parse answers JSON
	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, String> rawAnswers = mapper.readValue(
	            attempt.getAnswersJson(),
	            new TypeReference<Map<String, String>>() {}
	    );

	    Map<Long, String> answers = new LinkedHashMap<>();
	    for (Map.Entry<String, String> e : rawAnswers.entrySet()) {
	        answers.put(Long.valueOf(e.getKey()), e.getValue());
	    }

	    // 4️⃣ Fetch attempted questions (order preserved)
	    List<Long> questionIds = new ArrayList<>(answers.keySet());
	    List<Question> questions = questionRepo.findAllById(questionIds);

	    // 5️⃣ HTTP response config
	    response.setContentType("application/pdf");
	    response.setHeader(
	            "Content-Disposition",
	            "attachment; filename=attempt-" + id + ".pdf"
	    );

	    // 6️⃣ PDF setup (Landscape to match UI)
	    Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
	    PdfWriter.getInstance(document, response.getOutputStream());
	    document.open();

	    // 7️⃣ Fonts
	    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
	    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
	   
	    Font correctFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GREEN);
	    Font wrongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);

	    // 8️⃣ Title
	    document.add(new Paragraph("Exam Attempt Report", titleFont));
	    document.add(Chunk.NEWLINE);

	    DateTimeFormatter formatter =
	            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	    // 9️⃣ User + Attempt Meta (matches frontend card)
	    PdfPTable meta = new PdfPTable(4);
	    meta.setWidthPercentage(100);
	    meta.setSpacingAfter(12);

	    meta.addCell("User ID");
	    meta.addCell(attempt.getUsername());
	    meta.addCell("Name");
	    meta.addCell(userDetails != null
	            ? userDetails.getFirstName() + " " + userDetails.getLastName()
	            : "NA");

	    meta.addCell("Email");
	    meta.addCell(userDetails != null ? userDetails.getEmail() : "NA");
	    meta.addCell("Department");
	    meta.addCell(userDetails != null ? userDetails.getDepartment() : "NA");

	    meta.addCell("Quiz");
	    meta.addCell(attempt.getQuizType());
	    meta.addCell("Score");
	    meta.addCell(String.valueOf(attempt.getScore()));

	    meta.addCell("Submitted At");
	    meta.addCell(attempt.getSubmittedAt().format(formatter));
	    meta.addCell("");
	    meta.addCell("");

	    document.add(meta);

	    // 🔟 Questions (UI-aligned blocks)
	    int qNo = 1;

	    for (Question q : questions) {

	        String userAnswer = answers.get(q.getId());
	        boolean correct =
	                userAnswer != null && userAnswer.equals(q.getCorrectAnswer());

	        PdfPTable qTable = new PdfPTable(2);
	        qTable.setWidthPercentage(100);
	        qTable.setSpacingBefore(6);
	        qTable.setSpacingAfter(10);

	        PdfPCell qHeader = new PdfPCell(
	                new Phrase("Q" + qNo++ + ". " + q.getQuestionText(), headerFont));
	        qHeader.setColspan(2);
	        qHeader.setBorder(Rectangle.NO_BORDER);
	        qTable.addCell(qHeader);

	        qTable.addCell("A. " + q.getOptionA());
	        qTable.addCell("");

	        qTable.addCell("B. " + q.getOptionB());
	        qTable.addCell("");

	        qTable.addCell("C. " + q.getOptionC());
	        qTable.addCell("");

	        qTable.addCell("D. " + q.getOptionD());
	        qTable.addCell("");

	        qTable.addCell("Your Answer");
	        qTable.addCell(userAnswer != null ? userAnswer : "Not Answered");

	        qTable.addCell("Correct Answer");
	        qTable.addCell(q.getCorrectAnswer());

	        PdfPCell resultCell = new PdfPCell(
	                new Phrase(correct ? "Correct" : "Wrong",
	                        correct ? correctFont : wrongFont));
	        resultCell.setColspan(2);
	        resultCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
	        resultCell.setBorder(Rectangle.NO_BORDER);

	        qTable.addCell(resultCell);

	        document.add(qTable);
	    }

	    document.close();
	}


}

