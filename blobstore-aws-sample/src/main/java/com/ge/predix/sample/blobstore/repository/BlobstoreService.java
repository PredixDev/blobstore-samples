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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


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
    private AmazonS3Client s3Client;

    /**
     * Name of the bucket created in BlobStore
     */
    private String bucket;

    /**
     * BlobStore Endpoint
     */
    private String url;

    public BlobstoreService(AmazonS3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public BlobstoreService(AmazonS3Client s3Client, String bucket, String url) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.url = url;
    }


    /**
     * Adds a new Blob to the binded bucket in the Object Store
     *
     * @param obj S3Object to be added
     * @throws Exception
     */
    public void put(S3Object obj) throws Exception {
        if (obj == null) {
            log.error("put(): Empty file provided");
            throw new Exception("File is null");
        }
        InputStream is = obj.getObjectContent();

        List<PartETag> partETags = new ArrayList<>();

        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, obj.getKey());
        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
        try {

            int i = 1;
            int currentPartSize = 0;
            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            int byteValue;
            while ((byteValue = is.read()) != -1) {
                tempBuffer.write(byteValue);
                currentPartSize = tempBuffer.size();
                if (currentPartSize == (50 * 1024 * 1024)) //make this a const
                {
                    byte[] b = tempBuffer.toByteArray();
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

                    UploadPartRequest uploadPartRequest = new UploadPartRequest()
                            .withBucketName(bucket).withKey(obj.getKey())
                            .withUploadId(initResponse.getUploadId()).withPartNumber(i++)
                            .withInputStream(byteStream)
                            .withPartSize(currentPartSize);
                    partETags.add(s3Client.uploadPart(uploadPartRequest).getPartETag());

                    tempBuffer.reset();
                }
            }
            log.info("currentPartSize: " + currentPartSize);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(currentPartSize);
            obj.setObjectMetadata(objectMetadata);

            if (i == 1 && currentPartSize < (5 * 1024 * 1024)) // make this a const
            {
                s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                        bucket, obj.getKey(), initResponse.getUploadId()
                ));

                byte[] b = tempBuffer.toByteArray();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

                PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, obj.getKey(), byteStream, obj.getObjectMetadata());
                s3Client.putObject(putObjectRequest);
                return;
            }

            if (currentPartSize > 0 && currentPartSize <= (50 * 1024 * 1024)) // make this a const
            {
                byte[] b = tempBuffer.toByteArray();
                ByteArrayInputStream byteStream = new ByteArrayInputStream(b);

                log.info("currentPartSize: " + currentPartSize);
                log.info("byteArray: " + b);

                UploadPartRequest uploadPartRequest = new UploadPartRequest()
                        .withBucketName(bucket).withKey(obj.getKey())
                        .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withInputStream(byteStream)
                        .withPartSize(currentPartSize);
                partETags.add(s3Client.uploadPart(uploadPartRequest).getPartETag());
            }
        } catch (Exception e) {
            log.error("put(): Exception occurred in put(): " + e.getMessage());
            s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                    bucket, obj.getKey(), initResponse.getUploadId()
            ));
            throw e;
        }
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest()
                .withBucketName(bucket)
                .withPartETags(partETags)
                .withUploadId(initResponse.getUploadId())
                .withKey(obj.getKey());

        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
    }

    /**
     * Get the Blob from the binded bucket
     *
     * @param fileName String
     * @throws Exception
     */
    public InputStream get(String fileName, String range) throws Exception {

        if (range != null && !range.isEmpty()) {
            String[] r = range.split(":");
            if (r.length != 2) {
                throw new Exception("Invalid range format");
            }

            try {
                long start = Long.parseLong(r[0]);
                long end = Long.parseLong(r[1]);

                GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucket, fileName);
                rangeObjectRequest.setRange(start, end);
                S3Object objectPortion = s3Client.getObject(rangeObjectRequest);

                InputStream objectData = objectPortion.getObjectContent();

                return objectData;

            } catch (NumberFormatException e) {
                throw new Exception("Invalid range specified ", e);
            }
        } else {
            try {
                S3Object object = s3Client.getObject(new GetObjectRequest(bucket, fileName));
                InputStream objectData = object.getObjectContent();

                return objectData;

            } catch (Exception e) {
                log.error("Exception Occurred in get(): " + e.getMessage());
                throw e;
            }
        }

    }

//    public AccessControlList getObjectACL()

    /**
     * Gets the list of available Blobs for the binded bucket from the BlobStore.
     *
     * @return List<BlobFile> List of Blobs
     */
    public List<S3Object> get() {
        List<S3Object> objs = new ArrayList<>();
        try {
            // Get the List from BlobStore
            ObjectListing objectList = s3Client.listObjects(bucket);

            for (S3ObjectSummary objectSummary :
                    objectList.getObjectSummaries()) {
                objs.add(s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey())));
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
     * @param fileName String of file to be removed
     */
    public void delete(String fileName) {
        try {
            s3Client.deleteObject(bucket, fileName);
            if (log.isDebugEnabled())
                log.debug("delete(): Successfully deleted the file = " + fileName);
        } catch (Exception e) {
            log.error("delete(): Exception Occurred in delete(): " + e.getMessage());
            throw e;
        }
    }
}
