package me.timothy.dcrts.utils;

import java.awt.Point;
import java.awt.Rectangle;

import me.timothy.dcrts.DCRTSEntry;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

public class GUtils {
	public static final long CLICK_DELAY = 100;
	public static int width;
	public static int height;
	
	/**
	 * Draws the specified string to the graphics, with the width and 
	 * height precalculated
	 * @param graphics the graphics
	 * @param str the string
	 * @param obj the rectangle object to fill with the appropriate location
	 * @param x the x
	 * @param y the y
	 * @param width the width
	 * @param height the height
	 */
	private static void draw(Graphics graphics, String str, Rectangle obj, int x, int y,
			int width, int height) {
		if(x < 0) {
			x = DCRTSEntry.instance.getContainer().getWidth() + x;
			obj.x = x;
		}
		if(y < 0) {
			y = DCRTSEntry.instance.getContainer().getHeight() + y;
			obj.y = y;
		}
		
		graphics.drawString(str, x, y);
		
		if(obj != null)
			obj.setBounds(x, y, width, height);
	}
	
	/**
	 * Draw the specified string to the graphics at the specified x and y
	 * location.
	 * @param graphics The graphics
	 * @param str the string to draw
	 * @param obj the rectangle object to fill with the appropriate location
	 * @param x the x location
	 * @param y the y location
	 */
	public static void draw(Graphics graphics, String str, Rectangle obj, int x, int y) {
		int width = graphics.getFont().getWidth(str);
		int height = graphics.getFont().getHeight(str);
		draw(graphics, str, obj, x, y, width, height);
	}

	/**
	 * Draws the specified string centered for the x-location on the specified
	 * graphics.
	 * @param graphics the graphics
	 * @param str the string to draw
	 * @param obj the rectangle object to fill with the appropriate location
	 * @param y the y location
	 */
	public static void drawCenteredX(Graphics graphics, String str, Rectangle obj, int y) {
		int strWidth = graphics.getFont().getWidth(str);
		
		int x = (width / 2) - (strWidth / 2);
		draw(graphics, str, obj, x, y, strWidth, graphics.getFont().getHeight(str));
	}

	/**
	 * Draws a hover effect at the specified location
	 * @param graphics the graphics
	 * @param string the string
	 * @param rectangle the location
	 */
	public static void drawHoverEffect(Graphics graphics, String string,
			Rectangle rectangle) {
		if(mouseInside(rectangle)) {
			if(!Mouse.isButtonDown(0))
				draw(graphics, string, rectangle, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
			else {
				Color old = graphics.getColor();
				graphics.setColor(Color.yellow);
				draw(graphics, string, rectangle, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
				graphics.setColor(old);
			}
		}
	}
	
	public static boolean mouseInside(Rectangle rectangle) {
		int mouseX = Mouse.getX();
		int mouseY = Mouse.getY();
		
		return rectangle.contains(new Point(mouseX, height - mouseY));
	}
	
	public static void setWidth(int nWidth) { width = nWidth; }
	public static void setHeight(int nHeight) { height = nHeight; }

	
	public static void drawCenteredInRect(Graphics graphics, Color text, String string,
			org.newdawn.slick.geom.Rectangle rectangle) {
		int strW = graphics.getFont().getWidth(string);
		int strH = graphics.getFont().getHeight(string);
		
		int x = Math.round(rectangle.getX() + (rectangle.getWidth() / 2) - (strW / 2));
		int y = Math.round(rectangle.getY() + (rectangle.getHeight() / 2) - (strH / 2));
		
		graphics.drawString(string, x, y);
	}
}
