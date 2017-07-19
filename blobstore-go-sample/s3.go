package main

import (
	"regexp"
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
	enableSSE  bool
}

// NewS3Handler returns an instance of S3Handler.
func NewS3Handler(accessKeyID, secretAccessKey, bucketName, endpoint string, enableSSE bool) *S3Handler {
	region, forcePathStyle := regionAndPathStyleFromEndpoint(endpoint)
	disableSSL := true

	logLevel := aws.LogDebugWithRequestErrors
	awsConfig := aws.Config{
		Credentials:      credentials.NewStaticCredentials(accessKeyID, secretAccessKey, ""),
		Region:           &region,
		Endpoint:         &endpoint,
		DisableSSL:       &disableSSL,
		LogLevel:         &logLevel,
		S3ForcePathStyle: &forcePathStyle,
	}

	s := session.New(&awsConfig)

	svc := s3.New(s)

	return &S3Handler{
		s:          s,
		svc:        svc,
		bucketName: bucketName,
		enableSSE:  enableSSE,
	}
}

func regionAndPathStyleFromEndpoint(endpoint string) (string, bool) {
	re := regexp.MustCompile("^[0-9.:]*$")
	if re.MatchString(endpoint) {
		fmt.Println("forcing path style")
		return "us-east-1", true
	}

	if strings.Contains(endpoint, "amazonaws") {
		if strings.HasPrefix(endpoint, "s3.") {
			return "us-east-1", false
		}
		if strings.HasPrefix(endpoint, "s3-") {
			splitEndpoint := strings.Split(endpoint, ".")
			return strings.TrimPrefix(splitEndpoint[0], "s3-"), false
		}
	}

	return "us-east-1", false

}

// BucketHandler handles requests at the bucket level.
func (s *S3Handler) BucketHandler(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case "POST":
		contentType := r.Header.Get("Content-Type")
		if strings.Contains(strings.ToUpper(contentType), "MULTIPART/FORM-DATA") {
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

			uploadInput := s3manager.UploadInput{
				Body:        file,
				Bucket:      &s.bucketName,
				Key:         &fileName,
				ContentType: &contentType,
			}
			if s.enableSSE {
				uploadInput.ServerSideEncryption = &sse
			}

			result, err := uploader.Upload(&uploadInput)

			if err != nil {
				log.Println("Failed to upload.", "Error:", err)
				w.WriteHeader(http.StatusInternalServerError)
				return
			}
			log.Println("Successfully uploaded data.", "FileName:", fileName, "uploadID:", result.UploadID)

			out, err := s.svc.HeadObject(&s3.HeadObjectInput{
				Bucket: &s.bucketName,
				Key:    &fileName,
			})
			if err != nil {
				log.Println(err)
			}
			fmt.Println("Object Metadata -- ", out.GoString())

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

		objectInput := s3.PutObjectInput{
			Body:        file,
			Bucket:      &s.bucketName,
			Key:         &fileName,
			ContentType: &contentType,
		}

		if s.enableSSE {
			objectInput.ServerSideEncryption = &sse
		}

		res, err := s.svc.PutObject(&objectInput)

		if res.ServerSideEncryption != nil {
			fmt.Println("Object Metadata -- ", res.GoString())
		}

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

		if resp.ServerSideEncryption != nil {
			log.Println("Get Object server side encryption", *resp.ServerSideEncryption)
		}

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
