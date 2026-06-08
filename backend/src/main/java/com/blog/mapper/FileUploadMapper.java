package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.FileUpload;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileUploadMapper extends BaseMapper<FileUpload> {
}
