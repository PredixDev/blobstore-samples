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
package com.ge.predix.sample.blobstore.connector.cloudfoundry;

import org.springframework.cloud.service.BaseServiceInfo;

/**
 * Represents the ServiceInfo Object for the ObjectStore
 *
 * @since Feb 2015
 */
public class BlobstoreServiceInfo extends BaseServiceInfo {

    /**
     * Access Key for Object Store
     */
    private String objectStoreAccessKey;

    /**
     * Secret Key for Object Store
     */
    private String objectStoreSecretKey;

    /**
     * Bucket name
     */
    private String bucket;

    /**
     * Object Store URL
     */
    private String url;

    public BlobstoreServiceInfo(String id, String objectStoreAccessKey, String objectStoreSecretKey, String bucket) {
        super(id);
        this.objectStoreAccessKey = objectStoreAccessKey;
        this.objectStoreSecretKey = objectStoreSecretKey;
        this.bucket = bucket;
    }

    public BlobstoreServiceInfo(String id, String objectStoreAccessKey, String objectStoreSecretKey,
                                String bucket, String url) {
        super(id);
        this.objectStoreAccessKey = objectStoreAccessKey;
        this.objectStoreSecretKey = objectStoreSecretKey;
        this.bucket = bucket;
        this.url = url;
    }

    @ServiceProperty
    public String getObjectStoreAccessKey() {
        return objectStoreAccessKey;
    }

    @ServiceProperty
    public String getObjectStoreSecretKey() {
        return objectStoreSecretKey;
    }

    @ServiceProperty
    public String getBucket() {
        return bucket;
    }

    @ServiceProperty
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "BlobstoreServiceInfo [objectStoreAccessKey="
                + objectStoreAccessKey + ", bucket=" + bucket + ", url=" + url
                + "]";
    }
}
