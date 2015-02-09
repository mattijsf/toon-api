import com.sqcubes.toon.api.ToonClient;
import com.sqcubes.toon.api.model.ToonSchemeState;
import com.sqcubes.toon.api.persistence.ToonFilePersistenceHandler;
import com.sqcubes.toon.api.exception.ToonLoginFailedException;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

@Ignore
public class ToonClientPrivateTest {
    File persistenceFile;
    ToonFilePersistenceHandler persistenceHandler;
    ToonClient toonClient;

    @Before
    public void setUp() throws IOException, InterruptedException {
        Thread.sleep(1000);
        persistenceFile = new File(".toon-persistence-test");
        persistenceHandler = new ToonFilePersistenceHandler(persistenceFile.getAbsolutePath());
        toonClient = new ToonClient(HttpClients.createDefault(), persistenceHandler);
//        toonClient.setProxy(new HttpHost("127.0.0.1", 8080, "http"));
    }

    @After
    public final void tearDown() throws IOException {
    }

    @Test
    public void testLoginSetTemperature() throws ToonLoginFailedException {
        boolean updated = toonClient.setTemperature(19.5f);
        assertTrue(updated);
    }

    @Test
    public void testLoginSetScheme() throws ToonLoginFailedException {
        boolean updated = toonClient.setSchemeState(ToonSchemeState.AWAY);
        assertTrue(updated);
    }
}