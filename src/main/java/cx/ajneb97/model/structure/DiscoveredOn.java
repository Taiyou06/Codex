package cx.ajneb97.model.structure;

import java.util.Collections;
import java.util.List;

public class DiscoveredOn {

    private DiscoveredOnType type;
    private List<String> mobTypes;
    private String mobName;
    private String regionName;

    public DiscoveredOn(DiscoveredOnType type) {
        this.type = type;
    }

    public DiscoveredOnType getType() {
        return type;
    }

    public void setType(DiscoveredOnType type) {
        this.type = type;
    }

    public List<String> getMobTypes() {
        return mobTypes == null ? Collections.emptyList() : mobTypes;
    }

    public void setMobTypes(List<String> mobTypes) {
        this.mobTypes = mobTypes;
    }

    /** True when no mob_type filter is configured, or when the given type is in the list. */
    public boolean matchesMobType(String mobType) {
        return mobTypes == null || mobTypes.isEmpty() || mobTypes.contains(mobType);
    }

    public String getMobName() {
        return mobName;
    }

    public void setMobName(String mobName) {
        this.mobName = mobName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public enum DiscoveredOnType{
        MOB_KILL,
        MYTHIC_MOB_KILL,
        ELITE_MOB_KILL,
        WORLDGUARD_REGION,
        RESIDENCE_REGION
    }
}
