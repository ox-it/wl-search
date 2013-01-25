package org.sakaiproject.search.producer;

import org.apache.tika.Tika;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.search.api.StoredDigestContentProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * Produces indexable documents from binary files provided by the
 * {@link org.sakaiproject.content.api.ContentHostingService}.
 * <p>
 * The content is provided as a binary stream (if the index supports binary streams).
 * For backward compatibility reasons, the content can also be provided as a string,
 * once it has been parsed with Tika.<br />
 * The binary stream allows to offload the process of parsing the document.
 * </p>
 *
 * @author Colin Hebert
 */
public class BinaryContentHostingContentProducer extends ContentHostingContentProducer
        implements BinaryEntityContentProducer, StoredDigestContentProducer {
    private static final Logger logger = LoggerFactory.getLogger(BinaryContentHostingContentProducer.class);
    private static final byte[] EMPTY_DOCUMENT = new byte[0];
    private List<String> supportedResourceTypes;
    private long documentMaximumSize = Long.MAX_VALUE;
    private Tika tika = new Tika();

    @Override
    public boolean isContentFromReader(String reference) {
        return false;
    }

    @Override
    public Reader getContentReader(String reference) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is deprecated because BinaryContentHostingContentProducer is supposed to provide
     * binary streams only.<br />
     * For compatibility reasons, it's possible to obtain the content of the file through this method thanks to Tika.
     * </p>
     *
     * @deprecated Use {@link #getContentStream(String)} as the content is a binary stream.
     */
    @Override
    @Deprecated
    public String getContent(String reference) {
        try {
            return tika.parseToString(getContentStream(reference));
        } catch (Exception e) {
            logger.error("Error while trying to get the content of '" + reference + "' with tika", e);
            return "";
        }
    }

    @Override
    protected boolean isResourceTypeSupported(String resourceType) {
        return supportedResourceTypes.contains(resourceType);
    }

    @Override
    public InputStream getContentStream(String reference) {
        ContentResource contentResource;
        try {
            contentResource = contentHostingService.getResource(getId(reference));

            if (contentResource.getContentLength() > documentMaximumSize) {
                logger.info("The document '" + reference + "' was bigger (" + contentResource.getContentLength() + "B) "
                        + "than the maximum expected size " + documentMaximumSize + "B, its content won't be handled");
                return new ByteArrayInputStream(EMPTY_DOCUMENT);
            } else {
                return contentResource.streamContent();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to obtain content from " + reference, ex);
        }
    }

    @Override
    public String getContentType(String reference) {
        try {
            return contentHostingService.getResource(getId(reference)).getContentType();
        } catch (Exception e) {
            logger.info("Couldn't get the contentType of '" + reference + "'");
            return null;
        }
    }

    @Override
    public String getResourceName(String reference) {
        try {
            return contentHostingService.getResource(getId(reference)).getReference();
        } catch (Exception e) {
            logger.info("Couldn't get the contentType of '" + reference + "'");
            return null;
        }
    }

    public void setSupportedResourceTypes(List<String> supportedResourceTypes) {
        this.supportedResourceTypes = supportedResourceTypes;
    }

    public void setDocumentMaximumSize(long documentMaximumSize) {
        this.documentMaximumSize = documentMaximumSize;
    }
}