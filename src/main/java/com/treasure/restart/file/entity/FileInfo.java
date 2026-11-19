
package com.treasure.restart.file.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileInfo {

    private Long id;

    private Long ownerId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 服务器存储文件名
     */
    private String storedName;

    /**
     * 文件相对路径
     */
    private String filePath;

    /**
     * MIME 类型
     */
    private String fileType;

    /**
     * 文件大小，单位 Byte
     */
    private Long fileSize;

    private LocalDateTime createdAt;
}
