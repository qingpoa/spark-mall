package com.sparkleshop.service.user.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.user.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OssUtils {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> ALLOWED_IMAGE_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp");

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    public String upload(String filetype, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        validateImageFile(file, suffix);

        String yearMonthPath = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        String objectName = filetype + "/" + yearMonthPath + "/" + IdUtil.randomUUID().replace("-", "") + suffix;

        try {
            byte[] content = file.getBytes();
            ossClient.putObject(
                    ossProperties.getBucketName(),
                    objectName,
                    new ByteArrayInputStream(content)
            );
        } catch (IOException e) {
            throw new BusinessException(Result.SERVER_ERROR, "文件上传失败");
        }

        return ossProperties.getDomain() + "/" + objectName;
    }

    public void delete(String url) throws OSSException {
        if (url == null) {
            return;
        }
        if (!url.contains(ossProperties.getDomain())) {
            log.warn("跳过OSS删除：URL不属于当前环境域名, url={}", url);
            return;
        }
        String objectName = url.replace(ossProperties.getDomain() + "/", "");
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
        } catch (OSSException | ClientException e) {
            log.error("删除OSS文件失败: {}", objectName, e);
        }
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
