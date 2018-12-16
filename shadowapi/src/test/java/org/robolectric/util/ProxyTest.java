package org.robolectric.util;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.util.ReflectionHelpers.defaultsFor;
import static org.robolectric.util.ReflectionHelpers.proxyFor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.util.ReflectionHelpers.WithType;

@RunWith(JUnit4.class)
public class ProxyTest {

  @Test
  public void proxyFor_shouldCallPrivateMethod() throws Exception {
    SomeClass someClass = new SomeClass("c");
    assertThat(
        proxyFor(_SomeClass_.class, someClass)
            .someMethod("a", "b"))
        .isEqualTo("a-b-c (someMethod)");
  }

  @Test
  public void proxyFor_shouldHonorWithTypeAnnotationForParams() throws Exception {
    SomeClass someClass = new SomeClass("c");
    assertThat(
        proxyFor(_SomeClass_.class, someClass)
            .anotherMethod("a", "b"))
        .isEqualTo("a-b-c (anotherMethod)");
  }

  @Test
  public void proxyFor_shouldCallDefaultMethods() throws Exception {
    SomeClass someClass = new SomeClass("c");
    assertThat(
        proxyFor(_SomeClass_.class, someClass)
            .defaultMethod("someMethod", "a", "b"))
        .isEqualTo("a-b-c (someMethod)");
    assertThat(
        proxyFor(_SomeClass_.class, someClass)
            .defaultMethod("anotherMethod", "a", "b"))
        .isEqualTo("a-b-c (anotherMethod)");
  }

  //////////////////////

  interface _SomeClass_ {

    String someMethod(String a, String b);

    String anotherMethod(@WithType("java.lang.String") Object a, String b);

    default String defaultMethod(String which, String a, String b) {
      switch (which) {
        case "someMethod":
          return someMethod(a, b);
        case "anotherMethod":
          return anotherMethod(a, b);
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  public static class SomeClass {

    private String c;

    public SomeClass(String c) {
      this.c = c;
    }

    private String someMethod(String a, String b) {
      return a + "-" + b + "-" + c + " (someMethod)";
    }

    private String anotherMethod(String a, String b) {
      return a + "-" + b + "-" + c + " (anotherMethod)";
    }
  }

}
