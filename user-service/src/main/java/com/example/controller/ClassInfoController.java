package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.result.ResultView;
import com.example.domain.dto.ClassImportDTO;
import com.example.domain.po.ClassInfo;
import com.example.domain.po.College;
import com.example.domain.vo.PageResponseVO;
import com.example.service.ClassInfoService;
import com.example.service.CollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/classInfo")
public class ClassInfoController {

    @Autowired
    private ClassInfoService classInfoService;

    @Autowired
    private CollegeService collegeService;

    @GetMapping("/list")
    public ResultView<?> list(@RequestParam Integer current,
                              @RequestParam Integer size,
                              @RequestParam(required = false) String className,
                              @RequestParam(required = false) Long collegeId) {
        Page<ClassInfo> page = Page.of(current, size);
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<ClassInfo>()
                .like(StringUtils.hasText(className), ClassInfo::getClassName, className)
                .eq(collegeId != null, ClassInfo::getCollegeId, collegeId);
        Page<ClassInfo> result = classInfoService.page(page, wrapper);
        return ResultView.success(new PageResponseVO<>(result.getTotal(), result.getSize(), current, result.getRecords()));
    }

    @PostMapping("/saveOrUpdate")
    public ResultView<?> saveOrUpdate(@RequestBody ClassInfo classInfo) {
        if (!StringUtils.hasText(classInfo.getClassName())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "班级名称不能为空");
        }
        if (classInfo.getCollegeId() == null) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "学院不能为空");
        }
        classInfoService.saveOrUpdate(classInfo);
        return ResultView.success(true);
    }

    @DeleteMapping("/{classId}")
    public ResultView<?> delete(@PathVariable Long classId) {
        classInfoService.removeById(classId);
        return ResultView.success(true);
    }

    @PostMapping("/import")
    public ResultView<?> importClass(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "文件不能为空");
        }
        try {
            List<ClassImportDTO> rows = EasyExcel.read(file.getInputStream())
                    .head(ClassImportDTO.class)
                    .sheet()
                    .doReadSync();
            int success = 0;
            for (ClassImportDTO row : rows) {
                if (row == null || !StringUtils.hasText(row.getClassName()) || !StringUtils.hasText(row.getCollegeName())) {
                    continue;
                }
                College college = collegeService.lambdaQuery()
                        .eq(College::getCollegeName, row.getCollegeName())
                        .one();
                if (college == null) {
                    college = new College();
                    college.setCollegeName(row.getCollegeName());
                    collegeService.save(college);
                }
                ClassInfo exist = classInfoService.lambdaQuery()
                        .eq(ClassInfo::getClassName, row.getClassName())
                        .eq(ClassInfo::getCollegeId, college.getCollegeId())
                        .one();
                if (exist == null) {
                    exist = new ClassInfo();
                    exist.setClassName(row.getClassName());
                    exist.setCollegeId(college.getCollegeId());
                }
                if (StringUtils.hasText(row.getGrade())) {
                    exist.setGrade(row.getGrade());
                }
                if (StringUtils.hasText(row.getRemark())) {
                    exist.setRemark(row.getRemark());
                }
                classInfoService.saveOrUpdate(exist);
                success++;
            }
            return ResultView.success(success);
        } catch (IOException e) {
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入班级失败");
        }
    }
}
