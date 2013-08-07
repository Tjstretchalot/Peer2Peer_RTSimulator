package me.timothy.dcrts.graphics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.timothy.dcrts.ResourceManager;
import me.timothy.dcrts.net.event.CEventHandler;
import me.timothy.dcrts.net.event.CEventManager;
import me.timothy.dcrts.net.event.EventType;
import me.timothy.dcrts.utils.GUtils;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
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
	private List<Object> choices;
	private EnableChecker enabledCheck;
	private Point location;

	private boolean destroyed;
	
	private int width;
	private int heightPerRow;
	
	private Rectangle[] lastRenders;
	
	private List<Object> currentDisplay;
	private long lastPress;
	
	private Map<String, Object> meta;
	
	/**
	 * Creates a dropdown menu
	 * 
	 * @param choices the list of choices
	 * @param 
	 * 		location where the dropdown menu was originally clicked from.
	 * 		This will end up being somewhere on the edge of the dropdown
	 * 		menu, with a best-effort for it being on the top-left
	 * @param enabledCheck
	 * 		Checks if the choices are enabled based on the metadata from this menu
	 * @param background the color of the background of this dropdown. May be transparent
	 * @param highlight the color when a mouse is hovering over a segment
	 * @param pressHighlight the color when a mouse is pressed and hovering
	 * @param lines the color of the lines seperating the options
	 * @param text the color of the text
	 * @param textDisabled the color of disabled text
	 */
	public DropdownMenu(List<Object> choices, EnableChecker enabledCheck, Point location,
			Color background, Color highlight, Color pressHighlight,
			Color lines, Color text, Color textDisabled) {
		CEventManager.instance.register(this);
		this.choices = choices;
		this.enabledCheck = enabledCheck;
		this.location = location;
		this.background = background;
		this.highlight = highlight;
		this.pressHighlight = pressHighlight;
		this.lines = lines;
		this.text = text;
		this.textDisabled = textDisabled;
		
		this.currentDisplay = choices;
		
		lastRenders = new Rectangle[choices.size()];
		width = -1;
		heightPerRow = 50;
	}
	
	/**
	 * Creates a dropdown menu
	 * 
	 * @param choices the list of choices
	 * @param enabledCheck checks whether the objects are enabled. May use metadata
	 * 			from getMeta to diagnose information
	 * @param 
	 * 		location where the dropdown menu was originally clicked from.
	 * 		This will end up being somewhere on the edge of the dropdown
	 * 		menu, with a best-effort for it being on the top-left
	 */
	public DropdownMenu(List<Object> choices, EnableChecker enabledCheck, Point location) {
		this(	choices, 
				enabledCheck,
				location, 
				Color.white,
				new Color(135, 206, 250), // lightish blue
				new Color(30, 144, 255), // darker blue
				Color.lightGray,
				Color.darkGray,
				Color.gray);
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
		
		if(width == -1) {
			Font font = graphics.getFont();
			int maxWidth = -1;
			for(Object obj : currentDisplay) {
				if(getWidth(font, obj) > maxWidth)
					maxWidth = getWidth(font, obj);
			}
			width = maxWidth + 6;
		}
		
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
		
		
		graphics.setColor(lines);
		int y = topLeftY;
		for(int i = 0; i < currentDisplay.size(); i++) {
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
		
		for(int i = 0; i < currentDisplay.size(); i++) {
			if(enabledCheck.isEnabled(currentDisplay.get(i), getMeta()))
				graphics.setColor(text);
			else
				graphics.setColor(textDisabled);
			
			Object toDisp = currentDisplay.get(i);
			display(graphics, toDisp, lastRenders[i]);
		}
		
		graphics.setColor(old);
	}
	
	private void display(Graphics graphics, Object toDisp, Rectangle rect) {
		if(toDisp instanceof List<?>) {
			List<?> asList = (List<?>) toDisp;
			if(asList.size() < 1) {
				throw new IllegalArgumentException("Empty list passed as object to display parameters in dropdown menu");
			}
			
			String str = asList.get(0).toString();
			GUtils.drawCenteredInRect(graphics, text, str, rect);
			Image img = ResourceManager.getResource("right-arrow.png");
			
			float x = (rect.getX() + rect.getWidth()) - (img.getWidth() + 1);
			float y = rect.getY() + rect.getHeight() - img.getHeight();
			img.draw(x, y);
		}else {
			GUtils.drawCenteredInRect(graphics, text, toDisp.toString(), rect);
		}
	}
	
	private int getWidth(Font font, Object toDisp) {
		if(toDisp instanceof List<?>) {
			List<?> asList = (List<?>) toDisp;
			if(asList.size() < 1) {
				throw new IllegalArgumentException("Empty list passed as object to display parameters in dropdown menu");
			}
			
			String str = asList.get(0).toString();
			Image img = ResourceManager.getResource("right-arrow.png");
			return img.getWidth() + font.getWidth(str);
		}else {
			return font.getWidth(toDisp.toString()); 
		}
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
		
		if(!Mouse.isButtonDown(0) || (System.currentTimeMillis() - lastPress) < 500)
			return;
		
		for(int i = 0; i < lastRenders.length; i++) {
			if(isHighlighted(i)) {
				lastPress = System.currentTimeMillis();
				Object obj = currentDisplay.get(i);
				if(obj instanceof List<?>) {
					List<?> tmpList = (List<?>) obj;
					currentDisplay = new ArrayList<Object>();
					for(int j = 1; j < tmpList.size(); j++) {
						currentDisplay.add(tmpList.get(j));
					}
					lastRenders = new Rectangle[currentDisplay.size()];
					return;
				}
				CEventManager.instance.broadcast(EventType.DROPDOWN_MENU_CHOICE, this, currentDisplay.get(i), i);
				CEventManager.instance.broadcast(EventType.MENUS_DESTROYED);
			}
		}
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return currentDisplay.size() * heightPerRow;
	}
	
	// Utility methods
	
	public boolean isHighlighted(String choice) {
		return isHighlighted(choices.indexOf(choice));
	}
	
	public boolean isHighlighted(int index) {
		if(!enabledCheck.isEnabled(currentDisplay.get(index), getMeta()))
			return false;
		return lastRenders[index].contains(Mouse.getX(), GUtils.height - Mouse.getY());
	}
	
	public Map<String, Object> getMeta() {
		if(meta == null)
			meta = new HashMap<>();
		return meta;
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

	public boolean containsMouse() {
		return Mouse.getX() > location.getX() && Mouse.getX() < location.getX() + getWidth() &&
				Mouse.getY() > location.getY() && Mouse.getY() < location.getY() + getHeight();
	}
}
