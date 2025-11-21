package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.domain.po.ClassInfo;
import com.example.mapper.ClassInfoMapper;
import com.example.service.ClassInfoService;
import org.springframework.stereotype.Service;

@Service
public class ClassInfoServiceImpl extends ServiceImpl<ClassInfoMapper, ClassInfo> implements ClassInfoService {
}
