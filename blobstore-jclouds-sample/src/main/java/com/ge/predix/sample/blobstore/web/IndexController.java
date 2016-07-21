package com.ge.predix.sample.blobstore.web;

import com.ge.predix.sample.blobstore.entity.BlobFile;
import com.ge.predix.sample.blobstore.repository.BlobstoreService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;

/**
 * Created by 212071474 on 7/7/16.
 */
@Controller
public class IndexController {
    Log log = LogFactory.getLog(IndexController.class);

    @Autowired
    BlobstoreService objectStoreService;
    /**
     * Gets invoked for the root URL. Reads the BlobFiles from the Database
     * and updates the model.
     *
     * @param model to be updated for the view
     * @return String view name to be rendered
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("message", "Hello Boot!");

        try {
            // Get the data from database
            Iterable<BlobFile> images = objectStoreService.get();
            model.addAttribute("images", images);
        } catch (Exception e) {
            log.error(e.getMessage());
            model.addAttribute("images", new ArrayList<BlobFile>());
        }

        log.info("Model returned =" + model.toString());

        return "index";
    }

}
