package com.back_hexiang_studio.entity;

import lombok.Data;
import lombok.extern.java.Log;

@Data
public class Teacher {
    private Long teacherId;
    private Long userId;
    private Long directionId;
    private String officeLocation;
    private String title;
}
