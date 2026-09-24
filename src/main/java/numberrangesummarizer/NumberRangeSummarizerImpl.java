package numberrangesummarizer;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Produces a comma delimited list of numbers, collapsing runs of consecutive
 * integers into ranges.
 *
 * <p>Example: {@code "1,3,6,7,8,12,13,14,15,21,22,23,24,31"} summarizes to
 * {@code "1, 3, 6-8, 12-15, 21-24, 31"}.
 *
 * <p>Behaviour:
 * <ul>
 *     <li>{@link #collect(String)} accepts comma separated integers, tolerating
 *     surrounding whitespace and empty entries (e.g. {@code "1, 2,,3,"}).
 *     Negative numbers are supported. A {@code null} or blank input yields an
 *     empty collection. Any non-integer token results in an
 *     {@link IllegalArgumentException}.</li>
 *     <li>{@link #summarizeCollection(Collection)} does not assume its input is
 *     sorted or unique: numbers are de-duplicated and ordered ascending before
 *     being grouped. Any run of two or more consecutive numbers is written as
 *     {@code start-end}. A {@code null} or empty collection yields an empty
 *     string.</li>
 * </ul>
 *
 * <p>This class is stateless and therefore thread-safe.
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
