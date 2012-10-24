/**
 * 
 */
package de.findus.cydonia.events;


/**
 * @author Findus
 *
 */
public class RespawnEvent extends AbstractEvent {

	private int playerid;
	
	/**
	 * 
	 */
	public RespawnEvent() {
		setNetworkEvent(true);
	}

	/**
	 * @return the playerid
	 */
	public int getPlayerid() {
		return playerid;
	}

	/**
	 * @param playerid the playerid to set
	 */
	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}
}