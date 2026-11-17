package com.safayet.afsos_nama;

import com.safayet.afsos_nama.service.ProfilePhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProfilePhotoServiceTest {
    @Autowired ProfilePhotoService photos;
    @Autowired JdbcTemplate jdbc;

    private int user(String email) {
        jdbc.update("INSERT INTO users (name,email,password,role,enabled) VALUES (?,?,?,'STUDENT',true)",
                "Photo Tester", email, "test-hash");
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private MockMultipartFile png(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("photo", "photo.png", "image/png", output.toByteArray());
    }

    @Test
    void photoPersistsIsResizedReplacedAndRemovedForItsOwner() throws Exception {
        int owner = user("photo-owner@example.test");
        int other = user("photo-other@example.test");
        photos.upload(owner, png(600, 400, Color.RED));
        assertTrue(photos.exists(owner));
        byte[] original = photos.get(owner);
        var image = ImageIO.read(new ByteArrayInputStream(original));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertEquals(0xff, Byte.toUnsignedInt(original[0]));
        assertEquals(0xd8, Byte.toUnsignedInt(original[1]));
        assertNull(photos.get(other));
        assertFalse(photos.exists(other));
        assertFalse(photos.export(owner).get("base64").isBlank());
        photos.upload(owner, png(300, 500, Color.BLUE));
        assertFalse(java.util.Arrays.equals(original, photos.get(owner)));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_profile_photos WHERE user_id = ?", Integer.class, owner));
        photos.remove(owner);
        assertNull(photos.get(owner));
        assertTrue(photos.export(owner).isEmpty());
    }

    @Test
    void invalidOversizedAndExcessiveDimensionUploadsLeaveExistingPhotoIntact() throws Exception {
        int owner = user("photo-validation@example.test");
        photos.upload(owner, png(10, 10, Color.GREEN));
        byte[] saved = photos.get(owner);
        assertThrows(IllegalArgumentException.class, () -> photos.upload(owner,
                new MockMultipartFile("photo", "fake.png", "image/png",
                        "<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> photos.upload(owner,
                new MockMultipartFile("photo", new byte[2 * 1024 * 1024 + 1])));
        assertThrows(IllegalArgumentException.class, () -> photos.upload(owner, png(4097, 1, Color.BLACK)));
        assertArrayEquals(saved, photos.get(owner));
    }
}
