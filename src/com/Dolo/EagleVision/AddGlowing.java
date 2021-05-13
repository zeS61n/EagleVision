package com.Dolo.EagleVision;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer;

public class AddGlowing {
	
	//Code from Spigot
	public static void addGlow(JavaPlugin plugin, ProtocolManager pm, Player player , Entity en , Map<String,List<String>> shareMap) {
	    PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
	    packet.getIntegers().write(0, en.getEntityId()); //Set packet's entity id
	    WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
	    Serializer serializer = Registry.get(Byte.class); //Found this through google, needed for some stupid reason
	    watcher.setEntity(player); //Set the new data watcher's target
	    watcher.setObject(0, serializer, (byte) (0x40)); //Set status to glowing, found on protocol page
	    packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects()); //Make the packet's datawatcher the one we created
	   
	    try {
	    	pm.sendServerPacket(player, packet);
	    	player.sendMessage( "标记：" + en.getType().toString());
	    	for(String playerName : shareMap.get(player.getName())){
	    		if(playerName != null){
	    			pm.sendServerPacket(plugin.getServer().getPlayer(playerName), packet);
	    			//player.sendMessage( "send标记：" + en.getType().toString());
	    		}
	    	}       
	    } catch (InvocationTargetException e) {
	        e.printStackTrace();
	    }
	    
	    //reset
	    Integer glowTime =  Integer.parseInt((plugin.getConfig().getString("glowing time")));
	    if(glowTime >= 9999 || glowTime < 0) return;
    	new BukkitRunnable(){
    	    int s = glowTime;
    	    @Override    
    	    public void run(){
    	        s--;
    	        if(s==0){
    	        	try {//共享取消！
    	        		watcher.setObject(0, serializer, (byte) (0));
    	        	    packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
    	        		pm.sendServerPacket(player, packet);
						for(String playerName : shareMap.get(player.getName())){
							if(playerName != null){
								pm.sendServerPacket(plugin.getServer().getPlayer(playerName), packet);
							}
				    	}  
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
    	            cancel();
    	        }
    	    } 
    	}.runTaskTimer(plugin, 0L, 20L);
    	
	}
}
