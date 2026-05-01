package cx.ajneb97.model.structure;

import cx.ajneb97.model.item.CommonItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Category {
    private String name;
    private ArrayList<Discovery> discoveries;
    private CommonItem defaultLevelUnlockedItem;
    private CommonItem defaultLevelBlockedItem;
    private CommonItem categoryItem;
    private List<String> defaultRewardsPerDiscovery;
    private List<String> defaultRewardsAllDiscoveries;
    private RewardsMode rewardsMode;
    private List<String> rewardsTemplate;
    private Map<String, String> templateDefaults;

    public enum RewardsMode {
        OVERRIDE,
        ADDITIVE
    }

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Discovery> getDiscoveries() {
        return discoveries;
    }

    public void setDiscoveries(ArrayList<Discovery> discoveries) {
        this.discoveries = discoveries;
    }

    public CommonItem getDefaultLevelUnlockedItem() {
        return defaultLevelUnlockedItem;
    }

    public void setDefaultLevelUnlockedItem(CommonItem defaultLevelUnlockedItem) {
        this.defaultLevelUnlockedItem = defaultLevelUnlockedItem;
    }

    public CommonItem getDefaultLevelBlockedItem() {
        return defaultLevelBlockedItem;
    }

    public void setDefaultLevelBlockedItem(CommonItem defaultLevelBlockedItem) {
        this.defaultLevelBlockedItem = defaultLevelBlockedItem;
    }

    public CommonItem getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(CommonItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    public List<String> getDefaultRewardsPerDiscovery() {
        return defaultRewardsPerDiscovery;
    }

    public void setDefaultRewardsPerDiscovery(List<String> defaultRewardsPerDiscovery) {
        this.defaultRewardsPerDiscovery = defaultRewardsPerDiscovery;
    }

    public List<String> getDefaultRewardsAllDiscoveries() {
        return defaultRewardsAllDiscoveries;
    }

    public void setDefaultRewardsAllDiscoveries(List<String> defaultRewardsAllDiscoveries) {
        this.defaultRewardsAllDiscoveries = defaultRewardsAllDiscoveries;
    }

    public RewardsMode getRewardsMode() {
        return rewardsMode;
    }

    public void setRewardsMode(RewardsMode rewardsMode) {
        this.rewardsMode = rewardsMode;
    }

    public List<String> getRewardsTemplate() {
        return rewardsTemplate;
    }

    public void setRewardsTemplate(List<String> rewardsTemplate) {
        this.rewardsTemplate = rewardsTemplate;
    }

    public Map<String, String> getTemplateDefaults() {
        return templateDefaults;
    }

    public void setTemplateDefaults(Map<String, String> templateDefaults) {
        this.templateDefaults = templateDefaults;
    }

    public Discovery getDiscovery(String id){
        for(Discovery d : discoveries){
            if(d.getId().equals(id)){
                return d;
            }
        }
        return null;
    }

    public void addDiscovery(Discovery discovery){
        discoveries.add(discovery);
    }

    public void removeDiscovery(String id){
        discoveries.removeIf(l -> l.getId().equals(id));
    }

}
