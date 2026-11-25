package vn.edu.learningai.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tiện ích cắt nguồn / lấy mẫu nguồn cho AI (T8).
 * Khi nguồn vượt quá ngân sách ký tự (AI_MAX_SOURCE_CHARS, mặc định 20.000):
 * - head: chỉ lấy phần đầu (hành vi cũ).
 * - spread: lấy mẫu rải đều các đoạn văn từ đầu, thân bài đến cuối bài, luôn giữ mở đầu và kết luận.
 */
public final class SourceSampler {

    public static final String SEPARATOR = "\n[...]\n";
    private static final int MAX_PARAGRAPH_CHUNK = 1500;
    private static final int MIN_SPREAD_BUDGET = 50;

    private SourceSampler() {
    }

    public record SampleResult(String text, boolean truncated) {
    }

    public static SampleResult sample(String sourceText, int budgetChars, String strategy) {
        if (sourceText == null || sourceText.length() <= budgetChars) {
            return new SampleResult(sourceText == null ? "" : sourceText, false);
        }

        if (strategy == null || !"spread".equalsIgnoreCase(strategy.trim())) {
            return new SampleResult(safeTruncate(sourceText, budgetChars), true);
        }

        return sampleSpread(sourceText, budgetChars);
    }

    private static SampleResult sampleSpread(String sourceText, int budgetChars) {
        if (budgetChars < MIN_SPREAD_BUDGET) {
            return new SampleResult(safeTruncate(sourceText, budgetChars), true);
        }

        List<String> blocks = splitIntoBlocks(sourceText, MAX_PARAGRAPH_CHUNK);
        if (blocks.size() <= 1) {
            return new SampleResult(safeTruncate(sourceText, budgetChars), true);
        }

        int maxEndBlockSize = Math.max(100, budgetChars / 3);

        String firstBlock = blocks.get(0);
        if (firstBlock.length() > maxEndBlockSize) {
            firstBlock = safeTruncate(firstBlock, maxEndBlockSize);
        }

        String lastBlock = blocks.get(blocks.size() - 1);
        if (lastBlock.length() > maxEndBlockSize) {
            lastBlock = safeTail(lastBlock, maxEndBlockSize);
        }

        int minRequired = firstBlock.length() + lastBlock.length() + SEPARATOR.length();
        if (minRequired > budgetChars) {
            int availableForText = budgetChars - SEPARATOR.length();
            if (availableForText >= 10) {
                int halfFirst = availableForText / 2;
                int halfLast = availableForText - halfFirst;
                String res = safeTruncate(firstBlock, halfFirst) + SEPARATOR + safeTail(lastBlock, halfLast);
                if (res.length() > budgetChars) {
                    res = safeTruncate(res, budgetChars);
                }
                return new SampleResult(res, true);
            }
            return new SampleResult(safeTruncate(sourceText, budgetChars), true);
        }

        if (blocks.size() == 2) {
            String res = firstBlock + SEPARATOR + lastBlock;
            if (res.length() > budgetChars) {
                res = safeTruncate(res, budgetChars);
            }
            return new SampleResult(res, true);
        }

        int remainingBudget = budgetChars - firstBlock.length() - lastBlock.length() - (2 * SEPARATOR.length());
        List<String> midBlocks = blocks.subList(1, blocks.size() - 1);

        List<Integer> selectedMidIndices = selectSpreadIndices(midBlocks, remainingBudget);

        StringBuilder sb = new StringBuilder(budgetChars);
        sb.append(firstBlock);

        for (int idx : selectedMidIndices) {
            sb.append(SEPARATOR).append(midBlocks.get(idx));
        }

        sb.append(SEPARATOR).append(lastBlock);

        String result = sb.toString();
        if (result.length() > budgetChars) {
            result = safeTruncate(result, budgetChars);
        }

        return new SampleResult(result, true);
    }

    private static List<Integer> selectSpreadIndices(List<String> midBlocks, int remainingBudget) {
        if (midBlocks.isEmpty() || remainingBudget <= 0) {
            return Collections.emptyList();
        }

        long totalMidLength = 0;
        for (String b : midBlocks) {
            totalMidLength += b.length();
        }

        if (totalMidLength <= remainingBudget) {
            List<Integer> all = new ArrayList<>(midBlocks.size());
            for (int i = 0; i < midBlocks.size(); i++) {
                all.add(i);
            }
            return all;
        }

        double avgLen = (double) totalMidLength / midBlocks.size();
        int estimatedK = (int) Math.max(1, Math.floor(remainingBudget / (avgLen + SEPARATOR.length())));
        estimatedK = Math.min(estimatedK, midBlocks.size());

        List<Integer> selected = new ArrayList<>();
        int currentChars = 0;
        double step = (double) midBlocks.size() / (estimatedK + 1);

        for (int i = 1; i <= estimatedK; i++) {
            int candidateIdx = (int) Math.round(i * step);
            if (candidateIdx >= midBlocks.size()) {
                candidateIdx = midBlocks.size() - 1;
            }
            if (!selected.contains(candidateIdx)) {
                String candidate = midBlocks.get(candidateIdx);
                int cost = candidate.length() + SEPARATOR.length();
                if (currentChars + cost <= remainingBudget) {
                    selected.add(candidateIdx);
                    currentChars += cost;
                }
            }
        }

        Collections.sort(selected);
        return selected;
    }

    private static List<String> splitIntoBlocks(String text, int maxBlockChars) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\r?\n\r?\n+");
        if (paragraphs.length <= 1) {
            paragraphs = text.split("\r?\n+");
        }

        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > maxBlockChars) {
                result.addAll(chunkLongParagraph(trimmed, maxBlockChars));
            } else {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static List<String> chunkLongParagraph(String text, int maxBlockChars) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int len = text.length();

        while (start < len) {
            int end = Math.min(start + maxBlockChars, len);
            if (end < len) {
                int breakPoint = -1;
                for (int i = end; i > start + (maxBlockChars / 2); i--) {
                    char c = text.charAt(i - 1);
                    if (c == '.' || c == '?' || c == '!' || c == ' ' || c == ';') {
                        breakPoint = i;
                        break;
                    }
                }
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }
            chunks.add(safeTruncate(text.substring(start, end), maxBlockChars).trim());
            start = end;
            while (start < len && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        return chunks;
    }

    public static String safeTruncate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        int end = limit;
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    public static String safeTail(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        int start = text.length() - limit;
        if (start < text.length() && Character.isLowSurrogate(text.charAt(start))) {
            start++;
        }
        return text.substring(start);
    }
}
