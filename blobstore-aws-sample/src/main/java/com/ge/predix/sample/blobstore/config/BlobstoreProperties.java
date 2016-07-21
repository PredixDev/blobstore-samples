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
package com.ge.predix.sample.blobstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local Config to access BlobStore
 *
 * @since Feb 2015
 */
@ConfigurationProperties(prefix = "blobstore", locations = "classpath:application.yml")
public class BlobstoreProperties {

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String url;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String objectStoreAccessKey) {
        this.accessKey = objectStoreAccessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String objectStoreSecretKey) {
        this.secretKey = objectStoreSecretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
