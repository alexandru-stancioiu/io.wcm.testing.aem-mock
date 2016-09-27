package io.wcm.testing.mock.aem;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Revision;
import com.day.image.Layer;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.builder.ContentBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of {@link AssetManager}
 */
class MockAssetManager implements AssetManager {

    private final ContentBuilder contentBuilder;
    private final ResourceResolver resourceResolver;

    MockAssetManager(final ResourceResolver resourceResolver) {
        this.contentBuilder = new ContentBuilder(resourceResolver);
        this.resourceResolver = resourceResolver;
    }

    @Override
    public Asset restore(String s) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Revision> getRevisions(String s, Calendar calendar) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Asset createAssetForBinary(String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Asset getAssetForBinary(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAssetForBinary(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Asset createAsset(String path, InputStream inputStream, String mimeType, boolean autoSave) {
        try {
            // create asset
            contentBuilder.resource(path, ImmutableMap.<String, Object>builder()
                    .put(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET)
                    .build());
            contentBuilder.resource(path + "/" + JcrConstants.JCR_CONTENT, ImmutableMap.<String, Object>builder()
                    .put(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT)
                    .build());
            String renditionsPath = path + "/" + JcrConstants.JCR_CONTENT + "/" + DamConstants.RENDITIONS_FOLDER;
            contentBuilder.resource(renditionsPath, ImmutableMap.<String, Object>builder()
                    .put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER)
                    .build());

            // store asset metadata
            Map<String, Object> metadataProps = new HashMap<>();

            // try to detect image with/height if input stream contains image data
            byte[] data = IOUtils.toByteArray(inputStream);
            try (InputStream is = new ByteArrayInputStream(data)) {
                try {
                    Layer layer = new Layer(is);
                    metadataProps.put(DamConstants.TIFF_IMAGEWIDTH, layer.getWidth());
                    metadataProps.put(DamConstants.TIFF_IMAGELENGTH, layer.getHeight());
                } catch (Throwable ex) {
                    // ignore
                }
            }

            contentBuilder.resource(path + "/" + JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER, metadataProps);

            // store original rendition
            try (InputStream is = new ByteArrayInputStream(data)) {
                new ContentLoader(this.resourceResolver).binaryFile(is, renditionsPath + "/" + DamConstants.ORIGINAL_FILE, mimeType);
            }

            if (autoSave) {
                resourceResolver.commit();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create asset at " + path, ex);
        }

        return resourceResolver.getResource(path).adaptTo(Asset.class);
    }

    @Override public Revision createRevision(Asset asset, String s, String s1) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override public String assignAssetID(Asset asset) throws PathNotFoundException, RepositoryException {
        throw new UnsupportedOperationException();
    }
}
