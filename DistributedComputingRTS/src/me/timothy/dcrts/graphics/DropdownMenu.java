package me.timothy.dcrts.graphics;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import me.timothy.dcrts.net.event.CEventHandler;
import me.timothy.dcrts.net.event.CEventManager;
import me.timothy.dcrts.net.event.EventType;
import me.timothy.dcrts.utils.GUtils;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

/**
 * A drop down menu that can be used on its own. Accepts
 * a set of arguments and a location and can render itself.
 * 
 * Creates a DROPDOWN_MENU_CHOICE when pressed. Listens
 * for MENUS_DESTROYED and appropriately destroys
 * itself.
 * 
 * Arguments for DROPDOWN_MENU_CHOICE:
 * 	DropdownMenu (this menu)
 *  String (which option was pressed)
 *  int (the index of the option)
 * 
 * @author Timothy
 */
public class DropdownMenu {
	private Color background;
	private Color lines;
	private Color text;
	private Color textDisabled;
	private Color highlight;
	private Color pressHighlight;
	private List<String> choices;
	private List<Boolean> enabled;
	private Point location;

	private boolean destroyed;
	
	private int width;
	private int heightPerRow;
	
	private Rectangle[] lastRenders;
	
	/**
	 * Creates a dropdown menu
	 * 
	 * @param choices the list of choices
	 * @param 
	 * 		location where the dropdown menu was originally clicked from.
	 * 		This will end up being somewhere on the edge of the dropdown
	 * 		menu, with a best-effort for it being on the top-left
	 * @param enabled 
	 * 		a list of the same length as choices, each boolean saying whether
	 * 		that choice is enabled
	 * @param background the color of the background of this dropdown. May be transparent
	 * @param highlight the color when a mouse is hovering over a segment
	 * @param pressHighlight the color when a mouse is pressed and hovering
	 * @param lines the color of the lines seperating the options
	 * @param text the color of the text
	 * @param textDisabled the color of disabled text
	 */
	public DropdownMenu(List<String> choices, List<Boolean> enabled, Point location,
			Color background, Color highlight, Color pressHighlight,
			Color lines, Color text, Color textDisabled) {
		CEventManager.instance.register(this);
		this.choices = choices;
		this.enabled = enabled;
		this.location = location;
		this.background = background;
		this.highlight = highlight;
		this.pressHighlight = pressHighlight;
		this.lines = lines;
		this.text = text;
		this.textDisabled = textDisabled;
		
		lastRenders = new Rectangle[choices.size()];
		width = 100;
		heightPerRow = 50;
	}
	/**
	 * Creates a dropdown menu
	 * 
	 * @param choices the list of choices
	 * @param enabled 
	 * 		a list of the same length as choices, each boolean saying whether
	 * 		that choice is enabled
	 * @param 
	 * 		location where the dropdown menu was originally clicked from.
	 * 		This will end up being somewhere on the edge of the dropdown
	 * 		menu, with a best-effort for it being on the top-left
	 */
	public DropdownMenu(List<String> choices, List<Boolean> enabled, Point location) {
		this(	choices, 
				enabled,
				location, 
				Color.white,
				new Color(135, 206, 250), // lightish blue
				new Color(30, 144, 255), // darker blue
				Color.lightGray,
				Color.gray,
				Color.darkGray);
	}
	
	/**
	 * Removes all listeners and associated holds
	 * this menu has. MUST be called to prevent
	 * memory leaks.
	 */
	public void destroy() {
		CEventManager.instance.unregister(this);
		destroyed = true;
	}
	
