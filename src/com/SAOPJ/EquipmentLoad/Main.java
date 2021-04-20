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
			getLogger().info("����Ѽ���");
		}
		
		public void onDisable()
		{
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
				int load = PlayerLoad.get(player.getName());
				String message = this.getConfig().getString("fuzhongMessage");
				sender.sendMessage( message + load);		
				return true;
			}
			return false;
		}
		
		
		
		//��������װ���¼�  ���㸺��ֵ
		@EventHandler(ignoreCancelled=true)
		public void onEquip(ArmorEquipEvent event){
			
			int load = PlayerLoad.get(event.getPlayer().getName());
			
			//����װ��  ԭ����װ��
			if(event.getNewArmorPiece() != null && event.getNewArmorPiece().getType() != Material.AIR && (event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() == Material.AIR)){
				int NewLoad = getLoad(event.getNewArmorPiece());
				load = load + NewLoad;
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
			//event.getPlayer().sendMessage("�㵱ǰװ�����أ�" + load);
		}
		

		
		//������������¼��㸺��
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
			//player.sendMessage("�㵱ǰװ�����أ� " + load);
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
		
		
		
		/*
		public class ItemInfo extends JavaPlugin{
			@Override
			public void onEnable(){
				getLogger().info("��Ʒ��Ϣ����������");
			}
			public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
				if(label.equalsIgnoreCase("iteminfo")){
					if(sender instanceof Player){
						Player player = (Player)sender;
						if(args.length==0){
							player.sendMessage("��a/iteminfo meta ��ȡ������Ʒ����Ϣ");
							return true;
						}else{
							if(args[0].equalsIgnoreCase("meta")){
								//����������е���Ʒ��Ϣ
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
				//������������ȡ������Ϣ�ˡ�
				if(item==null){
					player.sendMessage("��c������û���κ���Ʒ!");
					return;
				}
				int id,amount;
				short durability;
				byte data;
				String displayName;
				List<String> lore = new ArrayList<String>();
				Map<Enchantment, Integer> ench = new HashMap<Enchantment, Integer>();
				id = item.getTypeId();//��ȡID
				amount = item.getAmount();//��ȡ����
				durability = item.getDurability();//��ȡ�𻵳̶�
				data = item.getData().getData();//��ȡdata
				if(item.getItemMeta().hasDisplayName()){//�ж��Ƿ���������
					displayName = item.getItemMeta().getDisplayName();
				}else{
					displayName = "û���޸�����";
				}
				lore = item.getItemMeta().getLore();
				ench = item.getItemMeta().getEnchants();
				player.sendMessage("��a��Ʒ I D:��6"+id);
				player.sendMessage("��a��Ʒ����:��6"+amount);
				player.sendMessage("��a��Ʒ��:��6"+durability);
				player.sendMessage("��a��Ʒ����:��6"+data);
				player.sendMessage("��a��Ʒ����:��6"+displayName);
				if(item.getItemMeta().hasEnchants()){
					player.sendMessage("��a��Ʒ��ħ:");
					for(Enchantment e : ench.keySet()){
						player.sendMessage("��a��ħID:��6"+e.getId()+"��a,��ħ�ȼ�:��6"+ench.get(e));
					}
				}else{
					player.sendMessage("��a��Ʒ��ħ:��6û�и�ħ");
				}
				if(item.getItemMeta().hasLore()){
					player.sendMessage("��a��ƷLore:");
					for(String l : lore){
						player.sendMessage("��aLore:��f"+l);
					}
				}else{
					player.sendMessage("��a��ƷLore:��6û��lore��Ϣ");
				}
			}
			@Override
			public void onDisable(){
				getLogger().info("��Ʒ��Ϣ���ж�����");
			}
		}
		
		*/
		
		
		
		
		
		
		
		
		
		
}
