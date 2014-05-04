/**
 * 
 */
package de.encala.cydonia.share.events;

import de.encala.cydonia.game.level.Map;

/**
 * @author encala
 * 
 */
public class ConnectionInitEvent extends AbstractEvent {

	private String message;

	private String level;

	private Map map;

	public ConnectionInitEvent() {
		setNetworkEvent(false);
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @param level
	 *            the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}
}