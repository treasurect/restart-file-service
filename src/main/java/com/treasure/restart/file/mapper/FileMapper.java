
package com.treasure.restart.file.mapper;

import com.treasure.restart.file.entity.FileInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileMapper {

    /**
     * 新增文件
     */
    int insert(FileInfo fileInfo);

    /**
     * 根据文件ID查询当前用户文件
     */
    FileInfo selectByIdAndOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * 查询当前用户文件列表
     */
    List<FileInfo> selectAllByOwner(@Param("ownerId") Long ownerId);

    /**
     * 删除当前用户文件记录
     */
    int deleteByIdAndOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
