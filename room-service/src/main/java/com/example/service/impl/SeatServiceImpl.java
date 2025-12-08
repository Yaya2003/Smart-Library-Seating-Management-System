package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.domain.po.Seat;
import com.example.mapper.SeatMapper;
import com.example.service.SeatService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {

    @Override
    public String refreshQrToken(Long seatId) {
        Seat seat = this.getById(seatId);
        if (seat == null) {
            throw new IllegalStateException("座位不存在");
        }
        String newToken = UUID.randomUUID().toString().replace("-", "");
        seat.setQrToken(newToken);
        seat.setQrUpdatedAt(new Date());
        this.updateById(seat);
        return newToken;
    }

    @Override
    public void updateStatus(Long seatId, Integer status) {
        Seat seat = this.getById(seatId);
        if (seat == null) {
            throw new IllegalStateException("座位不存在");
        }
        if (!Objects.equals(status, seat.getStatus())) {
            seat.setStatus(status);
            seat.setUpdatedAt(new Date());
            this.updateById(seat);
        }
    }

    @Override
    public void saveSeatWithValidate(Seat seat) {
        if (seat.getRoomId() == null) {
            throw new IllegalArgumentException("房间不能为空");
        }
        if (!StringUtils.hasText(seat.getName())) {
            if (seat.getRowNo() != null && seat.getColNo() != null) {
                seat.setName("R" + seat.getRowNo() + "C" + seat.getColNo());
            }
        }
        if (!StringUtils.hasText(seat.getName())) {
            throw new IllegalArgumentException("座位名称不能为空");
        }
        Long currentId = seat.getId();
        Long count = this.lambdaQuery()
                .eq(Seat::getRoomId, seat.getRoomId())
                .eq(Seat::getName, seat.getName())
                .ne(currentId != null, Seat::getId, currentId)
                .count();
        if (count != null && count > 0) {
            throw new IllegalStateException("同一房间内座位名称已存在");
        }

        // 行列唯一
        if (seat.getRowNo() != null && seat.getColNo() != null) {
            Long rowCount = this.lambdaQuery()
                    .eq(Seat::getRoomId, seat.getRoomId())
                    .eq(Seat::getRowNo, seat.getRowNo())
                    .eq(Seat::getColNo, seat.getColNo())
                    .ne(currentId != null, Seat::getId, currentId)
                    .count();
            if (rowCount != null && rowCount > 0) {
                throw new IllegalStateException("该行列位置已存在座位");
            }
        }

        Date now = new Date();
        if (seat.getId() == null) {
            seat.setStatus(seat.getStatus() == null ? 1 : seat.getStatus());
            if (!StringUtils.hasText(seat.getQrToken())) {
                seat.setQrToken(UUID.randomUUID().toString().replace("-", ""));
            }
            if (seat.getQrUpdatedAt() == null) {
                seat.setQrUpdatedAt(now);
            }
            seat.setCreatedAt(now);
        }
        seat.setUpdatedAt(now);
        this.saveOrUpdate(seat);
    }
}
