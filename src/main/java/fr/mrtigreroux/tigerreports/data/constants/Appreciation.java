package fr.mrtigreroux.tigerreports.data.constants;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.objects.CustomItem;

public enum Appreciation {
    
    NONE("None", Message.NONE_FEMALE, -1, null),
    TRUE("True", Message.TRUE, 11, MenuRawItem.GREEN_CLAY),
    UNCERTAIN("Uncertain", Message.UNCERTAIN, 13, MenuRawItem.YELLOW_CLAY),
    FALSE("False", Message.FALSE, 15, MenuRawItem.RED_CLAY);
    
    private final String configName;
    private final Message displayName;
    private final int position;
    private final CustomItem icon;
    
    public static Appreciation from(String configName) {
        if (configName == null || configName.isEmpty()) {
            return NONE;
        }
        
        try {
            Appreciation appreciation = Appreciation.valueOf(configName.toUpperCase());
            return appreciation != null ? appreciation : NONE;
        } catch (IllegalArgumentException invalidAppreciation) {
            return NONE;
        }
    }
    
    Appreciation(String configName, Message displayName, int position, CustomItem icon) {
        this.configName = configName;
        this.displayName = displayName;
        this.position = position;
        this.icon = icon;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    public String getDisplayName() {
        return displayName.get();
    }
    
    public String getStatisticsName() {
        return getConfigName().toLowerCase() + "_appreciations";
    }
    
    public int getPosition() {
        return position;
    }
    
    public CustomItem getIcon() {
        return icon.clone();
    }
    
    public static Appreciation[] getValues() {
        return new Appreciation[] {
                TRUE, UNCERTAIN, FALSE
        };
    }
    
    public static Appreciation getAppreciationAtPosition(int position) {
        for (Appreciation appreciation : getValues()) {
            if (appreciation.getPosition() == position) {
                return appreciation;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return getConfigName();
    }
    
}
