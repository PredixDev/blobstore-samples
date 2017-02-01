/*
	blobstore-go-sample provides a sample app which demonstrates common operations that can be performed with a
	Predix Blobstore service instance.

	When started, an HTTP server is launched hosting a single page application. This application allows list,
	put, get, delete, multi-part upload operations on bucket and objects in Blobstore.

	Note: This sample is built to deploy to Cloud Foundry and will not work locally.
*/

package main

import (
	"fmt"
	"net/http"
	"os"
	"text/template"
	"time"

	"log"

	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/cloudfoundry-community/go-cfenv"
	"github.com/gorilla/mux"
)

const (
	accessKeyID     = "access_key_id"
	secretAccessKey = "secret_access_key"
	host            = "host"
	bucketName      = "bucket_name"
)

var (
	blobstoreServiceInstanceName = os.Getenv("BROKER_SERVICE_NAME")
	blobstoreAccessKeyID         string
	blobstoreSecretAccessKey     string
	blobstoreBucketName          string
	blobstoreHost                string
)

func main() {
	if blobstoreServiceInstanceName == "" {
		log.Println("BROKER_SERVICE_NAME not set")
		time.Sleep(5 * time.Second)
		os.Exit(2)
	}

	appEnv, err := cfenv.Current()
	if err != nil {
		log.Println("Error getting CF env:", err, ". Shutting down..")
		time.Sleep(5 * time.Second)
		os.Exit(3)
	}

	for _, v := range appEnv.Services {
		for _, service := range v {
			if service.Name == blobstoreServiceInstanceName {
				blobstoreAccessKeyID = service.Credentials[accessKeyID].(string)
				blobstoreSecretAccessKey = service.Credentials[secretAccessKey].(string)
				blobstoreHost = service.Credentials[host].(string)
				blobstoreBucketName = service.Credentials[bucketName].(string)
			}
		}
	}

	s3Handler := NewS3Handler(blobstoreAccessKeyID, blobstoreSecretAccessKey, blobstoreBucketName, blobstoreHost)

	r := mux.NewRouter()
	r.HandleFunc("/", s3Handler.serveTemplate)
	r.HandleFunc("/blob", s3Handler.BucketHandler)
	r.HandleFunc("/blob/{fileName}", s3Handler.ObjectHandler)
	r.PathPrefix("/static/").Handler(http.StripPrefix("/static/", http.FileServer(http.Dir("./static/"))))
	r.Handle("/", r)

	log.Println(http.ListenAndServe(":"+os.Getenv("PORT"), r))
}

func (s *S3Handler) serveTemplate(w http.ResponseWriter, r *http.Request) {
	fmt.Println("serving template")
	params := &s3.ListObjectsInput{
		Bucket: &blobstoreBucketName, // Required
	}
	resp, err := s.svc.ListObjects(params)
	if err != nil {
		log.Println(err)
	}
	var files []string
	for _, file := range resp.Contents {
		files = append(files, *file.Key)
	}
	t, err := template.ParseFiles("static/index.html")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	t.Execute(w, files)
}
