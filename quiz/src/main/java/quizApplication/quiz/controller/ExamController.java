package quizApplication.quiz.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import quizApplication.quiz.entity.ExamAttempt;
import quizApplication.quiz.entity.Question;
import quizApplication.quiz.entity.QuestionStatus;
import quizApplication.quiz.entity.Quiz;
import quizApplication.quiz.entity.UserDetails;
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuestionRepository;
import quizApplication.quiz.repository.QuizRepository;
import quizApplication.quiz.repository.UserDetailsRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
@Controller
@RequestMapping("/exam")

public class ExamController {
	
    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private ExamAttemptRepository examAttemptRepo;
    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private QuizRepository quizRepository;
    
    //private static final long EXAM_DURATION_MS = 30 * 60 * 1000; // 30 mins

    @GetMapping("/home")
    public String backToLoginAfterSummaryPage(HttpSession session) {
    	session.invalidate();
    	return "userLoginPage";
    }
    
    @PostMapping("/begin")
    public String startExam(@RequestParam String quizType,
                            HttpSession session,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {

        ExamAttempt pending =
                examAttemptRepo.findByUsernameAndCompletedFalse(principal.getName());

        if (pending != null) {
            return "redirect:/exam/resume";
        }

        Quiz quiz = quizRepository.findByTitle(quizType)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<Question> questions = questionRepo.findByQuizType(quizType);

        if (questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No questions available");
            return "redirect:/user/dashboard";
        }

        // ⏱️ TIMER SETUP (DYNAMIC)
        long durationMs = quiz.getTimeLimit() * 60L * 1000L;

        session.setAttribute("EXAM_START_TIME", System.currentTimeMillis());
        session.setAttribute("EXAM_DURATION_MS", durationMs);

        // rest unchanged...

        // STATUS MAP
        Map<Integer, QuestionStatus> statusMap = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            statusMap.put(i, QuestionStatus.NOT_VISITED);
        }

        session.setAttribute("questions", questions);
        session.setAttribute("currentQuestion", 0);
        session.setAttribute("statusMap", statusMap);
        session.setAttribute("answers", new HashMap<Long, String>());

        // 💾 CREATE ATTEMPT
        ExamAttempt attempt = new ExamAttempt();
        attempt.setUsername(principal.getName());
        attempt.setQuizType(quizType);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setCompleted(false);
        attempt.setCurrentQuestion(0);
        attempt.setAnswersJson("{}");
        attempt.setStatusJson("{}");
        attempt.setScore(0);

        examAttemptRepo.save(attempt);

        return "redirect:/exam/question";
    }



    @GetMapping("/instructions")
    public String instructions() {
        return "instructions";
    }



