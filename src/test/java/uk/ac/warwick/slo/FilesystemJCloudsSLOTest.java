package uk.ac.warwick.slo;

import org.apache.commons.io.FileUtils;
import org.jclouds.blobstore.BlobStoreContext;
import org.junit.After;
import org.junit.Before;
import uk.ac.warwick.TestUtils;

import java.io.File;

import static org.junit.Assert.*;

public class FilesystemJCloudsSLOTest extends AbstractJCloudsSLOTest {

    private BlobStoreContext context;
    private File tempDir;

    @Override
    @Before
    public void setup() throws Exception {
        tempDir = File.createTempFile("blobStore", ".root");
        assertTrue(tempDir.delete());
        assertTrue(tempDir.mkdir());

        context = TestUtils.createFilesystemBlobStoreContext(tempDir);

        super.setup();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();

        context.close();
        FileUtils.deleteDirectory(tempDir);
    }

    @Override
    BlobStoreContext getContext() {
        return context;
    }
}
