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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

import java.util.Map;
import java.util.StringTokenizer;

/**
 * Standard Implementation to create a Service Info.
 *
 * @since Feb 2015
 */
public class BlobstoreServiceInfoCreator extends CloudFoundryServiceInfoCreator<BlobstoreServiceInfo> {

    /**
     * Name of the service instance to bind
     */
    private static String OBJECT_STORE_SERVICE_NAME = System.getenv("BROKER_SERVICE_NAME");
    Log log = LogFactory.getLog(BlobstoreServiceInfoCreator.class);

    public BlobstoreServiceInfoCreator() {
        super(new Tags(OBJECT_STORE_SERVICE_NAME));
    }

    /**
     * Reads the VCAP_SERVICES and generates the Service Info Object
     *
     * @param serviceData injected by CloudFoundry with the metadata for the service
     * @return BlobstoreServiceInfo Service Info Object
     */
    @Override
    public BlobstoreServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> credentials = (Map<String, Object>) serviceData.get("credentials");

        String id = (String) serviceData.get("name");
        String objectStoreAccessKey = (String) credentials.get("access_key_id");
        String objectStoreSecretKey = (String) credentials.get("secret_access_key");
        String endPointWithBucket = (String) credentials.get("url");
        String host = (String) credentials.get("host");
        String bucket = (String) credentials.get("bucket_name");

        // Extract protocol for endpoint without bucket name
        StringTokenizer st = new StringTokenizer(endPointWithBucket, "://");
        String protocol = "";
        if (st.hasMoreTokens()) {
            protocol = st.nextToken();
        }

        String url = "";
        url = protocol + "://" + host;

        BlobstoreServiceInfo objectStoreInfo = new BlobstoreServiceInfo(id, objectStoreAccessKey, objectStoreSecretKey, bucket, url);
        log.info("createServiceInfo(): " + objectStoreInfo);

        return objectStoreInfo;
    }

    /**
     * Method that specifies the service to look for in VCAP_SERVICES.
     */
    @Override
    public boolean accept(Map<String, Object> serviceData) {
        if (log.isDebugEnabled())
            log.debug("accept(): invoked with service data? = " + (serviceData == null));
        String name = (String) serviceData.get("name");
        return name.startsWith(OBJECT_STORE_SERVICE_NAME);
    }

}
