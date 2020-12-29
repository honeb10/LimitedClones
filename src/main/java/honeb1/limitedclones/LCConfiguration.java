package honeb1.limitedclones;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class LCConfiguration {
    Plugin pl;

    ArrayList<Integer> maxClonesPermList;
    ArrayList<Integer> healAmountPermList;

    public LCConfiguration(JavaPlugin plugin){
        pl = plugin;
        loadConfig();
    }
    public void loadConfig(){
        pl.reloadConfig();
        FileConfiguration fileConfiguration = pl.getConfig();
        maxClonesPermList = (ArrayList<Integer>)fileConfiguration.getList("maxClonesPerms");
        healAmountPermList = (ArrayList<Integer>)fileConfiguration.getList("healAmountPerms");
    }
}