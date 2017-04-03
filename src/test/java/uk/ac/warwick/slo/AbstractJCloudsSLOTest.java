package uk.ac.warwick.slo;

import com.google.common.io.ByteSource;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.MultipartPart;
import org.jclouds.blobstore.domain.MultipartUpload;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.blobstore.strategy.internal.MultipartUploadSlicingAlgorithm;
import org.jclouds.io.PayloadSlicer;
import org.jclouds.io.internal.BasePayloadSlicer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.StreamSupport.*;
import static org.junit.Assert.*;

abstract class AbstractJCloudsSLOTest {

    private final PayloadSlicer payloadSlicer = new BasePayloadSlicer();

    private final String containerName = "slo-test";
    private final ByteSource in = ByteSource.wrap(new byte[38547913]);

    abstract BlobStoreContext getContext();

    @Before
    public void setup() throws Exception {
        if (!getContext().getBlobStore().containerExists(containerName))
            getContext().getBlobStore().createContainerInLocation(null, containerName);
    }

    @After
    public void tearDown() throws Exception {
        if (getContext().getBlobStore().containerExists(containerName))
            getContext().getBlobStore().deleteContainer(containerName);
    }

    private void putSLO(ByteSource in, String key) throws Exception {
        BlobStore blobStore = getContext().getBlobStore();

        long size = in.size();
        Blob blob =
            blobStore.blobBuilder(key)
                .payload(in)
                .contentDisposition(key)
                .contentLength(size)
                .build();

        long partSize =
            new MultipartUploadSlicingAlgorithm(blobStore.getMinimumMultipartPartSize(), blobStore.getMaximumMultipartPartSize(), blobStore.getMaximumNumberOfParts())
                .calculateChunkSize(size);

        MultipartUpload multipartUpload = blobStore.initiateMultipartUpload(containerName, blob.getMetadata(), PutOptions.NONE);

        AtomicInteger payloadCounter = new AtomicInteger();

        List<MultipartPart> parts = new ArrayList<>();
        stream(payloadSlicer.slice(blob.getPayload(), partSize).spliterator(), true)
            .forEachOrdered(payload ->
                parts.add(blobStore.uploadMultipartPart(multipartUpload, payloadCounter.incrementAndGet(), payload))
            );

        blobStore.completeMultipartUpload(multipartUpload, parts);
    }

    private void assertCanPutAndGetSLO(String key) throws Exception {
        putSLO(in, key);

        Blob blob = getContext().getBlobStore().getBlob(containerName, key);
        assertEquals(38547913L, blob.getMetadata().getSize().longValue());
    }

    @Test
    public void sloAscii() throws Exception {
        String key = "Nisan.rar";

        assertCanPutAndGetSLO(key);
    }

    @Test
    public void sloWithHighByteChars() throws Exception {
        String key = "Nişan.rar";

        assertCanPutAndGetSLO(key);
    }
}
