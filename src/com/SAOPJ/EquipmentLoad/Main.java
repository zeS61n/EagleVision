package com.SAOPJ.EquipmentLoad;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import com.codingforcookies.armorequip.ArmorListener;

public class Main extends JavaPlugin implements Listener{
	
		public void onEnable()
		{
			saveDefaultConfig();
			getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
			Bukkit.getServer().getPluginManager().registerEvents(this, this);
			
			//获取在线玩家负重/
			getLogger().info("插件已加载");
		}
		
		public void onDisable()
		{
			PlayerLoad.clear();
			mainHandNum.clear();
			getLogger().info("插件已卸载");
		}
		
		public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args)
		{
			if (cmd.getName().equalsIgnoreCase("FUZHONG") || cmd.getName().equalsIgnoreCase("FZ"))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage("控制台无法执行");
					return true;
				}
				
				Player player = (Player)sender;
				int load = getPlayerLoad(player.getName());
				String message = this.getConfig().getString("fuzhongMessage");
				@SuppressWarnings("deprecation")
				String loadLimit = String.valueOf(player.getMaxHealth()/2);
				sender.sendMessage( message + load + "/" + loadLimit );		
				return true;
			}
			return false;
		}
		
		
		
		//玩家登陆时 会同时出发PlayerItemHeldEvent事件
		//当玩家上线重新计算负重/*/*/*获取手持负重   且    做成方法
		HashMap<String,Integer> PlayerLoad = new HashMap<String, Integer>();
		@EventHandler(ignoreCancelled=true)
		public void onPlayerOnline(PlayerJoinEvent event){
			Player player = event.getPlayer();
			
			int load = 0;
			
			if(player.getInventory().getHelmet() != null){
				load = load + getLoad(player.getInventory().getHelmet());
			}
			if(player.getInventory().getChestplate() != null){
				load = load + getLoad(player.getInventory().getChestplate());
			}
			if(player.getInventory().getLeggings() != null){
				load = load + getLoad(player.getInventory().getLeggings());
			}
			if(player.getInventory().getBoots() != null){
				load = load + getLoad(player.getInventory().getBoots());
			}
//			int i = mainHandNum.get(player.getName());
//			player.sendMessage(String.valueOf(i));
//			if(player.getInventory().getItem(mainHandNum.get(player.getName()).intValue()) != null ){
//				
//				load = load + getLoad(player.getInventory().getItem(mainHandNum.get(player.getName()).intValue()));
//			}
			PlayerLoad.put(player.getName(), load);
			setLoad(player, load);
			
			//PlayerItemHeldEvent事件显示message
			//player.sendMessage("online你当前装备负重： " + load);
		}
		
		
		
		//检测快捷栏切换时手持武器负重
		HashMap<String,Integer> mainHandNum = new HashMap<String,Integer>();
		@EventHandler(ignoreCancelled=true)
		public void onHeldItem(PlayerItemHeldEvent event){
			Player player = event.getPlayer();
			
			//空手 换 道具
			int slot = event.getNewSlot();
			int oldSlot = event.getPreviousSlot();
			int load = PlayerLoad.get(player.getName());
			
			if(player.getInventory().getItem(oldSlot) == null && player.getInventory().getItem(slot) != null ){
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				
				//player.sendMessage("空手 换 道具");
			}
			
			//当 道具 切换 空手 时
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) == null ){
				
				ItemStack item = player.getInventory().getItem(oldSlot);
				
				int OldLoad = getLoad(item);
				load = load - OldLoad;
				
				//player.sendMessage("当 道具 切换 空手 时");
			}
			
			//当  道具  切换  道具  时
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && !player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				
				ItemStack item = player.getInventory().getItem(slot);
				ItemStack oldItem = player.getInventory().getItem(oldSlot);
				
				int newLoad = getLoad(item);
				int oldLoad = getLoad(oldItem);
				load = load - oldLoad + newLoad;
				
				//player.sendMessage("当  道具  切换  道具  时");
			}
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				
				//player.sendMessage("online时记录主手道具负重值");
				
			}
			else{
				player.sendMessage("onHeldItem:else");
				mainHandNum.put(player.getName(), slot);
				return;
			}
			player.sendMessage("记录主手位： " + String.valueOf(slot));
			mainHandNum.put(player.getName(), slot);
			PlayerLoad.put(player.getName(), load);
			setLoad(player,load);
			player.sendMessage("你当前装备负重： " + load);
		}
		
		
		
		//监听更换装备事件  计算负重值
		@EventHandler(ignoreCancelled=true)
		public void onEquip(ArmorEquipEvent event){
			
			int load = PlayerLoad.get(event.getPlayer().getName());
			
			//穿上装备  原本无装备
			if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && (event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() == Material.AIR)){
				
				event.getPlayer().sendMessage(event.getMethod().name());
				if(!event.getMethod().name().equals("HOTBAR")){
					int NewLoad = getLoad(event.getNewArmorPiece());
					load = load + NewLoad;
				}
			}
			
			//穿上装备  原本有装备
			else if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && event.getOldArmorPiece() != null && event.getOldArmorPiece().getType() != Material.AIR){
				int newLoad = getLoad(event.getNewArmorPiece());
				int oldLoad = getLoad(event.getOldArmorPiece());
				load = load - oldLoad + newLoad;
			}
			
			//脱下装备
			else if( (event.getNewArmorPiece() == null || event.getNewArmorPiece().getType() == Material.AIR ) && event.getOldArmorPiece() != null && event.getOldArmorPiece().getType() != Material.AIR){
				int oldLoad = getLoad(event.getOldArmorPiece());
				load = load - oldLoad;
			}
			else{
				getLogger().info("error onEquip");
				return;
			}
			PlayerLoad.put(event.getPlayer().getName(), load);
			setLoad(event.getPlayer(), load);
			event.getPlayer().sendMessage("你当前装备负重：" + load);
		}
		

		
		//检测打开背包 点击换道具到主手的事件
		//List<Integer> hotBarSlots = new ArrayList<Integer>(Arrays.asList(36,37,38,39,40,41,42,43,44));
		@EventHandler(ignoreCancelled=true)
		public void onSwapItemToHotBar(InventoryClickEvent event){
			if(event.getWhoClicked().getType() == EntityType.PLAYER ){
				Player player = (Player)event.getWhoClicked();
				
				
//				player.sendMessage("click:" + event.getClick().toString());
//				player.sendMessage("action" + event.getAction().toString());
				
				
				
//				boolean isHotBar = false;
//				for(int i : hotBarSlots){
//					if(event.getRawSlot() == i){
//						isHotBar = true;
//					}
//				}
				//检测是否主手
				//player.sendMessage("rawSlot: " + event.getSlot());
				//player.sendMessage("mainHandNum: " + mainHandNum.get(player.getName()));
				if(event.getSlot() == mainHandNum.get(player.getName())){
					//空手 换 道具 
					if(event.getAction() == InventoryAction.PLACE_ALL){
						player.sendMessage("place");
						ItemStack item = event.getCursor();
						player.sendMessage(item.getItemMeta().getDisplayName());
						int load = getLoad(item);
						
					}
					
					//道具 换 空手
					if(event.getAction() == InventoryAction.PICKUP_ALL){
						player.sendMessage("pick");
					}
					
					//道具  换  道具
					if(event.getAction() == InventoryAction.SWAP_WITH_CURSOR){
						player.sendMessage("swap");
					}
				}

				
			}
				
		}
		
		
		
		

		
		
		
		//获取道具的重量值
		public int getLoad(ItemStack item){
			int load = 0;
			if( item.getItemMeta().getLore() != null){
				List<String> lore = item.getItemMeta().getLore();
				String loreCheck = this.getConfig().getString("loreCheck");
				for(String l : lore){
					if( l.contains(loreCheck)){
						load = load + Integer.parseInt(l.split(loreCheck)[1]);
					}
				}
				return load;
			}
			return 0;
		}
		
		
		
		//超负重惩罚
		public void setLoad(Player player,int load){
			//int loadLimit1 = Integers.parseInt(this.getConfig().getString("loadLimit"));
			@SuppressWarnings("deprecation")
			double loadLimit = player.getMaxHealth()/2 ;
			
			if(load > loadLimit){
				PotionEffect slow = new PotionEffect(PotionEffectType.SLOW,99999,0);
				player.addPotionEffect(slow);
				String overLoadMessage = getConfig().getString("overLoadMessage");
				
				player.sendMessage(overLoadMessage.replace("&","§"));
			}
			if(load <= loadLimit && player.hasPotionEffect(PotionEffectType.SLOW)){
				player.removePotionEffect(PotionEffectType.SLOW);
			}
		}
		
		
	
		
		//查询当前负重值接口
		public int getPlayerLoad(String name){
			return PlayerLoad.get(name);
		}
		
		
		
		
		
		
		
		
		
}
