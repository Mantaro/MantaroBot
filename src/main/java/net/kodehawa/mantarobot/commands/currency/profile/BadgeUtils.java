/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.currency.profile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BadgeUtils {
    public static byte[] applyBadge(byte[] avatarBytes, byte[] badgeBytes, int startX, int startY, boolean allWhite) {
        BufferedImage avatar;
        BufferedImage badge;
        try {
            avatar = ImageIO.read(new ByteArrayInputStream(avatarBytes));
            badge = ImageIO.read(new ByteArrayInputStream(badgeBytes));
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        WritableRaster raster = badge.getRaster();

        if (allWhite) {
            for (int xx = 0, width = badge.getWidth(); xx < width; xx++) {
                for (int yy = 0, height = badge.getHeight(); yy < height; yy++) {
                    int[] pixels = raster.getPixel(xx, yy, (int[]) null);
                    pixels[0] = 255;
                    pixels[1] = 255;
                    pixels[2] = 255;
                    pixels[3] = pixels[3] == 255 ? 165 : 0;
                    raster.setPixel(xx, yy, pixels);
                }
            }
        }

        BufferedImage res = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        int circleCenterX = 88, circleCenterY = 88;
        int width = 32, height = 32;
        int circleRadius = 40;

        Graphics2D g2d = res.createGraphics();
        g2d.drawImage(avatar, 0, 0, 128, 128, null);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0, 0, 165, 165));
        g2d.fillOval(circleCenterX, circleCenterY, circleRadius, circleRadius);


        g2d.drawImage(badge, startX, startY, width, height, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(res, "png", baos);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return baos.toByteArray();
    }
}
