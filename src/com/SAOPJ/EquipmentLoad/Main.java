package com.SAOPJ.EquipmentLoad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import com.codingforcookies.armorequip.ArmorListener;

/*加上道具数量*负重*/
public class Main extends JavaPlugin implements Listener{
	
		boolean debug = false;
		boolean needUpdate = false;
		HashMap<String,Integer> PlayerLoad = new HashMap<String, Integer>();
		Map<String,Boolean> isMainHandNull = new HashMap<String,Boolean>();
		Map<String,Boolean> isOffHandNull = new HashMap<String,Boolean>();
	
		public void onEnable()
		{
			saveDefaultConfig();
			getServer().getPluginManager().registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
			Bukkit.getServer().getPluginManager().registerEvents(this, this);
			//默认关闭的debug模式
			if(this.getConfig().getString("debugMode").equals("true")){
				debug = true;
				getLogger().info("已开启Debug模式");
			}
			
			//获取在线玩家负重/
			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			    updatePlayerLoad(player);
			}
			
			getLogger().info("插件已加载");
		}
		
		public void onDisable()
		{
			getLogger().info("插件已卸载");
		}
		
		public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args)
		{
			if ( label.equalsIgnoreCase("FUZHONG") || label.equalsIgnoreCase("FZ"))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage("控制台无法执行");
					return true;
				}
				
				if(args.length == 0){
					Player player = (Player)sender;
					//int load = getPlayerLoad(player.getName());
					int load = updatePlayerLoad(player);
					String message = this.getConfig().getString("fuzhongMessage");
					@SuppressWarnings("deprecation")
					String loadLimit = String.valueOf(player.getMaxHealth()/2);
					sender.sendMessage( message + load + "/" + loadLimit );		
					return true;
				}
				
				if(args.length == 1 && args[0].equalsIgnoreCase("debug")){
					if(debug){
						debug = false;
						String offDebugMessage = this.getConfig().getString("offDebugMessage");
						sender.sendMessage( offDebugMessage.replace("&","§") );
					}
					else if(!debug){
						debug = true;
						String onDebugMessage = this.getConfig().getString("onDebugMessage");
						sender.sendMessage( onDebugMessage.replace("&","§") );
					}
					Player player = (Player)sender;
					player.sendMessage("isMainNull: " + String.valueOf(isMainHandNull(player)));
					player.sendMessage("isOffNull: " + String.valueOf(isOffHandNull(player)));
					return true;
				}
				else if(args.length == 1){
					String playerName = args[0];
					Player player = null;
					boolean isOnline = false;
					
					for (Player p : Bukkit.getServer().getOnlinePlayers()) {
						if( playerName.equalsIgnoreCase(p.getName()) ){
							isOnline = true;
							player = p;
							//sender.sendMessage(playerName);
						}
					}
					
					if(!isOnline){
						String message = this.getConfig().getString("pfzNoOlineMessage");
						sender.sendMessage(message.replace("&","§"));
						return true;
					}
					else if(isOnline){
						updatePlayerLoad(player);
						String message = this.getConfig().getString("pfzMessage");
						message = message.replace("%player", playerName);
						sender.sendMessage(message.replace("&","§") + PlayerLoad.get(playerName));
						return true;
					}
				}
				
				return false;
			}
			
			
			return false;
		}
		
		
		
		//当玩家上线重新计算负重  玩家登陆时 会同时触发PlayerItemHeldEvent事件
		@EventHandler(ignoreCancelled=true)
		public void onPlayerOnline(PlayerJoinEvent event){
			
			Player player = event.getPlayer();
			
			updatePlayerLoad(player);
			
			//PlayerItemHeldEvent事件显示message
			//player.sendMessage("online你当前装备负重： " + load);
		}
		
		
		
		//检测快捷栏切换时手持武器负重
		@EventHandler(ignoreCancelled=true)
		public void onHeldItem(PlayerItemHeldEvent event){
			if(event.isCancelled()) return;
			
			
			Player player = event.getPlayer();
			
			int slot = event.getNewSlot();
			int oldSlot = event.getPreviousSlot();
			int load = PlayerLoad.get(player.getName());
			
			//空手 换 道具
			if(player.getInventory().getItem(oldSlot) == null && player.getInventory().getItem(slot) != null ){
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				isMainHandNull.put(player.getName(), false);
				
				if(debug){
					player.sendMessage("空手 换 道具");
				}
			}
			
			//当 道具 切换 空手 时
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) == null ){
				ItemStack item = player.getInventory().getItem(oldSlot);
				
				int OldLoad = getLoad(item);
				load = load - OldLoad;
				isMainHandNull.put(player.getName(), true);
				
				if(debug){
					player.sendMessage("当 道具 切换 空手 时");
				}
			}
			
			//当  道具  切换  道具  时
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && !player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				ItemStack item = player.getInventory().getItem(slot);
				ItemStack oldItem = player.getInventory().getItem(oldSlot);
				
				int newLoad = getLoad(item);
				int oldLoad = getLoad(oldItem);
				load = load - oldLoad + newLoad;
				
				if(debug){
					player.sendMessage("当  道具  切换  道具  时");
				}
			}
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				
				/*
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				isMainHandNull.put(player.getName(), false);
				
				
				if(debug){
					player.sendMessage("joinGame时记录主手道具负重值: " + newLoad);
				}
				*/
			}
			else{
				if(debug){
					player.sendMessage("onHeldItem: Air");
				}
				return;
			}
			if(debug){
				player.sendMessage("记录主手位： " + String.valueOf(slot));
				player.sendMessage("你当前装备负重： " + load);
			}
			
			PlayerLoad.put(player.getName(), load);
			setLoad(player,load);
		}
		
		
		
		//监听更换装备事件  计算负重值
		@EventHandler(ignoreCancelled=true)
		public void onEquip(ArmorEquipEvent event){
			if(event.isCancelled()) return;
			
			int load = PlayerLoad.get(event.getPlayer().getName());
			
			//穿上装备  原本无装备
			if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && (event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() == Material.AIR)){
				
				if(debug){
					event.getPlayer().sendMessage("onEquip.Method: " + event.getMethod().name());
				}
				
				if(!event.getMethod().name().equals("HOTBAR")){
					int NewLoad = getLoad(event.getNewArmorPiece());
					load = load + NewLoad;
					
					if(debug){
						event.getPlayer().sendMessage("穿戴装备 原本无装备");
					}
				}
			}
			
			//穿上装备  原本有装备
			else if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && event.getOldArmorPiece() != null && event.getOldArmorPiece().getType() != Material.AIR){
				int newLoad = getLoad(event.getNewArmorPiece());
				int oldLoad = getLoad(event.getOldArmorPiece());
				load = load - oldLoad + newLoad;
				
				if(debug){
					event.getPlayer().sendMessage("穿戴装备 原本有装备");
				}
			}
			
			//脱下装备
			else if( (event.getNewArmorPiece() == null || event.getNewArmorPiece().getType() == Material.AIR ) && event.getOldArmorPiece() != null && event.getOldArmorPiece().getType() != Material.AIR){
				int oldLoad = getLoad(event.getOldArmorPiece());
				load = load - oldLoad;
				
				if(debug){
					event.getPlayer().sendMessage("脱下装备");
				}
			}
			else{
				if(debug){
					getLogger().info("error onEquip");
					event.getPlayer().sendMessage("error: onEquip");
				}
				return;
			}
			PlayerLoad.put(event.getPlayer().getName(), load);
			setLoad(event.getPlayer(), load);
			
			if(debug){
				event.getPlayer().sendMessage("你当前装备负重：" + load);
			}
		}
		

		//SlotType:CONTAINER下记录mainHand是否为空 
		//检测 在背包中   点击/热键/shift 换道具到主手/副手的事件
		@EventHandler(ignoreCancelled=true)
		public void onSwapItemToHotBar(InventoryClickEvent event){
			if(event.isCancelled()) return;
			if(event.getWhoClicked().getType() == EntityType.PLAYER ){
				Player player = (Player)event.getWhoClicked();
				
				//player.sendMessage("click:" + event.getClick().toString());
				
//				player.sendMessage("slotType: " + event.getSlotType().toString());
//				player.sendMessage("InvType: " + event.getClickedInventory().getType().toString());
//				player.sendMessage("MainHand: " + player.getItemInHand().getType().toString());

				//检测是否主手.副手.热键切换主手
				if(event.getSlot() == player.getInventory().getHeldItemSlot() || event.getSlot() == 40 || event.getHotbarButton() == player.getInventory().getHeldItemSlot() ){
					
					if(debug){
						player.sendMessage("ClickAction: " + event.getAction().toString());
					}
										
					//player.sendMessage("Cursor: " + event.getCursor().getItemMeta().getDisplayName());
					//player.sendMessage("CurrentItem: " + event.getCurrentItem().getItemMeta().getDisplayName());
					
					
					//空手 换 道具 
					if(event.getAction() == InventoryAction.PLACE_ALL){
						
						if(debug){
							player.sendMessage("空手→道具 | Action: Place");
						}
						
						
						ItemStack item = event.getCursor();
						int newLoad = getLoad(item);
						int load = PlayerLoad.get(player.getName());
						load = load + newLoad;
						
						
						if(event.getSlot() == player.getInventory().getHeldItemSlot()){
							isMainHandNull.put(player.getName(), false);
						}
						else if(event.getSlot() == 40){
							isOffHandNull.put(player.getName(), false);
						}
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						return;
					}
					
					//道具 换 空手
					else if(event.getAction() == InventoryAction.PICKUP_ALL){
						
						if(debug){
							player.sendMessage("道具→空手 | Action: Pick");
						}
						
						
						ItemStack item = event.getCurrentItem();
						int oldLoad = getLoad(item);
						int load = PlayerLoad.get(player.getName()); 
						load = load - oldLoad;

						if(event.getSlot() == player.getInventory().getHeldItemSlot()){
							isMainHandNull.put(player.getName(), true);
						}
						else if(event.getSlot() == 40){
							isOffHandNull.put(player.getName(), true);
						}
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						return;
					}
					
					//道具  换  道具
					else if(event.getAction() == InventoryAction.SWAP_WITH_CURSOR){
						
						if(debug){
							player.sendMessage("道具→道具 | Action: Swap");
						}
						
						ItemStack newItem = event.getCursor();
						ItemStack oldItem = event.getCurrentItem();
						int newLoad = getLoad(newItem);
						int oldLoad = getLoad(oldItem);
						int load = PlayerLoad.get(player.getName());
						load = load - oldLoad + newLoad;
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						return;
					}
					
					
					/**********************************弃用*******************************************/
					/*
					//空手 换 道具   By HOTBAR
					else if( event.getAction() == InventoryAction.HOTBAR_SWAP && (event.getCurrentItem().getType() != Material.AIR && player.getItemInHand().getType() == Material.AIR) || (event.getCurrentItem().getType() == Material.AIR && player.getItemInHand().getType() == Material.AIR) ){
						player.sendMessage("空手→道具 | Action: HotBar");
						
						ItemStack item = event.getCurrentItem();


						int newLoad = getLoad(item);
						int load = PlayerLoad.get(player.getName());
						load = load + newLoad;
						
						if(event.getSlot() == player.getInventory().getHeldItemSlot()){
							isMainHandNull.put(player.getName(), false);
						}
						else if(event.getSlot() == 40){
							isOffHandNull.put(player.getName(), false);
						}
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						player.sendMessage(String.valueOf(load));
						return;
					}
					
					//道具 换 空手  By HOTBAR
					else if( event.getAction() == InventoryAction.HOTBAR_SWAP && event.getCurrentItem().getType() == Material.AIR && player.getItemInHand().getType() != Material.AIR ){
						player.sendMessage("道具→空手 | Action: HotBar");
						
						//ItemStack item = player.getItemInHand();
						
						ItemStack item = player.getItemInHand();

						
						int oldLoad = getLoad(item);
						int load = PlayerLoad.get(player.getName()); 
						load = load - oldLoad;
						
						if(event.getSlot() == player.getInventory().getHeldItemSlot()){
							isMainHandNull.put(player.getName(), true);
						}
						else if(event.getSlot() == 40){
							isOffHandNull.put(player.getName(), true);
						}

						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						player.sendMessage(String.valueOf(load));
						return;
						
					}
					
					//道具  换  道具  By HOTBAR
					else if( event.getAction() == InventoryAction.HOTBAR_SWAP && event.getCurrentItem().getType() != Material.AIR && player.getItemInHand().getType() != Material.AIR ){
						player.sendMessage("道具→道具 | Action: HotBar");
						
						ItemStack newItem = null;
						ItemStack oldItem = null;
						newItem = event.getCurrentItem();
						oldItem = player.getItemInHand();
						
						int newLoad = getLoad(newItem);
						int oldLoad = getLoad(oldItem);
						int load = PlayerLoad.get(player.getName());
						load = load - oldLoad + newLoad;
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						player.sendMessage(String.valueOf(load));
						return;
					}
					
					*/
					/**************************************************OFF HAND*****************************************************/
					/*
					if(event.getSlot() == 40){
						//空手 换 道具   By HOTBAR  on OffHand
						if( event.getAction() == InventoryAction.HOTBAR_SWAP && event.getCurrentItem().getType() == Material.AIR ){
							player.sendMessage("空手→道具 | Action: HotBar");
							
							ItemStack item = null;
							//player.sendMessage(String.valueOf(player.getItemInHand().getType().toString()));
							if(event.getHotbarButton() == player.getInventory().getHeldItemSlot()){
								item = event.getCurrentItem();
							}
							if(event.getSlot() == 40){
								
								//player.sendMessage(String.valueOf(event.getClickedInventory().getItem(event.getHotbarButton())));
								item = event.getClickedInventory().getItem(event.getHotbarButton());
							}
							
							
							int newLoad = getLoad(item);
							int load = PlayerLoad.get(player.getName());
							load = load + newLoad;
							
							if(event.getSlot() == player.getInventory().getHeldItemSlot()){
								isMainHandNull.put(player.getName(), false);
							}
							else if(event.getSlot() == 40){
								isOffHandNull.put(player.getName(), false);
							}
							
							PlayerLoad.put(player.getName(), load);
							setLoad(player, load);
							player.sendMessage(String.valueOf(load));
							return;
						}
						
						//道具 换 空手  By HOTBAR on OffHand
						else if( event.getAction() == InventoryAction.HOTBAR_SWAP && event.getCurrentItem().getType() != Material.AIR ){
							player.sendMessage("道具→空手 | Action: HotBar");
							
							//ItemStack item = player.getItemInHand();
							
							ItemStack item = null;
							if(event.getHotbarButton() == player.getInventory().getHeldItemSlot()){
								item = player.getItemInHand();
							}
							if(event.getSlot() == 40){
								//player.sendMessage(String.valueOf(event.getHotbarButton()));
								item = event.getClickedInventory().getItem(40);
							}
							
							int oldLoad = getLoad(item);
							int load = PlayerLoad.get(player.getName()); 
							load = load - oldLoad;
							
							if(event.getSlot() == player.getInventory().getHeldItemSlot()){
								isMainHandNull.put(player.getName(), true);
							}
							else if(event.getSlot() == 40){
								isOffHandNull.put(player.getName(), true);
							}
	
							PlayerLoad.put(player.getName(), load);
							setLoad(player, load);
							player.sendMessage(String.valueOf(load));
							return;
							
						}
						
						//道具  换  道具  By HOTBAR on OffHand
						else if( event.getAction() == InventoryAction.HOTBAR_SWAP && event.getCurrentItem().getType() != Material.AIR ){
							player.sendMessage("道具→道具 | Action: HotBar");
							
							if(event.getHotbarButton() == player.getInventory().getHeldItemSlot() && event.getSlot() == 40 ){
								player.sendMessage("主副手互换");
								return;
							}
							
							ItemStack newItem = null;
							ItemStack oldItem = null;
							if(event.getSlot() == 40){
								newItem = event.getClickedInventory().getItem(event.getHotbarButton());
								newItem = event.getClickedInventory().getItem(40);
							}
							else{
								newItem = event.getCurrentItem();
								oldItem = player.getItemInHand();
							}
							
							int newLoad = getLoad(newItem);
							int oldLoad = getLoad(oldItem);
							int load = PlayerLoad.get(player.getName());
							load = load - oldLoad + newLoad;
							
							PlayerLoad.put(player.getName(), load);
							setLoad(player, load);
							player.sendMessage(String.valueOf(load));
							return;
						}
					}
					*/
					/*****************************************************************************************************************/
					
					
					//道具 换 空手    BY SHIFT on MainHand or OffHand
					else if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && ( !isMainHandNull(player) || !isOffHandNull(player) ) ){
						
						if(debug){
							player.sendMessage("道具→空手 | Action: Shift");
						}
						
						
						ItemStack item = null;
						if( event.getSlot() == player.getInventory().getHeldItemSlot() ){
							 item = player.getItemInHand();
						}
						else if( event.getSlot() == 40 ){
							 item = player.getInventory().getItem(40);
						}
						
						int oldLoad = getLoad(item);
						int load = PlayerLoad.get(player.getName());
						load = load - oldLoad;
						
						if(event.getSlot() == player.getInventory().getHeldItemSlot()){
							isMainHandNull.put(player.getName(), true);
						}
						else if(event.getSlot() == 40){
							isOffHandNull.put(player.getName(), true);
						}
						
						PlayerLoad.put(player.getName(), load);
						setLoad(player, load);
						//player.sendMessage(String.valueOf(load));
						return;
					}
					
					
					
					else{
						needUpdate = true;
						return;
					}
					
				}
				
				//空手 Shift 道具  on MainHand or OffHand
				else if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&  ( isMainHandNull(player) || isOffHandNull(player) ) ){
//					if(player.getItemInHand().getType() == Material.AIR){
//						//player.sendMessage(player.getItemInHand().getItemMeta().getDisplayName());
//						player.sendMessage("事件前状态");
//					}
//					if(player.getInventory().getItem(player.getInventory().getHeldItemSlot()) == null ){
//						player.sendMessage("事件前状态2");
//					}
					needUpdate = true;
				}

			}
				
		}
		
		
		//处理Q键丢弃物品
		@EventHandler(ignoreCancelled=true)
		public void onDropItem(PlayerDropItemEvent event){
			if(event.isCancelled()) return;

			Player player = event.getPlayer();
			//player.sendMessage("Drop");

			//player.sendMessage(player.getOpenInventory().getType().toString());
			
			if( player.getItemInHand().getType() == Material.AIR && !isMainHandNull(player) ){
				isMainHandNull.put(player.getName(), true);
				
				if(debug){
					player.sendMessage("Drop!MainHand");
				}
				
				
				ItemStack item = event.getItemDrop().getItemStack();
				int load = PlayerLoad.get(player.getName());
				int oldLoad = getLoad(item);
				load = load - oldLoad;
				PlayerLoad.put(player.getName(), load);
				setLoad(player, load);
				
				//player.sendMessage(String.valueOf(load));
			}
			if(player.getInventory().getItem(40) == null && !isOffHandNull(player) ){
				isOffHandNull.put(player.getName(), true);
				
				if(debug){
					player.sendMessage("Drop!OffHand");
				}
							
				ItemStack item = event.getItemDrop().getItemStack();
				int load = PlayerLoad.get(player.getName());
				int oldLoad = getLoad(item);
				load = load - oldLoad;
				PlayerLoad.put(player.getName(), load);
				setLoad(player, load);
				
				//player.sendMessage(String.valueOf(load));
			}
			
		}
		
		
		//捡起道具 记录是否捡到主手位
		@EventHandler(ignoreCancelled=true)
		public void onPickItem(PlayerPickupItemEvent event){
			if(event.isCancelled())	return;
			if(event.getPlayer().getItemInHand().getType() == Material.AIR){
				
				new BukkitRunnable(){     
				    int s = 1;
				    @Override    
				    public void run(){
				        s--;
				        if(s==0){
				        	if(debug){
				        		event.getPlayer().sendMessage("PickUpItem: " + event.getPlayer().getInventory().getItemInMainHand().getItemMeta().toString());
				        	}
				        	updatePlayerLoad((Player)event.getPlayer());
				            cancel();
				        }
				    } 
				}.runTaskTimer(this, 0L, 10L);	
			}
		}
		
		
		//shift空手→主手  热键空手→副手
		@EventHandler(ignoreCancelled=true)
		public void onCloseInv(InventoryCloseEvent event){
			if(event.getPlayer().getType() == EntityType.PLAYER){
				if(needUpdate){
					updatePlayerLoad((Player)event.getPlayer());
					needUpdate = false;
				}
			}
		}
		
		
		//主副手互换道具
		@EventHandler(ignoreCancelled=true)
		public void onSwapHandItems(PlayerSwapHandItemsEvent event){
			if(event.isCancelled())	return;
			
			//event.getPlayer().sendMessage( event.getMainHandItem().getType().toString());
			//event.getPlayer().sendMessage( event.getOffHandItem().getType().toString());
			if( event.getMainHandItem().getType() == Material.AIR ){
				isMainHandNull.put(event.getPlayer().getName(),true);
			}
			else if( event.getMainHandItem().getType() != Material.AIR ){
				isMainHandNull.put(event.getPlayer().getName(),false);
			}
			if( event.getOffHandItem().getType() == Material.AIR ){
				isOffHandNull.put(event.getPlayer().getName(),true);
			}
			else if( event.getOffHandItem().getType() != Material.AIR ){
				isOffHandNull.put(event.getPlayer().getName(),false);
			}
			//event.getPlayer().sendMessage("Swap");
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
			double loadLimit = 10 + ((player.getMaxHealth() -20) / 10 ) ;
			
			if(load > loadLimit){
				if(debug){
					PotionEffect slow = new PotionEffect(PotionEffectType.SLOW,99999,0);
					player.addPotionEffect(slow);
				}
				
				String overLoadMessage = getConfig().getString("overLoadMessage");
				player.sendMessage(overLoadMessage.replace("&","§"));
			}
			if(load <= loadLimit && player.hasPotionEffect(PotionEffectType.SLOW)){
				player.removePotionEffect(PotionEffectType.SLOW);
			}
		}
		
		
		
		//重新获取全身负重
		public int updatePlayerLoad(Player player){
			int load = 0;
			isMainHandNull.put(player.getName(),true);
			isOffHandNull.put(player.getName(), true);
			
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
			if(player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType() != Material.AIR){
				load = load + getLoad(player.getInventory().getItemInMainHand());
				isMainHandNull.put(player.getName(),false);
			}
			if(player.getInventory().getItemInOffHand() != null && player.getInventory().getItemInOffHand().getType() != Material.AIR){
				load = load + getLoad(player.getInventory().getItemInOffHand());
				isOffHandNull.put(player.getName(), false);
			}
			PlayerLoad.put(player.getName(), load);
			setLoad(player, load);
			
			//int mainslot = player.getInventory().getHeldItemSlot();
			//mainHandNum.put(player.getName(), mainslot);
			return load;
		}
	
		
		
		//查询当前负重值接口
		public int getPlayerLoad(String name){
			return PlayerLoad.get(name);
		}
		
		//查询主副手是否为空
		public boolean isMainHandNull(Player player){
			return isMainHandNull.get(player.getName());
		}
		public boolean isOffHandNull(Player player){
			return isOffHandNull.get(player.getName());
		}
		
		
		
		
		
		
		
		
}
