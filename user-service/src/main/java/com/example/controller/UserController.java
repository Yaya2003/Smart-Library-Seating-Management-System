package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.repository.TokenRepository;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.common.util.JwtUtil;
import com.example.domain.dto.UserLoginDTO;
import com.example.domain.dto.UserRegisterDTO;
import com.example.domain.po.User;
import com.example.domain.vo.PageResponseVO;
import com.example.domain.vo.UserLoginVO;
import com.example.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(path = "/auth/register")
    public ResultView<?> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO, user);
        userService.saveOrUpdate(user);
        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/login")
    public ResultView<?> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        User loginUser = userService.getById(userLoginDTO.getUserId());
        if (loginUser == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The id is not exits");
        }

        if (Objects.equals(loginUser.getState(), "2")) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The user is locked");
        }

        if (!userLoginDTO.getUserName().equals(loginUser.getUserName())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The userName is not exits");
        }

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), loginUser.getPassword())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The password is not correct");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("userName", loginUser.getUserName());
        claims.put("roles", loginUser.getRoles());
        claims.put("state", loginUser.getState());

        String token = JwtUtil.generateToken(claims);
        String freshToken = JwtUtil.generateRefreshToken(claims);

        tokenRepository.saveToken(token);
//        tokenRepository.saveToken(freshToken);

        return ResultView.success(new UserLoginVO(token, freshToken));
    }

    @GetMapping(path = "/auth/loginByUserId")
    public ResultView<?> loginByUserId(@RequestParam Long userId) {
        User loginUser = userService.getById(userId);
        if (loginUser == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The id is not exits");
        }

        if (loginUser.getState().equals("2")) {
            return ResultView.error(HttpStatus.FORBIDDEN.value(), "该账号已被封禁，请联系管理员");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("userName", loginUser.getUserName());
        claims.put("roles", loginUser.getRoles());
        claims.put("state", loginUser.getState());

        String token = JwtUtil.generateToken(claims);
        String freshToken = JwtUtil.generateRefreshToken(claims);

        tokenRepository.saveToken(token);
        return ResultView.success(new UserLoginVO(token, freshToken));
    }

    @GetMapping(path = "/auth/getUserInfo")
    public ResultView<?> getUserInfo() {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        User user = userService.getById(id);
        return ResultView.success(user);
    }

    @GetMapping(path = "systemManage/getUserList")
    public ResultView<?> getUserList(@RequestParam Integer current,
                                     @RequestParam Integer size,
                                     @RequestParam(required = false) Long userId,
                                     @RequestParam(required = false) String userName,
                                     @RequestParam(required = false) String gender,
                                     @RequestParam(required = false) String department,
                                     @RequestParam(required = false) String className,
                                     @RequestParam(required = false) String state) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> roles = userSession.getRoles();
        Page<User> page = Page.of(current, size);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .like(userId != null, User::getUserId, userId)
                .like(userName != null, User::getUserName, userName)
                .eq(gender != null, User::getGender, gender)
                .eq(department != null, User::getDepartment, department)
                .eq(className != null, User::getClassName, className)
                .eq(state != null, User::getState, state);

        // 根据用户角色设置不同的过滤条件
        if (roles.contains("R_SUPER")) {
            // 超级管理员可以看到所有用户，包括管理员
            // 不需要额外过滤
        } else if (roles.contains("R_ADMIN")) {
            // 普通管理员不能看到超级管理员和其他管理员
            queryWrapper.notLike(User::getRoles, "R_SUPER");
        } else {
            // 普通用户只能看到普通用户
            queryWrapper.notLike(User::getRoles, "R_SUPER")
                    .notLike(User::getRoles, "R_ADMIN");
        }

        Page<User> userPage = userService.page(page, queryWrapper);
        return ResultView.success(new PageResponseVO<>(userPage.getTotal(), userPage.getSize(), current, userPage.getRecords()));
    }

    @DeleteMapping(path = "/auth/deleteUser/{userId}")
    public ResultView<?> deleteUser(@PathVariable Long userId) {
        userService.removeById(userId);
        return ResultView.success(true);
    }

    @DeleteMapping(path = "/auth/deleteBatchUser")
    public ResultView<?> deleteBatchUser(@RequestBody List<Long> userIds) {
        userService.removeByIds(userIds);
        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/addFaceInfo")
    public ResultView<?> addFaceInfo() {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userSession.getUserId();
        userService.update(new LambdaUpdateWrapper<User>().set(User::getFaceInfo, 1).eq(User::getUserId, userId));
        return ResultView.success(true);
    }
}
