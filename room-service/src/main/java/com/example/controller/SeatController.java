package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.result.ResultView;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.po.Reservation;
import com.example.domain.po.Seat;
import com.example.domain.po.Room;
import com.example.mapper.ReservationMapper;
import com.example.service.RoomService;
import com.example.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@RestController
@RequestMapping("/room/seat")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private Gson gson;

    /**
     * 简单列表，支持按房间过滤
     */
    @GetMapping("/list")
    public ResultView<?> list(@RequestParam(required = false) Long roomId) {
        List<Seat> seats = seatService.list(new LambdaQueryWrapper<Seat>()
                .eq(roomId != null, Seat::getRoomId, roomId));
        return ResultView.success(seats);
    }

    /**
     * 分页列表，支持房间和名称模糊查询
     */
    @GetMapping("/page")
    public ResultView<?> page(@RequestParam(defaultValue = "1") long page,
                              @RequestParam(defaultValue = "10") long size,
                              @RequestParam(required = false) Long roomId,
                              @RequestParam(required = false) String name) {
        Page<Seat> p = new Page<>(page, size);
        Page<Seat> result = seatService.page(p, new LambdaQueryWrapper<Seat>()
                .eq(roomId != null, Seat::getRoomId, roomId)
                .like(StringUtils.hasText(name), Seat::getName, name));
        return ResultView.success(result);
    }

    /**
     * 房间布局：返回该房间所有座位状态和二维码更新时间，供前端绘制布局
     */
    @GetMapping("/layout")
    public ResultView<?> layout(@RequestParam Long roomId) {
        List<Seat> seats = seatService.list(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId));
        return ResultView.success(seats);
    }

    @GetMapping("/{id}")
    public ResultView<?> detail(@PathVariable Long id) {
        return ResultView.success(seatService.getById(id));
    }

    /**
     * 保存/编辑座位，带唯一性校验
     */
    @PostMapping("/save")
    public ResultView<?> saveOrUpdateSeat(@RequestBody @Valid Seat seat) {
        try {
            seatService.saveSeatWithValidate(seat);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return ResultView.success(true);
    }

    /**
     * 返回当前二维码 token 以及可用链接（若未传 baseUrl 则返回前端路径）
     */
    @GetMapping("/{id}/qrLink")
    public ResultView<?> qrLink(@PathVariable Long id,
                                @RequestParam(required = false) String baseUrl) {
        Seat seat = seatService.getById(id);
        if (seat == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "座位不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("qrToken", seat.getQrToken());
        data.put("qrUpdatedAt", seat.getQrUpdatedAt());
        // 默认生成的前端路径
        String link = StringUtils.hasText(baseUrl)
                ? baseUrl + "?seatId=" + seat.getId() + "&token=" + seat.getQrToken()
                : "/seat-reservation?seatId=" + seat.getId() + "&token=" + seat.getQrToken();
        data.put("link", link);
        return ResultView.success(data);
    }

    @PostMapping("/{id}/refreshQr")
    public ResultView<?> refreshQr(@PathVariable Long id) {
        try {
            String token = seatService.refreshQrToken(id);
            Map<String, Object> data = new HashMap<>();
            data.put("qrToken", token);
            data.put("qrUpdatedAt", new Date());
            data.put("link", "/seat-reservation?seatId=" + id + "&token=" + token);
            return ResultView.success(data);
        } catch (IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResultView<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            seatService.updateStatus(id, status);
            return ResultView.success(true);
        } catch (IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    /**
     * 从 room.seat_info 添加桌子或座位，并同步到 seat 表（座位时）
     */
    @PostMapping("/layout/add")
    public ResultView<?> addFromLayout(@RequestBody LayoutAddRequest request) {
        if (request.roomId() == null || request.rowNo() == null || request.colNo() == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间与行列不能为空");
        }
        Room room = roomService.getById(request.roomId());
        if (room == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在");
        }
        int rows = room.getRow() == null ? 0 : room.getRow();
        int cols = room.getColumn() == null ? 0 : room.getColumn();
        int[][] seatInfo;
        try {
            seatInfo = gson.fromJson(room.getSeatInfo(), int[][].class);
        } catch (Exception e) {
            seatInfo = null;
        }
        if (seatInfo == null || seatInfo.length == 0) {
            seatInfo = new int[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    seatInfo[i][j] = -1;
                }
            }
        }
        if (request.rowNo() >= seatInfo.length || request.colNo() >= seatInfo[0].length) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "行列超出房间布局范围");
        }

        int current = seatInfo[request.rowNo()][request.colNo()];
        // type: seat/desk
        if ("desk".equalsIgnoreCase(request.type())) {
            if (current == 0 || current == 2) {
                return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该位置已存在桌面/座位标记");
            }
            seatInfo[request.rowNo()][request.colNo()] = 2; // 2 表示桌子
            room.setSeatInfo(gson.toJson(seatInfo));
            roomService.updateById(room);
            return ResultView.success(true);
        }

        // seat
        if (current == 2) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该位置为桌子，不能新增座位");
        }
        Seat seat = new Seat();
        seat.setRoomId(request.roomId());
        seat.setRowNo(request.rowNo());
        seat.setColNo(request.colNo());
        seat.setName(StringUtils.hasText(request.name()) ? request.name() : "R" + request.rowNo() + "C" + request.colNo());
        seat.setStatus(1);
        seatService.saveSeatWithValidate(seat);

        seatInfo[request.rowNo()][request.colNo()] = 0; // 0 表示可预约座位
        room.setSeatInfo(gson.toJson(seatInfo));
        roomService.updateById(room);

        return ResultView.success(seat);
    }

    /**
     * 删除座位：校验未完成预约，清理布局标记
     */
    @DeleteMapping("/{id}")
    public ResultView<?> delete(@PathVariable Long id) {
        Seat seat = seatService.getById(id);
        if (seat == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "座位不存在");
        }

        // 阻止删除有未完成预约的座位
        Long activeCount = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, id)
                .in(Reservation::getStatus,
                        ReservationStatus.PENDING.name(),
                        ReservationStatus.CHECKED_IN.name()));
        if (activeCount != null && activeCount > 0) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该座位存在未完成预约，无法删除");
        }

        Room room = roomService.getById(seat.getRoomId());
        if (room == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在");
        }

        // 重置 seat_info 中对应坐标为 -1
        int[][] seatInfo;
        try {
            seatInfo = gson.fromJson(room.getSeatInfo(), int[][].class);
        } catch (Exception e) {
            seatInfo = null;
        }
        if (seatInfo != null && seat.getRowNo() != null && seat.getColNo() != null
                && seat.getRowNo() >= 0 && seat.getColNo() >= 0
                && seat.getRowNo() < seatInfo.length
                && seat.getColNo() < seatInfo[0].length) {
            seatInfo[seat.getRowNo()][seat.getColNo()] = -1;
            room.setSeatInfo(gson.toJson(seatInfo));
            roomService.updateById(room);
        }

        seatService.removeById(id);
        return ResultView.success(true);
    }

    private record LayoutAddRequest(Long roomId, Integer rowNo, Integer colNo, String type, String name) {
    }

    /**
     * 删除桌子：仅修改 room.seat_info 标记
     */
    @PostMapping("/layout/removeDesk")
    public ResultView<?> removeDesk(@RequestBody LayoutRemoveRequest request) {
        if (request.roomId() == null || request.rowNo() == null || request.colNo() == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间与行列不能为空");
        }
        Room room = roomService.getById(request.roomId());
        if (room == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在");
        }
        int[][] seatInfo;
        try {
            seatInfo = gson.fromJson(room.getSeatInfo(), int[][].class);
        } catch (Exception e) {
            seatInfo = null;
        }
        if (seatInfo == null || request.rowNo() >= seatInfo.length || request.colNo() >= seatInfo[0].length) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "行列超出房间布局范围");
        }
        if (seatInfo[request.rowNo()][request.colNo()] != 2) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "当前位置不是桌子，无法删除");
        }
        seatInfo[request.rowNo()][request.colNo()] = -1;
        room.setSeatInfo(gson.toJson(seatInfo));
        roomService.updateById(room);
        return ResultView.success(true);
    }

    /**
     * 批量导出房间座位二维码压缩包（可选行列过滤）
     */
    @PostMapping("/exportQrZip")
    public ResponseEntity<byte[]> exportQrZip(@RequestBody QrExportRequest request) {
        if (request.roomId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("roomId required".getBytes());
        }
        List<Seat> seats = seatService.list(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, request.roomId));
        if (seats.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("no seats for roomId " + request.roomId).getBytes());
        }
        // 过滤指定行列
        if (request.positions != null && !request.positions.isEmpty()) {
            seats = seats.stream().filter(seat -> request.positions.stream().anyMatch(p ->
                    p != null && p.rowNo != null && p.colNo != null
                            && p.rowNo.equals(seat.getRowNo())
                            && p.colNo.equals(seat.getColNo())
            )).toList();
            if (seats.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("no seats match requested positions".getBytes());
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Seat seat : seats) {
                // 确保二维码 token 存在
                if (!StringUtils.hasText(seat.getQrToken())) {
                    seatService.refreshQrToken(seat.getId());
                    seat = seatService.getById(seat.getId());
                }
                String link = StringUtils.hasText(request.baseUrl)
                        ? request.baseUrl + "?seatId=" + seat.getId() + "&token=" + seat.getQrToken()
                        : "/seat-reservation?seatId=" + seat.getId() + "&token=" + seat.getQrToken();
                BufferedImage qrImg = buildQrWithText(link, seat.getName());
                ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(qrImg, "png", imgOut);
                ZipEntry entry = new ZipEntry((StringUtils.hasText(seat.getName()) ? seat.getName() : ("seat-" + seat.getId())) + ".png");
                zos.putNextEntry(entry);
                zos.write(imgOut.toByteArray());
                zos.closeEntry();
            }
            zos.finish();
            zos.flush();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("export error: " + e.getMessage()).getBytes());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=seat-qr-room-" + request.roomId + ".zip");
        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    private record LayoutRemoveRequest(Long roomId, Integer rowNo, Integer colNo) {
    }

    /**
     * 删除座位标记（无 seat 记录时使用）
     */
    @PostMapping("/layout/removeSeat")
    public ResultView<?> removeSeatMark(@RequestBody LayoutRemoveRequest request) {
        if (request.roomId() == null || request.rowNo() == null || request.colNo() == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间与行列不能为空");
        }
        Room room = roomService.getById(request.roomId());
        if (room == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在");
        }
        // 若该位置已有 seat 记录，提示用删除座位接口
        Seat existed = seatService.getOne(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, request.roomId())
                .eq(Seat::getRowNo, request.rowNo())
                .eq(Seat::getColNo, request.colNo())
                .last("limit 1"));
        if (existed != null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该位置有座位记录，请在座位详情中删除");
        }
        int[][] seatInfo;
        try {
            seatInfo = gson.fromJson(room.getSeatInfo(), int[][].class);
        } catch (Exception e) {
            seatInfo = null;
        }
        if (seatInfo == null || request.rowNo() >= seatInfo.length || request.colNo() >= seatInfo[0].length) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "行列超出房间布局范围");
        }
        int current = seatInfo[request.rowNo()][request.colNo()];
        if (current != 0 && current != 1) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "当前位置不是座位标记");
        }
        seatInfo[request.rowNo()][request.colNo()] = -1;
        room.setSeatInfo(gson.toJson(seatInfo));
        roomService.updateById(room);
        return ResultView.success(true);
    }

    /**
     * 批量删除座位/桌子标记；若有 seat 记录则同步删除
     */
    @PostMapping("/layout/batchDelete")
    public ResultView<?> batchDelete(@RequestBody BatchDeleteRequest request) {
        if (request.roomId == null || request.positions == null || request.positions.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间与位置不能为空");
        }
        Room room = roomService.getById(request.roomId);
        if (room == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间不存在");
        }
        int[][] seatInfo;
        try {
            seatInfo = gson.fromJson(room.getSeatInfo(), int[][].class);
        } catch (Exception e) {
            seatInfo = null;
        }
        if (seatInfo == null || seatInfo.length == 0) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "房间布局为空");
        }
        for (Position p : request.positions) {
            if (p == null || p.rowNo == null || p.colNo == null) {
                continue;
            }
            if (p.rowNo < 0 || p.colNo < 0 || p.rowNo >= seatInfo.length || p.colNo >= seatInfo[0].length) {
                continue;
            }
            // 删除 seat 记录（若存在）
            Seat seat = seatService.getOne(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getRoomId, request.roomId)
                    .eq(Seat::getRowNo, p.rowNo)
                    .eq(Seat::getColNo, p.colNo)
                    .last("limit 1"));
            if (seat != null) {
                seatService.removeById(seat.getId());
            }
            // 无论桌子/座位，统一置为 -1
            seatInfo[p.rowNo][p.colNo] = -1;
        }
        room.setSeatInfo(gson.toJson(seatInfo));
        roomService.updateById(room);
        return ResultView.success(true);
    }

    private static class BatchDeleteRequest {
        public Long roomId;
        public List<Position> positions;
    }

    private static class Position {
        public Integer rowNo;
        public Integer colNo;
    }

    private static class QrExportRequest {
        public Long roomId;
        public String baseUrl;
        public List<Position> positions;
    }

    private BufferedImage buildQrWithText(String content, String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 280, 280, hints);
        BufferedImage qr = MatrixToImageWriter.toBufferedImage(matrix);
        int padding = 40;
        BufferedImage merged = new BufferedImage(qr.getWidth(), qr.getHeight() + padding, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = merged.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, merged.getWidth(), merged.getHeight());
        g.drawImage(qr, 0, 0, null);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String display = StringUtils.hasText(text) ? text : "Seat";
        FontMetrics fm = g.getFontMetrics();
        int x = Math.max(0, (merged.getWidth() - fm.stringWidth(display)) / 2);
        g.drawString(display, x, qr.getHeight() + (padding + fm.getAscent()) / 2);
        g.dispose();
        return merged;
    }
}
