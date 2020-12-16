package honeb1.limitedclones;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import sun.jvm.hotspot.opto.Block;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LimitedClones extends JavaPlugin implements Listener {
    FileConfiguration config;
    Map<String,Integer> clonesCount = new HashMap<>();
    int maxClones;

    @EventHandler
    public void onPlayerJoin( PlayerJoinEvent event ){
        Player p =  event.getPlayer();
        if( !p.hasPermission( "limitedclones.player" ) ){
            //return;
        }
        String name = p.getName();
        if(!clonesCount.containsKey(name)){
            setClones(p,maxClones);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player p = event.getEntity();
        if(p.hasPermission( "limitedclones.bypass" )){
            return;
        }
        String name = p.getName();
        int clones = getClones(p);

        Location spawnLoc = p.getBedSpawnLocation();
        if( spawnLoc != null ){
            if( clones < 1 ){
                p.setBedSpawnLocation( null );
                p.sendMessage(ChatColor.RED + "残機がなくなったため、初期スポーンに戻ります" );
                setClones(p,maxClones);
            }
            else{
                setClones(p,clones-1);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        Player p = event.getPlayer();
        tellClones(p);
    }

    @Override
    public  boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){


       if( cmd.getName().equalsIgnoreCase("setclones") &&
           args.length == 2 ){
           try{
               Player p = getServer().getPlayer( args[0] );
               int valueToSet = Integer.valueOf(args[1]);
               setClones(p,valueToSet);
               sender.sendMessage( args[0]+"の残機数を" + valueToSet +"体に設定しました。" );
               p.sendMessage( ChatColor.AQUA + "あなたの残機数が" + valueToSet + "体に設定されました。" );
           }catch (NumberFormatException e){
               sender.sendMessage( "正しい数値を指定してください。" );
           }catch (NullPointerException e){
               sender.sendMessage("プレイヤーが見つかりません。");
           }
       }
       //最大数設定
       if( args.length == 1 &&
           cmd.getName().equalsIgnoreCase( "setmaxclones" ) ){
           try {
               int valueToSet = Integer.valueOf(args[0]);
               config.set("MaxClones", valueToSet);
               maxClones = valueToSet;
               sender.sendMessage("残機の最大数を" + valueToSet + "体に設定しました。");
               return true;
           }catch (NumberFormatException e){
               sender.sendMessage( "正しい数値を指定してください。" );
           }
        }
        if ( cmd.getName().equalsIgnoreCase( "checkclonesother" ) &&
             args.length == 1 ){
            //他人確認
            Player p = getServer().getPlayer( args[0] );
            try{
                tellClones(sender,p);
            }catch (NullPointerException e) {
                sender.sendMessage("プレイヤーが見つかりません。");
            }
        }
        if( cmd.getName().equalsIgnoreCase( "checkclones" )  ){

            //自己確認
            if (!(sender instanceof Player)) {
                sender.sendMessage("ゲーム内でのみ実行可能です。");
                return true;
            }
            Player p = ((Player) sender);
            tellClones(sender,p);
        }
        return false;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getServer().getPluginManager().registerEvents( this, this );
        config = this.getConfig();
        maxClones = config.getInt("MaxClones");
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new Placeholder(this).register();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.saveConfig();
    }

    public void tellClones( CommandSender t, Player p ){
        if( p.hasPermission("limitedclones.bypass") ){
            t.sendMessage(ChatColor.YELLOW + p.getDisplayName() + "の残機数は無制限です。");
        }else {
            t.sendMessage(ChatColor.YELLOW + p.getDisplayName() + "の現在の残機数：" + getClones(p) + "体");
        }
    }
    public void tellClones( Player p ){
        if( p.hasPermission( "limitedclones.bypass" ) )return;
        p.sendMessage(ChatColor.YELLOW + "現在の残機数：" + getClones(p) + "体");
    }

    public int getClones( Player p ){
        if( p.hasPermission( "limitedclones.bypass" ) ) return -1;
        Integer clones = clonesCount.get(p.getName());
        return clones;
    }
    public void setClones( Player p, int count ){
        clonesCount.put(p.getName(),count);
    }
}
