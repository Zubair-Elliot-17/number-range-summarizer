package numberrangesummarizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    @Test
    @DisplayName("Sample from the specification produces the expected result")
    void sampleInputEndToEnd() {
        Collection<Integer> collected = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31", summarizer.summarizeCollection(collected));
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        void parsesCommaSeparatedNumbersInOrder() {
            assertEquals(Arrays.asList(1, 3, 6, 7), summarizer.collect("1,3,6,7"));
        }

        @Test
        void singleNumber() {
            assertEquals(Collections.singletonList(42), summarizer.collect("42"));
        }

        @Test
        void trimsWhitespaceAroundNumbers() {
            assertEquals(Arrays.asList(1, 2, 3), summarizer.collect("  1 ,\t2,  3  "));
        }

        @Test
        void ignoresEmptyEntries() {
            assertEquals(Arrays.asList(1, 2, 3), summarizer.collect(",1,,2, ,3,"));
        }

        @Test
        void supportsNegativeNumbersAndIntegerBounds() {
            assertEquals(Arrays.asList(-5, 0, Integer.MIN_VALUE, Integer.MAX_VALUE),
                    summarizer.collect("-5,0,-2147483648,2147483647"));
        }

        @Test
        void preservesDuplicatesAndOrder() {
            assertEquals(Arrays.asList(3, 1, 3), summarizer.collect("3,1,3"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n", ",", " , , "})
        void nullBlankOrOnlyDelimitersYieldsEmptyCollection(String input) {
            assertTrue(summarizer.collect(input).isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1,a,3", "1.5", "1;2;3", "1 2", "2147483648", "--1", "one"})
        void rejectsInvalidTokens(String input) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> summarizer.collect(input));
            assertTrue(e.getMessage().startsWith("Invalid number in input"));
        }
    }

    @Nested
    @DisplayName("summarizeCollection")
    class SummarizeCollection {

        @ParameterizedTest(name = "[{0}] -> \"{1}\"")
        @CsvSource(delimiter = '|', value = {
                "5                    | 5",
                "1,2                  | 1-2",
                "1,2,3,4,5            | 1-5",
                "1,3,5,7              | 1, 3, 5, 7",
                "1,2,4,5,7            | 1-2, 4-5, 7",
                "10,1,3,2,11          | 1-3, 10-11",
                "1,1,2,2,3,5,5        | 1-3, 5",
                "-3,-2,-1,0,1,5       | -3-1, 5",
                "-10,-8,-7            | -10, -8--7",
        })
        void summarizes(String numbers, String expected) {
            assertEquals(expected, summarizer.summarizeCollection(toList(numbers)));
        }

        @Test
        void nullYieldsEmptyString() {
            assertEquals("", summarizer.summarizeCollection(null));
        }

        @Test
        void emptyYieldsEmptyString() {
            assertEquals("", summarizer.summarizeCollection(Collections.emptyList()));
        }

        @Test
        void doesNotOverflowAtIntegerBounds() {
            List<Integer> input = Arrays.asList(
                    Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

            assertEquals("-2147483648--2147483647, 2147483646-2147483647",
                    summarizer.summarizeCollection(input));
        }

        @Test
        void minAndMaxAreNotTreatedAsConsecutive() {
            assertEquals("-2147483648, 2147483647",
                    summarizer.summarizeCollection(Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE)));
        }

        @Test
        void doesNotModifyInput() {
            List<Integer> input = new ArrayList<>(Arrays.asList(3, 1, 2, 2));

            summarizer.summarizeCollection(input);

            assertEquals(Arrays.asList(3, 1, 2, 2), input);
        }

        @Test
        void rejectsNullElements() {
            assertThrows(NullPointerException.class,
                    () -> summarizer.summarizeCollection(Arrays.asList(1, null, 3)));
        }

        @Test
        void handlesLargeInput() {
            List<Integer> input = IntStream.rangeClosed(1, 1_000_000)
                    .filter(n -> n % 1000 != 0)
                    .boxed()
                    .collect(Collectors.toList());
            Collections.reverse(input);

            String result = summarizer.summarizeCollection(input);

            assertTrue(result.startsWith("1-999, 1001-1999, "));
            assertTrue(result.endsWith(", 998001-998999, 999001-999999"));
            assertEquals(1000, result.split(", ").length);
        }

        private List<Integer> toList(String csv) {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        }
    }
}
