/*******************************************************************************
 * Copyright 2016 General Electric Company.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.sample.blobstore.repository;

import com.ge.predix.sample.blobstore.entity.BlobFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.jclouds.blobstore.options.GetOptions.Builder.range;
import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;


/**
 * This is Core Object Store Service Class. Has methods for operating on the Object Store.
 *
 * @since Feb 2015
 */
public class BlobstoreService {

    Log log = LogFactory.getLog(BlobstoreService.class);

    /**
     * Instance of BlobStore
     */
    private BlobStore blobStore;

    /**
     * Name of the bucket created in BlobStore
     */
    private String bucket;

    /**
     * BlobStore Endpoint 
     */
    private String url;

    public BlobstoreService(BlobStore blobStore, String bucket) {
        this.blobStore = blobStore;
        this.bucket = bucket;
    }

    public BlobstoreService(BlobStore blobStore, String bucket, String url) {
        this.blobStore = blobStore;
        this.bucket = bucket;
        this.url = url;
    }

    public BlobFile createBlobFileObject(String id, String name, InputStream file) {
        return new BlobFile(id, bucket, name, file, url);
    }


    /**
     * Adds a new Blob to the binded bucket in the Object Store
     *
     * @param file File to be added
     * @throws Exception
     */
    public void put(BlobFile file, String contentType) throws Exception {
        try {

            if (file != null) {

                // Build the Blob object to persist
                Blob blob = blobStore.blobBuilder(file.getId())
                        .payload(file.getFile())
                        .contentDisposition(file.getName())
                        .contentLength(file.getFile().available())
                        .contentType(contentType)
                        .build();

                // Upload the Blob
                String eTag = blobStore.putBlob(bucket, blob, multipart());

                if (log.isDebugEnabled())
                    log.debug("put(): Successfully added the file = " + file.getId() + ", eTag = " + eTag + ", Name = " + file.getName());

            } else {
                log.error("put(): Empty file provided");
                throw new Exception("File is null");
            }
        } catch (ContainerNotFoundException e) {
            log.error("put(): Exception occurred in put(): " + e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("put(): Exception occurred in put(): " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("put(): Exception occurred in put(): " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the Blob from the binded bucket
     *
     * @param file Blobfile
     * @throws Exception
     */
    public InputStream get(BlobFile file, String range) throws Exception {

        if (range != null && !range.isEmpty()) {
            String[] r = range.split(":");
            if (r.length != 2) {
                throw new Exception("Invalid range format");
            }

            try {
                long start = Long.parseLong(r[0]);
                long end = Long.parseLong(r[1]);

                Blob blobFile = blobStore.getBlob(bucket, file.getId(), range(start, end));
                return blobFile.getPayload().openStream();

            } catch (NumberFormatException e) {
                throw new Exception("Invalid range specified ", e);
            }
        } else {
            try {
                Blob blobFile = blobStore.getBlob(bucket, file.getId());
                return blobFile.getPayload().openStream();

            } catch (Exception e) {
                log.error("Exception Occurred in get(): " + e.getMessage());
                throw e;
            }
        }

    }

    /**
     * Gets the list of available Blobs for the binded bucket from the BlobStore.
     *
     * @return List<BlobFile> List of Blobs
     */
    public List<BlobFile> get() {
        List<BlobFile> objs = new ArrayList<>();
        try {
            // Get the List from BlobStore
            PageSet<? extends StorageMetadata> list = blobStore.list(bucket);

            if (list != null) {
                log.debug("get(): Returned List count = " + list.size());

                // Iterate and form the list to be returned
                for (Iterator<? extends StorageMetadata> it = list.iterator(); it.hasNext(); ) {
                    StorageMetadata storageMetadata = it.next();
                    objs.add(new BlobFile(storageMetadata.getName(), bucket, storageMetadata.getName(), null, storageMetadata.getUri().toString()));
                }
            } else {
                log.debug("get(): Returned List is null");
            }

        } catch (Exception e) {
            log.error("Exception occurred in get(): " + e.getMessage());
            throw e;
        }

        return objs;
    }


    /**
     * Delete the Blob from the binded bucket
     *
     * @param file Blobfile to be removed
     */
    public void delete(BlobFile file) {
        try {
            blobStore.removeBlob(bucket, file.getId());
            if (log.isDebugEnabled())
                log.debug("delete(): Successfully deleted the file = " + file.getId());
        } catch (Exception e) {
            log.error("delete(): Exception Occurred in delete(): " + e.getMessage());
            throw e;
        }
    }

    /**
     *Get Object ACL
     *
     */
    public void getObjectACL(BlobFile file) {
        try {
            blobStore.removeBlob(bucket, file.getId());
            if (log.isDebugEnabled())
                log.debug("getObjectACL(): Successfully got object ACL = " + file.getId());
        } catch (Exception e) {
            log.error("delete(): Exception Occurred in delete(): " + e.getMessage());
            throw e;
        }
    }
}
