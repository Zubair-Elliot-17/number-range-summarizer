# Number Range Summarizer

A Java 8 implementation of the `NumberRangeSummarizer` interface. It produces a comma delimited list of numbers and groups sequential numbers into a range.

```
Input:  "1,3,6,7,8,12,13,14,15,21,22,23,24,31"
Result: "1, 3, 6-8, 12-15, 21-24, 31"
```

## Usage

```java
NumberRangeSummarizer summarizer = new NumberRangeSummarizerImpl();

Collection<Integer> numbers = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");
String summary = summarizer.summarizeCollection(numbers); // "1, 3, 6-8, 12-15, 21-24, 31"
```

## Build and test

You need JDK 8 or newer. Maven doesn't need to be installed because the project includes the Maven wrapper.

```bash
./mvnw verify        # macOS / Linux
mvnw.cmd verify      # Windows
```

CI runs the tests on Java 8, 11, 17 and 21.

## Design decisions

The specification leaves some cases open. This implementation handles them as follows:

| Case | Behaviour |
|------|-----------|
| `null`, blank or delimiter-only input to `collect` | Returns an empty collection. |
| Whitespace and empty entries (`" 1, 2,,3,"`) | Tolerated. Tokens are trimmed and empty tokens are skipped. |
| Non-integer or out-of-range token (`"1,a"`, `"2147483648"`) | Throws `IllegalArgumentException` naming the bad token. Bad data is never skipped silently. |
| Unsorted input to `summarizeCollection` | Sorted ascending before grouping. The caller's collection is not modified. |
| Duplicates (`1,1,2`) | Removed, giving `1-2`. |
| Two consecutive numbers (`4,5`) | Printed as a range, `4-5`, because they are sequential. |
| Negative numbers | Supported, e.g. `-3,-2,-1` gives `-3--1`. |
| `Integer.MIN_VALUE` / `Integer.MAX_VALUE` | Adjacency is checked with `long` arithmetic, so there is no overflow. |
| `null` or empty collection to `summarizeCollection` | Returns `""`. |
| `null` element in the collection | Throws `NullPointerException`. |

### Performance

- `collect` parses the input in one pass with `Pattern.splitAsStream` and a precompiled pattern, so no intermediate array of tokens is built.
- `summarizeCollection` unboxes into an `IntStream` and sorts and de-duplicates primitives (`int[]`), which avoids per-element boxing overhead. It then builds the output in one linear pass with a single `StringBuilder`.
- Overall cost is **O(n log n)** time because of the sort, and O(n) extra space. The test suite includes a one-million-element case.

## Project structure

```
src/main/java/numberrangesummarizer/
    NumberRangeSummarizer.java        # provided interface (unchanged)
    NumberRangeSummarizerImpl.java    # implementation
src/test/java/numberrangesummarizer/
    NumberRangeSummarizerImplTest.java  # JUnit 5 tests
```
