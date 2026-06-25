package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /** 单文件上传 */
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "文件为空");
        }
        try {
            String url = saveFile(file);
            return ApiResponse.ok(Map.of("url", url));
        } catch (IOException e) {
            return ApiResponse.fail(500, "上传失败: " + e.getMessage());
        }
    }

    /** 多文件上传 */
    @PostMapping("/upload/batch")
    public ApiResponse<?> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                urls.add(saveFile(file));
            } catch (IOException e) {
                return ApiResponse.fail(500, "上传失败: " + e.getMessage());
            }
        }
        return ApiResponse.ok(Map.of("urls", urls));
    }

    private String saveFile(MultipartFile file) throws IOException {
        // 按日期分目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dir = Paths.get(uploadDir, dateDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(newName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/" + uploadDir + "/" + dateDir + "/" + newName;
    }
}
