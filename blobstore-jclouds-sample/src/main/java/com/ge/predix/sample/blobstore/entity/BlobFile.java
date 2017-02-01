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
package com.ge.predix.sample.blobstore.entity;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class BlobFile {

    /**
     * Unique generated for the file
     */
    private String id;
    /**
     * Bucket Name
     */
    private String bucket;

    /**
     * Original File Name
     */
    private String name;

    /**
     * ObjectStore URL to the location of the file
     */
    private String url;

    /**
     * Actual File Object (as InputStream)
     */
    private InputStream file;

    public BlobFile() {
    }

    public BlobFile(String id, String bucket, String name, InputStream file) {
        this.id = id;
        this.bucket = bucket;
        this.name = name;
        this.file = file;
    }

    public BlobFile(String id, String bucket, String name, InputStream file,
                    String url) {
        this.id = id;
        this.bucket = bucket;
        this.name = name;
        this.file = file;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBucket() {
        return bucket;
    }

    public String getName() {
        return name;
    }

    public InputStream getFile() {
        return file;
    }

    public URL getUrl() throws MalformedURLException {
        return new URL(url);
    }

    @Override
    public String toString() {
        return "BlobFile [id=" + id + ", bucket=" + bucket + ", name=" + name
                + ", url=" + url + "]";
    }
}