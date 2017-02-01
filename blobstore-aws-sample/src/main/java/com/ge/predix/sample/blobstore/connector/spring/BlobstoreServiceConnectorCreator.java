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
package com.ge.predix.sample.blobstore.connector.spring;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.ge.predix.sample.blobstore.connector.cloudfoundry.BlobstoreServiceInfo;
import com.ge.predix.sample.blobstore.repository.BlobstoreService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.service.AbstractServiceConnectorCreator;
import org.springframework.cloud.service.ServiceConnectorConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Service Connector Creates the necessary context for Object Store.
 *
 * @since Feb 2015
 */
public class BlobstoreServiceConnectorCreator extends AbstractServiceConnectorCreator<BlobstoreService, BlobstoreServiceInfo> {

    /**
     * setting storage provider as S3
     */
    public static String STORAGE_PROVIDER = "s3";
    Log log = LogFactory.getLog(BlobstoreServiceConnectorCreator.class);

    /**
     * @param serviceInfo Object Store Service Info Object
     * @return Properties required properties
     */
    public static Properties buildProperties(BlobstoreServiceInfo serviceInfo) {
        Properties props = new Properties();
        return props;
    }

    /**
     * Creates the BlobStore context using S3Client
     *
     * @param serviceInfo Object Store Service Info Object
     * @param serviceConnectorConfig Cloud Foundry Service Connector Configuration
     *
     * @return BlobstoreService Instance of the ObjectStore Service
     */
    @Override
    public BlobstoreService create(BlobstoreServiceInfo serviceInfo, ServiceConnectorConfig serviceConnectorConfig) {
        log.info("create() invoked with serviceInfo? = " + (serviceInfo == null));
     ClientConfiguration config = new ClientConfiguration();
        config.setProtocol(Protocol.HTTPS);

        S3ClientOptions options = new S3ClientOptions();
        config.setSignerOverride("S3SignerType");

        BasicAWSCredentials creds = new BasicAWSCredentials(serviceInfo.getObjectStoreAccessKey(), serviceInfo.getObjectStoreSecretKey());
        AmazonS3Client s3Client = new AmazonS3Client(creds, config);
        s3Client.setEndpoint(serviceInfo.getUrl());
        s3Client.setS3ClientOptions(options);

        try {
            // Remove the Credentials from the Object Store URL
            URL url = new URL(serviceInfo.getUrl());
            String urlWithoutCredentials = url.getProtocol() + "://" + url.getHost();

            // Return BlobstoreService
            return new BlobstoreService(s3Client, serviceInfo.getBucket(), urlWithoutCredentials);
        } catch (MalformedURLException e) {
            log.error("create(): Couldnt parse the URL provided by VCAP_SERVICES. Exception = " + e.getMessage());
            throw new RuntimeException("Blobstore URL is Invalid", e);
        }
    }

}
