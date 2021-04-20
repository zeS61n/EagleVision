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
			
			//��ȡ������Ҹ���/
			getLogger().info("����Ѽ���");
		}
		
		public void onDisable()
		{
			PlayerLoad.clear();
			mainHandNum.clear();
			getLogger().info("�����ж��");
		}
		
		public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args)
		{
			if (cmd.getName().equalsIgnoreCase("FUZHONG") || cmd.getName().equalsIgnoreCase("FZ"))
			{
				if (!(sender instanceof Player))
				{
					sender.sendMessage("����̨�޷�ִ��");
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
		
		
		
		//��ҵ�½ʱ ��ͬʱ����PlayerItemHeldEvent�¼�
		//������������¼��㸺��/*/*/*��ȡ�ֳָ���   ��    ���ɷ���
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
			
			//PlayerItemHeldEvent�¼���ʾmessage
			//player.sendMessage("online�㵱ǰװ�����أ� " + load);
		}
		
		
		
		//��������л�ʱ�ֳ���������
		HashMap<String,Integer> mainHandNum = new HashMap<String,Integer>();
		@EventHandler(ignoreCancelled=true)
		public void onHeldItem(PlayerItemHeldEvent event){
			Player player = event.getPlayer();
			
			//���� �� ����
			int slot = event.getNewSlot();
			int oldSlot = event.getPreviousSlot();
			int load = PlayerLoad.get(player.getName());
			
			if(player.getInventory().getItem(oldSlot) == null && player.getInventory().getItem(slot) != null ){
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				
				//player.sendMessage("���� �� ����");
			}
			
			//�� ���� �л� ���� ʱ
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) == null ){
				
				ItemStack item = player.getInventory().getItem(oldSlot);
				
				int OldLoad = getLoad(item);
				load = load - OldLoad;
				
				//player.sendMessage("�� ���� �л� ���� ʱ");
			}
			
			//��  ����  �л�  ����  ʱ
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && !player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				
				ItemStack item = player.getInventory().getItem(slot);
				ItemStack oldItem = player.getInventory().getItem(oldSlot);
				
				int newLoad = getLoad(item);
				int oldLoad = getLoad(oldItem);
				load = load - oldLoad + newLoad;
				
				//player.sendMessage("��  ����  �л�  ����  ʱ");
			}
			else if(player.getInventory().getItem(oldSlot) != null && player.getInventory().getItem(slot) != null && player.getInventory().getItem(oldSlot).equals(player.getInventory().getItem(slot))){
				ItemStack item = player.getInventory().getItem(slot);
				
				int newLoad = getLoad(item);
				load = load + newLoad;
				
				//player.sendMessage("onlineʱ��¼���ֵ��߸���ֵ");
				
			}
			else{
				player.sendMessage("onHeldItem:else");
				mainHandNum.put(player.getName(), slot);
				return;
			}
			player.sendMessage("��¼����λ�� " + String.valueOf(slot));
			mainHandNum.put(player.getName(), slot);
			PlayerLoad.put(player.getName(), load);
			setLoad(player,load);
			player.sendMessage("�㵱ǰװ�����أ� " + load);
		}
		
		
		
		//��������װ���¼�  ���㸺��ֵ
		@EventHandler(ignoreCancelled=true)
		public void onEquip(ArmorEquipEvent event){
			
			int load = PlayerLoad.get(event.getPlayer().getName());
			
			//����װ��  ԭ����װ��
			if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && (event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() == Material.AIR)){
				
				event.getPlayer().sendMessage(event.getMethod().name());
				if(!event.getMethod().name().equals("HOTBAR")){
					int NewLoad = getLoad(event.getNewArmorPiece());
					load = load + NewLoad;
				}
			}
			
			//����װ��  ԭ����װ��
			else if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && event.getOldArmorPiece() != null && event.getOldArmorPiece().getType() != Material.AIR){
				int newLoad = getLoad(event.getNewArmorPiece());
				int oldLoad = getLoad(event.getOldArmorPiece());
				load = load - oldLoad + newLoad;
			}
			
			//����װ��
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
			event.getPlayer().sendMessage("�㵱ǰװ�����أ�" + load);
		}
		

		
		//���򿪱��� ��������ߵ����ֵ��¼�
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
				//����Ƿ�����
				//player.sendMessage("rawSlot: " + event.getSlot());
				//player.sendMessage("mainHandNum: " + mainHandNum.get(player.getName()));
				if(event.getSlot() == mainHandNum.get(player.getName())){
					//���� �� ���� 
					if(event.getAction() == InventoryAction.PLACE_ALL){
						player.sendMessage("place");
						ItemStack item = event.getCursor();
						player.sendMessage(item.getItemMeta().getDisplayName());
						int load = getLoad(item);
						
					}
					
					//���� �� ����
					if(event.getAction() == InventoryAction.PICKUP_ALL){
						player.sendMessage("pick");
					}
					
					//����  ��  ����
					if(event.getAction() == InventoryAction.SWAP_WITH_CURSOR){
						player.sendMessage("swap");
					}
				}

				
			}
				
		}
		
		
		
		

		
		
		
		//��ȡ���ߵ�����ֵ
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
		
		
		
		//�����سͷ�
		public void setLoad(Player player,int load){
			//int loadLimit1 = Integers.parseInt(this.getConfig().getString("loadLimit"));
			@SuppressWarnings("deprecation")
			double loadLimit = player.getMaxHealth()/2 ;
			
			if(load > loadLimit){
				PotionEffect slow = new PotionEffect(PotionEffectType.SLOW,99999,0);
				player.addPotionEffect(slow);
				String overLoadMessage = getConfig().getString("overLoadMessage");
				
				player.sendMessage(overLoadMessage.replace("&","��"));
			}
			if(load <= loadLimit && player.hasPotionEffect(PotionEffectType.SLOW)){
				player.removePotionEffect(PotionEffectType.SLOW);
			}
		}
		
		
	
		
		//��ѯ��ǰ����ֵ�ӿ�
		public int getPlayerLoad(String name){
			return PlayerLoad.get(name);
		}
		
		
		
		
		
		
		
		
		
}
