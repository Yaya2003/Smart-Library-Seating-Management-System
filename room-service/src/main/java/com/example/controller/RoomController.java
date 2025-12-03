package com.example.controller;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.po.Reservation;
import com.example.domain.po.Room;
import com.example.domain.vo.PageResponseVO;
import com.example.mapper.ReservationMapper;
import com.example.service.RoomService;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = "/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private Gson gson;

    @PostMapping(path = "/saveRoom")
    public ResultView<?> saveRoom(@RequestBody @Valid Room room) {
        if (room.getRoomId() == null) {
            // 新建：seat_info 未提供时初始化为空格
            if (room.getSeatInfo() == null || room.getSeatInfo().isBlank()) {
                int[][] seatInfo = new int[room.getRow()][room.getColumn()];
                for (int[] strings : seatInfo) {
                    Arrays.fill(strings, -1);
                }
                room.setSeatInfo(gson.toJson(seatInfo));
            }
            room.setCreateAt(new Date());
            room.setUpdateAt(new Date());
            roomService.save(room);
        } else {
            // 更新：仅更新基础字段，不触碰 seat_info，避免大字段锁和覆盖
            LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<Room>()
                    .eq(Room::getRoomId, room.getRoomId())
                    .set(room.getRoomName() != null, Room::getRoomName, room.getRoomName())
                    .set(room.getFloor() != null, Room::getFloor, room.getFloor())
                    .set(room.getRow() != null, Room::getRow, room.getRow())
                    .set(room.getColumn() != null, Room::getColumn, room.getColumn())
                    .set(room.getState() != null, Room::getState, room.getState())
                    .set(Room::getUpdateAt, new Date());
            boolean ok = roomService.update(wrapper);
            if (!ok) {
                return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在或更新失败");
            }
        }
        return ResultView.success(true);
    }

    @GetMapping(path = "/getRoomList")
    public ResultView<?> getRoomList(@RequestParam Integer current,
                                     @RequestParam Integer size,
                                     @RequestParam(required = false) Integer floor ,
                                     @RequestParam(required = false) String roomName,
                                     @RequestParam(required = false) String state) {
        Page<Room> page = Page.of(current, size);
        Page<Room> roomPage = roomService.page(page, new LambdaQueryWrapper<Room>()
                .eq(floor != null, Room::getFloor, floor)
                .like(roomName != null, Room::getRoomName, roomName)
                .eq(state != null, Room::getState, state));
        System.out.println(roomPage.getRecords());
        return ResultView.success(new PageResponseVO<>(roomPage.getTotal(), roomPage.getSize(), current, roomPage.getRecords()));
    }

    @GetMapping(path = "/getAllRooms")
    public ResultView<?> getAllRooms() {
        List<Room> rooms = roomService.list();
        return ResultView.success(rooms);
    }

    @DeleteMapping(path = "/deleteRoom/{roomId}")
    public ResultView<?> deleteRoom(@PathVariable Long roomId) {
        System.out.println("delete roomId: " + roomId);
        roomService.removeById(roomId);
        return ResultView.success(true);
    }

    @DeleteMapping(path = "/deleteBatchRoom")
    public ResultView<?> deleteBatchRoom(@RequestBody List<Long> roomIds) {
        roomService.removeByIds(roomIds);
        System.out.println(roomIds);
        return ResultView.success(true);
    }

    @GetMapping(path = "/getFloors")
    public ResultView<?> getFloors() {
        List<Room> list = roomService.list();
        List<Integer> floors = list.stream().map(Room::getFloor).distinct().sorted().toList();
        return ResultView.success(floors);
    }

    @GetMapping(path = "/getRoomsByFloor")
    public ResultView<?> getRoomsByFloor(@RequestParam Integer floor) {
        UserSession userSession = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
            if (principal instanceof UserSession us) {
                userSession = us;
            }
        } catch (Exception ignored) {
        }
        Long id = userSession != null ? userSession.getUserId() : null;
        List<String> roles = userSession != null ? userSession.getRoles() : List.of();
        if (roles.contains("R_USER")) {
            List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getUserId, id));
            List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getFloor, floor));
            for (Reservation reservation : reservations) {
                for (Room room : rooms) {
                    if (room.getRoomId().equals(reservation.getRoomId())) {
                        String seatInfo = room.getSeatInfo();
                        int[][] seatArray = gson.fromJson(seatInfo, int[][].class);
                        seatArray[reservation.getRow()][reservation.getColumn()] = 3;
                        room.setSeatInfo(gson.toJson(seatArray));
                    }
                }
            }
            return ResultView.success(rooms);
        }

        List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getFloor, floor));
         return ResultView.success(rooms);
    }

    @GetMapping(path = "/getRoomsByReservation")
    public ResultView<?> getRoomsByReservation(@RequestParam(required = false) Integer floor,
                                               @RequestParam(required = false) String date,
                                               @RequestParam(required = false) String time) {
        UserSession userSession = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
            if (principal instanceof UserSession us) {
                userSession = us;
            }
        } catch (Exception ignored) {
        }
        Long id = userSession != null ? userSession.getUserId() : null;
        List<String> activeStatuses = List.of(
                ReservationStatus.PENDING.name(),
                ReservationStatus.CHECKED_IN.name()
        );
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(StringUtils.hasText(date), Reservation::getDate, date)
                .eq(StringUtils.hasText(time), Reservation::getTime, time)
                .in(Reservation::getStatus, activeStatuses));
        List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>()
                .eq(floor != null, Room::getFloor, floor));
        if (rooms.isEmpty()) {
            return ResultView.success(rooms);
        }
        for (Reservation reservation : reservations) {
            for (Room room : rooms) {
                if (room.getRoomId().equals(reservation.getRoomId())) {
                    String seatInfo = room.getSeatInfo();
                    int[][] seatArray = gson.fromJson(seatInfo, int[][].class);
                    seatArray[reservation.getRow()][reservation.getColumn()] = (id != null && Objects.equals(id, reservation.getUserId()) ? 3 : 1);
                    room.setSeatInfo(gson.toJson(seatArray));
                }
            }
        }
        return ResultView.success(rooms);
    }
}
