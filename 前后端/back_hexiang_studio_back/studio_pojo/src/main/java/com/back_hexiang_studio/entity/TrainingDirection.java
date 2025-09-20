package com.back_hexiang_studio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDirection implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long directionId;

    private String directionName;

    private String description;
}
