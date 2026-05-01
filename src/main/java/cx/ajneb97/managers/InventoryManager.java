package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.config.MainConfigManager;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.internal.CommonVariable;
import cx.ajneb97.model.inventory.CommonInventory;
import cx.ajneb97.model.inventory.CommonInventoryItem;
import cx.ajneb97.model.inventory.InventoryPlayer;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    private Codex plugin;
    private ArrayList<CommonInventory> inventories;
    private ArrayList<InventoryPlayer> players;

    public InventoryManager(Codex plugin){
        this.plugin = plugin;
        this.inventories = new ArrayList<>();
        this.players = new ArrayList<>();
    }

    public ArrayList<CommonInventory> getInventories() {
        return inventories;
    }

    public void setInventories(ArrayList<CommonInventory> inventories) {
        this.inventories = inventories;
    }

    public ArrayList<InventoryPlayer> getPlayers() {
        return players;
    }

    public CommonInventory getInventory(String name){
        for(CommonInventory inventory : inventories){
            if(inventory.getName().equals(name)){
                return inventory;
            }
        }
        // Paginated names like "category_regions;2" share the base inventory
        // template — strip the suffix and try again.
        int sep = name.indexOf(';');
        if(sep > 0){
            String base = name.substring(0, sep);
            for(CommonInventory inventory : inventories){
                if(inventory.getName().equals(base)){
                    return inventory;
                }
            }
        }
        return null;
    }

    public InventoryPlayer getInventoryPlayer(Player player){
        for(InventoryPlayer inventoryPlayer : players){
            if(inventoryPlayer.getPlayer().equals(player)){
                return inventoryPlayer;
            }
        }
        return null;
    }

    public void removeInventoryPlayer(Player player){
        players.removeIf(p -> p.getPlayer().equals(player));
    }

    public void openInventory(InventoryPlayer inventoryPlayer){
        String fullName = inventoryPlayer.getInventoryName();
        CommonInventory inventory = getInventory(fullName);
        if(inventory == null) return;
        MainConfigManager mainConfigManager = plugin.getConfigsManager().getMainConfigManager();

        // ----- Pagination setup -----
        // Pagination kicks in when the inventory declares discovery_slots AND
        // its base name resolves to a Category. The page is encoded as a
        // semicolon suffix (category_regions;2 = page 2). Page 1 has no suffix.
        String baseName = fullName;
        int requestedPage = 1;
        int sep = fullName.indexOf(';');
        if(sep > 0){
            baseName = fullName.substring(0, sep);
            try { requestedPage = Math.max(1, Integer.parseInt(fullName.substring(sep+1))); }
            catch(NumberFormatException ignored){ requestedPage = 1; }
        }

        List<Integer> discoverySlots = inventory.getDiscoverySlots();
        boolean paginated = discoverySlots != null && !discoverySlots.isEmpty();
        Category paginatedCategory = null;
        int totalPages = 1;
        int currentPage = 1;
        List<Discovery> pageDiscoveries = new ArrayList<>();
        if(paginated && baseName.startsWith("category_")){
            paginatedCategory = plugin.getCategoryManager().getCategory(baseName.substring("category_".length()));
            if(paginatedCategory != null){
                int per = discoverySlots.size();
                int total = paginatedCategory.getDiscoveries().size();
                totalPages = Math.max(1, (int)Math.ceil(total / (double)per));
                currentPage = Math.min(requestedPage, totalPages);
                int from = (currentPage - 1) * per;
                int to = Math.min(from + per, total);
                if(from < to){
                    pageDiscoveries = new ArrayList<>(paginatedCategory.getDiscoveries().subList(from, to));
                }
                // Reflect the clamped page back so subsequent clicks track correctly.
                String clampedName = currentPage == 1 ? baseName : baseName + ";" + currentPage;
                inventoryPlayer.setInventoryName(clampedName);
            } else {
                paginated = false;
            }
        }

        // Title supports %page% / %total_pages% placeholders.
        String title = applyPageVars(inventory.getTitle(), currentPage, totalPages);
        Inventory inv;
        if(mainConfigManager.isUseMiniMessage()){
            inv = MiniMessageUtils.createInventory(inventory.getSlots(),title);
        }else{
            inv = Bukkit.createInventory(null,inventory.getSlots(), MessagesManager.getLegacyColoredMessage(title));
        }

        List<CommonInventoryItem> items = inventory.getItems();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        Player player = inventoryPlayer.getPlayer();
        java.util.Set<Integer> reservedSlots = paginated ? new java.util.HashSet<>(discoverySlots) : java.util.Collections.emptySet();

        //Add items for all inventories
        for(CommonInventoryItem itemInventory : items){
            for(int slot : itemInventory.getSlots()){
                // Auto-pagination owns these slots — skip any explicit declarations.
                if(reservedSlots.contains(slot)) continue;

                String type = itemInventory.getType();
                if(type != null){
                    ItemStack item = null;
                    if(type.startsWith("discovery: ")){
                        item = setDiscovery(type.replace("discovery: ",""),inventoryPlayer);
                    }else if(type.startsWith("category: ")){
                        item = setCategory(type.replace("category: ",""),player);
                    }else if(type.equals("next_page")){
                        if(paginated && currentPage < totalPages){
                            item = buildPageNavItem(itemInventory, player, baseName, currentPage + 1, currentPage, totalPages);
                        }
                    }else if(type.equals("previous_page")){
                        if(paginated && currentPage > 1){
                            int target = currentPage - 1;
                            item = buildPageNavItem(itemInventory, player, baseName, target, currentPage, totalPages);
                        }
                    }
                    if(item != null){
                        item = setItemActions(itemInventory,item);
                        inv.setItem(slot,item);
                    }
                    continue;
                }

                ItemStack item = commonItemManager.createItemFromCommonItem(itemInventory.getItem(),player);
                item = applyPageVars(item, currentPage, totalPages);

                String openInventory = itemInventory.getOpenInventory();
                if(openInventory != null) {
                    item = ItemUtils.setTagStringItem(plugin,item, "codex_open_inventory", openInventory);
                }
                item = setItemActions(itemInventory,item);

                inv.setItem(slot,item);
            }
        }

        // Auto-fill paginated discovery slots from the category's discovery list.
        if(paginated && paginatedCategory != null){
            for(int i=0; i<discoverySlots.size() && i<pageDiscoveries.size(); i++){
                int slot = discoverySlots.get(i);
                ItemStack item = setDiscovery(pageDiscoveries.get(i).getId(), inventoryPlayer);
                if(item != null){
                    inv.setItem(slot, item);
                }
            }
        }

        inventoryPlayer.getPlayer().openInventory(inv);
        players.add(inventoryPlayer);
    }

    private ItemStack buildPageNavItem(CommonInventoryItem itemInventory, Player player,
                                        String baseName, int targetPage, int currentPage, int totalPages){
        if(itemInventory.getItem() == null) return null;
        CommonItem template = itemInventory.getItem().clone();
        ArrayList<CommonVariable> vars = new ArrayList<>();
        vars.add(new CommonVariable("%page%", String.valueOf(currentPage)));
        vars.add(new CommonVariable("%total_pages%", String.valueOf(totalPages)));
        vars.add(new CommonVariable("%target_page%", String.valueOf(targetPage)));
        plugin.getCommonItemManager().replaceVariables(template, vars, player);
        ItemStack item = plugin.getCommonItemManager().createItemFromCommonItem(template, player);
        String target = targetPage <= 1 ? baseName : baseName + ";" + targetPage;
        item = ItemUtils.setTagStringItem(plugin, item, "codex_open_inventory", target);
        return item;
    }

    private String applyPageVars(String s, int currentPage, int totalPages){
        if(s == null) return null;
        return s.replace("%page%", String.valueOf(currentPage))
                .replace("%total_pages%", String.valueOf(totalPages));
    }

    private ItemStack applyPageVars(ItemStack item, int currentPage, int totalPages){
        if(item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        boolean changed = false;
        if(meta.hasDisplayName()){
            String dn = meta.getDisplayName();
            String nd = applyPageVars(dn, currentPage, totalPages);
            if(!nd.equals(dn)){ meta.setDisplayName(nd); changed = true; }
        }
        if(meta.hasLore()){
            List<String> lore = meta.getLore();
            List<String> nl = new ArrayList<>(lore.size());
            boolean loreChanged = false;
            for(String line : lore){
                String n = applyPageVars(line, currentPage, totalPages);
                if(!n.equals(line)) loreChanged = true;
                nl.add(n);
            }
            if(loreChanged){ meta.setLore(nl); changed = true; }
        }
        if(changed) item.setItemMeta(meta);
        return item;
    }

    private ItemStack setItemActions(CommonInventoryItem commonItem, ItemStack item) {
        List<String> clickActions = commonItem.getClickActions();
        if(clickActions != null && !clickActions.isEmpty()) {
            String actionsList = "";
            for(int i=0;i<clickActions.size();i++) {
                if(i==clickActions.size()-1) {
                    actionsList=actionsList+clickActions.get(i);
                }else {
                    actionsList=actionsList+clickActions.get(i)+"|";
                }
            }
            item = ItemUtils.setTagStringItem(plugin, item, "codex_item_actions", actionsList);
        }
        return item;
    }

    private void clickOnDiscoveryItem(InventoryPlayer inventoryPlayer,String discoveryName,ClickType clickType){
        String categoryName = inventoryPlayer.getInventoryName().replace("category_","").split(";")[0];
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return;
        }

        Discovery discovery = category.getDiscovery(discoveryName);
        if(discovery == null){
            return;
        }

        Player player = inventoryPlayer.getPlayer();
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        boolean hasDiscovered = playerDataManager.hasDiscovery(player,category.getName(),discoveryName);

        if(!hasDiscovered){
            return;
        }

        List<String> actions = discovery.getClickActions();
        if(actions != null){
            int cooldown = discovery.getClickActionsCooldown();
            if(cooldown != 0){
                long millisActionsExecuted = playerDataManager.getMillisActionsExecuted(player,category.getName(),discoveryName);
                long millisAvailable = millisActionsExecuted+(cooldown*1000L);
                long currentMillis = System.currentTimeMillis();

                if(millisActionsExecuted != 0 && millisAvailable > currentMillis){
                    FileConfiguration messagesConfig = plugin.getMessagesConfig();
                    MessagesManager msgManager = plugin.getMessagesManager();
                    String timeString = TimeUtils.getTime((millisAvailable-currentMillis)/1000,msgManager);
                    msgManager.sendMessage(player,messagesConfig.getString("clickActionsCooldown")
                            .replace("%time%",timeString),true);
                    return;
                }

                playerDataManager.setMillisActionsExecuted(player,category.getName(),discoveryName);
            }

            for(String action : actions){
                ActionUtils.executeAction(player,action,plugin,new ArrayList<>());
            }
        }
    }

    private void clickOnCategoryItem(InventoryPlayer inventoryPlayer, String categoryName, ClickType clickType){
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return;
        }

        Player player = inventoryPlayer.getPlayer();

        inventoryPlayer.setInventoryName("category_"+categoryName);
        openInventory(inventoryPlayer);
    }

    private void clickOnOpenInventoryItem(InventoryPlayer inventoryPlayer,String openInventory){
        inventoryPlayer.setInventoryName(openInventory);
        openInventory(inventoryPlayer);
    }

    private void clickActionsItem(InventoryPlayer inventoryPlayer,String itemCommands){
        String[] sep = itemCommands.split("\\|");
        for(String action : sep){
            ActionUtils.executeAction(inventoryPlayer.getPlayer(),action,plugin,new ArrayList<>());
        }
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, ClickType clickType){
        String itemActions = ItemUtils.getTagStringItem(plugin,item,"codex_item_actions");
        if(itemActions != null){
            clickActionsItem(inventoryPlayer,itemActions);
        }

        String categoryName = ItemUtils.getTagStringItem(plugin,item,"codex_category");
        if(categoryName != null){
            clickOnCategoryItem(inventoryPlayer,categoryName,clickType);
            return;
        }

        String discoveryName = ItemUtils.getTagStringItem(plugin,item,"codex_discovery");
        if(discoveryName != null){
            clickOnDiscoveryItem(inventoryPlayer,discoveryName,clickType);
            return;
        }

        String openInventory = ItemUtils.getTagStringItem(plugin,item,"codex_open_inventory");
        if(openInventory != null){
            clickOnOpenInventoryItem(inventoryPlayer,openInventory);
            return;
        }
    }

    public ItemStack setCategory(String categoryName,Player player){
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return null;
        }

        CommonItem commonItem;
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        ArrayList<CommonVariable> variables = new ArrayList<>();

        int max = category.getDiscoveries().size();
        int totalDiscoveries = playerDataManager.getTotalDiscoveries(player,categoryName);
        String unlockedVariable = OtherUtils.getCurrentUnlockedVariable(totalDiscoveries,max,plugin.getMessagesConfig());
        commonItem = category.getCategoryItem().clone();
        variables.add(new CommonVariable("%progress_bar%", OtherUtils.getProgressBar(totalDiscoveries,max,plugin.getConfigsManager().getMainConfigManager())));
        variables.add(new CommonVariable("%percentage%", OtherUtils.getPercentage(totalDiscoveries,max)+"%"));
        variables.add(new CommonVariable("%unlocked%", unlockedVariable));

        commonItemManager.replaceVariables(commonItem,variables,player);
        ItemStack item = commonItemManager.createItemFromCommonItem(commonItem,player);

        item = ItemUtils.setTagStringItem(plugin,item,"codex_category",categoryName);
        return item;
    }

    public ItemStack setDiscovery(String discoveryName,InventoryPlayer inventoryPlayer){
        // Category could be:
        // category_<name>
        // category_<name>;<something>
        String categoryName = inventoryPlayer.getInventoryName().replace("category_","").split(";")[0];
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return null;
        }

        Discovery discovery = category.getDiscovery(discoveryName);
        if(discovery == null){
            return null;
        }

        Player player = inventoryPlayer.getPlayer();

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();

        ArrayList<CommonVariable> variables = new ArrayList<>();
        PlayerDataDiscovery playerDataDiscovery = playerDataManager.getDiscovery(player,category.getName(),discoveryName);

        CommonItem commonItem;
        if(playerDataDiscovery != null){
            if(discovery.getCustomLevelUnlockedItem() != null){
                commonItem = discovery.getCustomLevelUnlockedItem();
            }else{
                commonItem = category.getDefaultLevelUnlockedItem();
            }
            variables.add(new CommonVariable("%name%",discovery.getName()));
            variables.add(new CommonVariable("%date%",playerDataDiscovery.getDiscoverDate()));
        }else{
            if(discovery.getCustomLevelBlockedItem() != null){
                commonItem = discovery.getCustomLevelBlockedItem();
            }else{
                commonItem = category.getDefaultLevelBlockedItem();
            }
        }

        commonItem = commonItem.clone();

        // Replace %description% variable
        List<String> description = discovery.getDescription();
        List<String> newLore = new ArrayList<>();
        List<String> lore = commonItem.getLore();
        for (String s : lore){
            if(s.contains("%description%")){
                newLore.addAll(description);
            }else{
                newLore.add(s);
            }
        }
        commonItem.setLore(newLore);

        commonItemManager.replaceVariables(commonItem,variables,player);
        ItemStack item = commonItemManager.createItemFromCommonItem(commonItem,player);

        item = ItemUtils.setTagStringItem(plugin,item,"codex_discovery",discoveryName);
        return item;
    }
}
