package com.Dolo.EagleVision;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class FileConfig {
	public static FileConfiguration load(JavaPlugin plugin,String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!(file.exists())) {//������
            try {//�����µĿ��ļ�
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    
    public static void saveDefaultConfig(JavaPlugin plugin,String fileName) {
        if (!new File(plugin.getDataFolder(), fileName).exists()) {//�ж������ļ��Ƿ����
            InputStream in = plugin.getResource(fileName);//��ȡjar�ļ�����������ļ�
            if (in == null) {//jar������û����������ļ�
                plugin.getLogger().info("��������ļ�" + fileName + "������,�޷���ȡ");
                return;
            }
            //�ҵ������ļ�
            File file = new File(plugin.getDataFolder(), fileName);
            int lastIndex = fileName.lastIndexOf('/');
            //�ҵ�Ŀ¼
            File dir = new File(plugin.getDataFolder(), fileName.substring(0, lastIndex >= 0 ? lastIndex : 0));

            if (!dir.exists()) {//�ж��Ƿ��
                dir.mkdirs();//����Ŀ¼
            }
            try {
                //�������
                OutputStream out = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int len;
                //ѭ��д�뵽���������ȥ
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                //�ر���
                out.close();
                in.close();
            } catch (IOException ex) {
                return;
            }
        }
    }
}
