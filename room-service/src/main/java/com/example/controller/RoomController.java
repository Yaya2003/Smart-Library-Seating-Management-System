package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.common.util.ArrayUtil;
import com.example.domain.po.Reservation;
import com.example.domain.po.Room;
import com.example.domain.vo.PageResponseVO;
import com.example.mapper.ReservationMapper;
import com.example.service.RoomService;
import com.google.gson.Gson;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
        System.out.println(room.getSeatInfo());
        if (room.getRoomId() == null) {
            int[][] seatInfo = new int[room.getRow()][room.getColumn()];
            for (int[] strings : seatInfo) {
                Arrays.fill(strings, -1);
            }
            room.setSeatInfo(gson.toJson(seatInfo));
        } else if (room.getSeatInfo()!= null) {
            Room oldRoom = roomService.getById(room.getRoomId());
            int[][] seats = gson.fromJson(oldRoom.getSeatInfo(), int[][].class);
            int[][] newSeats = ArrayUtil.adjustArraySize(seats, room.getRow(), room.getColumn());
            ArrayUtil.copy2DArray(gson.fromJson(room.getSeatInfo(), int[][].class), newSeats);
            room.setSeatInfo(gson.toJson(newSeats));
        } else {
            Room oldRoom = roomService.getById(room.getRoomId());
            int[][] seats = gson.fromJson(oldRoom.getSeatInfo(), int[][].class);
            int[][] newSeats = ArrayUtil.adjustArraySize(seats, room.getRow(), room.getColumn());
            room.setSeatInfo(gson.toJson(newSeats));
        }
        roomService.saveOrUpdate(room);
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
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        List<String> roles = userSession.getRoles();
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
    public ResultView<?> getRoomsByReservation(@RequestParam Integer floor,
                                               @RequestParam(required = false) String date,
                                               @RequestParam(required = false) String time) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getDate, date)
                .eq(Reservation::getTime, time));
        List<Room> rooms = roomService.list(new LambdaQueryWrapper<Room>().eq(Room::getFloor, floor));
        for (Reservation reservation : reservations) {
            for (Room room : rooms) {
                if (room.getRoomId().equals(reservation.getRoomId())) {
                    String seatInfo = room.getSeatInfo();
                    int[][] seatArray = gson.fromJson(seatInfo, int[][].class);
                    seatArray[reservation.getRow()][reservation.getColumn()] = (Objects.equals(id, reservation.getUserId()) ? 3 : 1);
                    room.setSeatInfo(gson.toJson(seatArray));
                }
            }
        }
        return ResultView.success(rooms);
    }
}
