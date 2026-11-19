package com.treasure.restart.file.service;

import com.treasure.restart.file.entity.FileInfo;
import com.treasure.restart.file.mapper.FileMapper;

import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Resource
    private FileMapper fileMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 上传文件
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo upload(MultipartFile file, Long userId, String dir) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户身份无效");
        }

        String originalName = getOriginalName(file);
        String extension = getExtension(originalName);

        // 生成随机文件名
        String storedName = UUID.randomUUID() + extension;

        // 处理自定义目录
        Path rootPath = Paths.get(uploadPath).toAbsolutePath().normalize();

        Path relativeDir = (dir == null || dir.isBlank()) ? Paths.get("") : Paths.get(dir).normalize();

        if (relativeDir.isAbsolute() || relativeDir.startsWith("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法文件目录");
        }

        Path relativePath = relativeDir.resolve(storedName).normalize();

        Path targetPath = rootPath.resolve(relativePath).normalize();

        // 防止目录穿越
        if (!targetPath.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法文件路径");
        }

        try {
            // 创建目录
            Files.createDirectories(targetPath.getParent());

            // 保存物理文件
            Files.copy(file.getInputStream(), targetPath);

            // 保存数据库记录
            FileInfo fileInfo = new FileInfo();

            fileInfo.setOwnerId(userId);
            fileInfo.setOriginalName(originalName);
            fileInfo.setStoredName(storedName);

            // 数据库只保存相对路径，统一使用 /
            fileInfo.setFilePath(relativePath.toString().replace("\\", "/"));

            fileInfo.setFileType(file.getContentType());
            fileInfo.setFileSize(file.getSize());

            fileMapper.insert(fileInfo);

            return fileInfo;

        } catch (Exception e) {

            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
                // 保留原始异常
            }

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败", e);
        }
    }


    /**
     * 查询文件详情
     */
    @Override
    public FileInfo getFileInfoById(Long id, Long userId) {

        FileInfo fileInfo = fileMapper.selectByIdAndOwner(id, userId);

        if (fileInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }

        return fileInfo;
    }

    /**
     * 查询当前用户文件列表
     */
    @Override
    public List<FileInfo> list(Long userId) {

        return fileMapper.selectAllByOwner(userId);
    }

    /**
     * 获取文件真实路径
     */
    @Override
    public Path getPhysicalPath(Long id, Long userId) {

        FileInfo fileInfo = getFileInfoById(id, userId);

        Path rootPath = Paths.get(uploadPath).toAbsolutePath().normalize();

        Path filePath = rootPath.resolve(fileInfo.getFilePath()).normalize();

        // 再次检查路径安全
        if (!filePath.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法文件路径");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件实体不存在");
        }

        return filePath;
    }

    /**
     * 删除文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {

        FileInfo fileInfo = getFileInfoById(id, userId);

        Path rootPath = Paths.get(uploadPath).toAbsolutePath().normalize();

        Path filePath = rootPath.resolve(fileInfo.getFilePath()).normalize();

        if (!filePath.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法文件路径");
        }

        try {

            // 删除磁盘文件
            Files.deleteIfExists(filePath);

            // 删除数据库记录
            int rows = fileMapper.deleteByIdAndOwner(id, userId);

            if (rows == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件删除失败");
            }

        } catch (IOException e) {

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "磁盘文件删除失败", e);
        }
    }

    /**
     * 获取安全的原始文件名
     */
    private String getOriginalName(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        // 兼容 Windows 路径
        originalName = originalName.replace("\\", "/");
        int index = originalName.lastIndexOf("/");
        if (index >= 0) {
            originalName = originalName.substring(index + 1);
        }
        if (originalName.isBlank() || originalName.equals(".") || originalName.equals("..")) {
            return "file";
        }
        return originalName;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index);
    }
}
