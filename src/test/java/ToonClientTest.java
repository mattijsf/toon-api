import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.sqcubes.toon.api.ToonClient;
import com.sqcubes.toon.api.exception.ToonAuthenticationFailedException;
import com.sqcubes.toon.api.exception.ToonException;
import com.sqcubes.toon.api.exception.ToonLoginFailedException;
import com.sqcubes.toon.api.exception.ToonNotAuthenticatedException;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.*;

public class ToonClientTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSupportedTemperatures(){
    }

    @Test
    public void testInvalidAuthentication() throws IOException, ToonException {
        MockWebServer server = createAndStartServer();
        try{
            ToonClient toon = new ToonClient(HttpClients.createDefault(),server.getUrl("/"));

            // invalid login response (success = false)
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-login-credentials-invalid.json")));

            thrown.expect(ToonAuthenticationFailedException.class );
            toon.authenticate("not_used@email.com", "some_password");
        }
        finally {
            server.shutdown();
        }
    }

    @Test
    public void testValidAuthentication() throws Exception {
        MockWebServer server = createAndStartServer();
        try{
            ToonClient toon = new ToonClient(HttpClients.createDefault(), server.getUrl("/"));

            // login response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-login-credentials-valid.json")));

            // login agreement response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));

            boolean loggedIn = toon.authenticate("not_used@email.com", "some_password");

            assertTrue(loggedIn);
        }
        finally {
            server.shutdown();
        }
    }

    @Test
    public void testSetTemperatureNoLogin() throws IOException, ToonException {
        MockWebServer server = createAndStartServer();
        try{
            ToonClient toon = new ToonClient(HttpClients.createDefault(), server.getUrl("/"));

            thrown.expect(ToonNotAuthenticatedException.class);
            toon.setTemperature(20.0f);
        }
        finally {
            server.shutdown();
        }
    }

    @Test
    public void testSetTemperatureLogin() throws IOException, ToonException {
        MockWebServer server = createAndStartServer();
        try{
            ToonClient toon = new ToonClient(HttpClients.createDefault(), server.getUrl("/"));

            // login response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-login-credentials-valid.json")));

            // login agreement response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));

            // set temperature response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));

            // authenticate / login
            boolean loggedIn = toon.authenticate("not_used@email.com", "some_password");
            assertTrue(loggedIn);

            // set temperature
            boolean temperatureSet = toon.setTemperature(20.5f);
            assertTrue(temperatureSet);
        }
        finally {
            server.shutdown();
        }
    }

    @Test
    public void testReLoginRequired() throws IOException, ToonException {
        MockWebServer server = createAndStartServer();
        try{
            ToonClient toon = new ToonClient(HttpClients.createDefault(), server.getUrl("/"));

            // login response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-login-credentials-valid.json")));

            // login agreement response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));

            // set temperature attempt failed
            server.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody(html("toon-response-error-html.html")));

            // re-login login response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-login-credentials-valid.json")));

            // re-login agreement response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));

            // set temperature response
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(json("toon-response-success.json")));


            // authenticate / login
            boolean loggedIn = toon.authenticate("not_used@email.com", "some_password");
            assertTrue(loggedIn);

            // set temperature
            boolean temperatureSet = toon.setTemperature(20.5f);
            assertTrue(temperatureSet);
        }
        finally {
            server.shutdown();
        }
    }

    private MockWebServer createAndStartServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.play();

        return server;
    }

    private String json(String filename) throws IOException {
        assertTrue("provided filename[" + filename + "] should end with .json", filename.endsWith(".json"));
        return IOUtils.toString(this.getClass().getResourceAsStream(filename)).replace("\n","");
    }

    private String html(String filename) throws IOException {
        assertTrue("provided filename[" + filename + "] should end with .html", filename.endsWith(".html"));
        return IOUtils.toString(this.getClass().getResourceAsStream(filename)).replace("\n","");
    }
}