package com.back_hexiang_studio.dv.dto.meterial;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

@Data
public class MaterialUploadDto {

    private List<MultipartFile> files;

    private Long categoryId;  // 改为Long类型，自动类型转换

    private String description;


    private String isPublic;
}