    @GetMapping("/question")
    public String question(@RequestParam(required = false) Integer n,
                           Model model,
                           HttpSession session) {

        List<Question> questions =
                (List<Question>) session.getAttribute("questions");

        if (questions == null || questions.isEmpty()) {
            return "redirect:/user/dashboard";
        }

        Long startTime = (Long) session.getAttribute("EXAM_START_TIME");
        if (startTime == null) {
            return "redirect:/exam/instructions";
        }

        Long durationMs = (Long) session.getAttribute("EXAM_DURATION_MS");

        if (startTime == null || durationMs == null) {
            return "redirect:/exam/instructions";
        }

        long elapsed = System.currentTimeMillis() - startTime;
        long remainingMillis = durationMs - elapsed;

        if (remainingMillis <= 0) {
            return "redirect:/exam/submit";
        }

        model.addAttribute("remainingSeconds", remainingMillis / 1000);

        int index = (n != null) ? n : (int) session.getAttribute("currentQuestion");
        index = Math.max(0, Math.min(index, questions.size() - 1));
        session.setAttribute("currentQuestion", index);

        // ---------------- STATUS MAP ----------------
        Map<Integer, QuestionStatus> statusMap =
                (Map<Integer, QuestionStatus>) session.getAttribute("statusMap");

        if (statusMap == null) {
            statusMap = new HashMap<>();
        }

        // ---------------- ANSWERS ----------------
        Map<Long, String> answers =
                (Map<Long, String>) session.getAttribute("answers");

        Question currentQuestion = questions.get(index);

        String selectedAnswer = answers != null
                ? answers.get(currentQuestion.getId())
                : null;

        model.addAttribute("selectedAnswer", selectedAnswer);
        if (answers == null) {
            answers = new HashMap<>();
            session.setAttribute("answers", answers);
        }


        // ---------------- MARK VISITED ----------------
        QuestionStatus currentStatus = statusMap.get(index);
        if (currentStatus == null || currentStatus == QuestionStatus.NOT_VISITED) {
            statusMap.put(index, QuestionStatus.VISITED);
        }

        // ---------------- BUILD PALETTE CLASSES ----------------
        List<String> paletteClasses = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {

            QuestionStatus s = statusMap.get(i);
            String cssClass = "not_visited";

            if (s != null) {
                switch (s) {
                    case VISITED:
                        cssClass = "visited";
                        break;
                    case ATTEMPTED:
                        cssClass = "attempted";
                        break;
                    case REVIEW:
                        cssClass = "review";
                        break;
                    case ATTEMPTED_REVIEW:
                        cssClass = "attempted review"; 
                        break;
                    default:
                        cssClass = "not_visited";
                }
            }

            // highlight current question
            if (i == index) {
                cssClass += " current";
            }

            paletteClasses.add(cssClass);
        }

        // ---------------- MODEL + SESSION ----------------
        session.setAttribute("statusMap", statusMap);

        model.addAttribute("paletteClasses", paletteClasses);
        model.addAttribute("question", questions.get(index));
        model.addAttribute("index", index);
        model.addAttribute("total", questions.size());
        model.addAttribute("statusMap", statusMap);

        System.out.println("STATUS MAP = " + statusMap);

        return "quizPage";
    }

    
    @PostMapping("/save")
    public String saveAnswer(@RequestParam int index,
                             @RequestParam Long questionId,
                             @RequestParam(required = false) String answer,
                             @RequestParam String action,
                             HttpSession session,
                             Principal principal) throws Exception {

        Map<Long, String> answers =
                (Map<Long, String>) session.getAttribute("answers");

        if (answers == null) {
            answers = new HashMap<>();
        }

        Map<Integer, QuestionStatus> statusMap =
                (Map<Integer, QuestionStatus>) session.getAttribute("statusMap");

        List<Question> questions =
                (List<Question>) session.getAttribute("questions");

        if ("clear".equals(action)) {
            answers.remove(questionId);
            statusMap.put(index, QuestionStatus.VISITED);
        }

        else if ("next".equals(action)) {
            if (answer != null) {
                answers.put(questionId, answer);
                statusMap.put(index, QuestionStatus.ATTEMPTED);
            } else {
                statusMap.put(index, QuestionStatus.VISITED);
            }
        }

        else if ("review".equals(action)) {
            if (answer != null) {
                answers.put(questionId, answer);
                statusMap.put(index, QuestionStatus.ATTEMPTED_REVIEW);
            } else {
                statusMap.put(index, QuestionStatus.REVIEW);
            }
        }

        // 💾 SAVE TO SESSION
        session.setAttribute("answers", answers);
        session.setAttribute("statusMap", statusMap);

        // 💾 SAVE TO DATABASE (KEY STEP)
        ExamAttempt attempt =
                examAttemptRepo.findByUsernameAndCompletedFalse(principal.getName());

        ObjectMapper mapper = new ObjectMapper();
        attempt.setAnswersJson(mapper.writeValueAsString(answers));
        attempt.setStatusJson(mapper.writeValueAsString(statusMap));
        attempt.setCurrentQuestion(index);

        examAttemptRepo.save(attempt);

        int nextIndex = Math.min(index + 1, questions.size() - 1);
        session.setAttribute("currentQuestion", nextIndex);

        return "redirect:/exam/question?n=" + nextIndex;
    }


