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
        if (!(file.exists())) {//不存在
            try {//创建新的空文件
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    
    public static void saveDefaultConfig(JavaPlugin plugin,String fileName) {
        if (!new File(plugin.getDataFolder(), fileName).exists()) {//判断配置文件是否存在
            InputStream in = plugin.getResource(fileName);//读取jar文件里面的配置文件
            if (in == null) {//jar包里面没有这个配置文件
                plugin.getLogger().info("插件配置文件" + fileName + "不存在,无法读取");
                return;
            }
            //找到配置文件
            File file = new File(plugin.getDataFolder(), fileName);
            int lastIndex = fileName.lastIndexOf('/');
            //找到目录
            File dir = new File(plugin.getDataFolder(), fileName.substring(0, lastIndex >= 0 ? lastIndex : 0));

            if (!dir.exists()) {//判断是否村
                dir.mkdirs();//创建目录
            }
            try {
                //打开输出流
                OutputStream out = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int len;
                //循环写入到外面的配置去
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                //关闭流
                out.close();
                in.close();
            } catch (IOException ex) {
                return;
            }
        }
    }
}
