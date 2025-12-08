package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.po.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    long countByCreatedAtBetween(@Param("date") String date,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}

