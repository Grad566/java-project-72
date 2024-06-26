package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlChecksRepository;
import hexlet.code.repository.UrlsRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

class AppTest {
    Javalin app;
    private static MockWebServer server;

    @BeforeEach
    public final void setApp() throws IOException, SQLException {
        app = App.getApp();
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        server = new MockWebServer();
        var html = Files.readString(Paths.get("src/test/resources/htmlForMock.html"));
        var serverResponse = new MockResponse()
                .addHeader("Content-Type", "text/html; charset=utf-8")
                .setResponseCode(200)
                .setBody(html);
        server.enqueue(serverResponse);
        server.start();
    }

    @AfterAll
    public static void shutdownMock() throws IOException {
        server.shutdown();
    }

    @Test
    public void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
    }

    @Test
    public void testAddUri() {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=http://localhost:7070/abracodabre";
            var response = client.post("/urls", requestBody);
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("http://localhost:7070");

            var url = UrlsRepository.getByName("http://localhost:7070")
                    .orElse(new Url("")).getName();
            assertThat(url).contains("http://localhost:7070");
        });
    }

    @Test
    public void testAddWrongUrl() {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=123";
            var response = client.post("/urls", requestBody);
            assertThat(response.code()).isEqualTo(200);

            var urls = UrlsRepository.getByName("123");
            assertThat(urls).isEmpty();
        });
    }

    @Test
    public void testShowAddedSites() {
        JavalinTest.test(app, (server, client) -> {
            var url1 = new Url("https://github.com");
            var url2 = new Url("http://localhost:7070");
            UrlsRepository.save(url1);
            UrlsRepository.save(url2);

            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);

            var responseBody = response.body().string();
            assertThat(responseBody.contains("https://github.com"));
            assertThat(responseBody.contains("http://localhost:7070"));
        });
    }

    @Test
    public void testShowSingleUrl() {
        JavalinTest.test(app, (server, client) -> {
            var url = new Url("https://github.com");
            UrlsRepository.save(url);
            var response = client.get("/urls/" + url.getId());

            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("https://github.com");
        });
    }

    @Test
    public void testNotAddedUrl() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/23498");
            assertThat(response.code()).isEqualTo(404);
        });
    }


    @Test
    public void testUrlCheckInnerContent() {
        var baseUrl = server.url("/").toString();
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=" + baseUrl;
            client.post("/urls", requestBody);
            client.post("/urls/1/checks");

            var urlCheck = UrlChecksRepository.getUrlChecksByUrlId(1L).get(0);

            assertThat(urlCheck.getStatusCode()).isEqualTo(200);
            assertThat(urlCheck.getH1()).contains("I am a h1");
            assertThat(urlCheck.getTitle()).contains("I am a title");
            assertThat(urlCheck.getDescription()).contains("I am a content");
        });
    }

}
