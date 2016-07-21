/*
Copyright (c) 2016 SmartyStreets

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

NOTE: Various optional and subordinate components carry their own licensing
requirements and restrictions.  Use of those components is subject to the terms
and conditions outlined the respective license of each component.
*/

package main

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"io/ioutil"
	"net/http"
	"sort"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go/aws/request"
)

const (
	timeFormatS3   = time.RFC1123Z
	subresourcesS3 = "acl,lifecycle,location,logging,notification,partNumber,policy,requestPayment,torrent,uploadId,uploads,versionId,versioning,versions,website"
)

// SignV2 is a v2 HTTP authentication scheme that is compatible with the AWS Go SDK.
func SignV2(request *request.Request) {
	credentials, _ := request.Config.Credentials.Get()
	httpReq := request.HTTPRequest

	// Add the X-Amz-Security-Token header when using STS
	if credentials.SessionToken != "" {
		httpReq.Header.Set("X-Amz-Security-Token", credentials.SessionToken)
	}

	prepareRequestS3(httpReq)

	stringToSign := stringToSignS3(httpReq)
	signature := signatureS3(stringToSign, credentials.SecretAccessKey)

	authHeader := "AWS " + credentials.AccessKeyID + ":" + signature
	httpReq.Header.Set("Authorization", authHeader)
}

func prepareRequestS3(request *http.Request) *http.Request {

	lengthOfPayLoad := getBodyLength(request)
	if lengthOfPayLoad > 0 {
		request.ContentLength = int64(lengthOfPayLoad)
	}

	urlParts := strings.Split(request.URL.Opaque, "/")
	request.URL.Path = "/" + urlParts[len(urlParts)-1]                   // gets the path
	request.URL.Path = strings.Replace(request.URL.Path, "%20", "+", -1) // the aws v2 signing method requires that spaces be encoded as "+"

	request.Header.Set("x-amz-date", time.Now().UTC().Format(timeFormatS3))
	request.URL.Opaque = ""

	if request.URL.Path == "" {
		request.URL.Path += "/"
	}
	return request
}

func getBodyLength(request *http.Request) int {
	if request.Body == nil {
		return 0
	}
	payload, _ := ioutil.ReadAll(request.Body)
	request.Body = ioutil.NopCloser(bytes.NewReader(payload))
	return len(payload)
}

func stringToSignS3(request *http.Request) string {

	str := request.Method + "\n"

	if request.Header.Get("Content-Md5") != "" {
		str += request.Header.Get("Content-Md5")
	}

	str += "\n"

	str += request.Header.Get("Content-Type") + "\n"

	if request.Header.Get("Date") != "" {
		str += request.Header.Get("Date")
	}

	str += "\n"

	canonicalHeaders := canonicalAmzHeadersS3(request)
	if canonicalHeaders != "" {
		str += canonicalHeaders
	}

	str += canonicalResourceS3(request)

	return str
}

func canonicalAmzHeadersS3(request *http.Request) string {
	var headers []string

	for header := range request.Header {
		standardized := strings.ToLower(strings.TrimSpace(header))
		if strings.HasPrefix(standardized, "x-amz") {
			headers = append(headers, standardized)
		}
	}

	sort.Strings(headers)

	for i, header := range headers {
		headers[i] = header + ":" + strings.Replace(request.Header.Get(header), "\n", " ", -1)
	}

	if len(headers) > 0 {
		return strings.Join(headers, "\n") + "\n"
	}
	return ""
}

func canonicalResourceS3(request *http.Request) string {
	res := ""

	if request.Host == "" {
		request.Host = request.URL.Host
	}

	bucketname := strings.Split(request.Host, ".")[0]
	res += "/" + bucketname

	res += request.URL.Path

	for _, subres := range strings.Split(subresourcesS3, ",") {
		if strings.HasPrefix(request.URL.RawQuery, subres) {
			if strings.HasSuffix(request.URL.RawQuery, "=") {
				request.URL.RawQuery = strings.TrimRight(request.URL.RawQuery, "=")
			}
			res += "?" + request.URL.RawQuery
			break
		}
	}

	return res
}

func signatureS3(stringToSign string, secretAccessKey string) string {
	hashed := hmacSHA1([]byte(secretAccessKey), stringToSign)
	return base64.StdEncoding.EncodeToString(hashed)
}

func hmacSHA1(key []byte, content string) []byte {
	mac := hmac.New(sha1.New, key)
	mac.Write([]byte(content))
	return mac.Sum(nil)
}
