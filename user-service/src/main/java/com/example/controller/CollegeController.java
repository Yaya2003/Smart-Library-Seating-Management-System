package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.result.ResultView;
import com.example.domain.dto.CollegeImportDTO;
import com.example.domain.po.College;
import com.example.domain.vo.PageResponseVO;
import com.example.service.CollegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/college")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @GetMapping("/list")
    public ResultView<?> list(@RequestParam Integer current,
                              @RequestParam Integer size,
                              @RequestParam(required = false) String collegeName) {
        Page<College> page = Page.of(current, size);
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<College>()
                .like(StringUtils.hasText(collegeName), College::getCollegeName, collegeName);
        Page<College> result = collegeService.page(page, wrapper);
        return ResultView.success(new PageResponseVO<>(result.getTotal(), result.getSize(), current, result.getRecords()));
    }

    @PostMapping("/saveOrUpdate")
    public ResultView<?> saveOrUpdate(@RequestBody College college) {
        if (!StringUtils.hasText(college.getCollegeName())) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "学院名称不能为空");
        }
        collegeService.saveOrUpdate(college);
        return ResultView.success(true);
    }

    @DeleteMapping("/{collegeId}")
    public ResultView<?> delete(@PathVariable Long collegeId) {
        collegeService.removeById(collegeId);
        return ResultView.success(true);
    }

    @PostMapping("/import")
    public ResultView<?> importCollege(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultView.error(HttpStatus.BAD_REQUEST.value(), "文件不能为空");
        }
        try {
            List<CollegeImportDTO> rows = EasyExcel.read(file.getInputStream())
                    .head(CollegeImportDTO.class)
                    .sheet()
                    .doReadSync();
            int success = 0;
            for (CollegeImportDTO row : rows) {
                if (row == null || !StringUtils.hasText(row.getCollegeName())) {
                    continue;
                }
                College exist = collegeService.lambdaQuery()
                        .eq(College::getCollegeName, row.getCollegeName())
                        .one();
                if (exist == null) {
                    exist = new College();
                    exist.setCollegeName(row.getCollegeName());
                }
                if (StringUtils.hasText(row.getCollegeCode())) {
                    exist.setCollegeCode(row.getCollegeCode());
                }
                if (StringUtils.hasText(row.getRemark())) {
                    exist.setRemark(row.getRemark());
                }
                collegeService.saveOrUpdate(exist);
                success++;
            }
            return ResultView.success(success);
        } catch (IOException e) {
            return ResultView.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "导入学院失败");
        }
    }
}
