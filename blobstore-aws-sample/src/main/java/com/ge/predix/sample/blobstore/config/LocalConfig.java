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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.ge.predix.sample.blobstore.repository.BlobstoreService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@Profile("default")
@EnableConfigurationProperties({BlobstoreProperties.class})
public class LocalConfig {
    Log log = LogFactory.getLog(LocalConfig.class);

    @Autowired
    private BlobstoreProperties objectStoreProperties;

    @Bean
    public BlobstoreService objectStoreService() {
        log.info("objectStoreService(): " + objectStoreProperties.getAccessKey()
                + objectStoreProperties.getSecretKey() + ", " + objectStoreProperties.getBucket());

        AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(objectStoreProperties.getAccessKey(), objectStoreProperties.getSecretKey()));
        s3Client.setEndpoint(objectStoreProperties.getUrl());

        try {
            // Remove the Credentials from the Object Store URL
            URL url = new URL(objectStoreProperties.getUrl());
            String urlWithoutCredentials = url.getProtocol() + "://" + url.getHost();

            // Return BlobstoreService
            return new BlobstoreService(s3Client, objectStoreProperties.getBucket(), urlWithoutCredentials);
        } catch (MalformedURLException e) {
            log.error("create(): Couldnt parse the URL provided by VCAP_SERVICES. Exception = " + e.getMessage());
            throw new RuntimeException("Blobstore URL is Invalid", e);
        }
    }
}
