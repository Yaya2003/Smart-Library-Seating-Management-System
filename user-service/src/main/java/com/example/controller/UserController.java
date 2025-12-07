package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.repository.TokenRepository;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.common.util.JwtUtil;
import com.example.domain.dto.EmailCodeLoginDTO;
import com.example.domain.dto.StudentImportDTO;
import com.example.domain.dto.TeacherImportDTO;
import com.example.domain.dto.UpdateProfileDTO;
import com.example.domain.dto.UserLoginDTO;
import com.example.domain.dto.UserRegisterDTO;
import com.example.domain.dto.ChangePasswordDTO;
import com.example.domain.dto.ResetPasswordByEmailDTO;
import com.example.domain.po.ClassInfo;
import com.example.domain.po.College;
import com.example.domain.po.User;
import com.example.domain.vo.PageResponseVO;
import com.example.domain.vo.UserLoginVO;
import com.example.service.ClassInfoService;
import com.example.service.CollegeService;
import com.example.service.EmailCodeService;
import com.example.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@RestController
@RequestMapping
public class UserController {

    private static final String DEFAULT_PASSWORD_HASH = "$2a$10$LZIYw70bwSUTiNTAT6slZOaORtjMWbQPVFjYTovrtV6WECkiSypxO";
    private static final String ROLE_STUDENT = "[\"R_USER\"]";
    private static final String ROLE_TEACHER = "[\"R_ADMIN\"]";

    @Autowired
    private UserService userService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private CollegeService collegeService;

    @Autowired
    private ClassInfoService classInfoService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${templates.import-student:classpath:templates/student-import.xlsx}")
    private String studentTemplatePath;

    @Value("${templates.import-teacher:classpath:templates/teacher-import.xlsx}")
    private String teacherTemplatePath;

