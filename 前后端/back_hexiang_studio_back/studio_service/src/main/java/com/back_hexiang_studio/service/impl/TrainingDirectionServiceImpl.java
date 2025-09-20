package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.TrainingDirection;
import com.back_hexiang_studio.mapper.TrainingDirectionMapper;
import com.back_hexiang_studio.service.TrainingDirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainingDirectionServiceImpl implements TrainingDirectionService {

    @Autowired
    private TrainingDirectionMapper trainingDirectionMapper;

    @Override
    public List<TrainingDirection> getAllTrainingDirections() {
        return trainingDirectionMapper.getAllTrainingDirections();
    }
} 