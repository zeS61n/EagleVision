package com.SAOPJ.EquipmentLoad;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
			getLogger().info("插件已加载");
		}
		
		public void onDisable()
		{
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
				int load = PlayerLoad.get(player.getName());
				String message = this.getConfig().getString("fuzhongMessage");
				sender.sendMessage( message + load);		
				return true;
			}
			return false;
		}
		
		
		
		//监听更换装备事件  计算负重值
		@EventHandler(ignoreCancelled=true)
		public void onEquip(ArmorEquipEvent event){
			
			int load = PlayerLoad.get(event.getPlayer().getName());
			
			//穿上装备  原本无装备
			if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && (event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() == Material.AIR)){
				int NewLoad = getLoad(event.getNewArmorPiece());
				load = load + NewLoad;
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
			//event.getPlayer().sendMessage("你当前装备负重：" + load);
		}
		

		
		//当玩家上线重新计算负重
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
			
			PlayerLoad.put(player.getName(), load);
			setLoad(player, load);
			//player.sendMessage("你当前装备负重： " + load);
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
		
		
		
		/*
		public class ItemInfo extends JavaPlugin{
			@Override
			public void onEnable(){
				getLogger().info("物品信息插件加载完毕");
			}
			public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
				if(label.equalsIgnoreCase("iteminfo")){
					if(sender instanceof Player){
						Player player = (Player)sender;
						if(args.length==0){
							player.sendMessage("§a/iteminfo meta 获取手上物品的信息");
							return true;
						}else{
							if(args[0].equalsIgnoreCase("meta")){
								//返回玩家手中的物品信息
								getItemInfo(player,player.getItemInHand());
								return true;
							}
						}
					}
				}
				return false;
			}
			@SuppressWarnings("deprecation")
			public void getItemInfo(Player player,ItemStack item){
				//接下来就来获取所有信息了。
				if(item==null){
					player.sendMessage("§c你手上没有任何物品!");
					return;
				}
				int id,amount;
				short durability;
				byte data;
				String displayName;
				List<String> lore = new ArrayList<String>();
				Map<Enchantment, Integer> ench = new HashMap<Enchantment, Integer>();
				id = item.getTypeId();//获取ID
				amount = item.getAmount();//获取数量
				durability = item.getDurability();//获取损坏程度
				data = item.getData().getData();//获取data
				if(item.getItemMeta().hasDisplayName()){//判断是否设置名称
					displayName = item.getItemMeta().getDisplayName();
				}else{
					displayName = "没有修改名称";
				}
				lore = item.getItemMeta().getLore();
				ench = item.getItemMeta().getEnchants();
				player.sendMessage("§a物品 I D:§6"+id);
				player.sendMessage("§a物品数量:§6"+amount);
				player.sendMessage("§a物品损坏:§6"+durability);
				player.sendMessage("§a物品种类:§6"+data);
				player.sendMessage("§a物品名称:§6"+displayName);
				if(item.getItemMeta().hasEnchants()){
					player.sendMessage("§a物品附魔:");
					for(Enchantment e : ench.keySet()){
						player.sendMessage("§a附魔ID:§6"+e.getId()+"§a,附魔等级:§6"+ench.get(e));
					}
				}else{
					player.sendMessage("§a物品附魔:§6没有附魔");
				}
				if(item.getItemMeta().hasLore()){
					player.sendMessage("§a物品Lore:");
					for(String l : lore){
						player.sendMessage("§aLore:§f"+l);
					}
				}else{
					player.sendMessage("§a物品Lore:§6没有lore信息");
				}
			}
			@Override
			public void onDisable(){
				getLogger().info("物品信息插件卸载完毕");
			}
		}
		
		*/
		
		
		
		
		
		
		
		
		
		
}
