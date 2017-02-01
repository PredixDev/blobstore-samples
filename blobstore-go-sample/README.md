## Blobstore Go Sample Application

This is a sample application for integrating with Predix Blobstore. This is built using Go and uses the AWS Blobstore client (S3 APIs) to connect to the store. The sample app has a simple web interface that lets you do the following:

- Add a new object (single object or multipart upload available)
- List all objects in the store
- Download an object
- Delete an object

The sample app uses Predix Blobstore to store the objects.

### Prerequisites
Go version 1.6.2 

### Steps

1. Login to Predix.
2. Create an instance of the predix-blobstore service, for example: `cf create-service predix-blobstore <plan> <my_blobstore_instance>`.
3. Clone the sample project. <p> `git clone http://github.com/PredixDev/blobstore-samples.git`
4. Change to the blobstore-go-sample sub directory. <p> `cd blobstore-samples/blobstore/blobstore-go-sample`
5. In the manifest.yml file, enter the name of your blobstore app, update the BROKER_SERVICE_NAME value with your Blobstore instance name, and add your Blobstore instance to the services section. <p>

    ```java
    applications:
      - name: blobstore-go-sample
        memory: 256M
        instances: 1
        timeout: 180
        buildpack: https://github.com/cloudfoundry/go-buildpack.git 
        env:
         BROKER_SERVICE_NAME: <my-blobstore-instance>
        services: 
         - <my-blobstore-instance>
    ```
7. From the project's home directory, push the application: `cf push`
8. View the environment variables for your application: `cf env <application_name>`
9. Copy and paste the URL that is returned in your environment variables for your application into a browser window.
10. Click Upload to upload your file. You can upload both a single object and multipart objects.


<p><b>NOTE: If you upload an object with the same file name as an existing object, the existing object is replaced by the new object.</b></p>
<p><b>NOTE: Use only the following characters for your file name:
<br>
  - Alphanumeric characters [0-9a-zA-Z]<br>
  - Special characters !, -, _, ., *, ', (, and )</b></p>
