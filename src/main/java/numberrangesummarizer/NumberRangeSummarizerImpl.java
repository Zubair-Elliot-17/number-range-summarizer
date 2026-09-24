package numberrangesummarizer;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Summarizes numbers into a comma delimited list, grouping sequential runs into ranges,
 * e.g. "1,3,6,7,8" becomes "1, 3, 6-8".
 */
public class NumberRangeSummarizerImpl implements NumberRangeSummarizer {

    private static final Pattern COMMA = Pattern.compile(",");
    private static final String ITEM_DELIMITER = ", ";
    private static final char RANGE_DELIMITER = '-';

    @Override
    public Collection<Integer> collect(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return COMMA.splitAsStream(input)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(NumberRangeSummarizerImpl::parse)
                .collect(Collectors.toList());
    }

    @Override
    public String summarizeCollection(Collection<Integer> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        int[] numbers = input.stream()
                .map(n -> Objects.requireNonNull(n, "input must not contain null values"))
                .mapToInt(Integer::intValue)
                .sorted()
                .distinct()
                .toArray();

        StringBuilder result = new StringBuilder();
        int rangeStart = numbers[0];
        int previous = numbers[0];

        for (int i = 1; i < numbers.length; i++) {
            int current = numbers[i];
            // Compare as long so that Integer.MAX_VALUE + 1 cannot overflow.
            if ((long) current - previous != 1) {
                appendRange(result, rangeStart, previous);
                result.append(ITEM_DELIMITER);
                rangeStart = current;
            }
            previous = current;
        }
        appendRange(result, rangeStart, previous);

        return result.toString();
    }

    private static void appendRange(StringBuilder sb, int start, int end) {
        sb.append(start);
        if (start != end) {
            sb.append(RANGE_DELIMITER).append(end);
        }
    }

    private static Integer parse(String token) {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in input: '" + token + "'", e);
        }
    }
}
