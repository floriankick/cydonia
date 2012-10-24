/**
 * 
 */
package de.findus.cydonia.events;

import com.jme3.network.serializing.Serializable;

/**
 * @author Findus
 *
 */
@Serializable
public interface Event {

	/**
	 * Returns true if this event should be forwarded or broadcasted over the network connection.
	 * @return network forwarding
	 */
	public boolean isNetworkEvent();
	
	/**
	 * Activates/deactivates network forwarding for this event.
	 * @param net network forwarding
	 */
	public void setNetworkEvent(boolean net);
}
