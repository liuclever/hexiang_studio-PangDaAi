package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.dv.vo.StudioInfoVo;
import com.back_hexiang_studio.entity.StudioInfo;
import com.back_hexiang_studio.mapper.StudioInfoMapper;
import com.back_hexiang_studio.service.StudioInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudioInfoServiceImpl implements StudioInfoService {

    @Autowired
    private StudioInfoMapper studioInfoMapper;
    
    /**
     * 获取工作室信息
     * @return 工作室信息
     */
    @Override
    public StudioInfoVo getStudioInfo() {
        return studioInfoMapper.getStudioInfo();
    }
    
    /**
     * 更新工作室信息
     * @param studioInfo 工作室信息
     */
    @Override
    public void updateStudioInfo(StudioInfo studioInfo) {
        studioInfoMapper.updateStudioInfo(studioInfo);
    }
} 