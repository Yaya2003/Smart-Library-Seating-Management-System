package com.example.controller;

import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.po.Reservation;
import com.example.domain.po.Seat;
import com.example.domain.vo.ReservationVO;
import com.example.mapper.SeatMapper;
import com.example.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatMapper seatMapper;

    /**
     * 用户创建预约（兼容旧前端：可传 seatId 或 roomId+row+column，以及 timeSlot 或 time 的中文值）
     */
    @PostMapping("/saveReservation")
    public ResultView<?> saveReservation(@RequestBody SaveReservationRequest request) {
        if (request == null || !StringUtils.hasText(request.date())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "缺少参数");
        }
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        Reservation reservation = new Reservation();
        reservation.setSeatId(request.seatId());
        reservation.setRoomId(request.roomId());
        reservation.setRow(request.row());
        reservation.setColumn(request.column());
        reservation.setDate(request.date());
        reservation.setTimeSlot(request.timeSlot());
        reservation.setTime(request.time());
        try {
            reservationService.createReservation(reservation, userSession);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return ResultView.success(true);
    }

    /**
     * 用户取消预约（兼容旧前端：可传 seatId 或 roomId+row+column+date+time/timeSlot）
     */
    @DeleteMapping("/deleteReservation")
    public ResultView<?> deleteReservation(@RequestBody @Valid Reservation reservation) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN")
                        || userSession.getRoles().contains("R_TEACHER")
                        || userSession.getRoles().contains("R_SUPER"));
        Long targetId = reservation.getReservationId();
        if (targetId == null) {
            String slotCode = normalizeTimeSlot(reservation.getTimeSlot(), reservation.getTime());
            Long seatId = reservation.getSeatId();
            if (seatId == null && reservation.getRoomId() != null && reservation.getRow() != null && reservation.getColumn() != null) {
                Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getRoomId, reservation.getRoomId())
                        .eq(Seat::getRowNo, reservation.getRow())
                        .eq(Seat::getColNo, reservation.getColumn()));
                if (seat != null) {
                    seatId = seat.getId();
                }
            }
            Reservation match = reservationService.getOne(new LambdaQueryWrapper<Reservation>()
                    .eq(seatId != null, Reservation::getSeatId, seatId)
                    .eq(reservation.getRoomId() != null, Reservation::getRoomId, reservation.getRoomId())
                    .eq(Reservation::getUserId, userSession.getUserId())
                    .eq(StringUtils.hasText(reservation.getDate()), Reservation::getDate, reservation.getDate())
                    .eq(StringUtils.hasText(slotCode), Reservation::getTimeSlot, slotCode)
                    .eq(Reservation::getStatus, ReservationStatus.PENDING.name())
                    .last("limit 1"));
            if (match != null) {
                targetId = match.getReservationId();
            }
        }
        if (targetId == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "未找到可取消的预约");
        }
        // 非管理员需要满足时间限制：开始前 10 分钟以上才能取消
        Reservation db = reservationService.getById(targetId);
        if (db == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "预约不存在或已被取消");
        }
        if (!canUserCancelByTime(db, isAdmin)) {
            return ResultView.error(HttpStatus.FORBIDDEN.value(), "距离开始时间不足10分钟，无法取消该预约");
        }

        reservationService.cancelReservation(targetId, userSession, isAdmin);
        return ResultView.success(true);
    }

    /**
     * 普通用户取消自己的预约 (通过路径参数指定预约ID)
     */
    @DeleteMapping("/user/cancel/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResultView<?> cancelReservationByUser(@PathVariable Long reservationId) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }

        try {
            Reservation db = reservationService.getById(reservationId);
            if (db == null) {
                return ResultView.error(HttpStatus.BAD_REQUEST.value(), "预约不存在或已被取消");
            }
            if (!canUserCancelByTime(db, false)) {
                return ResultView.error(HttpStatus.FORBIDDEN.value(), "距离开始时间不足10分钟，无法取消该预约");
            }

            reservationService.cancelReservation(reservationId, userSession, false);
        } catch (IllegalStateException e) {
            return ResultView.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "取消预约失败: " + e.getMessage());
        }

        return ResultView.success(true);
    }

    /**
     * 当前用户预约列表
     */
    @GetMapping("/getReservationList")
    public ResultView<?> getReservationList() {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        Long userId = userSession.getUserId();
        List<ReservationVO> list = reservationService.getReservation(userId);
        return ResultView.success(list);
    }

    /**
     * 查询当前用户活跃预约数量（PENDING、CHECKED_IN），供前端做按钮控制
     */
    @GetMapping("/activeCount")
    public ResultView<?> activeCount() {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.success(0L);
        }
        Long userId = userSession.getUserId();
        List<String> activeStatuses = Arrays.asList(ReservationStatus.PENDING.name(), ReservationStatus.CHECKED_IN.name());
        long count = reservationService.count(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, activeStatuses));
        return ResultView.success(count);
    }

    /**
     * 扫码签到接口
     */
    @PostMapping("/checkin")
    public ResultView<?> checkin(@RequestBody @Valid CheckinRequest request) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        try {
            reservationService.checkIn(request.getReservationId(), request.getSeatId(), request.getQrToken(), userSession);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return ResultView.success(true);
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public ResultView<?> detail(@PathVariable Long id) {
        Reservation res = reservationService.getById(id);
        if (res == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "预约不存在");
        }
        return ResultView.success(res);
    }

    /**
     * 管理端列表（过滤查询）
     */
    @PostMapping("/admin/list")
    public ResultView<?> listByAdmin(@RequestBody(required = false) Reservation probe) {
        List<ReservationVO> list = reservationService.listAll(probe);
        return ResultView.success(list);
    }

    /**
     * 管理端列表（GET 便于无请求体调用）
     */
    @GetMapping("/admin/list")
    public ResultView<?> listByAdminGet(@RequestParam(required = false) Long roomId,
                                        @RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Long seatId,
                                        @RequestParam(required = false) String date,
                                        @RequestParam(required = false) String timeSlot,
                                        @RequestParam(required = false) String status) {
        Reservation probe = new Reservation();
        probe.setRoomId(roomId);
        probe.setUserId(userId);
        probe.setSeatId(seatId);
        probe.setDate(date);
        probe.setTimeSlot(timeSlot);
        probe.setStatus(status);
        List<ReservationVO> list = reservationService.listAll(probe);
        return ResultView.success(list);
    }

    /**
     * 管理端保存/编辑预约
     */
    @PostMapping("/admin/save")
    public ResultView<?> saveByAdmin(@RequestBody @Valid Reservation reservation) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        try {
            reservationService.saveByAdmin(reservation, userSession);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return ResultView.success(true);
    }

    /**
     * 管理端批量预约（管理员/老师）
     */
    @PostMapping("/admin/batchSave")
    public ResultView<?> batchSave(@RequestBody BatchSaveRequest request) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }
        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN") || userSession.getRoles().contains("R_TEACHER") || userSession.getRoles().contains("R_SUPER"));
        if (!isAdmin) {
            return ResultView.error(HttpStatus.FORBIDDEN.value(), "无权批量预约");
        }
        if (request == null || request.reservations == null || request.reservations.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "预约列表不能为空");
        }
        int success = 0;
        StringBuilder failed = new StringBuilder();
        for (SingleReserve r : request.reservations) {
            if (r == null || r.seatId() == null || !StringUtils.hasText(r.date()) || !StringUtils.hasText(r.timeSlot())) {
                failed.append("参数缺失;");
                continue;
            }
            Reservation reservation = new Reservation();
            reservation.setSeatId(r.seatId());
            reservation.setDate(r.date());
            reservation.setTimeSlot(r.timeSlot());
            if (StringUtils.hasText(r.status())) {
                reservation.setStatus(r.status());
            }
            if (r.userId() != null) {
                reservation.setUserId(r.userId());
            }
            try {
                reservationService.saveByAdmin(reservation, userSession);
                success++;
            } catch (Exception e) {
                failed.append("seat ").append(r.seatId()).append(" ").append(e.getMessage()).append(";");
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        data.put("failed", failed.toString());
        return ResultView.success(data);
    }

    /**
     * 管理端删除
     */
    @DeleteMapping("/admin/delete/{id}")
    public ResultView<?> deleteByAdmin(@PathVariable Long id) {
        reservationService.deleteByAdmin(id);
        return ResultView.success(true);
    }

    /**
     * 管理端批量删除
     */
    @PostMapping("/admin/batchDelete")
    public ResultView<?> batchDeleteByAdmin(@RequestBody @Valid BatchDeleteRequest request) {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.error(HttpStatus.UNAUTHORIZED.value(), "未登录");
        }

        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN") ||
                        userSession.getRoles().contains("R_TEACHER") ||
                        userSession.getRoles().contains("R_SUPER"));
        if (!isAdmin) {
            return ResultView.error(HttpStatus.FORBIDDEN.value(), "无权批量删除预约");
        }

        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "预约ID不能为空");
        }
        reservationService.batchDeleteByAdmin(request.getIds());
        return ResultView.success(true);
    }

    /**
     * 批量取消/释放
     */
    @PostMapping("/batch/status")
    public ResultView<?> batchUpdateStatus(@RequestBody @Valid BatchStatusRequest request) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN")
                        || userSession.getRoles().contains("R_TEACHER")
                        || userSession.getRoles().contains("R_SUPER"));
        try {
            reservationService.batchUpdateStatus(request.getIds(), request.getStatus(), userSession, isAdmin);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
        return ResultView.success(true);
    }

    /**
     * 违约次数
     */
    @GetMapping("/violationCount")
    public ResultView<?> violationCount() {
        UserSession userSession = currentUserSession();
        if (userSession == null) {
            return ResultView.success(0);
        }
        int count = reservationService.getViolationCount(userSession.getUserId());
        return ResultView.success(count);
    }

    private record CheckinRequest(
            @NotNull Long reservationId,
            @NotNull Long seatId,
            @NotBlank String qrToken
    ) {
        public Long getReservationId() {
            return reservationId;
        }

        public Long getSeatId() {
            return seatId;
        }

        public String getQrToken() {
            return qrToken;
        }
    }

    private record BatchStatusRequest(
            @NotNull List<Long> ids,
            @NotBlank String status
    ) {
        public List<Long> getIds() {
            return ids;
        }

        public String getStatus() {
            return status;
        }
    }

    private record BatchDeleteRequest(
            @NotEmpty List<Long> ids
    ) {
        public List<Long> getIds() {
            return ids;
        }
    }

    private record SaveReservationRequest(
            Long seatId,
            Long roomId,
            Integer row,
            Integer column,
            @NotBlank String date,
            String timeSlot,
            String time
    ) {
    }

    private record BatchSaveRequest(List<SingleReserve> reservations) {
    }

    private record SingleReserve(
            @NotNull Long seatId,
            @NotBlank String date,
            @NotBlank String timeSlot,
            Long userId,
            String status
      ) {
      }

      /**
       * 统一的用户取消时间校验逻辑：
       * - 提前预约：以时间段开始时间为准，需在开始前 >=10 分钟取消；
       * - 网站即时预约：以预约后 30 分钟为“开始”，需在预约后 20 分钟内取消。
       */
      private boolean canUserCancelByTime(Reservation reservation, boolean isAdmin) {
          if (isAdmin) {
              // 管理员不受时间限制
              return true;
          }
          if (reservation == null || reservation.getDate() == null || reservation.getTimeSlot() == null) {
              return true;
          }

          try {
              LocalDate date = LocalDate.parse(reservation.getDate());
              String slotCode = reservation.getTimeSlot().toUpperCase(Locale.ROOT);
              com.example.domain.enums.TimeSlot slot = com.example.domain.enums.TimeSlot.valueOf(slotCode);

              LocalDateTime now = LocalDateTime.now();
              LocalDateTime slotStart = LocalDateTime.of(date, slot.getStart());
              LocalDateTime slotBasedExpire = slotStart.plusMinutes(30);

              LocalDateTime expiresAt = reservation.getExpiresAt();
              boolean advanceReservation = expiresAt != null && expiresAt.equals(slotBasedExpire);

              if (advanceReservation) {
                  // 提前预约：必须在开始前 >=10 分钟取消
                  LocalDateTime lastCancelTime = slotStart.minusMinutes(10);
                  return !now.isAfter(lastCancelTime);
              } else {
                  // 网站即时预约：开始时间视为预约后 30 分钟；提前 10 分钟即“预约后 20 分钟”
                  Date createAt = reservation.getCreateAt();
                  if (createAt == null) {
                      return true;
                  }
                  LocalDateTime created = LocalDateTime.ofInstant(createAt.toInstant(), ZoneId.systemDefault());
                  LocalDateTime lastCancelTime = created.plusMinutes(20);
                  return !now.isAfter(lastCancelTime);
              }
          } catch (Exception ignored) {
              // 解析异常时不强制限制，避免老数据导致无法取消
              return true;
          }
      }

    private UserSession currentUserSession() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                    : null;
            if (principal instanceof UserSession us) {
                return us;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

      private String normalizeTimeSlot(String timeSlot, String time) {
        String raw = StringUtils.hasText(timeSlot) ? timeSlot : time;
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        raw = raw.trim();
        if ("上午".equals(raw)) raw = "MORNING";
        if ("下午".equals(raw)) raw = "AFTERNOON";
        if ("晚上".equals(raw) || "夜间".equals(raw)) raw = "EVENING";
        return raw.toUpperCase(Locale.ROOT);
    }
}
