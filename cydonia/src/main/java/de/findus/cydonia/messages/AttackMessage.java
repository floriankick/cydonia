/**
 * 
 */
package de.findus.cydonia.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;


/**
 * @author Findus
 *
 */
@Serializable
public class AttackMessage extends AbstractMessage {

	private int playerid;
	
	/**
	 * Sets reliable to true.
	 */
	public AttackMessage() {
		setReliable(true);
	}

	public int getPlayerid() {
		return playerid;
	}

	public void setPlayerid(int playerid) {
		this.playerid = playerid;
	}
}
