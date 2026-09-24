package joserodpt.realhoppers.plugin.database;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;

import java.util.UUID;

/** One trait on one hopper, at its tier, with whatever else that trait keeps for itself. */
@DatabaseTable(tableName = "realhoppers_hopper_traits")
public class HopperTraitRow {

    @DatabaseField(columnName = "id", generatedId = true, allowGeneratedIdInsert = true)
    private UUID id;

    @DatabaseField(columnName = "hopper_location", canBeNull = false, index = true)
    private String hopperLocation;

    /** The {@link RHopperTrait} constant's name. */
    @DatabaseField(columnName = "trait", canBeNull = false)
    private String trait;

    @DatabaseField(columnName = "tier")
    private int tier;

    /** What {@link RHopperTraitBase#serializeSettings()} gave, or null. */
    @DatabaseField(columnName = "settings", dataType = DataType.LONG_STRING)
    private String settings;

    public HopperTraitRow() {
        //for ORMLite
    }

    public HopperTraitRow(final String hopperLocation, final RHopperTrait trait, final RHopperTraitBase base) {
        this.hopperLocation = hopperLocation;
        this.trait = trait.name();
        this.tier = base.getTier();
        this.settings = base.serializeSettings();
    }

    public String getHopperLocation() {
        return hopperLocation;
    }

    public String getTrait() {
        return trait;
    }

    public int getTier() {
        return tier;
    }

    public String getSettings() {
        return settings;
    }
}
