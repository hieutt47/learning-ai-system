package vn.edu.learningai.dto.response;

import java.util.List;

/** Câu hỏi trắc nghiệm do AI gợi ý, đã qua validate (contracts/ai-output.md). */
public record QuestionCandidate(String content, List<OptionCandidate> options, String explanation) {
}
