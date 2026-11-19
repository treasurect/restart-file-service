
package com.treasure.restart.file.controller;

import com.treasure.common.UserContext;
import com.treasure.common.response.BaseResponse;
import com.treasure.restart.file.entity.FileInfo;
import com.treasure.restart.file.service.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @jakarta.annotation.Resource
    private FileService fileService;

    /**
     * 文件访问URL前缀
     */
    private final String uploadDir;

    public FileController(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 普通文件上传
     * <p>
     * POST /api/file/upload
     * <p>
     * 存储目录：uploadPath
     */
    @PostMapping("/upload")
    public BaseResponse<List<String>> upload(@RequestParam("files") List<MultipartFile> files) {
        return BaseResponse.success("上传成功", processUploadFile(files, null));
    }

    /**
     * AI 图片上传
     * <p>
     * POST /api/file/upload/ai/images
     * <p>
     * 存储目录：uploadPath/ai/images
     */
    @PostMapping("/upload/ai/images")
    public BaseResponse<List<String>> uploadAIImages(@RequestParam("files") List<MultipartFile> files) {
        return BaseResponse.success("上传成功", processUploadFile(files, "ai/images"));
    }

    /**
     * AI 文件上传
     * <p>
     * POST /api/file/upload/ai/files
     * <p>
     * 存储目录：uploadPath/ai/files
     */
    @PostMapping("/upload/ai/files")
    public BaseResponse<List<String>> uploadAIFiles(@RequestParam("files") List<MultipartFile> files) {
        return BaseResponse.success("上传成功", processUploadFile(files, "ai/files"));
    }

    /**
     * 批量上传文件
     */
    private List<String> processUploadFile(List<MultipartFile> files, String dir) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        Long userId = UserContext.getUserId();

        List<String> wholeUrls = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                continue;
            }

            // 调用 Service 上传并保存数据库记录
            FileInfo fileInfo = fileService.upload(file, userId, dir);

            // 拼接文件访问地址
            String fileUrl = buildFileUrl(fileInfo.getFilePath());

            wholeUrls.add(fileUrl);
        }

        if (wholeUrls.isEmpty()) {
            throw new IllegalArgumentException("没有有效的上传文件");
        }

        return wholeUrls;
    }

    /**
     * 拼接文件访问URL
     */
    private String buildFileUrl(String filePath) {
        String prefix = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        String relativePath = filePath.replace("\\", "/");
        return prefix + relativePath;
    }

    /**
     * 文件详情
     * <p>
     * GET /api/file/{id}
     */
    @GetMapping("/{id}")
    public BaseResponse<FileInfo> getById(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        FileInfo fileInfo = fileService.getFileInfoById(id, userId);
        return BaseResponse.success("获取成功", fileInfo);
    }

    /**
     * 文件列表
     * <p>
     * GET /api/file/list
     */
    @GetMapping("/list")
    public BaseResponse<List<FileInfo>> list() {
        Long userId = UserContext.getUserId();
        List<FileInfo> files = fileService.list(userId);
        return BaseResponse.success("获取成功", files);
    }

    /**
     * 文件下载
     * <p>
     * GET /api/file/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {

        Long userId = UserContext.getUserId();

        FileInfo fileInfo = fileService.getFileInfoById(id, userId);

        Path path = fileService.getPhysicalPath(id, userId);

        Resource resource = new FileSystemResource(path);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(
                        fileInfo.getOriginalName(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileInfo.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(resource);
    }

    /**
     * 文件预览
     * <p>
     * GET /api/file/{id}/preview
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long id
    ) {

        Long userId = UserContext.getUserId();

        FileInfo fileInfo = fileService.getFileInfoById(id, userId);

        Path path = fileService.getPhysicalPath(id, userId);

        Resource resource = new FileSystemResource(path);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(
                        fileInfo.getOriginalName(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(fileInfo.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(resource);
    }

    /**
     * 删除文件
     * <p>
     * DELETE /api/file/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(
            @PathVariable Long id
    ) {

        Long userId = UserContext.getUserId();

        fileService.delete(id, userId);

        return BaseResponse.success("删除成功", null);
    }
}
