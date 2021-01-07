package honeb1.limitedclones;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class Placeholder extends PlaceholderExpansion{
    private  LimitedClones plugin;

    public Placeholder(LimitedClones pl){
        this.plugin = pl;
    }
    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public  boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "LimitedClones";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if(identifier.equalsIgnoreCase("clones_remaining")){
            if(!plugin.isClonesLimited(player)) {//無限
                return "∞";
            }
            return Integer.toString(plugin.getClones(player));
        }
        if(identifier.equalsIgnoreCase("clones_max")){
            if(!plugin.isClonesLimited(player)){//無限
                return "∞";
            }
            int max = plugin.getMaxClones(player);
            return Integer.toString(max);
        }
        return null;
    }
}
