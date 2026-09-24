package joserodpt.realhoppers.plugin.managers;

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

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.db.DatabaseTypeUtils;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.logger.NullLogBackend;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import joserodpt.realhoppers.api.config.RHSQLConfig;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.managers.DatabaseManagerAPI;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.plugin.database.HopperRow;
import joserodpt.realhoppers.plugin.database.HopperTraitRow;
import joserodpt.realhoppers.plugin.database.HopperWhitelistRow;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Keeps the hoppers in SQL, through ORMLite, the way RealSkywars keeps its players: SQLite by
 * default, MySQL or MariaDB through sql.yml.
 *
 * <p>Every query runs on one writer thread, in the order it was asked for. That keeps the single
 * connection to one thread at a time, and means a hopper deleted straight after a write cannot be
 * brought back by that write landing late.</p>
 */
public class DatabaseManager extends DatabaseManagerAPI {

    private final RealHoppers rh;
    private final ConnectionSource connectionSource;
    private final Dao<HopperRow, String> hopperDao;
    private final Dao<HopperTraitRow, UUID> traitDao;
    private final Dao<HopperWhitelistRow, UUID> whitelistDao;

    /** Hoppers changed since the last flush. Only ever touched on the main thread. */
    private final Set<RHopper> dirty = ConcurrentHashMap.newKeySet();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "RealHoppers-Database");
        t.setDaemon(true);
        return t;
    });

    public DatabaseManager(final RealHoppers rh) throws SQLException {
        this.rh = rh;

        //ORMLite logs every statement it prepares otherwise
        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        final String databaseURL = this.getDatabaseURL();
        this.connectionSource = new JdbcConnectionSource(databaseURL,
                RHSQLConfig.file().getString("username"),
                RHSQLConfig.file().getString("password"),
                DatabaseTypeUtils.createDatabaseType(databaseURL));

        TableUtils.createTableIfNotExists(this.connectionSource, HopperRow.class);
        TableUtils.createTableIfNotExists(this.connectionSource, HopperTraitRow.class);
        TableUtils.createTableIfNotExists(this.connectionSource, HopperWhitelistRow.class);

        this.hopperDao = DaoManager.createDao(this.connectionSource, HopperRow.class);
        this.traitDao = DaoManager.createDao(this.connectionSource, HopperTraitRow.class);
        this.whitelistDao = DaoManager.createDao(this.connectionSource, HopperWhitelistRow.class);
    }

    private String getDatabaseURL() {
        final String driver = RHSQLConfig.file().getString("driver", "SQLITE").toLowerCase();
        final String host = RHSQLConfig.file().getString("host", "localhost");
        final int port = RHSQLConfig.file().getInt("port", 3306);
        final String database = RHSQLConfig.file().getString("database", "RealHoppers");

        switch (driver) {
            case "mysql":
            case "mariadb":
                //the one connection is kept open for as long as the server runs, and MySQL drops
                //idle ones after wait_timeout
                return "jdbc:" + driver + "://" + host + ":" + port + "/" + database + "?autoReconnect=true";
            case "postgresql":
                return "jdbc:postgresql://" + host + ":" + port + "/" + database;
            default:
                final File folder = this.rh.getPlugin().getDataFolder();
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                return "jdbc:sqlite:" + new File(folder, database + ".db");
        }
    }

    /** One hopper as the database has it, with its traits and whitelist. */
    public static class StoredHopper {
        private final HopperRow hopper;
        private final List<HopperTraitRow> traits;
        private final List<HopperWhitelistRow> whitelist;

        StoredHopper(final HopperRow hopper, final List<HopperTraitRow> traits, final List<HopperWhitelistRow> whitelist) {
            this.hopper = hopper;
            this.traits = traits;
            this.whitelist = whitelist;
        }

        public HopperRow getHopper() {
            return hopper;
        }

        public List<HopperTraitRow> getTraits() {
            return traits;
        }

        public List<HopperWhitelistRow> getWhitelist() {
            return whitelist;
        }
    }

    /**
     * Every hopper in the database. Blocks until read: this is startup and reload, and nothing can
     * run before the hoppers are there. Goes through the writer so that a reload reads after
     * whatever was still being written.
     */
    public List<StoredHopper> loadAll() {
        try {
            return this.writer.submit(() -> {
                final Map<String, List<HopperTraitRow>> traits = this.traitDao.queryForAll().stream()
                        .collect(Collectors.groupingBy(HopperTraitRow::getHopperLocation));
                final Map<String, List<HopperWhitelistRow>> whitelists = this.whitelistDao.queryForAll().stream()
                        .collect(Collectors.groupingBy(HopperWhitelistRow::getHopperLocation));

                final List<StoredHopper> stored = new ArrayList<>();
                for (final HopperRow row : this.hopperDao.queryForAll()) {
                    stored.add(new StoredHopper(row,
                            traits.getOrDefault(row.getLocation(), Collections.emptyList()),
                            whitelists.getOrDefault(row.getLocation(), Collections.emptyList())));
                }
                return stored;
            }).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            this.rh.getLogger().severe("Could not read the hoppers from the database: " + e.getCause().getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void markDirty(final RHopper hopper) {
        this.dirty.add(hopper);
    }

    /** A hopper's rows, taken on the main thread so the writer never reads a hopper that is changing. */
    private static final class Snapshot {
        private final HopperRow hopper;
        private final List<HopperTraitRow> traits = new ArrayList<>();
        private final List<HopperWhitelistRow> whitelist = new ArrayList<>();

        private Snapshot(final RHopper h) {
            this.hopper = new HopperRow(h);
            final String location = this.hopper.getLocation();
            h.getTraitMap().forEach((trait, base) -> this.traits.add(new HopperTraitRow(location, trait, base)));
            h.getWhitelist().forEach((uuid, name) -> this.whitelist.add(new HopperWhitelistRow(location, uuid, name)));
        }
    }

    @Override
    public void flush(final boolean async) {
        if (this.dirty.isEmpty() || this.writer.isShutdown()) {
            return;
        }

        final List<Snapshot> snapshots = new ArrayList<>();
        for (final RHopper hopper : new ArrayList<>(this.dirty)) {
            this.dirty.remove(hopper);
            //a hopper that has been deleted can still mark itself on the way out - stopping its
            //traits saves its balance - and writing it would bring it back
            if (this.rh.getHopperManager().getHopper(hopper.getBlock()) != hopper) {
                continue;
            }
            snapshots.add(new Snapshot(hopper));
        }

        if (snapshots.isEmpty()) {
            return;
        }

        final Runnable write = () -> snapshots.forEach(this::write);
        if (async) {
            this.writer.execute(write);
        } else {
            this.await(this.writer.submit(write));
        }
    }

    private void write(final Snapshot snapshot) {
        final String location = snapshot.hopper.getLocation();
        try {
            //the traits and whitelist are a handful of rows each, so they are replaced outright
            //rather than compared against what is stored
            TransactionManager.callInTransaction(this.connectionSource, () -> {
                this.hopperDao.createOrUpdate(snapshot.hopper);
                this.deleteChildren(location);
                if (!snapshot.traits.isEmpty()) {
                    this.traitDao.create(snapshot.traits);
                }
                if (!snapshot.whitelist.isEmpty()) {
                    this.whitelistDao.create(snapshot.whitelist);
                }
                return null;
            });
        } catch (final SQLException e) {
            this.rh.getLogger().severe("Could not save the hopper at " + location + ": " + e.getMessage());
        }
    }

    private void deleteChildren(final String location) throws SQLException {
        final DeleteBuilder<HopperTraitRow, UUID> traits = this.traitDao.deleteBuilder();
        traits.where().eq("hopper_location", location);
        traits.delete();

        final DeleteBuilder<HopperWhitelistRow, UUID> whitelist = this.whitelistDao.deleteBuilder();
        whitelist.where().eq("hopper_location", location);
        whitelist.delete();
    }

    @Override
    public void delete(final RHopper hopper) {
        this.dirty.remove(hopper);
        if (this.writer.isShutdown()) {
            return;
        }

        final String location = hopper.getSerializedLocation();
        this.writer.execute(() -> {
            try {
                TransactionManager.callInTransaction(this.connectionSource, () -> {
                    this.hopperDao.deleteById(location);
                    this.deleteChildren(location);
                    return null;
                });
            } catch (final SQLException e) {
                this.rh.getLogger().severe("Could not delete the hopper at " + location + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<OwnedHopper>> getOwnedHoppers(final UUID owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.hopperDao.queryForEq("owner_uuid", owner).stream()
                        .map(row -> new OwnedHopper(row.getLocation(), row.getName(), row.getAccess()))
                        .collect(Collectors.toList());
            } catch (final SQLException e) {
                this.rh.getLogger().severe("Could not read the hoppers of " + owner + ": " + e.getMessage());
                return Collections.<OwnedHopper>emptyList();
            }
        }, this.writer);
    }

    @Override
    public void close() {
        this.flush(false);
        this.writer.shutdown();
        try {
            if (!this.writer.awaitTermination(30, TimeUnit.SECONDS)) {
                this.rh.getLogger().severe("The database took too long to finish writing. Some hopper changes may be lost.");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            this.connectionSource.close();
        } catch (final Exception e) {
            this.rh.getLogger().warning("Could not close the database connection: " + e.getMessage());
        }
    }

    private void await(final Future<?> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            this.rh.getLogger().severe("Could not save hoppers: " + e.getCause().getMessage());
        }
    }
}
