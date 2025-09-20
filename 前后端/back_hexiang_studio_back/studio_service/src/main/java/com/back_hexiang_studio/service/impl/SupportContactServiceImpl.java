package com.back_hexiang_studio.service.impl;

import com.back_hexiang_studio.entity.SupportContact;
import com.back_hexiang_studio.mapper.SupportContactMapper;
import com.back_hexiang_studio.service.SupportContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupportContactServiceImpl implements SupportContactService {
    
    @Autowired
    private SupportContactMapper supportContactMapper;
    
    @Override
    public List<SupportContact> getAllActiveContacts() {
        return supportContactMapper.getAllActiveContacts();
    }
} 