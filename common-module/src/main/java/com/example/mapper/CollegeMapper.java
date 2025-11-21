package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.po.College;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CollegeMapper extends BaseMapper<College> {
}
