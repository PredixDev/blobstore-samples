package main

import (
	"strings"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/gorilla/mux"

	"log"

	"fmt"
	"io"
	"net/http"
)

// S3Handler contains a number of HTTP handler methods for various S3 operations.
type S3Handler struct {
	s          *session.Session
	svc        *s3.S3
	bucketName string
}

// NewS3Handler returns an instance of S3Handler.
func NewS3Handler(accessKeyID, secretAccessKey, bucketName, endpoint string) *S3Handler {

	region := "us-east-1" // The SDK requires a region. However, the endpoint will override this region.
	disableSSL := true
	logLevel := aws.LogDebugWithRequestErrors
	awsConfig := aws.Config{
		Credentials: credentials.NewStaticCredentials(accessKeyID, secretAccessKey, ""),
		Region:      &region,
		Endpoint:    &endpoint,
		DisableSSL:  &disableSSL,
		LogLevel:    &logLevel,
	}

	s := session.New(&awsConfig)

	svc := s3.New(s)

	svc.Handlers.Sign.Clear()
	svc.Handlers.Sign.PushBack(SignV2)

	return &S3Handler{
		s:          s,
		svc:        svc,
		bucketName: bucketName,
	}
}

// BucketHandler handles requests at the bucket level.
func (s *S3Handler) BucketHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case "POST":
		contentType := r.Header.Get("Content-Type")
		if strings.ToUpper(contentType) == "MULTIPART/FORM-DATA" {
			fmt.Println("start multipart")
			r.ParseMultipartForm(32 << 20)
			file, handler, err := r.FormFile("file")
			if err != nil {
				log.Println(err)
				w.WriteHeader(http.StatusBadRequest)
				return
			}
			defer file.Close()
			fileName := handler.Filename
			contentType = handler.Header.Get("Content-Type")

			uploader := s3manager.NewUploader(s.s)
			svc := uploader.S3.(*s3.S3) // in multiPartUpload we don't use the s.svc
			svc.Handlers.Sign.Clear()
			svc.Handlers.Sign.PushBack(SignV2)

			result, err := uploader.Upload(&s3manager.UploadInput{
				Body:   file,
				Bucket: &s.bucketName,
				Key:    &fileName,
				ContentType: &contentType,
			})

			if err != nil {
				log.Println("Failed to upload.", "Error:", err)
			}

			log.Println("Successfully uploaded data.", "FileName:", fileName, "uploadID:", result.UploadID)
			fmt.Println("end multi end")
			http.Redirect(w, r, "/", http.StatusFound)
			return

		}
		r.ParseMultipartForm(32 << 20)
		file, handler, err := r.FormFile("file")
		if err != nil {
			log.Println(err)
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		defer file.Close()

		fileName := handler.Filename

		contentType = handler.Header.Get("Content-Type")
		_, err = s.svc.PutObject(&s3.PutObjectInput{
			Body:   file,
			Bucket: &s.bucketName,
			Key:    &fileName,
			ContentType: &contentType,
		})

		if err != nil {
			log.Println("failed to upload data to", "", s.bucketName, "/", fileName, "Error:", err)
			return
		}
		http.Redirect(w, r, "/", http.StatusFound)

	case "GET":
		params := &s3.ListObjectsInput{
			Bucket: &s.bucketName, // Required
		}
		resp, err := s.svc.ListObjects(params)
		if err != nil {
			log.Println(err)
		}
		var files []string
		for _, file := range resp.Contents {
			files = append(files, *file.Key)
		}
		fmt.Println("End Get All Objects")
		http.Redirect(w, r, "/", http.StatusOK)
	default:
		w.WriteHeader(http.StatusMethodNotAllowed)
	}

}

// ObjectHandler accepts a file from the request and adds that file to the bucket.
func (s *S3Handler) ObjectHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case "GET":
		fmt.Println("Begin Get Object")
		fileName := mux.Vars(r)["fileName"]
		fmt.Println("Get Object", "filename", fileName)

		input := &s3.GetObjectInput{
			Bucket: &s.bucketName,
			Key:    &fileName,
		}

		resp, err := s.svc.GetObject(input)
		if err != nil {
			log.Println(err)
		}
		defer resp.Body.Close()

		w.Header().Set("Content-Disposition", "attachment; filename="+fileName)
		w.Header().Set("Content-Type", *resp.ContentType)
		io.Copy(w, resp.Body)
		fmt.Println("End Get Object")

	case "DELETE":
		fileName := mux.Vars(r)["fileName"]

		params := &s3.DeleteObjectInput{
			Bucket: &s.bucketName,
			Key:    &fileName,
		}

		_, err := s.svc.DeleteObject(params)
		if err != nil {
			log.Println(err)
			return
		}

		http.Redirect(w, r, "/", http.StatusFound)

	default:
		w.WriteHeader(http.StatusMethodNotAllowed)
	}
}

// MultiPartUpload does the same as PutObject except that it first breaks
// apart the file into pieces before sending the file to the bucket. This
// is better for larger files.
func (s *S3Handler) MultiPartUpload(w http.ResponseWriter, r *http.Request) {
}
