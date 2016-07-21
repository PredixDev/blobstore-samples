package com.ge.predix.sample.blobstore.web;

import com.ge.predix.sample.blobstore.entity.BlobFile;
import com.ge.predix.sample.blobstore.repository.BlobstoreService;
import com.wordnik.swagger.annotations.Api;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Primary Controller for the BlobStore Demo app
 *
 * @since Feb 2015
 */
@RestController
@RequestMapping("/v1")
@Api(value = "/v1", description = "Blobstore operations")
public class BlobstoreController {

    Log log = LogFactory.getLog(BlobstoreController.class);

    @Autowired
    BlobstoreService objectStoreService;

    /**
     * Delete a Blob File from the database and Object Store
     *
     * @param id name of the Blob to be deleted
     * @return String view name to be rendered
     */
    @RequestMapping(value = "/blob/{id:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<InputStreamResource> deleteFile(@PathVariable(value="id") String id) {

        if (id != null) {
            try {
                BlobFile blobFile = new BlobFile();
                blobFile.setId(id);
                objectStoreService.delete(blobFile);
                log.info(id + " deleted from ObjectStore.");
            } catch (Exception e) {
                log.error("deleteFile(): Exception occurred : " + e.getMessage());
                throw e;
            }
        }

        return new ResponseEntity<InputStreamResource>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get a Blob File from the Object Store
     *
     * @param id name of the Blob to be download
     * @return String view name to be rendered
     * @throws Exception
     */
    @RequestMapping(value = "/blob/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getFile(@PathVariable(value = "id") String id,
                                                       @RequestParam(value = "range", required = false) String range)
            throws Exception {

        log.info("Get file : " + id);
        if (id != null) {
            try {
                HttpHeaders respHeaders = new HttpHeaders();

                    BlobFile blobFile = new BlobFile();
                    blobFile.setId(id);
                    log.info("Get the file : " + blobFile);

                    if (blobFile != null) {
                        respHeaders.setContentDispositionFormData("attachment", blobFile.getId());
                        InputStreamResource isr = new InputStreamResource(objectStoreService.get(blobFile, range));
                        return new ResponseEntity<InputStreamResource>(isr, respHeaders, HttpStatus.OK);
                    } else {
                        throw new Exception("Unable to find BlobFile in the repo.");
                    }


            } catch (Exception e) {
                log.error("getFile(): Exception occurred : " + e.getMessage());
                throw e;
            }
        }

        // Default to 200, when input is missing
        return new ResponseEntity<InputStreamResource>(HttpStatus.OK);
    }

    /**
     * Handles uploading the BlobFile to the Object Store.
     *
     * @param file to be uploaded
     * @return String view name to be rendered
     * @throws Exception
     */
    @RequestMapping(value = "/blob", method = RequestMethod.POST)
    public ResponseEntity<InputStreamResource> handleFileUpload(@RequestParam("file") MultipartFile file) throws Exception {

        if (file != null) {

            String filename = file.getOriginalFilename();
            String blobFileId = filename;


            BlobFile blobFile = objectStoreService.createBlobFileObject(blobFileId, filename, file.getInputStream());


            try {
                objectStoreService.put(blobFile);
                log.info(blobFile.getName() + " put to ObjectStore.");
            } catch (Exception e) {
                log.error("handleFileUpload();: Exception occurred : " + e.getMessage());
                throw e;
            }
            log.info("handleFileUpload(): Successfully uploaded");
        }

        return new ResponseEntity<InputStreamResource>(HttpStatus.OK);

    }
}
