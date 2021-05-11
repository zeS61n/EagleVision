package com.Dolo.EagleVision;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer;

public class Main extends JavaPlugin implements Listener{
	
	ProtocolManager pm;
	Map<String,Boolean> isEVMap = new HashMap<String,Boolean>();
	
	@Override
	public void onEnable()
	{
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		pm = ProtocolLibrary.getProtocolManager();
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		    isEVMap.put(player.getName(), false);
		}
		getLogger().info("插件已加载");
	}
	
	public void onDisable()
	{
		pm = ProtocolLibrary.getProtocolManager();
		pm.getPacketListeners().forEach( e -> pm.removePacketListener(e));
		getLogger().info("插件已卸载");
		
	}
	
	public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args)
	{
		if ( label.equalsIgnoreCase("EagleVision") || label.equalsIgnoreCase("ev"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("控制台无法执行");
				return true;
			}
			
			Player p = (Player) sender;
			if(args.length == 0){
				/****开启鹰眼模式****/
				if(!isEVMap.get(p.getName())){
					isEVMap.put(p.getName(), true);
					sender.sendMessage("开启鹰眼！");
					return true;
				}
				else if(isEVMap.get(p.getName())){
					isEVMap.put(p.getName(), false);
					sender.sendMessage("关闭鹰眼！");
					return true;
				}
				
			}
			
			return false;
		}
		return false;
	}
	
	
	@EventHandler//(ignoreCancelled=true)
	public void onPlayerClickEntity(PlayerInteractEvent e){
		
		Player p = e.getPlayer();
		e.getPlayer().sendMessage(e.getAction().toString());
		
		if(!p.isSneaking()) return;
		if(!isEVMap.get(p.getName())) return;
		if(e.getAction().equals(Action.LEFT_CLICK_AIR)){
			Entity entity = getCursorTarget(e.getPlayer(), 10);
			
			if(entity == null){
				e.getPlayer().sendMessage( "无标记");
				return;
			}
			
			addGlow(e.getPlayer(),entity);
			e.getPlayer().sendMessage( "标记：" + entity.getType().toString());
			return;
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onPlayerOnline(PlayerJoinEvent e){
		isEVMap.put(e.getPlayer().getName(), false);
	}
	
	
	public void addGlow(Player player , Entity en) {
	    PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
	    packet.getIntegers().write(0, en.getEntityId()); //Set packet's entity id
	    WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
	    Serializer serializer = Registry.get(Byte.class); //Found this through google, needed for some stupid reason
	    watcher.setEntity(player); //Set the new data watcher's target
	    watcher.setObject(0, serializer, (byte) (0x40)); //Set status to glowing, found on protocol page
	    packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects()); //Make the packet's datawatcher the one we created
	    try {
	        pm.sendServerPacket(player, packet);
	    } catch (InvocationTargetException e) {
	        e.printStackTrace();
	    }
	}
	
	
	public Entity getCursorTarget(Player p, double range){
        Entity target;
        Iterator<Entity> entities;
        Location loc = p.getEyeLocation();
        Vector vec = loc.getDirection().multiply(0.15);
        Block block;
        while( ((range-=0.1)>0) && ((block = loc.getWorld().getBlockAt(loc)).isLiquid() || block.isEmpty()) ){
        	entities = loc.getWorld().getNearbyEntities(loc.add(vec), 0.001, 0.001, 0.001).iterator();
	        while(entities.hasNext()){
	        	if((target = entities.next()) != p){
	        		return target;
	                }
	            }
        }
	    return null;
	}
		
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*test for GlowAPI
	@EventHandler
	public void onJoin(final PlayerJoinEvent event) {
	    //Delay the update by a few ticks until the player is actually on the server
	    Bukkit.getScheduler().runTaskLater(this, new Runnable() {
	        @Override
	        public void run() {
	            //Set the event's player glowing in DARK_AQUA for all online players
	            GlowAPI.setGlowing(event.getPlayer(), GlowAPI.Color.DARK_AQUA, Bukkit.getOnlinePlayers());
	        }
	    }, 10);
	}
	*/
	
	/*error setGlowing
	public void setGlowing(Player p , Entity en){
		PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
		
		packet.getIntegers().write(0,en.getEntityId());
		packet.getmea
		WrappedWatchableObject> Meradata = 
		
		packet.getIntegers().write(0, (int)en.getEntityId());	
		packet.getBytes().write(0, (byte)(24&255));				
		packet.getBytes().write(1, (byte)(0&255));				
		packet.getIntegers().write(1, (int)100);				
		packet.getBytes().write(2,(byte)1);						
		
		try{
			pm.sendServerPacket(p, packet,false);
		} catch (InvocationTargetException ex) {
			ex.printStackTrace();
		}
	}
	*/
}