    @PostMapping(path = "/auth/register")
    public ResultView<?> register(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        if (!emailCodeService.verifyCode(userRegisterDTO.getEmail(), userRegisterDTO.getCode())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "邮箱验证码错误或已过期");
        }

        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO, user);
        userService.saveOrUpdate(user);
        emailCodeService.clearCode(userRegisterDTO.getEmail());
        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/login")
    public ResultView<?> login(@RequestBody @Valid UserLoginDTO userLoginDTO) {
        User loginUser = userService.getById(userLoginDTO.getUserId());
        if (loginUser == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "用户id不存在");
        }

        if (Objects.equals(loginUser.getState(), "2")) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该用户已被锁定");
        }

        if (!userLoginDTO.getUserName().equals(loginUser.getUserName())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "用户名不存在");
        }

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), loginUser.getPassword())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "密码错误");
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

    @PostMapping(path = "/auth/loginByEmailCode")
    public ResultView<?> loginByEmailCode(@RequestBody @Valid EmailCodeLoginDTO emailCodeLoginDTO) {
        if (!emailCodeService.verifyCode(emailCodeLoginDTO.getEmail(), emailCodeLoginDTO.getCode())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "邮箱验证码错误或已过期");
        }

        User loginUser = userService.lambdaQuery().eq(User::getEmail, emailCodeLoginDTO.getEmail()).one();
        if (loginUser == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该邮箱未注册");
        }

        if ("2".equals(loginUser.getState())) {
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
        emailCodeService.clearCode(emailCodeLoginDTO.getEmail());
        return ResultView.success(new UserLoginVO(token, freshToken));
    }

    @GetMapping(path = "/auth/loginByUserId")
    public ResultView<?> loginByUserId(@RequestParam Long userId) {
        User loginUser = userService.getById(userId);
        if (loginUser == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "The id is not exits");
        }

        if ("2".equals(loginUser.getState())) {
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

    @PostMapping(path = "/auth/profile")
    public ResultView<?> updateProfile(@RequestBody @Valid UpdateProfileDTO updateProfileDTO) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userSession.getUserId();

        if (updateProfileDTO.getEmail() != null && !updateProfileDTO.getEmail().isBlank()) {
            boolean emailExists = userService.lambdaQuery()
                    .eq(User::getEmail, updateProfileDTO.getEmail())
                    .ne(User::getUserId, userId)
                    .exists();
            if (emailExists) {
                return ResultView.error(HttpStatus.BAD_REQUEST.value(), "邮箱已被占用");
            }
        }

        User update = new User();
        BeanUtils.copyProperties(updateProfileDTO, update);
        update.setUserId(userId);

        userService.updateById(update);
        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/changePassword")
    public ResultView<?> changePassword(@RequestBody @Valid ChangePasswordDTO changePasswordDTO) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userSession.getUserId();
        User user = userService.getById(userId);
        if (user == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "用户不存在");
        }

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "旧密码不正确");
        }

        if (passwordEncoder.matches(changePasswordDTO.getNewPassword(), user.getPassword())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "新密码不能与旧密码相同");
        }

        String encoded = passwordEncoder.encode(changePasswordDTO.getNewPassword());
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, userId)
                .set(User::getPassword, encoded));

        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/resetPasswordByEmail")
    public ResultView<?> resetPasswordByEmail(@RequestBody @Valid ResetPasswordByEmailDTO dto) {
        if (!emailCodeService.verifyCode(dto.getEmail(), dto.getCode())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "邮箱验证码错误或已过期");
        }

        User user = userService.lambdaQuery()
                .eq(User::getEmail, dto.getEmail())
                .one();
        if (user == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该邮箱未绑定用户");
        }

        String encoded = passwordEncoder.encode(dto.getNewPassword());
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, user.getUserId())
                .set(User::getPassword, encoded));

        emailCodeService.clearCode(dto.getEmail());
        return ResultView.success(true);
    }

    @PostMapping(path = "/auth/sendEmailCode")
    public ResultView<?> sendEmailCode(@RequestParam @jakarta.validation.constraints.Email String email) {
        if (emailCodeService.isLimited(email)) {
            return ResultView.error(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁，请稍后再试");
        }
        try {
            emailCodeService.sendCode(email);
        } catch (MailException e) {
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "发送验证码失败");
        }
        return ResultView.success(true);
    }

    @GetMapping(path = "/auth/getUserInfo")
    public ResultView<?> getUserInfo() {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        User user = userService.getById(id);
        if (user != null) {
            boolean isDefault = DEFAULT_PASSWORD_HASH.equals(user.getPassword());
            user.setDefaultPassword(isDefault);
        }
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
                                     @RequestParam(required = false) Long collegeId,
                                     @RequestParam(required = false) Long classId,
                                     @RequestParam(required = false) String role,
                                     @RequestParam(required = false) Integer faceInfo,
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
                .eq(collegeId != null, User::getCollegeId, collegeId)
                .eq(classId != null, User::getClassId, classId)
                .like(role != null, User::getRoles, role)
                .eq(state != null, User::getState, state)
                .eq(faceInfo != null, User::getFaceInfo, faceInfo);

        // 根据用户角色设置不同的过滤条件
        if (roles.contains("R_SUPER")) {
            // 超级管理员可以看到所有用户
        } else if (roles.contains("R_ADMIN")) {
            // 普通管理员不能看到超级管理员
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

    @PostMapping(path = "/auth/saveOrUpdateUser")
    public ResultView<?> saveOrUpdateUser(@RequestBody User user) {
        if (user.getUserId() == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "学号不能为空");
        }

        User existing = userService.getById(user.getUserId());

        if (existing == null) {
            // 新增用户设定默认密码（与注册保持一致的初始密码）
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(DEFAULT_PASSWORD_HASH);
            }
            userService.save(user);
        } else {
            LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                    .eq(User::getUserId, user.getUserId())
                    .set(user.getUserName() != null, User::getUserName, user.getUserName())
                    .set(user.getEmail() != null, User::getEmail, user.getEmail())
                    .set(user.getPhone() != null, User::getPhone, user.getPhone())
                    .set(user.getClassName() != null, User::getClassName, user.getClassName())
                    .set(user.getClassId() != null, User::getClassId, user.getClassId())
                    .set(user.getDepartment() != null, User::getDepartment, user.getDepartment())
                    .set(user.getCollegeId() != null, User::getCollegeId, user.getCollegeId())
                    .set(user.getGender() != null, User::getGender, user.getGender())
                    .set(user.getAge() != null, User::getAge, user.getAge())
                    .set(user.getState() != null, User::getState, user.getState())
                    .set(user.getRoles() != null, User::getRoles, user.getRoles());
            userService.update(updateWrapper);
        }
        return ResultView.success(true);
    }

    @PostMapping(path = "/systemManage/importStudents")
    public ResultView<?> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "文件不能为空");
        }
        try {
            List<StudentImportDTO> rows = EasyExcel.read(file.getInputStream())
                    .head(StudentImportDTO.class)
                    .sheet()
                    .doReadSync();
            List<Long> failed = new ArrayList<>();
            for (StudentImportDTO row : rows) {
                if (row == null || row.getUserId() == null || !StringUtils.hasText(row.getUserName())) {
                    continue;
                }
                String gender = normalizeGender(row.getGender());
                College college = ensureCollege(row.getCollegeName());
                ClassInfo classInfo = ensureClass(row.getClassName(), college);

                User user = userService.getById(row.getUserId());
                if (user == null) {
                    user = new User();
                    user.setUserId(row.getUserId());
                    user.setState("1");
                    user.setPassword(DEFAULT_PASSWORD_HASH);
                }
                user.setUserName(row.getUserName());
                user.setGender(gender);
                user.setAge(row.getAge());
                user.setPhone(row.getPhone());
                user.setEmail(row.getEmail());
                user.setDepartment(row.getCollegeName());
                user.setClassName(row.getClassName());
                if (college != null) {
                    user.setCollegeId(college.getCollegeId());
                }
                if (classInfo != null) {
                    user.setClassId(classInfo.getClassId());
                }
                user.setRoles(ROLE_STUDENT);
                boolean saved = userService.saveOrUpdate(user);
                if (!saved) {
                    failed.add(row.getUserId());
                }
            }
            if (failed.isEmpty()) {
                return ResultView.success(true);
            }
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "部分数据导入失败: " + failed);
        } catch (IOException e) {
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入学生失败");
        }
    }

    @PostMapping(path = "/systemManage/importTeachers")
    public ResultView<?> importTeachers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "文件不能为空");
        }
        try {
            List<TeacherImportDTO> rows = EasyExcel.read(file.getInputStream())
                    .head(TeacherImportDTO.class)
                    .sheet()
                    .doReadSync();
            List<Long> failed = new ArrayList<>();
            for (TeacherImportDTO row : rows) {
                if (row == null || row.getUserId() == null || !StringUtils.hasText(row.getUserName())) {
                    continue;
                }
                String gender = normalizeGender(row.getGender());
                College college = ensureCollege(row.getCollegeName());

                User user = userService.getById(row.getUserId());
                if (user == null) {
                    user = new User();
                    user.setUserId(row.getUserId());
                    user.setState("1");
                    user.setPassword(DEFAULT_PASSWORD_HASH);
                }
                user.setUserName(row.getUserName());
                user.setGender(gender);
                user.setAge(row.getAge());
                user.setPhone(row.getPhone());
                user.setEmail(row.getEmail());
                user.setDepartment(row.getCollegeName());
                if (college != null) {
                    user.setCollegeId(college.getCollegeId());
                }
                user.setClassId(null);
                user.setClassName(null);
                user.setRoles(ROLE_TEACHER);
                boolean saved = userService.saveOrUpdate(user);
                if (!saved) {
                    failed.add(row.getUserId());
                }
            }
            if (failed.isEmpty()) {
                return ResultView.success(true);
            }
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "部分数据导入失败: " + failed);
        } catch (IOException e) {
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入老师失败");
        }
    }

    @GetMapping(path = "/systemManage/template/student")
    public void downloadStudentTemplate(HttpServletResponse response) throws IOException {
        downloadTemplate(response, studentTemplatePath, "student-import.xlsx");
    }

    @GetMapping(path = "/systemManage/template/teacher")
    public void downloadTeacherTemplate(HttpServletResponse response) throws IOException {
        downloadTemplate(response, teacherTemplatePath, "teacher-import.xlsx");
    }

    private void downloadTemplate(HttpServletResponse response, String location, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encodedName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedName);

        Resource resource = resourceLoader.getResource(location);
        if (resource.exists() && resource.isReadable()) {
            try (InputStream in = resource.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }
            return;
        }
        response.setStatus(HttpStatus.NOT_FOUND.value());
    }

    private String normalizeGender(String genderText) {
        if (!StringUtils.hasText(genderText)) {
            return null;
        }
        String value = genderText.trim();
        if ("男".equals(value) || "1".equals(value)) {
            return "1";
        }
        if ("女".equals(value) || "2".equals(value)) {
            return "2";
        }
        return null;
    }

    private College ensureCollege(String collegeName) {
        if (!StringUtils.hasText(collegeName)) {
            return null;
        }
        College college = collegeService.lambdaQuery()
                .eq(College::getCollegeName, collegeName)
                .one();
        if (college == null) {
            college = new College();
            college.setCollegeName(collegeName);
            collegeService.save(college);
        }
        return college;
    }

    private ClassInfo ensureClass(String className, College college) {
        if (!StringUtils.hasText(className) || college == null) {
            return null;
        }
        ClassInfo classInfo = classInfoService.lambdaQuery()
                .eq(ClassInfo::getClassName, className)
                .eq(ClassInfo::getCollegeId, college.getCollegeId())
                .one();
        if (classInfo == null) {
            classInfo = new ClassInfo();
            classInfo.setClassName(className);
            classInfo.setCollegeId(college.getCollegeId());
            classInfoService.save(classInfo);
        }
        return classInfo;
    }
}
