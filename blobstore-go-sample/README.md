## Blobstore Go Sample Application

This is a sample application for integrating with Predix Blobstore. This is built using Go and uses the AWS Blobstore client (S3 APIs) to connect to the store. The sample app has a simple web interface that lets you do the following:

- Add a new object (single object or multipart upload available)
- List all objects in the store
- Download an object
- Delete an object

The sample app uses the Predix Blobstore to store the objects.

### Prerequisites
Go version 1.7.5 

**Note**: We recommend `aws-go-sdk v1.4.3`. Other versions are not necessarily supported.

### Steps

1. Login to Predix.
2. Create an instance of the predix-blobstore service, for example: `cf create-service predix-blobstore <plan> <my_blobstore_instance>`.
3. Clone the sample project. <p> `git clone http://github.com/PredixDev/blobstore-samples.git`
4. Change to the blobstore-go-sample sub directory. <p> `cd blobstore-samples/blobstore/blobstore-go-sample`
5. In the manifest.yml file:
    - Enter the name of your blobstore app
    - Update the BROKER_SERVICE_NAME value with your Blobstore instance name
    - Update the ENABLE_SERVER_SIDE_ENCRYPTION value to indicate whether or not to enable server side encryption
    - Add your Blobstore instance to the services section
    ```java
    applications:
      - name: blobstore-go-sample
        memory: 256M
        instances: 1
        timeout: 180
        buildpack: https://github.com/cloudfoundry/go-buildpack.git
        env:
         BROKER_SERVICE_NAME: <my-blobstore-instance>
         ENABLE_SERVER_SIDE_ENCRYPTION: false
        services:
         - <my-blobstore-instance>
    ```
7. From the project's home directory, push the application: `cf push`
8. View the environment variables for your application: `cf env <application_name>`
9. Copy and paste the URL that is returned in your environment variables for your application into a browser window.
10. Click Upload to upload your file. You can upload both a single object and multipart objects.


<p><b>NOTE: If you upload an object with the same file name as an existing object, the existing object is replaced by the new object.</b></p>
<p><b>NOTE: If you change the ENABLE_SERVER_SIDE\_ENCRYPTION environment variable, use `cf restart` instead of `cf restage` for the change take effect</b></p>
<p><b>NOTE: Use only the following characters for your file name:
<br>
  - Alphanumeric characters [0-9a-zA-Z]<br>
  - Special characters !, -, _, ., *, ', (, and )</b></p>
