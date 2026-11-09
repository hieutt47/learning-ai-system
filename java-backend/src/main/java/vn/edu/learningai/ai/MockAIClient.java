package vn.edu.learningai.ai;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.dto.response.CardCandidate;
import vn.edu.learningai.dto.response.FlashcardCandidate;
import vn.edu.learningai.dto.response.OptionCandidate;
import vn.edu.learningai.dto.response.QuestionCandidate;
import vn.edu.learningai.dto.response.QuizCandidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI giả lập để demo offline: không gọi mạng, trả candidate cùng cấu trúc với AI thật,
 * dựng từ các câu trong nguồn. Đọc TASK/COUNT từ header của prompt (AiPromptBuilder).
 */
public class MockAIClient implements AIClient {

    private static final Pattern TASK = Pattern.compile("^TASK: (QUIZ|FLASHCARD)$", Pattern.MULTILINE);
    private static final Pattern COUNT = Pattern.compile("^COUNT: (\\d+)$", Pattern.MULTILINE);
    private static final Pattern SOURCE = Pattern.compile("<source>\\n(.*)\\n</source>", Pattern.DOTALL);
    private static final int MAX_SENTENCE = 300;

    private static final List<String> DISTRACTORS = List.of(
        "Bài học không đề cập đến nội dung này.",
        "Nhận định này trái với nội dung bài học.",
        "Nội dung này chỉ đúng trong trường hợp đặc biệt, không phải ý của bài học.");

    private final JsonMapper jsonMapper;

    public MockAIClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String generate(String prompt) {
        String task = find(TASK, prompt, "QUIZ");
        int count = Integer.parseInt(find(COUNT, prompt, "1"));
        List<String> sentences = sentences(find(SOURCE, prompt, ""));

        Object candidate = "FLASHCARD".equals(task)
            ? new FlashcardCandidate(cards(sentences, count))
            : new QuizCandidate(questions(sentences, count));
        return jsonMapper.writeValueAsString(candidate);
    }

    private static List<QuestionCandidate> questions(List<String> sentences, int count) {
        List<QuestionCandidate> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String fact = sentences.get(i % sentences.size());
            List<OptionCandidate> options = new ArrayList<>();
            for (String d : DISTRACTORS) {
                options.add(new OptionCandidate(d, false));
            }
            options.add(i % (DISTRACTORS.size() + 1), new OptionCandidate(fact, true));
            result.add(new QuestionCandidate(
                "Câu " + (i + 1) + ": Theo nội dung bài học, phát biểu nào sau đây là đúng?",
                options,
                "Bài học có nêu: \"" + fact + "\""));
        }
        return result;
    }

    private static List<CardCandidate> cards(List<String> sentences, int count) {
        List<CardCandidate> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new CardCandidate(
                "Ý chính số " + (i + 1) + " của bài học là gì?",
                sentences.get(i % sentences.size())));
        }
        return result;
    }

    private static List<String> sentences(String source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Arrays.stream(source.split("(?<=[.!?])\\s+|\\n+"))
            .map(String::strip)
            .filter(s -> s.length() >= 15)
            .map(s -> s.length() > MAX_SENTENCE ? s.substring(0, MAX_SENTENCE) + "…" : s)
            .forEach(unique::add);
        if (unique.isEmpty()) {
            String all = source.strip();
            unique.add(all.isEmpty() ? "Nội dung bài học." : all.substring(0, Math.min(all.length(), MAX_SENTENCE)));
        }
        return new ArrayList<>(unique);
    }

    private static String find(Pattern pattern, String text, String fallback) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : fallback;
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String model() {
        return "mock-v1";
    }
}
