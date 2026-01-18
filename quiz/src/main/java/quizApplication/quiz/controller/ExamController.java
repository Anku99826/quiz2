package quizApplication.quiz.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import quizApplication.quiz.repository.ExamAttemptRepository;
import quizApplication.quiz.repository.QuestionRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
@Controller
@RequestMapping("/exam")

public class ExamController {
	
    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private ExamAttemptRepository examAttemptRepo;

    
    private static final long EXAM_DURATION_MS = 30 * 60 * 1000; // 30 mins

    @GetMapping("/home")
    public String backToLoginAfterSummaryPage(HttpSession session) {
    	session.invalidate();
    	return "userLoginPage";
    }
    
    @PostMapping("/begin")
    public String startExam(@RequestParam String quizType,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

         
        // 2. Fetch questions only if quiz is active
        List<Question> questions = questionRepo.findByQuizType(quizType);

        if (questions == null || questions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No questions available for selected exam");
            return "redirect:/user/dashboard";
        }

        // 3. Set exam start time ONCE
        session.setAttribute("EXAM_START_TIME", System.currentTimeMillis());

        // 4. Initialize status map
        Map<Integer, QuestionStatus> statusMap = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            statusMap.put(i, QuestionStatus.NOT_VISITED);
        }

        // 5. Store session attributes
        session.setAttribute("questions", questions);
        session.setAttribute("currentQuestion", 0);
        session.setAttribute("statusMap", statusMap);
        session.setAttribute("answers", new HashMap<Integer, Integer>());

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

        long elapsed = System.currentTimeMillis() - startTime;
        long remainingMillis = EXAM_DURATION_MS - elapsed;

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
        Map<Integer, String> answers =
                (Map<Integer, String>) session.getAttribute("answers");

        String selectedAnswer = answers != null ? answers.get(index) : null;
        model.addAttribute("selectedAnswer", selectedAnswer);

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
    public String saveAnswer(
            @RequestParam int index,
            @RequestParam(required = false) String answer,
            @RequestParam String action,
            HttpSession session,
            Principal principal) throws Exception {

        Map<Integer, String> answers =
                (Map<Integer, String>) session.getAttribute("answers");

        if (answers == null) {
            answers = new HashMap<>();
        }

        Map<Integer, QuestionStatus> statusMap =
                (Map<Integer, QuestionStatus>) session.getAttribute("statusMap");

        List<Question> questions =
                (List<Question>) session.getAttribute("questions");
     // ===== CLEAR =====
        if ("clear".equals(action)) {
            answers.remove(index);
            statusMap.put(index, QuestionStatus.VISITED);
        }

        // ===== SAVE & NEXT =====
        else if ("next".equals(action)) {
            if (answer != null) {
                answers.put(index, answer);
                statusMap.put(index, QuestionStatus.ATTEMPTED);
            } else {
                statusMap.put(index, QuestionStatus.VISITED);
            }
        }

        // ===== SAVE & MARK FOR REVIEW =====
        else if ("review".equals(action)) {
            if (answer != null) {
                answers.put(index, answer);
                statusMap.put(index, QuestionStatus.ATTEMPTED_REVIEW);
            } else {
                statusMap.put(index, QuestionStatus.REVIEW);
            }
        }


        // Store back in session
        session.setAttribute("answers", answers);
        session.setAttribute("statusMap", statusMap);
        session.setAttribute("currentQuestion", index);

        // ===== AUTO-SUBMIT =====
        if ("submit".equals(action)) {
            // Call your existing submit logic
            return submitExam(session, principal);
        }

        // ===== NAVIGATION =====
        int nextIndex = index + 1;
        if (nextIndex >= questions.size()) {
            nextIndex = questions.size() - 1;
        }

        session.setAttribute("currentQuestion", nextIndex);
        return "redirect:/exam/question?n=" + nextIndex;

    }


    @PostMapping("/submit")
    public String submitExam(HttpSession session, Principal principal) throws Exception {

    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	String candidateName = auth.getName();

        List<Question> questions =
                (List<Question>) session.getAttribute("questions");

        Map<Integer, String> answers =
                (Map<Integer, String>) session.getAttribute("answers");
        
        Map<Integer, String> answersOneBased = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            answersOneBased.put(entry.getKey() + 1, entry.getValue());
        }
        
        Map<Integer, QuestionStatus> statusMap =
                (Map<Integer, QuestionStatus>) session.getAttribute("statusMap");

        int total = questions.size();
        int attempted = answers.size();


        int score = 0;
        for (int i = 0; i < total; i++) {
            String userAnswer = answers.get(i);
            if (userAnswer != null &&
                userAnswer.equals(questions.get(i).getCorrectAnswer())) {
                score++;
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        ExamAttempt attempt = new ExamAttempt();
        attempt.setUsername(candidateName);
        attempt.setQuizType(questions.get(0).getQuizType());
        attempt.setScore(score);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setAnswersJson(mapper.writeValueAsString(answersOneBased)); 
        examAttemptRepo.save(attempt);

        return "redirect:/exam/summary";
    }

    
    @GetMapping("/summary")
    public String examSummary(HttpSession session, Model model, Principal principal) {

        if (principal == null) {
            return "redirect:/login/user";
        }

        String username = principal.getName();
        
    	if (username == null) {      
            return "redirect:/login/user";
        }
        
        // Fetch last attempt
        ExamAttempt attempt = examAttemptRepo.findByUsername(username);
        if(attempt ==null)
        {
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
        int review = 0; // If you want, store review questions separately in session

        model.addAttribute("candidateName", attempt.getUsername());
        model.addAttribute("examType", attempt.getQuizType());
        model.addAttribute("total", total);
        model.addAttribute("attempted", attempted);
        model.addAttribute("unattempted", unattempted);
        model.addAttribute("review", review);
        model.addAttribute("score", attempt.getScore());

        return "examSummary";
    }
   }

}
