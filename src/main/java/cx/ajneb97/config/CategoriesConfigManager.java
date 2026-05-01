package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.CategoryManager;
import cx.ajneb97.managers.CommonItemManager;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.OtherUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CategoriesConfigManager extends DataFolderConfigManager {

    public CategoriesConfigManager(Codex plugin, String folderName) {
        super(plugin, folderName);
    }

    @Override
    public void loadConfigs() {
        ArrayList<Category> categories = new ArrayList<>();
        CategoryManager categoryManager = plugin.getCategoryManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();

        ArrayList<CommonConfig> configFiles = getConfigs();
        for(CommonConfig commonConfig : configFiles){
            FileConfiguration config = commonConfig.getConfig();
            String name = commonConfig.getPath().replace(".yml","");

            CommonItem itemCategory = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.category");

            CommonItem itemDiscoveryUnlocked = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.discovery_unlocked");
            CommonItem itemDiscoveryBlocked = commonItemManager.getCommonItemFromConfig(config,"config.inventory_items.discovery_blocked");

            List<String> rewardsPerDiscovery = config.getStringList("config.rewards.per_discovery");
            List<String> rewardsAllDiscoveries = config.getStringList("config.rewards.all_discoveries");

            Category.RewardsMode rewardsMode = Category.RewardsMode.OVERRIDE;
            if(config.contains("config.rewards.mode")){
                try {
                    rewardsMode = Category.RewardsMode.valueOf(config.getString("config.rewards.mode").toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            List<String> rewardsTemplate = null;
            if(config.contains("config.rewards.template")){
                rewardsTemplate = config.getStringList("config.rewards.template");
            }

            Map<String, String> templateDefaults = null;
            if(config.contains("config.rewards.template_defaults")){
                templateDefaults = new HashMap<>();
                for(String tdKey : config.getConfigurationSection("config.rewards.template_defaults").getKeys(false)){
                    templateDefaults.put("%" + tdKey + "%", config.getString("config.rewards.template_defaults." + tdKey));
                }
            }

            ArrayList<Discovery> discoveries = new ArrayList<>();
            if(config.contains("discoveries")){
                for(String key : config.getConfigurationSection("discoveries").getKeys(false)){
                    String discoveryName = config.getString("discoveries."+key+".name");
                    List<String> discoveryDescription = config.getStringList("discoveries."+key+".description");

                    DiscoveredOn discoveredOn = null;
                    if(config.contains("discoveries."+key+".discovered_on")){
                        discoveredOn = new DiscoveredOn(
                                DiscoveredOn.DiscoveredOnType.valueOf(config.getString("discoveries."+key+".discovered_on.type"))
                        );
                        String pathValue = "discoveries."+key+".discovered_on.value";
                        discoveredOn.setMobName(config.getString(pathValue+".mob_name"));
                        discoveredOn.setMobTypes(readMobTypes(config, pathValue+".mob_type"));
                        discoveredOn.setRegionName(config.getString(pathValue+".region_name"));
                    }

                    CommonItem customDiscoveryItemUnlocked = null;
                    CommonItem customDiscoveryItemBlocked = null;
                    if(config.contains("discoveries."+key+".inventory_items.discovery_unlocked")){
                        customDiscoveryItemUnlocked = commonItemManager.getCommonItemFromConfig(config,"discoveries."+key+".inventory_items.discovery_unlocked");
                    }
                    if(config.contains("discoveries."+key+".inventory_items.discovery_blocked")){
                        customDiscoveryItemBlocked = commonItemManager.getCommonItemFromConfig(config,"discoveries."+key+".inventory_items.discovery_blocked");
                    }

                    List<String> rewards = null;
                    if(config.contains("discoveries."+key+".rewards")){
                        rewards = config.getStringList("discoveries."+key+".rewards");
                    }

                    Map<String, String> rewardVariables = null;
                    if(config.contains("discoveries."+key+".reward_variables")){
                        rewardVariables = new HashMap<>();
                        for(String rvKey : config.getConfigurationSection("discoveries."+key+".reward_variables").getKeys(false)){
                            rewardVariables.put("%" + rvKey + "%", config.getString("discoveries."+key+".reward_variables." + rvKey));
                        }
                    }

                    List<String> clickActions = null;
                    if(config.contains("discoveries."+key+".click_actions")){
                        clickActions = config.getStringList("discoveries."+key+".click_actions");
                    }

                    int clickActionsCooldown = config.getInt("discoveries."+key+".click_actions_cooldown");

                    Discovery discovery = new Discovery(key,name);
                    discovery.setName(discoveryName);
                    discovery.setDescription(discoveryDescription);
                    discovery.setDiscoveredOn(discoveredOn);
                    discovery.setCustomRewards(rewards);
                    discovery.setRewardVariables(rewardVariables);
                    discovery.setClickActions(clickActions);
                    discovery.setClickActionsCooldown(clickActionsCooldown);
                    discovery.setCustomLevelBlockedItem(customDiscoveryItemBlocked);
                    discovery.setCustomLevelUnlockedItem(customDiscoveryItemUnlocked);
                    discoveries.add(discovery);
                }
            }


            Category category = new Category(name);
            category.setCategoryItem(itemCategory);
            category.setDefaultLevelUnlockedItem(itemDiscoveryUnlocked);
            category.setDefaultLevelBlockedItem(itemDiscoveryBlocked);
            category.setDefaultRewardsAllDiscoveries(rewardsAllDiscoveries);
            category.setDefaultRewardsPerDiscovery(rewardsPerDiscovery);
            category.setRewardsMode(rewardsMode);
            category.setRewardsTemplate(rewardsTemplate);
            category.setTemplateDefaults(templateDefaults);
            category.setDiscoveries(discoveries);

            categories.add(category);
        }

        categoryManager.setCategories(categories);
    }

    @Override
    public void saveConfigs() {

    }

    /**
     * Reads mob_type as either a YAML list ({@code [foo, bar]}), a single
     * string, or a legacy {@code ;}-separated single string. Returns null when
     * the key isn't present so the caller can leave the filter empty.
     */
    private List<String> readMobTypes(FileConfiguration config, String path){
        if(!config.contains(path)) return null;
        if(config.isList(path)){
            return config.getStringList(path);
        }
        String raw = config.getString(path);
        if(raw == null || raw.isEmpty()) return null;
        if(raw.contains(";")){
            ArrayList<String> out = new ArrayList<>();
            for(String t : raw.split(";")){
                String trimmed = t.trim();
                if(!trimmed.isEmpty()) out.add(trimmed);
            }
            return out;
        }
        ArrayList<String> single = new ArrayList<>();
        single.add(raw);
        return single;
    }

    @Override
    public void createFiles() {
        new CommonConfig("history.yml",plugin,folderName,false).registerConfig();
        new CommonConfig("monsters.yml",plugin,folderName,false).registerConfig();
        new CommonConfig("regions.yml",plugin,folderName,false).registerConfig();
    }
}
