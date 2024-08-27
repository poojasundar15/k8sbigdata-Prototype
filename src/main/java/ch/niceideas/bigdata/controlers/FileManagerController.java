package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.utils.Pair;
import ch.niceideas.bigdata.services.FileManagerService;
import ch.niceideas.bigdata.services.FileManagerServiceImpl;
import ch.niceideas.bigdata.types.Node;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Controller
public class FileManagerController {

    private static final Logger logger = Logger.getLogger(FileManagerController.class);

    @Autowired
    private FileManagerService fileManagerService;

    @GetMapping("/file-manager-remove")
    @ResponseBody
    public String removeFileManager(@RequestParam("nodeAddress") String nodeAddress) {
        try {
            fileManagerService.removeFileManager(Node.fromAddress(nodeAddress));

            return ReturnStatusHelper.createOKStatus();
        } catch (JSONException e) {
            logger.error (e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @GetMapping("/file-manager-connect")
    @ResponseBody
    public String connectFileManager(@RequestParam("nodeAddress") String nodeAddress) {

       return navigateFileManager(nodeAddress, "/", ".");

    }

    @GetMapping("/file-manager-navigate")
    @ResponseBody
    public String navigateFileManager(
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("subFolder") String subFolder) {

        try {
            Pair<String, JSONObject> result = fileManagerService.navigateFileManager(Node.fromAddress(nodeAddress), folder, subFolder);

            return ReturnStatusHelper.createOKStatus (map -> {
                map.put("folder", result.getKey());
                map.put("content", result.getValue());
            });
        } catch (IOException e) {
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @GetMapping("/file-manager-create-file")
    @ResponseBody
    public String createFile(
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("fileName") String fileName) {

        try {
            Pair<String, JSONObject> result = fileManagerService.createFile(Node.fromAddress(nodeAddress), folder, fileName);

            return ReturnStatusHelper.createOKStatus (map -> {
                map.put("folder", result.getKey());
                map.put("content", result.getValue());
            });

        } catch (IOException e) {
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @GetMapping("/file-manager-open-file")
    @ResponseBody
    public String openFile(
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("file") String file) {

        try {
            return fileManagerService.openFile(Node.fromAddress(nodeAddress), folder, file).toString(2);

        } catch (JSONException e) {
            logger.error (e, e);
            return ReturnStatusHelper.createErrorStatus(e);

        }
    }

    @GetMapping(value = "/file-manager-download/{filename}")
    public void downloadFile(
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("file") String file,
            HttpServletResponse response) {
        response.setHeader("Content-Disposition", "attachment");
        fileManagerService.downloadFile (Node.fromAddress(nodeAddress), folder, file,
                new FileManagerServiceImpl.HttpServletResponseAdapter() {

            @Override
            public void setContentType(String type) {
                response.setContentType(type);
            }

            @Override
            public ServletOutputStream getOutputStream() throws IOException {
                return response.getOutputStream();
            }
        });
        try {
            response.flushBuffer();
        } catch (IOException ex) {
            logger.error("Download error. Filename was " + file, ex);
            throw new FileManagerServiceImpl.FileDownloadException("Download error. Filename was " + file, ex);
        }
    }

    @GetMapping(value = "/file-manager-delete")
    @ResponseBody
    public String deletePath (
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("file") String file) {

        try {
            String deletedPath =  fileManagerService.deletePath(Node.fromAddress(nodeAddress), folder, file);

            return ReturnStatusHelper.createOKStatus (map -> map.put("path", deletedPath));

        } catch (IOException e) {
            logger.error (e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @PostMapping("/file-manager-upload")
    @ResponseBody
    public String handleFileUpload(
            @RequestParam("nodeAddress") String nodeAddress,
            @RequestParam("folder") String folder,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {

        try {
            fileManagerService.uploadFile (Node.fromName(nodeAddress), folder, filename, file.getInputStream());

            return ReturnStatusHelper.createOKStatus (map -> map.put("file", file.getName()));

        } catch (IOException e) {
            logger.error (e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }
}
