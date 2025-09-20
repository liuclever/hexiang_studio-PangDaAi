package com.back_hexiang_studio.mapper;

import com.back_hexiang_studio.entity.TrainingDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingDirectionMapper {

    @Select("SELECT direction_id as directionId, direction_name as directionName, description FROM training_direction ORDER BY direction_id")
    List<TrainingDirection> getAllTrainingDirections();
} 