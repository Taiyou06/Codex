package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.CommonItemManager;
import cx.ajneb97.managers.InventoryManager;
import cx.ajneb97.model.inventory.CommonInventory;
import cx.ajneb97.model.inventory.CommonInventoryItem;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.utils.OtherUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class InventoryConfigManager {

    private Codex plugin;
    private CommonConfig configFile;


    public InventoryConfigManager(Codex plugin){
        this.plugin = plugin;
        this.configFile = new CommonConfig("inventory.yml",plugin,null, false);
        this.configFile.registerConfig();
        if(this.configFile.isFirstTime() && OtherUtils.isLegacy()){
            checkAndFix();
        }
    }

    public void checkAndFix(){
        FileConfiguration config = configFile.getConfig();
        config.set("inventories.main_inventory.0;8;36;44.item.id","STAINED_GLASS_PANE:14");
        config.set("inventories.main_inventory.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_history.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_history.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_history.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_regions.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_regions.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_regions.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        config.set("inventories.category_monsters.39.item.id","SKULL_ITEM:3");
        config.set("inventories.category_monsters.0;8;36;44.item.id","STAINED_GLASS_PANE:11");
        config.set("inventories.category_monsters.1;7;9;17;27;35;37;43.item.id","STAINED_GLASS_PANE:15");

        configFile.saveConfig();
    }

    public void configure(){
        FileConfiguration config = configFile.getConfig();
        InventoryManager inventoryManager = plugin.getInventoryManager();

        ArrayList<CommonInventory> inventories = new ArrayList<>();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        if(config.contains("inventories")) {
            for(String key : config.getConfigurationSection("inventories").getKeys(false)) {
                int slots = config.getInt("inventories."+key+".slots");
                String title = config.getString("inventories."+key+".title");

                List<CommonInventoryItem> items = new ArrayList<>();
                for(String slotString : config.getConfigurationSection("inventories."+key).getKeys(false)) {
                    if(!slotString.equals("slots") && !slotString.equals("title") && !slotString.equals("discovery_slots")) {
                        String path = "inventories."+key+"."+slotString;
                        CommonItem item = null;
                        if(config.contains(path+".item")){
                            item = commonItemManager.getCommonItemFromConfig(config, path+".item");
                        }

                        String openInventory = config.contains(path+".open_inventory") ?
                                config.getString(path+".open_inventory") : null;

                        List<String> clickActions = config.contains(path+".click_actions") ?
                                config.getStringList(path+".click_actions") : null;

                        String type = config.contains(path+".type") ?
                                config.getString(path+".type") : null;

                        CommonInventoryItem inventoryItem = new CommonInventoryItem(slotString,item,openInventory,clickActions,type);
                        items.add(inventoryItem);
                    }
                }

                CommonInventory inv = new CommonInventory(key,slots,title,items);

                // Auto-pagination: list of slots that the plugin auto-fills with
                // discoveries from the matching Category, splitting across pages
                // of size = list length. Same syntax as item slot keys
                // (e.g. "10-16,19-25" or "10;11;12").
                if(config.contains("inventories."+key+".discovery_slots")){
                    String discoverySlotsString = config.getString("inventories."+key+".discovery_slots");
                    inv.setDiscoverySlots(parseSlotList(discoverySlotsString));
                }

                inventories.add(inv);
            }
        }
        inventoryManager.setInventories(inventories);
    }

    private List<Integer> parseSlotList(String spec){
        List<Integer> out = new ArrayList<>();
        if(spec == null || spec.isEmpty()) return out;
        for(String token : spec.split("[,;]")){
            String t = token.trim();
            if(t.isEmpty()) continue;
            try {
                if(t.contains("-")){
                    String[] range = t.split("-");
                    int from = Integer.parseInt(range[0].trim());
                    int to = Integer.parseInt(range[1].trim());
                    if(from > to){ int tmp = from; from = to; to = tmp; }
                    for(int i=from;i<=to;i++) out.add(i);
                } else {
                    out.add(Integer.parseInt(t));
                }
            } catch(NumberFormatException e){
                plugin.getLogger().warning("Invalid discovery_slots entry: "+t);
            }
        }
        return out;
    }

    public boolean reloadConfig(){
        if(!configFile.reloadConfig()){
            return false;
        }
        configure();
        return true;
    }

    public FileConfiguration getConfig(){
        return configFile.getConfig();
    }
}
