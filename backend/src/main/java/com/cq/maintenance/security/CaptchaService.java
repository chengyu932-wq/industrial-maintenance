package com.cq.maintenance.security;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.security.vo.CaptchaVO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PREFIX = "auth:captcha:";
    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public CaptchaService(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public CaptchaVO create() {
        String id = UUID.randomUUID().toString();
        String code = randomCode();
        redis.opsForValue().set(PREFIX + id, code, properties.getCaptchaTtl());
        return new CaptchaVO(id, "data:image/png;base64," + Base64.getEncoder().encodeToString(render(code)));
    }

    public void verify(String id, String input) {
        String expected = redis.opsForValue().getAndDelete(PREFIX + id);
        if (expected == null) throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
        if (!expected.equalsIgnoreCase(input.trim())) throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
    }

    private String randomCode() {
        StringBuilder result = new StringBuilder(4);
        for (int i = 0; i < 4; i++) result.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return result.toString();
    }

    private byte[] render(String code) {
        try {
            BufferedImage image = new BufferedImage(120, 42, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(242, 246, 252));
            graphics.fillRect(0, 0, 120, 42);
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(random.nextInt(160), random.nextInt(160), random.nextInt(160), 100));
                graphics.drawLine(random.nextInt(120), random.nextInt(42), random.nextInt(120), random.nextInt(42));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            graphics.setColor(new Color(28, 68, 130));
            graphics.drawString(code, 18, 30);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to render captcha", exception);
        }
    }
}
