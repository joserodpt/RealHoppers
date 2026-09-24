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
import joserodpt.realhoppers.api.hopper.RHopper;
import org.bukkit.Location;

import java.util.UUID;

/**
 * One hopper: where it is, who owns it, what it is called and what it has banked. Its traits and
 * whitelist are rows of their own, keyed by the same location.
 */
@DatabaseTable(tableName = "realhoppers_hoppers")
public class HopperRow {

    /** {@code x:y:z:world}, the same key {@link RHopper#getSerializedLocation()} gives. */
    @DatabaseField(columnName = "location", id = true, canBeNull = false)
    private String location;

    @DatabaseField(columnName = "world", canBeNull = false)
    private String world;

    @DatabaseField(columnName = "x")
    private int x;

    @DatabaseField(columnName = "y")
    private int y;

    @DatabaseField(columnName = "z")
    private int z;

    @DatabaseField(columnName = "name")
    private String name;

    @DatabaseField(columnName = "owner_uuid", index = true)
    private UUID ownerUUID;

    @DatabaseField(columnName = "owner_name")
    private String ownerName;

    /** {@code PUBLIC} or {@code PRIVATE}. */
    @DatabaseField(columnName = "access")
    private String access;

    @DatabaseField(columnName = "balance")
    private double balance;

    @DatabaseField(columnName = "xp")
    private int xp;

    /** The location of the hopper this one points at, or null. */
    @DatabaseField(columnName = "link")
    private String link;

    @DatabaseField(columnName = "created_at")
    private long createdAt;

    public HopperRow() {
        //for ORMLite
    }

    public HopperRow(final RHopper hopper) {
        final Location l = hopper.getLocation();
        this.location = hopper.getSerializedLocation();
        this.world = l.getWorld() == null ? "" : l.getWorld().getName();
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();
        this.name = hopper.getName();
        this.ownerUUID = hopper.getOwner();
        this.ownerName = hopper.getOwnerName();
        this.access = hopper.getAccess().name();
        this.balance = hopper.getBalance();
        this.xp = hopper.getXp();
        this.link = hopper.hasLink() ? hopper.getLink().getSerializedLocation() : null;
        this.createdAt = hopper.getCreatedAt();
    }

    public String getLocation() {
        return location;
    }

    public String getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getAccess() {
        return access;
    }

    public double getBalance() {
        return balance;
    }

    public int getXp() {
        return xp;
    }

    public String getLink() {
        return link;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
