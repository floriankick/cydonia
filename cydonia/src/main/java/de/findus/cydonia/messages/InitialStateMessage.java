/**
 * 
 */
package de.findus.cydonia.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

import de.findus.cydonia.level.WorldState;

/**
 * @author Findus
 *
 */
@Serializable
public class InitialStateMessage extends AbstractMessage {

	private WorldState worldState;

	
	public InitialStateMessage() {
		setReliable(true);
	}


	public WorldState getWorldState() {
		return worldState;
	}


	public void setWorldState(WorldState worldState) {
		this.worldState = worldState;
	}
}
