package com.dlab.rna.upload.mapper;

import com.dlab.rna.upload.model.UploadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadTaskMapper {
    UploadTask findByFileId(@Param("fileId") String fileId);
    int insert(UploadTask task);
    int updateState(@Param("fileId") String fileId,
                    @Param("state") String state,
                    @Param("updatedAt") java.time.LocalDateTime updatedAt);
}