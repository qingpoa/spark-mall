package com.sparkleshop.service.product.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.product.config.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtils {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> ALLOWED_IMAGE_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp");

    private final OSS ossClient;
    private final OssProperties ossProperties;

    public String uploadImage(String directory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST, "上传文件不能为空");
        }

        String suffix = resolveSuffix(file.getOriginalFilename());
        validateImageFile(file, suffix);

        String yearMonthPath = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        String objectName = directory + "/" + yearMonthPath + "/" + IdUtil.randomUUID().replace("-", "") + suffix;
        try {
            byte[] content = file.getBytes();
            ossClient.putObject(
                    ossProperties.getBucketName(),
                    objectName,
                    new ByteArrayInputStream(content)
            );
        } catch (IOException | OSSException | ClientException exception) {
            log.error("上传OSS图片失败: {}", objectName, exception);
            throw new BusinessException(Result.SERVER_ERROR, "文件上传失败");
        }

        return ossProperties.getDomain() + "/" + objectName;
    }

    private String resolveSuffix(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private void validateImageFile(MultipartFile file, String suffix) {
        if (!ALLOWED_IMAGE_SUFFIXES.contains(suffix)) {
            throw new BusinessException(Result.BAD_REQUEST, "仅支持上传图片文件");
        }
        if (!StrUtil.startWithIgnoreCase(file.getContentType(), "image/")) {
            throw new BusinessException(Result.BAD_REQUEST, "仅支持上传图片文件");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new BusinessException(Result.BAD_REQUEST, "上传文件不是有效图片");
            }
        } catch (IOException exception) {
            throw new BusinessException(Result.BAD_REQUEST, "图片校验失败");
        }
    }
}
