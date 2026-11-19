
package com.treasure.restart.file.service;

import com.treasure.restart.file.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface FileService {

    FileInfo upload(MultipartFile file, Long userId, String dir);

    FileInfo getFileInfoById(Long id, Long userId);

    List<FileInfo> list(Long userId);

    Path getPhysicalPath(Long id, Long userId);

    void delete(Long id, Long userId);
}
