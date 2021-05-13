package com.Dolo.EagleVision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

//set which type block can ignore and lock
//蓄力 title进度条 致盲 粒子圈圈 发光标记
//末影之眼 shift右键 打开Gui 加入map<list（）>
public class Main extends JavaPlugin implements Listener{
	
	public Configuration mesConfig;
	ProtocolManager pm;
	Map<String,Boolean> isEVMap = new HashMap<String,Boolean>();
	Map<String,List<String>> shareMap = new HashMap<String,List<String>>();
	
	@Override
	public void onEnable()
	{
		this.saveDefaultConfig();
		FileConfig.saveDefaultConfig(this, "message.yml");
		this.mesConfig = FileConfig.load(this, "message.yml");
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		pm = ProtocolLibrary.getProtocolManager();
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		    isEVMap.put(player.getName(), false);
		    List<String> playerNames = new ArrayList<String>();
		    shareMap.put(player.getName(), playerNames);
		}
		getLogger().info("插件已加载喵");
	}
	
	public void onDisable()
	{
		pm = ProtocolLibrary.getProtocolManager();
		pm.getPacketListeners().forEach( e -> pm.removePacketListener(e));
		getLogger().info("插件已卸载喵");
		
	}
	
	public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args)
	{
		if ( label.equalsIgnoreCase("EagleVision") || label.equalsIgnoreCase("ev"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("§c控制台无法执行的哦！");
				return true;
			}
			
			Player p = (Player) sender;
			if(args.length == 0){
				/****开启鹰眼模式****/
				if(!isEVMap.get(p.getName())){
					isEVMap.put(p.getName(), true);
					
					String onVision = this.mesConfig.getString("onVision").replace("&","§");
					sender.sendMessage(onVision);
					return true;
				}
				else if(isEVMap.get(p.getName())){
					isEVMap.put(p.getName(), false);
					String offVision = this.mesConfig.getString("offVision").replace("&","§");
					sender.sendMessage(offVision);
					return true;
				}	
			}
			//查询共享玩家
			if(args.length == 1 && args[0].equalsIgnoreCase("list")){
				String list = "";
				for(String pn : shareMap.get(p.getName())){
					list = list + pn + " , ";
				}
				String checkSharePlayers = this.mesConfig.getString("checkSharePlayers").replace("&","§");
				sender.sendMessage(checkSharePlayers + list);
				return true;
			}
			if(args.length == 1 && args[0].equalsIgnoreCase("near")){
				List<Entity> tagEntities = p.getNearbyEntities(5, 5, 5);
				for(Entity en : tagEntities){
					AddGlowing.addGlow(this, pm, p, en, shareMap);
				}
				return true;
			}
			
			return false;
		}
		return false;
	}
	
	//标记
	@EventHandler(priority = EventPriority.NORMAL)//(ignoreCancelled=true)
	public void onPlayerClickEntity(PlayerInteractEvent e){
		
		Player p = e.getPlayer();
		//e.getPlayer().sendMessage(e.getAction().toString());
		
		if(!p.isSneaking()) return;
		if(e.getAction().equals(Action.PHYSICAL)) return;
			
		//保护我方末影之眼
		if(e.getAction().equals(Action.RIGHT_CLICK_AIR)|| e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			if(p.getItemInHand().getType().equals(Material.EYE_OF_ENDER))
				e.setCancelled(true);
		
		if(!isEVMap.get(p.getName())) return;
		
		double range = 20;
		try {
			range =  Double.parseDouble(this.getConfig().getString("tag range"));
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
		
		Entity entity = getCursorTarget(e.getPlayer(), range);
		
		if(entity == null){
			//e.getPlayer().sendMessage( "无标记");
			return;
		}
		
		AddGlowing.addGlow(this, pm, e.getPlayer(),entity, shareMap);
		//e.getPlayer().sendMessage( "标记：" + entity.getType().toString());
		return;
	}
	
	
	
	//共享
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerClickPlayer(PlayerInteractEntityEvent e){
		Player p = e.getPlayer();
		
		if(!p.isSneaking()) return;
		if(!e.getRightClicked().getType().equals(EntityType.PLAYER)) return;
		if(!p.getItemInHand().getType().equals(Material.EYE_OF_ENDER)) return;
		
		for(String pn : shareMap.get(p.getName())){
			if(pn.equalsIgnoreCase(e.getRightClicked().getName())){
				p.sendMessage("已存在");
				return;
			}
		}
		shareMap.get(p.getName()).add(e.getRightClicked().getName());
		shareMap.get(e.getRightClicked().getName()).add(p.getName());
		e.getPlayer().sendMessage(e.getHand().toString());
	}
	
	
	
	@EventHandler(ignoreCancelled=true)
	public void onPlayerOnline(PlayerJoinEvent e){
		isEVMap.put(e.getPlayer().getName(), false);
		List<String> playerNames = new ArrayList<String>();
		shareMap.put(e.getPlayer().getName(), playerNames);
	}
	
	//检测鼠标指针上的生物
	//Code from http://www.mcbbs.net/thread-773838-1-1.html
	public Entity getCursorTarget(Player p, double range){
        Entity target;
        Iterator<Entity> entities;
        Location loc = p.getEyeLocation();
        Vector vec = loc.getDirection().multiply(0.15);
        Block block = null;
        List<String> igBlock = this.getConfig().getStringList("ignored block");
        //p.sendMessage(igBlock.toString());
        
        while( ((range-=0.1)>0) && ((block = loc.getWorld().getBlockAt(loc)).isLiquid() || block.isEmpty()) || igBlock.toString().contains(block.getType().toString()) ){
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
}