	public void render(GameContainer container, Graphics graphics) {
		if(destroyed)
			throw new IllegalArgumentException("This dropdown menu has been destroyed!");
		int topLeftX = location.x;
		int topLeftY = location.y;
		
		if(container.getWidth() < topLeftX + getWidth()) {
			topLeftX = location.x - getWidth(); // switch dropdown to going to the right
		}
		
		if(container.getHeight() < topLeftY + getHeight()) {
			topLeftY = location.y - getHeight(); // switch dropdown to going up
		}
		
		Color old = graphics.getColor();
		
		graphics.setColor(background);
		graphics.fillRect(topLeftX, topLeftY, getWidth(), getHeight());
		
		// draw lines
		
		graphics.setColor(lines);
		int y = topLeftY;
		for(int i = 0; i < choices.size(); i++) {
			lastRenders[i] = new Rectangle(topLeftX, y, getWidth(), heightPerRow);
			if(isHighlighted(i)) {
				if(!Mouse.isButtonDown(0)) // update method will handle the actual press
					graphics.setColor(highlight);
				else 
					graphics.setColor(pressHighlight);
				
				graphics.fill(lastRenders[i]);
				graphics.setColor(lines);
			}
			graphics.drawLine(topLeftX + 3, y, topLeftX + getWidth() - 3, y);
			y += heightPerRow;
		}
		
		// draw choices
		
		for(int i = 0; i < choices.size(); i++) {
			if(enabled.get(i))
				graphics.setColor(text);
			else
				graphics.setColor(textDisabled);
			GUtils.drawCenteredInRect(graphics, text, choices.get(i), lastRenders[i]);
		}
		
		graphics.setColor(old);
	}
	
	/**
	 * May call a DROPDOWN_MENU_CHOICE event if appropriate.
	 * 
	 * Doesn't do anything if called prior to the first render.
	 * 
	 * @param container the container
	 * @param delta the delta
	 */
	public void update(GameContainer container, int delta) {
		if(destroyed)
			throw new IllegalArgumentException("This dropdown menu has been destroyed!");
		if(lastRenders[0] == null)
			return;
		
		if(!Mouse.isButtonDown(0))
			return;
		
		for(int i = 0; i < lastRenders.length; i++) {
			if(isHighlighted(i)) {
				CEventManager.instance.broadcast(EventType.DROPDOWN_MENU_CHOICE, this, choices.get(i), i);
				CEventManager.instance.broadcast(EventType.MENUS_DESTROYED); // TODO check if we're opening a new menu
			}
		}
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return choices.size() * heightPerRow;
	}
	
	// Utility methods
	
	public boolean isHighlighted(String choice) {
		return isHighlighted(choices.indexOf(choice));
	}
	
	public boolean isHighlighted(int index) {
		return lastRenders[index].contains(Mouse.getX(), GUtils.height - Mouse.getY());
	}
	
	@CEventHandler(event=EventType.MENUS_DESTROYED)
	public void onMenusDestroyed() {
		destroy();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((background == null) ? 0 : background.hashCode());
		result = prime * result + ((choices == null) ? 0 : choices.hashCode());
		result = prime * result + (destroyed ? 1231 : 1237);
		result = prime * result
				+ ((highlight == null) ? 0 : highlight.hashCode());
		result = prime * result + Arrays.hashCode(lastRenders);
		result = prime * result + ((lines == null) ? 0 : lines.hashCode());
		result = prime * result
				+ ((location == null) ? 0 : location.hashCode());
		result = prime * result
				+ ((pressHighlight == null) ? 0 : pressHighlight.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DropdownMenu other = (DropdownMenu) obj;
		if (background == null) {
			if (other.background != null)
				return false;
		} else if (!background.equals(other.background))
			return false;
		if (choices == null) {
			if (other.choices != null)
				return false;
		} else if (!choices.equals(other.choices))
			return false;
		if (destroyed != other.destroyed)
			return false;
		if (highlight == null) {
			if (other.highlight != null)
				return false;
		} else if (!highlight.equals(other.highlight))
			return false;
		if (!Arrays.equals(lastRenders, other.lastRenders))
			return false;
		if (lines == null) {
			if (other.lines != null)
				return false;
		} else if (!lines.equals(other.lines))
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (pressHighlight == null) {
			if (other.pressHighlight != null)
				return false;
		} else if (!pressHighlight.equals(other.pressHighlight))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
}
