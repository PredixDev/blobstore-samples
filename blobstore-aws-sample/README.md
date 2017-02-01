## Blobstore AWS Sample Application

This is a sample application for integrating with Predix Blobstore. This is built on Spring (Spring Boot, Spring MVC) technology and uses an AWS S3 client (S3 APIs) to connect to the store. The sample app has a simple web interface that lets you do the following:

- Add a new object (single object or multipart upload available)
- List all objects in the store
- Download an object
- Delete an object   

The sample app uses Predix Blobstore to store objects and their metadata.

### Steps

1. Login to Predix.
2. Create an instance of the predix-blobstore service, for example: <p> `cf create-service predix-blobstore <plan> <my_blobstore_instance>`.
3. Clone the sample project. <p> `git clone http://github.com/PredixDev/blobstore-samples.git`
4. Change to the object-store sub directory. <p> `cd blobstore-samples/blobstore-aws-sample`
5. Run <p> `mvn clean package`
6. In the manifest.yml file, enter the name of your blobstore app, update the BROKER_SERVICE_NAME value with your Blobstore instance name, and add your Blobstore instance to the services section. <p>

    ```java
    - name: blobstore-aws-sample
        memory: 512M
        instances: 1
        path: target/blobstore-aws-sample-1.0.0-SNAPSHOT.jar
        timeout: 180
        buildpack: java_buildpack
        env:
          BROKER_SERVICE_NAME: <my-blobstore-instance>
        services:
         - <my-blobstore-instance>
    ```
7. From the project's home directory, push the application: `cf push`
8. View the environment variables for your application: `cf env <application_name>`
9. Copy and paste the URL that is returned in your environment variables for your application into a browser window.
10. Click Upload to upload your file. You can upload both single object and multipart objects.
<p> <b>NOTE: If you upload an object with the same file name as an existing object, the existing object is replaced by the new object.</b> 


