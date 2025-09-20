package com.back_hexiang_studio.controller.admin;

import com.back_hexiang_studio.entity.TrainingDirection;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.TrainingDirectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/training-directions")
@Slf4j
public class TrainingDirectionController {

    @Autowired
    private TrainingDirectionService trainingDirectionService;

    /**
     * 获取所有培训方向
     * @return
     */
    @GetMapping("/list")
    public Result<List<TrainingDirection>> getAllTrainingDirections() {
        log.info("获取所有培训方向");
        List<TrainingDirection> list = trainingDirectionService.getAllTrainingDirections();
        return Result.success(list);
    }
} 