    @PostMapping("/submit")
    public String submitExam(HttpSession session, Principal principal) throws Exception {
    
        ExamAttempt attempt =
                examAttemptRepo.findByUsernameAndCompletedFalse(principal.getName());

        List<Question> questions =
                (List<Question>) session.getAttribute("questions");

        Map<Long, String> answers =
                (Map<Long, String>) session.getAttribute("answers");

        int score = 0;
        for (Question q : questions) {
            String userAnswer = answers.get(q.getId());
            if (userAnswer != null && userAnswer.equals(q.getCorrectAnswer())) {
                score++;
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        attempt.setAnswersJson(mapper.writeValueAsString(answers));

        attempt.setScore(score);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setCompleted(true);

        examAttemptRepo.save(attempt);

      
        System.err.println("Exam submitted successfully with score: " + score);
        return "redirect:/exam/summary";
    }

    
    @GetMapping("/resume")
    public String resumeExam(HttpSession session, Principal principal) throws Exception {

        ExamAttempt attempt =
                examAttemptRepo.findByUsernameAndCompletedFalse(principal.getName());

        if (attempt == null) {
            return "redirect:/user/dashboard";
        }

        ObjectMapper mapper = new ObjectMapper();

        List<Question> questions =
                questionRepo.findByQuizType(attempt.getQuizType());

        Map<Long, String> answers =
                mapper.readValue(attempt.getAnswersJson(),
                    new TypeReference<Map<Long, String>>() {});

        Map<Integer, QuestionStatus> statusMap =
                mapper.readValue(attempt.getStatusJson(),
                    new TypeReference<Map<Integer, QuestionStatus>>() {});
        Quiz quiz = quizRepository.findByTitle(attempt.getQuizType())
                .orElseThrow();

        long durationMs = quiz.getTimeLimit() * 60L * 1000L;

        long elapsed =
            java.time.Duration.between(attempt.getStartedAt(), LocalDateTime.now()).toMillis();

        session.setAttribute("EXAM_START_TIME",
                System.currentTimeMillis() - elapsed);

        session.setAttribute("EXAM_DURATION_MS", durationMs);

        session.setAttribute("questions", questions);
        session.setAttribute("answers", answers);
        session.setAttribute("statusMap", statusMap);
        session.setAttribute("currentQuestion", attempt.getCurrentQuestion());

     

        return "redirect:/exam/question?n=" + attempt.getCurrentQuestion();
    }

    
    
    @GetMapping("/summary")
    public String examSummary(HttpSession session, Model model, Principal principal) {
    	System.err.println("Inside exam summary");
        if (principal == null) {
            return "redirect:/login/user";
        }

        String username = principal.getName();
        UserDetails user = userDetailsRepository.findByUserid(username);
        String fullName = Optional.ofNullable(user)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .filter(name -> !name.contains("null")) // guards against null first/last name
                .orElse(username);
        
       
       
        // Fetch last attempt
        ExamAttempt attempt = examAttemptRepo.findByUsername(username);
        if(attempt == null)
        {	System.err.println("attempt is null");
        	return "redirect:/login/user";
        }
        
        else {
        	

        // Parse answersJson
        Map<Integer, String> answers = new HashMap<>();

        if (attempt.getAnswersJson() != null && !attempt.getAnswersJson().isBlank()) {
            answers = new ObjectMapper().readValue(
                attempt.getAnswersJson(),
                new TypeReference<Map<Integer, String>>() {}
            );
        }


        List<Question> questions = questionRepo.findByQuizType(attempt.getQuizType());

        int total = questions.size();
        int attempted = (int) answers.values().stream().filter(a -> a != null && !a.isEmpty()).count();
        int unattempted = total - attempted;

        model.addAttribute("candidateName", fullName);
        model.addAttribute(
        	    "userid",
        	    user != null ? user.getUserid() : username
        	);

        model.addAttribute("examType", attempt.getQuizType());
        model.addAttribute("total", total);
        model.addAttribute("attempted", attempted);
        model.addAttribute("unattempted", unattempted);
        model.addAttribute("timeTaken",
        	    String.format("%d hours %d minutes",
        	        java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toHours(),
        	        java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMinutes() % 60
        	    )
        	);

 
        
        System.err.println("before return statement");
        session.removeAttribute("questions");
        session.removeAttribute("answers");
        return "examSummary";
    }
   }

}
