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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/** One player on one hopper's whitelist. */
@DatabaseTable(tableName = "realhoppers_hopper_whitelist")
public class HopperWhitelistRow {

    @DatabaseField(columnName = "id", generatedId = true, allowGeneratedIdInsert = true)
    private UUID id;

    @DatabaseField(columnName = "hopper_location", canBeNull = false, index = true)
    private String hopperLocation;

    @DatabaseField(columnName = "player_uuid", canBeNull = false)
    private UUID playerUUID;

    /** Their name when they were added, for showing the list without a lookup per player. */
    @DatabaseField(columnName = "player_name")
    private String playerName;

    public HopperWhitelistRow() {
        //for ORMLite
    }

    public HopperWhitelistRow(final String hopperLocation, final UUID playerUUID, final String playerName) {
        this.hopperLocation = hopperLocation;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    public String getHopperLocation() {
        return hopperLocation;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }
}
