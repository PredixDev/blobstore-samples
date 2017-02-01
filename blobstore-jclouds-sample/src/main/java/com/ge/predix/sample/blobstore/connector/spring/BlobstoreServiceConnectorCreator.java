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

import com.ge.predix.sample.blobstore.connector.cloudfoundry.BlobstoreServiceInfo;
import com.ge.predix.sample.blobstore.repository.BlobstoreService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.springframework.cloud.service.AbstractServiceConnectorCreator;
import org.springframework.cloud.service.ServiceConnectorConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.jclouds.Constants.PROPERTY_RELAX_HOSTNAME;
import static org.jclouds.Constants.PROPERTY_TRUST_ALL_CERTS;
import static org.jclouds.s3.reference.S3Constants.PROPERTY_S3_VIRTUAL_HOST_BUCKETS;

/**
 * Service Connector Creates the necessary context for Object Store.
 *
 * @since Feb 2015
 */
public class BlobstoreServiceConnectorCreator extends AbstractServiceConnectorCreator<BlobstoreService, BlobstoreServiceInfo> {

    /**
     * JCloud APIs uses provider "aws-s3".
     */
    public static String STORAGE_PROVIDER = "s3";
    Log log = LogFactory.getLog(BlobstoreServiceConnectorCreator.class);

    /**
     *
     * Properties include:
     * PROPERTY_TRUST_ALL_CERTS - Trust all certificates (including Selfsigned)
     * PROPERTY_RELAX_HOSTNAME - Optional host name check
     *
     * @param serviceInfo Object Store Service Info Object
     * @return Properties required properties
     */
    public static Properties buildProperties(BlobstoreServiceInfo serviceInfo) {
        Properties props = new Properties();
        props.setProperty(PROPERTY_TRUST_ALL_CERTS, "true");
        props.setProperty(PROPERTY_RELAX_HOSTNAME, "true");
        props.setProperty(PROPERTY_S3_VIRTUAL_HOST_BUCKETS, "true");
        props.setProperty("jclouds.mpu.parallel.degree", "4");

        return props;
    }

    /**
     * Creates the BlobStore context using JCloud APIs
     *
     * @param serviceInfo Object Store Service Info Object
     * @param serviceConnectorConfig Cloud Foundry Service Connector Configuration
     *
     * @return BlobstoreService Instance of the ObjectStore Service
     */
    @Override
    public BlobstoreService create(BlobstoreServiceInfo serviceInfo, ServiceConnectorConfig serviceConnectorConfig) {
        log.info("create() invoked with serviceInfo? = " + (serviceInfo == null));

        // Initialize the BlobStoreContext
        BlobStoreContext context = ContextBuilder.newBuilder(STORAGE_PROVIDER)
                .overrides(buildProperties(serviceInfo))
                .endpoint(serviceInfo.getUrl())
                .credentials(serviceInfo.getObjectStoreAccessKey(), serviceInfo.getObjectStoreSecretKey())
                .buildView(BlobStoreContext.class);

        // Access the BlobStore
        BlobStore blobStore = context.getBlobStore();

        try {
            // Remove the Credentials from the Object Store URL
            URL url = new URL(serviceInfo.getUrl());
            String urlWithoutCredentials = url.getProtocol() + "://" + url.getHost();

            // Return BlobstoreService
            return new BlobstoreService(blobStore, serviceInfo.getBucket(), urlWithoutCredentials);
        } catch (MalformedURLException e) {
            log.error("create(): Couldnt parse the URL provided by VCAP_SERVICES. Exception = " + e.getMessage());
            throw new RuntimeException("Blobstore URL is Invalid", e);
        }
    }

}
