package lang;

import org.junit.Test;

public class StringNormalizer {
  public String normalize(String s) { throw new RuntimeException(); }

  // Workaround for bugs in Gradle/JUnit
  @Test public void thisIsNotATest() {}


  public static class NoOp extends StringNormalizer {
    // Workaround for bugs in Gradle/JUnit
    @Test public void thisIsNotATest() {}

    @Override
    public String normalize(String s) {
      return s;
    }
  }
  public static class TabToSpace extends StringNormalizer {
    // Workaround for bugs in Gradle/JUnit
    @Test public void thisIsNotATest() {}

    @Override
    public String normalize(String s) {
      return s.replace('\t', ' ');
    }
  }
  public static class WhitespaceNormalize extends StringNormalizer {
    // Workaround for bugs in Gradle/JUnit
    @Test public void thisIsNotATest() {}

    @Override
    public String normalize(String s) {
      return s.replaceAll("[\t ]+", " ");
    }
  }
}
