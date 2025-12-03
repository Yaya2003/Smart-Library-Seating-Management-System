package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.po.Seat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {
}
