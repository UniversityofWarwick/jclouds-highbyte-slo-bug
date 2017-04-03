package uk.ac.warwick;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.TransientApiMetadata;
import org.jclouds.filesystem.FilesystemApiMetadata;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.swift.v1.SwiftApiMetadata;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public abstract class TestUtils {

    public static BlobStoreContext createSwiftBlobStoreContext() {
        return ContextBuilder.newBuilder(new SwiftApiMetadata())
            .overrides(new Properties() {{
                try {
                    load(TestUtils.class.getResourceAsStream("/swift.properties"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }})
            .modules(Collections.singleton(new SLF4JLoggingModule()))
            .buildView(BlobStoreContext.class);
    }

    public static BlobStoreContext createFilesystemBlobStoreContext(File baseDir) {
        return ContextBuilder.newBuilder(new FilesystemApiMetadata())
            .overrides(new Properties() {{
                setProperty(FilesystemConstants.PROPERTY_BASEDIR, baseDir.getAbsolutePath());
            }})
            .modules(Collections.singleton(new SLF4JLoggingModule()))
            .buildView(BlobStoreContext.class);
    }

    public static BlobStoreContext createTransientBlobStoreContext() {
        return ContextBuilder.newBuilder(new TransientApiMetadata())
            .modules(Collections.singleton(new SLF4JLoggingModule()))
            .buildView(BlobStoreContext.class);
    }
}
