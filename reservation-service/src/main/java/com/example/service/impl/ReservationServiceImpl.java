package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.session.UserSession;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.enums.TimeSlot;
import com.example.domain.po.Reservation;
import com.example.domain.po.Room;
import com.example.domain.po.Seat;
import com.example.domain.po.User;
import com.example.domain.po.FaceRecognitionLog;
import com.example.domain.vo.ReservationVO;
import com.example.mapper.ReservationMapper;
import com.example.mapper.RoomMapper;
import com.example.mapper.SeatMapper;
import com.example.mapper.UserMapper;
import com.example.mapper.FaceRecognitionLogMapper;
import com.example.service.ReservationService;
import com.example.websocket.SeatStatusMessage;
import com.example.websocket.SeatWebSocketHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    private static final int BREACH_THRESHOLD = 3;
    private static final int BAN_DAYS = 7;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SeatWebSocketHandler seatWebSocketHandler;

    @Autowired
    private FaceRecognitionLogMapper faceRecognitionLogMapper;

    @Override
    public boolean createReservation(Reservation reservation, UserSession userSession) {
        enforceExpiration();
        normalizeReservationPayload(reservation);
        Assert.notNull(reservation.getSeatId(), "座位不能为空");
        Assert.notNull(reservation.getDate(), "预约日期不能为空");
        Assert.notNull(reservation.getTimeSlot(), "时间段不能为空");
        Long userId = userSession.getUserId();
        reservation.setUserId(userId);
        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN")
                        || userSession.getRoles().contains("R_TEACHER")
                        || userSession.getRoles().contains("R_SUPER"));

        LocalDate targetDate = LocalDate.parse(reservation.getDate());
        LocalDate today = LocalDate.now();
        if (targetDate.isBefore(today) || targetDate.isAfter(today.plusDays(7))) {
            throw new IllegalArgumentException("仅可预约今日到未来7天的座位");
        }

        String slotCode = reservation.getTimeSlot().toUpperCase(Locale.ROOT);
        reservation.setTimeSlot(slotCode);
        reservation.setTime(slotCode);
        TimeSlot slot = TimeSlot.valueOf(slotCode);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slotStart = LocalDateTime.of(targetDate, slot.getStart());
        LocalDateTime slotEnd = LocalDateTime.of(targetDate, slot.getEnd());
        boolean isToday = targetDate.isEqual(today);

        // 当天预约的限制
        if (isToday) {
            // 时间段已结束，不能预约
            if (now.toLocalTime().isAfter(slot.getEnd())) {
                throw new IllegalArgumentException("该时间段已结束，无法预约");
            }
            // 本时间段内，且距离结束 < 30 分钟，禁止预约
            boolean sameSlotReservationNow = !now.isBefore(slotStart) && now.isBefore(slotEnd);
            if (sameSlotReservationNow) {
                LocalDateTime latestBookTime = slotEnd.minusMinutes(30);
                if (!now.isBefore(latestBookTime)) {
                    throw new IllegalArgumentException("距离该时间段结束不足30分钟，无法预约");
                }
            }
        }

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat == null) {
            throw new IllegalArgumentException("座位不存在");
        }
        if (!Objects.equals(seat.getStatus(), 1)) {
            throw new IllegalStateException("座位已停用，无法预约");
        }
        reservation.setRoomId(seat.getRoomId());
        reservation.setSeatName(seat.getName());

        Room room = roomMapper.selectById(seat.getRoomId());
        if (room != null) {
            reservation.setRoomName(room.getRoomName());
            reservation.setRow(seat.getRowNo());
            reservation.setColumn(seat.getColNo());
        }

        if (!isAdmin) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getBanUntil() != null && user.getBanUntil().after(new Date())) {
                throw new IllegalStateException("您已被暂时禁止预约，请稍后再试");
            }
            long activeCount = this.count(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, userId)
                    .in(Reservation::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.CHECKED_IN.name()));
            if (activeCount > 0) {
                throw new IllegalStateException("每个账号仅允许一个有效预约");
            }
        }

        long seatOccupied = this.count(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, reservation.getSeatId())
                .eq(Reservation::getDate, reservation.getDate())
                .eq(Reservation::getTimeSlot, reservation.getTimeSlot())
                .in(Reservation::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.CHECKED_IN.name()));
        if (seatOccupied > 0) {
            throw new IllegalStateException("该座位该时间段已有预约");
        }

        reservation.setStatus(ReservationStatus.PENDING.name());
        Date nowDate = new Date();
        reservation.setCreateAt(nowDate);
        reservation.setUpdateAt(nowDate);

        // 过期时间用于自动违约：
        // - 本时间段预约：从创建时刻起 30 分钟内必须签到
        // - 未来时间段预约：从时间段开始起 10 分钟内必须签到
        boolean sameSlotReservation = isToday && !now.isBefore(slotStart) && now.isBefore(slotEnd);
        LocalDateTime expiresAt;
        if (sameSlotReservation) {
            expiresAt = now.plusMinutes(30);
        } else {
            expiresAt = slotStart.plusMinutes(10);
        }
        reservation.setExpiresAt(expiresAt);

        boolean saved = this.save(reservation);
        if (saved) {
            notifySeatStatus(reservation, ReservationStatus.PENDING.name());
        }
        return saved;
    }



    @Override
    public void cancelReservation(Long reservationId, UserSession userSession, boolean isAdmin) {
        enforceExpiration();
        Assert.notNull(reservationId, "预约ID不能为空");
        Reservation db = this.getById(reservationId);
        if (db == null) {
            return;
        }
        if (!isAdmin && !Objects.equals(db.getUserId(), userSession.getUserId())) {
            throw new IllegalStateException("无权取消该预约");
        }
        if (!ReservationStatus.PENDING.name().equals(db.getStatus())) {
            throw new IllegalStateException("当前状态不可取消");
        }
        db.setStatus(ReservationStatus.CANCELLED.name());
        db.setUpdateAt(new Date());
        this.updateById(db);
        notifySeatStatus(db, ReservationStatus.CANCELLED.name());
    }

    @Override
    public void checkIn(Long reservationId, Long seatId, String qrToken, UserSession userSession) {
        enforceExpiration();
        Assert.notNull(reservationId, "预约ID不能为空");
        Assert.notNull(seatId, "座位ID不能为空");
        Assert.notNull(qrToken, "二维码token不能为空");

        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN")
                        || userSession.getRoles().contains("R_TEACHER")
                        || userSession.getRoles().contains("R_SUPER"));

        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || !Objects.equals(seat.getStatus(), 1)) {
            throw new IllegalStateException("座位不可用");
        }
        if (!qrToken.equals(seat.getQrToken())) {
            throw new IllegalStateException("二维码无效或已过期");
        }

        Reservation db = this.getById(reservationId);
        if (db == null) {
            throw new IllegalStateException("预约不存在");
        }
        if (!Objects.equals(db.getSeatId(), seatId)) {
            throw new IllegalStateException("预约与座位不匹配");
        }
        if (!Objects.equals(db.getUserId(), userSession.getUserId())) {
            throw new IllegalStateException("仅预约人可签到");
        }
        if (!ReservationStatus.PENDING.name().equals(db.getStatus())) {
            throw new IllegalStateException("当前状态不可签到");
        }

        LocalDate targetDate = LocalDate.parse(db.getDate());
        if (!targetDate.isEqual(LocalDate.now())) {
            throw new IllegalStateException("不在预约日期");
        }

        TimeSlot slot = TimeSlot.valueOf(db.getTimeSlot().toUpperCase(Locale.ROOT));
        LocalDateTime slotStart = LocalDateTime.of(targetDate, slot.getStart());
        LocalDateTime slotEnd = LocalDateTime.of(targetDate, slot.getEnd());

        LocalDateTime now = LocalDateTime.now();
        Date createAt = db.getCreateAt();
        LocalDateTime created = createAt != null
                ? LocalDateTime.ofInstant(createAt.toInstant(), ZoneId.systemDefault())
                : null;

        // 本时间段预约：创建时间在 [slotStart, slotEnd) 内
        boolean sameSlotReservation =
                created != null
                        && !created.isBefore(slotStart)
                        && created.isBefore(slotEnd);

        LocalDateTime allowStart;
        LocalDateTime allowEnd;
        if (sameSlotReservation) {
            // 本时间段内预约：从创建时刻起 30 分钟内允许签到
            allowStart = created;
            allowEnd = created.plusMinutes(30);
        } else {
            // 未来时间段预约：从时间段开始起 10 分钟内允许签到
            allowStart = slotStart;
            allowEnd = slotStart.plusMinutes(10);
        }

        if (now.isBefore(allowStart)) {
            throw new IllegalStateException("未到签到时间");
        }
        if (now.isAfter(allowEnd)) {
            throw new IllegalStateException("已超过签到时间");
        }

        db.setStatus(ReservationStatus.CHECKED_IN.name());
        db.setCheckinAt(now);
        db.setUpdateAt(new Date());
        this.updateById(db);
        notifySeatStatus(db, ReservationStatus.CHECKED_IN.name());
    }


    @Override
    public List<ReservationVO> getReservation(Long userId) {
        enforceExpiration();
        List<ReservationVO> reservationVOList = new ArrayList<>();
        List<Reservation> reservations = this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId));
        for (Reservation reservation : reservations) {
            ReservationVO reservationVO = buildReservationVO(reservation);
            if (reservationVO != null) {
                reservationVO.setViolationCount(getViolationCount(userId));
                reservationVOList.add(reservationVO);
            }
        }
        return reservationVOList;
    }

    @Override
    public List<ReservationVO> listAll(Reservation probe) {
        enforceExpiration();
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (probe != null) {
            // 基于预约自身字段的过滤
            wrapper.eq(probe.getRoomId() != null, Reservation::getRoomId, probe.getRoomId());
            wrapper.eq(probe.getSeatId() != null, Reservation::getSeatId, probe.getSeatId());
            wrapper.eq(StringUtils.hasText(probe.getDate()), Reservation::getDate, probe.getDate());
            wrapper.eq(StringUtils.hasText(probe.getTimeSlot()), Reservation::getTimeSlot, probe.getTimeSlot());
            wrapper.eq(StringUtils.hasText(probe.getStatus()), Reservation::getStatus, probe.getStatus());

            // 基于用户信息的过滤（班级、性别、年龄、手机号、邮箱、学院等）
            boolean hasUserFilter =
                    probe.getUserId() != null
                            || probe.getAge() != null
                            || StringUtils.hasText(probe.getGender())
                            || StringUtils.hasText(probe.getClassName())
                            || StringUtils.hasText(probe.getDepartment())
                            || StringUtils.hasText(probe.getPhone())
                            || StringUtils.hasText(probe.getEmail());

            if (hasUserFilter) {
                LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(probe.getUserId() != null, User::getUserId, probe.getUserId());
                userWrapper.eq(probe.getAge() != null, User::getAge, probe.getAge());
                userWrapper.eq(StringUtils.hasText(probe.getGender()), User::getGender, probe.getGender());
                userWrapper.like(StringUtils.hasText(probe.getClassName()), User::getClassName, probe.getClassName());
                userWrapper.like(StringUtils.hasText(probe.getDepartment()), User::getDepartment, probe.getDepartment());
                userWrapper.like(StringUtils.hasText(probe.getPhone()), User::getPhone, probe.getPhone());
                userWrapper.like(StringUtils.hasText(probe.getEmail()), User::getEmail, probe.getEmail());

                List<User> users = userMapper.selectList(userWrapper);
                if (users.isEmpty()) {
                    return new ArrayList<>();
                }
                List<Long> userIds = new ArrayList<>();
                for (User u : users) {
                    if (u.getUserId() != null) {
                        userIds.add(u.getUserId());
                    }
                }
                if (userIds.isEmpty()) {
                    return new ArrayList<>();
                }
                wrapper.in(Reservation::getUserId, userIds);
            }
        }
        List<Reservation> list = this.list(wrapper);
        List<ReservationVO> result = new ArrayList<>();
        for (Reservation reservation : list) {
            ReservationVO vo = buildReservationVO(reservation);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public void saveByAdmin(Reservation reservation, UserSession userSession) {
        enforceExpiration();
        normalizeReservationPayload(reservation);
        Assert.notNull(reservation.getSeatId(), "座位不能为空");
        Assert.notNull(reservation.getDate(), "预约日期不能为空");
        Assert.notNull(reservation.getTimeSlot(), "时间段不能为空");
        if (reservation.getUserId() == null) {
            reservation.setUserId(userSession.getUserId());
        }

        boolean isAdmin = userSession.getRoles() != null &&
                (userSession.getRoles().contains("R_ADMIN")
                        || userSession.getRoles().contains("R_TEACHER")
                        || userSession.getRoles().contains("R_SUPER"));

        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat == null) {
            throw new IllegalArgumentException("座位不存在");
        }
        reservation.setRoomId(seat.getRoomId());
        reservation.setSeatName(seat.getName());
        Room room = roomMapper.selectById(seat.getRoomId());
        if (room != null) {
            reservation.setRoomName(room.getRoomName());
            reservation.setRow(seat.getRowNo());
            reservation.setColumn(seat.getColNo());
        }

        String slotCode = reservation.getTimeSlot().toUpperCase(Locale.ROOT);
        reservation.setTimeSlot(slotCode);
        reservation.setTime(slotCode);

        boolean isUpdate = reservation.getReservationId() != null;

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, reservation.getSeatId())
                .eq(Reservation::getDate, reservation.getDate())
                .eq(Reservation::getTimeSlot, reservation.getTimeSlot())
                .in(Reservation::getStatus, ReservationStatus.PENDING.name(), ReservationStatus.CHECKED_IN.name());

        if (isUpdate) {
            wrapper.ne(Reservation::getReservationId, reservation.getReservationId());
        }

        long seatOccupied = this.count(wrapper);

        if (seatOccupied > 0 && !isAdmin) {
            throw new IllegalStateException("该座位该时间段已有预约");
        }

        if (seatOccupied > 0 && isAdmin) {
            List<Reservation> conflicts = this.list(wrapper);
            for (Reservation conflict : conflicts) {
                notifySeatStatus(conflict, ReservationStatus.CANCELLED.name());
                this.removeById(conflict.getReservationId());
            }
        }

        Date now = new Date();
        if (!isUpdate) {
            reservation.setStatus(StringUtils.hasText(reservation.getStatus()) ? reservation.getStatus() : ReservationStatus.PENDING.name());
            reservation.setCreateAt(now);
        }
        reservation.setUpdateAt(now);
        reservation.setExpiresAt(calculateExpireAt(LocalDate.parse(reservation.getDate()), reservation.getTimeSlot()));
        this.saveOrUpdate(reservation);
        notifySeatStatus(reservation, reservation.getStatus());
    }

    @Override
    public void deleteByAdmin(Long reservationId) {
        Assert.notNull(reservationId, "预约ID不能为空");
        Reservation existing = this.getById(reservationId);
        this.removeById(reservationId);
        notifySeatStatus(existing, ReservationStatus.CANCELLED.name());
    }

    @Override
    public void batchDeleteByAdmin(List<Long> reservationIds) {
        Assert.notEmpty(reservationIds, "预约ID不能为空");
        for (Long id : reservationIds) {
            Reservation existing = this.getById(id);
            this.removeById(id);
            notifySeatStatus(existing, ReservationStatus.CANCELLED.name());
        }
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, String targetStatus, UserSession userSession, boolean isAdmin) {
        enforceExpiration();
        Assert.notEmpty(ids, "ID must not be empty");
        Assert.notNull(targetStatus, "Status must not be empty");

        for (Long id : ids) {
            Reservation res = this.getById(id);
            if (res == null) {
                continue;
            }
            if (!isAdmin && !Objects.equals(res.getUserId(), userSession.getUserId())) {
                throw new IllegalStateException("No permission to operate reservation");
            }

            res.setStatus(targetStatus);
            res.setUpdateAt(new Date());
            this.updateById(res);
            notifySeatStatus(res, targetStatus);
        }
    }

    @Override
    public int getViolationCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getViolationCount() == null) {
            return 0;
        }
        return user.getViolationCount();
    }

    private ReservationVO buildReservationVO(Reservation reservation) {
        ReservationVO reservationVO = new ReservationVO();
        Room room = roomMapper.selectById(reservation.getRoomId());
        User user = reservation.getUserId() != null ? userMapper.selectById(reservation.getUserId()) : null;
        if (reservation.getSeatId() != null) {
            Seat seat = seatMapper.selectById(reservation.getSeatId());
            if (seat != null) {
                reservationVO.setSeatId(seat.getId());
                reservationVO.setSeatName(seat.getName());
            }
        }
        if (room != null) {
            BeanUtils.copyProperties(room, reservationVO);
        }
        BeanUtils.copyProperties(reservation, reservationVO);
        reservationVO.setReservationId(reservation.getReservationId());
        reservationVO.setUserId(reservation.getUserId());
        reservationVO.setRoomId(reservation.getRoomId());
        if (user != null) {
            reservationVO.setUserName(user.getUserName());
            reservationVO.setAge(user.getAge());
            reservationVO.setGender(user.getGender());
            reservationVO.setClassName(user.getClassName());
            reservationVO.setDepartment(user.getDepartment());
            reservationVO.setPhone(user.getPhone());
            reservationVO.setEmail(user.getEmail());
        }
        return reservationVO;
    }

    private void notifySeatStatus(Reservation reservation, String statusOverride) {
        if (reservation == null || seatWebSocketHandler == null) {
            return;
        }
        String status = statusOverride != null ? statusOverride : reservation.getStatus();
        SeatStatusMessage message = new SeatStatusMessage(
                reservation.getRoomId(),
                reservation.getSeatId(),
                reservation.getRow(),
                reservation.getColumn(),
                reservation.getUserId(),
                reservation.getDate(),
                reservation.getTimeSlot(),
                status
        );
        seatWebSocketHandler.broadcast(message);
    }

    private LocalDateTime calculateExpireAt(LocalDate date, String timeSlotCode) {
        TimeSlot slot = TimeSlot.valueOf(timeSlotCode.toUpperCase(Locale.ROOT));
        LocalDateTime slotStart = LocalDateTime.of(date, slot.getStart());
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        // 与 createReservation 保持一致：若是今天且当前时间已过开始时间，则从当前时间起算 30 分钟
        if (date.isEqual(today) && now.isAfter(slotStart)) {
            slotStart = now;
        }
        return slotStart.plusMinutes(30);
    }

    /**
     * 自动释放过期未签到的预约，并在时间段结束后释放已签到座位
     */
    private void enforceExpiration() {
        LocalDateTime now = LocalDateTime.now();

        // 1) 未签到的预约：按照 expiresAt 判定违约
        List<Reservation> expired = this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, ReservationStatus.PENDING.name())
                .lt(Reservation::getExpiresAt, now));
        for (Reservation reservation : expired) {
            boolean adminUser = false;
            if (reservation.getUserId() != null) {
                User user = userMapper.selectById(reservation.getUserId());
                if (user != null && user.getRoles() != null) {
                    String roles = user.getRoles();
                    adminUser = roles.contains("R_ADMIN")
                            || roles.contains("R_TEACHER")
                            || roles.contains("R_SUPER");
                }
            }

            reservation.setUpdateAt(new Date());
            if (adminUser) {
                // 管理员/老师的预约到期只自动释放，不计入违约
                reservation.setStatus(ReservationStatus.RELEASED.name());
                this.updateById(reservation);
                notifySeatStatus(reservation, ReservationStatus.RELEASED.name());
            } else {
                reservation.setStatus(ReservationStatus.BREACH.name());
                this.updateById(reservation);
                notifySeatStatus(reservation, ReservationStatus.BREACH.name());
                increaseViolation(reservation.getUserId());
            }
        }

        // 2) 已签到的预约：时间段结束后统一释放座位（不记违约）
        List<Reservation> checkedInList = this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, ReservationStatus.CHECKED_IN.name()));
        for (Reservation reservation : checkedInList) {
            try {
                LocalDate date = LocalDate.parse(reservation.getDate());
                String slotCode = reservation.getTimeSlot().toUpperCase(Locale.ROOT);
                TimeSlot slot = TimeSlot.valueOf(slotCode);
                LocalDateTime slotEnd = LocalDateTime.of(date, slot.getEnd());
                if (now.isAfter(slotEnd)) {
                    reservation.setStatus(ReservationStatus.RELEASED.name());
                    reservation.setUpdateAt(new Date());
                    this.updateById(reservation);
                    notifySeatStatus(reservation, ReservationStatus.RELEASED.name());
                }
            } catch (Exception ignored) {
                // 无法解析的老数据跳过
            }
        }
    }

    /**
     * 扫码立即入座：不受“最后30分钟不可预约”限制
     * - 若本时间段已有该用户的预约，则直接视为签到
     * - 若无，则创建一条已签到预约
     */
    public Reservation scanCheckin(Long seatId, String qrToken, UserSession userSession) {
        enforceExpiration();
        Assert.notNull(seatId, "座位ID不能为空");
        Assert.notNull(qrToken, "二维码token不能为空");

        Seat seat = seatMapper.selectById(seatId);
        if (seat == null || !Objects.equals(seat.getStatus(), 1)) {
            throw new IllegalStateException("座位不可用");
        }
        if (!qrToken.equals(seat.getQrToken())) {
            throw new IllegalStateException("二维码无效或已过期");
        }

        Long userId = userSession.getUserId();

        // 计算当前所属时间段
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        TimeSlot currentSlot = null;
        for (TimeSlot s : TimeSlot.values()) {
            LocalTime start = s.getStart();
            LocalTime end = s.getEnd();
            if (!nowTime.isBefore(start) && nowTime.isBefore(end)) {
                currentSlot = s;
                break;
            }
        }
        if (currentSlot == null) {
            throw new IllegalStateException("当前不在任何可用时间段，无法入座");
        }

        String slotCode = currentSlot.name();
        String dateStr = today.toString();

        // 查询是否已有该用户在本时间段的预约
        Reservation existing = this.getOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .eq(Reservation::getDate, dateStr)
                .eq(Reservation::getTimeSlot, slotCode)
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus,
                        ReservationStatus.PENDING.name(),
                        ReservationStatus.CHECKED_IN.name())
                .last("limit 1"));

        if (existing != null) {
            if (ReservationStatus.CHECKED_IN.name().equals(existing.getStatus())) {
                return existing;
            }
            // PENDING -> 扫码签到
            existing.setStatus(ReservationStatus.CHECKED_IN.name());
            existing.setCheckinAt(now);
            existing.setUpdateAt(new Date());
            this.updateById(existing);
            notifySeatStatus(existing, ReservationStatus.CHECKED_IN.name());
            return existing;
        }

        // 创建一条新的已签到预约
        Reservation reservation = new Reservation();
        reservation.setSeatId(seatId);
        reservation.setUserId(userId);
        reservation.setRoomId(seat.getRoomId());
        reservation.setSeatName(seat.getName());

        Room room = roomMapper.selectById(seat.getRoomId());
        if (room != null) {
            reservation.setRoomName(room.getRoomName());
            reservation.setRow(seat.getRowNo());
            reservation.setColumn(seat.getColNo());
        }

        reservation.setDate(dateStr);
        reservation.setTimeSlot(slotCode);
        reservation.setTime(slotCode);
        reservation.setStatus(ReservationStatus.CHECKED_IN.name());

        Date nowDate = new Date();
        reservation.setCreateAt(nowDate);
        reservation.setCheckinAt(now);
        reservation.setUpdateAt(nowDate);

        // expiresAt 对 CHECKED_IN 仅用于“到点释放”
        LocalDateTime slotEnd = LocalDateTime.of(today, currentSlot.getEnd());
        reservation.setExpiresAt(slotEnd);

        this.save(reservation);
        notifySeatStatus(reservation, ReservationStatus.CHECKED_IN.name());
        return reservation;
    }

    private void increaseViolation(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int count = user.getViolationCount() == null ? 0 : user.getViolationCount();
        count += 1;
        user.setViolationCount(count);
        if (count >= BREACH_THRESHOLD) {
            Instant banUntil = Instant.now().plus(BAN_DAYS, ChronoUnit.DAYS);
            user.setBanUntil(Date.from(banUntil.atZone(ZoneId.systemDefault()).toInstant()));
        }
        userMapper.updateById(user);
    }

    /**
     * 兼容旧前端传入的 row/column/中文时间段等参数
     */
    private void normalizeReservationPayload(Reservation reservation) {
        if (reservation.getSeatId() == null
                && reservation.getRoomId() != null
                && reservation.getRow() != null
                && reservation.getColumn() != null) {
            Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getRoomId, reservation.getRoomId())
                    .eq(Seat::getRowNo, reservation.getRow())
                    .eq(Seat::getColNo, reservation.getColumn()));
            if (seat != null) {
                reservation.setSeatId(seat.getId());
            }
        }
        String slotCode = normalizeTimeSlot(reservation.getTimeSlot(), reservation.getTime());
        reservation.setTimeSlot(slotCode);
        reservation.setTime(slotCode);
    }

    private String normalizeTimeSlot(String timeSlot, String time) {
        String raw = StringUtils.hasText(timeSlot) ? timeSlot : time;
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("时间段不能为空");
        }
        raw = raw.trim();
        if ("上午".equals(raw)) raw = "MORNING";
        if ("下午".equals(raw)) raw = "AFTERNOON";
        if ("晚上".equals(raw) || "夜间".equals(raw)) raw = "EVENING";
        return raw.toUpperCase(Locale.ROOT);
    }
}
