package com.safayet.afsos_nama.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfilePhotoService {
    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private final JdbcTemplate jdbc;

    public boolean exists(int userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM user_profile_photos WHERE user_id = ?", Boolean.class, userId));
    }

    public byte[] get(int userId) {
        return jdbc.query("SELECT image_data FROM user_profile_photos WHERE user_id = ?",
                (rs, row) -> rs.getBytes(1), userId).stream().findFirst().orElse(null);
    }

    public void upload(int userId, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Choose a JPEG or PNG photo.");
        }
        if (photo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Choose a photo smaller than 2 MB.");
        }
        byte[] image;
        try {
            image = normalize(photo.getBytes());
        } catch (IOException exception) {
            throw new IllegalArgumentException("This photo could not be read. Choose a JPEG or PNG image.");
        }
        jdbc.update("""
                INSERT INTO user_profile_photos (user_id, image_data) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE image_data = VALUES(image_data)
                """, userId, image);
    }

    private byte[] normalize(byte[] data) throws IOException {
        // Inspect actual file contents and dimensions before allocating decoded pixels.
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Choose a valid JPEG or PNG image.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw new IllegalArgumentException("Only JPEG and PNG photos are supported.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096) {
                    throw new IllegalArgumentException("Use a photo no larger than 4096 pixels on either side.");
                }
                BufferedImage source = reader.read(0);
                BufferedImage square = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                var graphics = square.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, 256, 256);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int side = Math.min(width, height);
                    int x = (width - side) / 2;
                    int y = (height - side) / 2;
                    graphics.drawImage(source, 0, 0, 256, 256, x, y, x + side, y + side, null);
                } finally {
                    graphics.dispose();
                }
                // Re-encoding strips uploaded metadata and fixes the served media type.
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(square, "jpeg", output)) {
                    throw new IOException("JPEG encoding unavailable");
                }
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        }
    }

    public void remove(int userId) {
        jdbc.update("DELETE FROM user_profile_photos WHERE user_id = ?", userId);
    }

    public Map<String, String> export(int userId) {
        byte[] image = get(userId);
        return image == null ? Map.of()
                : Map.of("contentType", "image/jpeg", "base64", Base64.getEncoder().encodeToString(image));
    }
}
