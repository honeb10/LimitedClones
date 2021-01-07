package honeb1.limitedclones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sun.org.apache.xalan.internal.xsltc.runtime.ErrorMessages_zh_CN;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.concurrent.impl.FutureConvertersImpl;
import sun.jvm.hotspot.opto.Block;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public final class LimitedClones extends JavaPlugin implements Listener {
    LCConfiguration configuration;
    FileConfiguration config;
    FileConfiguration clonesRec;
    final String permMessage = "権限がありません。";
    final String invalidNumberMessage = "有効な数値を入力してください。";
    final String playerNotFoundMessage = "プレイヤーが見つかりません。";
    final String helpMessage =
            "§e==LimitedClones ヘルプ==\n" +
            "§e/clones set <プレイヤー> <数値> §7- §2プレイヤーの残機を設定\n" +
            "§e/clones add <プレイヤー> <数値> §7- §2プレイヤーの残機を指定した数値追加\n" +
            "§e/clones heal <プレイヤー> §7- §2プレイヤーの残機を権限で設定された数値追加\n" +
            "§e/clones help §7- §2コマンドのヘルプを表示\n";

    //WorldGuard
    public static StateFlag wgflag_limit_clones;
    boolean useWorldGuard = false;

    @EventHandler
    public void onPlayerJoin( PlayerJoinEvent event ){
        Player p =  event.getPlayer();
        String name = p.getName();
        if(!clonesRec.contains(name)){
            setClones(p,getMaxClones(p));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event){
        Player p = event.getEntity();
        if(!isClonesLimited(p)) return;;
        String name = p.getName();
        int clones = getClones(p);

        Location spawnLoc = p.getBedSpawnLocation();
        if( spawnLoc != null ){
            if( clones < 1 ){
                p.setBedSpawnLocation( null );
                p.sendMessage(ChatColor.RED + "残機がなくなったため、初期スポーンに戻ります" );
                setClones(p,getMaxClones(p));

                //コマンド実行

                for(String raw : configuration.commandsOnRunningOutOfClones){
                    if(raw.contains("clear %player%")){
                        //アイテム消去
                        event.getDrops().clear();
                        continue;
                    }
                    String replaced = raw.replace("%player%", p.getName());
                    getServer().dispatchCommand(getServer().getConsoleSender(), replaced);
                }
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
       if(!cmd.getName().equalsIgnoreCase("clones")) return false;

       if( args[0].equalsIgnoreCase("set") ){
           if(!sender.hasPermission("limitedclones.modify")){
               sender.sendMessage(permMessage);
               return false;
           }
           final String usage = "使い方: /clones set <プレイヤー名> <数値>";
           if(args.length != 3){
               sender.sendMessage(usage);
               return false;
           }
           try{
               Collection<Player> players = handleAtA(args[1]);
               for(Player p : players) {
                   int valueToSet = Integer.valueOf(args[2]);
                   setClones(p, valueToSet);
                   sender.sendMessage(p.getName() + "の残機数を" + valueToSet + "体に設定しました。");
                   p.sendMessage(ChatColor.AQUA + "あなたの残機数が" + valueToSet + "体に設定されました。");
               }
               return true;
           }catch (NumberFormatException e){
               sender.sendMessage(invalidNumberMessage);
               return false;
           }catch (NullPointerException e){
               sender.sendMessage(playerNotFoundMessage);
               return false;
           }
       }
       if(args[0].equalsIgnoreCase("heal")){
           if(!sender.hasPermission("limitedclones.modify")){
               sender.sendMessage(permMessage);
               return false;
           }
           final String usage = "使い方: /clones heal <プレイヤー名>";
           if(args.length != 2){
               sender.sendMessage(usage);
               return false;
           }
           try{
               Collection<Player> players = handleAtA(args[1]);
               for(Player p : players) {
                   int amount = getHealAmount(p);
                   addClones(p, amount);
                   sender.sendMessage(p.getName() + "に残機を付与しました。");
               }
               return true;
           }catch (NullPointerException e) {
               sender.sendMessage(playerNotFoundMessage);
               return false;
           }
       }
       if(args[0].equalsIgnoreCase("add")){
            if(!sender.hasPermission("limitedclones.modify")){
                sender.sendMessage(permMessage);
                return false;
            }
            final String usage = "使い方: /clones add <プレイヤー名> <数値>";
            if(args.length != 3){
                sender.sendMessage(usage);
                return false;
            }
            try{
                Collection<Player> players = handleAtA(args[1]);
                for(Player p : players) {
                    int amount = Integer.valueOf(args[2]);
                    addClones(p, amount);
                    sender.sendMessage(p.getName() + "に残機を付与しました。");
                }
                return true;
            }catch (NullPointerException e){
                sender.sendMessage(playerNotFoundMessage);
            }catch (NumberFormatException e){
                sender.sendMessage(invalidNumberMessage);
            }
        }
       if(args[0].equalsIgnoreCase("help")){
           sender.sendMessage(helpMessage);
           return true;
       }
       return false;
    }

    public Collection<Player> handleAtA(String arg){//プレイヤー名の代わりに@aで全員指定
        if(arg.equalsIgnoreCase("@a")){
            return (Collection<Player>) getServer().getOnlinePlayers();
        }
        Collection<Player> collection = new ArrayList<>();
        collection.add(getServer().getPlayer(arg));
        return collection;
    }

    final String[] subCommands = {
            "set", "heal", "add", "help"
    };
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("clones"))
            return super.onTabComplete(sender, command, alias, args);
        if (args.length == 1) {//サブコマンド一覧
            if (args[0].length() == 0) {
                return Arrays.asList(subCommands);
            }else {
                List<String> matched = new ArrayList<>();
                for (String s : subCommands) {
                    if (s.startsWith(args[0])) matched.add(s);
                }
                return matched;
            }
        }else{
            switch (args[0].toLowerCase()){
                case "set":
                case "add":
                case "heal":
                    switch (args.length){
                        case 2:
                            //プレイヤー名
                            return super.onTabComplete(sender, command, alias, args);
                        default://数字
                            return new ArrayList<>();
                    }
                case "help":
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getServer().getPluginManager().registerEvents( this, this );
        config = this.getConfig();
        configuration = new LCConfiguration(this);
        clonesRec = new YamlConfiguration();
        try{
            File recFile = new File("plugins/LimitedClones/clonesRecord.yml");
            if(!recFile.exists()) recFile.createNewFile();
            clonesRec.load(recFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new Placeholder(this).register();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.saveConfig();
        try {
            clonesRec.save("plugins/LimitedClones/clonesRecord.yml");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLoad() {
        if(Bukkit.getPluginManager().getPlugin("WorldGuard") != null){
            useWorldGuard = true;
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            try{
                StateFlag limitClonesFlag = new StateFlag("limit-clones", true);
                registry.register(limitClonesFlag);
                wgflag_limit_clones = limitClonesFlag;
            }catch (FlagConflictException e){
                Flag<?> existing = registry.get("limit-clones");
                if(existing instanceof StateFlag){
                    wgflag_limit_clones = (StateFlag)existing;
                }else{
                }
            }
        }
    }

    public void tellClones(CommandSender t, Player p ){

        if(!isClonesLimited(p)){
            t.sendMessage(ChatColor.YELLOW + p.getDisplayName() + "の残機数は無制限です。");
        }else {
            t.sendMessage(ChatColor.YELLOW + p.getDisplayName() + "の現在の残機数：" + getClones(p) + "体");
        }
    }
    public void tellClones( Player p ){
        if(!isClonesLimited(p)) return;
        p.sendMessage(ChatColor.YELLOW + "現在の残機数：" + getClones(p) + "体");
    }

    public int getClones( Player p ){
        return clonesRec.getInt(p.getName());
    }
    public void setClones( Player p, int count ){
        clonesRec.set(p.getName(),count);
    }

    public void addClones( Player p, int amount ){
        int res = Math.min(getClones(p) + amount, getMaxClones(p));
        int added = (res - getClones(p));
        setClones(p,res);
        if(added > 0)
            p.sendMessage( ChatColor.AQUA + "あなたに残機が" + added + "体付与されました。" );
    }

    public boolean getFlagState( StateFlag flag, Player p ){
        LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(p);
        Location loc = p.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
        ApplicableRegionSet set = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(loc));
        return set.testState(player,flag);
    }

    public int getMaxClones(Player p){
        int res = -1;
        for(int i : configuration.maxClonesPermList){
            Permission perm = new Permission(("limitedclones.maxclones." + i), PermissionDefault.FALSE);
            if(!p.hasPermission(perm)) continue;
            //更新
            if(i > res ) res = i;
        }
        return res;
    }
    public int getHealAmount(Player p){
        int res = 1;
        for(int i : configuration.healAmountPermList){
            Permission perm = new Permission(("limitedclones.healamount." + i), PermissionDefault.FALSE);
            if(!p.hasPermission(perm)) continue;
            //更新
            if(i > res ) res = i;
        }
        return res;
    }

    public boolean isClonesLimited(Player p){
        return (!useWorldGuard || getFlagState(wgflag_limit_clones, p)) &&
               !p.hasPermission("limitedclones.bypass");
    }
}
