package com.valhalla.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link DevReloadController}'s dev/prod switch.
 *
 * <p>The interesting branch is the disabled one: in the packaged WAR the templates directory does
 * not exist, so the endpoint answers 404. Only the enabled branch was ever reachable before, because
 * a test run from the repo root always finds {@code src/main/webapp/WEB-INF/templates}.
 */
public class DevReloadControllerTest {

  @Test
  public void shouldAnswerWithTheTokenWhenLiveReloadIsEnabled(
    @TempDir Path templatesDir,
    @TempDir Path jsDir
  ) throws IOException {
    Files.writeString(templatesDir.resolve("page.html"), "<p>hola</p>");
    Files.writeString(jsDir.resolve("app.js"), "const a = 1;");

    var response = new DevReloadController(templatesDir, jsDir, true).currentToken();

    assertThat(response.getStatusCode().value(), is(200));
    assertThat(response.getBody(), containsString(":"));
  }

  @Test
  public void shouldAnswerNotFoundWhenLiveReloadIsDisabled(
    @TempDir Path templatesDir,
    @TempDir Path jsDir
  ) {
    var response = new DevReloadController(templatesDir, jsDir, true).currentToken();
    assertThat(
      "the enabled branch must work for the disabled case to mean anything",
      response.getStatusCode().value(),
      is(200)
    );

    response = new DevReloadController(templatesDir, jsDir, false).currentToken();

    assertThat(response.getStatusCode().value(), is(404));
  }

  @Test
  public void shouldExposeNoTokenToViewsWhenLiveReloadIsDisabled(
    @TempDir Path templatesDir,
    @TempDir Path jsDir
  ) {
    assertThat(new DevReloadController(templatesDir, jsDir, false).signature(), is(nullValue()));
    assertThat(new DevReloadController(templatesDir, jsDir, true).signature(), not(nullValue()));
  }
}
