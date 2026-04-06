package com.artillexstudios.axplayerwarps.database.impl;

import com.artillexstudios.axapi.utils.Pair;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.database.Database;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.user.Users;
import com.artillexstudios.axplayerwarps.user.WarpUser;
import com.artillexstudios.axplayerwarps.utils.ThreadUtils;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Base implements Database {
    private final HashBiMap<String, UUID> userNameCache = HashBiMap.create();
    public Connection getConnection() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public void setup() {
        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_players (
                	id INT NOT NULL AUTO_INCREMENT,
                	uuid VARCHAR(36) NOT NULL,
                	name VARCHAR(128) NOT NULL,
                	PRIMARY KEY (id),
                	UNIQUE (uuid)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_currencies (
                	id INT NOT NULL AUTO_INCREMENT,
                	currency VARCHAR(512) NOT NULL,
                	PRIMARY KEY (id),
                	UNIQUE (currency)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_worlds (
                	id INT NOT NULL AUTO_INCREMENT,
                	world VARCHAR(512) NOT NULL,
                	PRIMARY KEY (id),
                	UNIQUE (world)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_warps (
                	id INT NOT NULL AUTO_INCREMENT,
                	owner_id INT NOT NULL,
                	world_id INT NOT NULL,
                	x FLOAT NOT NULL,
                	y FLOAT NOT NULL,
                	z FLOAT NOT NULL,
                	yaw FLOAT NOT NULL,
                	pitch FLOAT NOT NULL,
                	name VARCHAR(1024) NOT NULL,
                	description TEXT DEFAULT null,
                	category_id INT DEFAULT null,
                	icon_id INT DEFAULT null,
                	created BIGINT NOT NULL,
                    currency_id INT DEFAULT null,
                    price DOUBLE NOT NULL DEFAULT '0',
                    earned_money DOUBLE NOT NULL DEFAULT '0',
                    access TINYINT NOT NULL DEFAULT '0',
                	PRIMARY KEY (id)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_visits (
                	id INT NOT NULL AUTO_INCREMENT,
                	visitor_id INT NOT NULL,
                	warp_id INT,
                	date BIGINT,
                	PRIMARY KEY (id)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_ratings (
                	id INT NOT NULL AUTO_INCREMENT,
                	reviewer_id INT NOT NULL,
                	warp_id INT NOT NULL,
                	stars TINYINT NOT NULL,
                	date BIGINT,
                	PRIMARY KEY (id)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_categories (
                	id INT NOT NULL AUTO_INCREMENT,
                	category VARCHAR(512) NOT NULL,
                	PRIMARY KEY (id),
                	UNIQUE (category)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_materials (
                	id INT NOT NULL AUTO_INCREMENT,
                	material VARCHAR(512) NOT NULL,
                	PRIMARY KEY (id),
                	UNIQUE (material)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_favorites (
                	id INT NOT NULL AUTO_INCREMENT,
                	player_id INT NOT NULL,
                	warp_id INT NOT NULL,
                	date BIGINT,
                	PRIMARY KEY (id)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_whitelisted (
                	id INT NOT NULL AUTO_INCREMENT,
                	player_id INT NOT NULL,
                	warp_id INT NOT NULL,
                	date BIGINT,
                	PRIMARY KEY (id)
                );
        """);

        execute("""
                CREATE TABLE IF NOT EXISTS axplayerwarps_blacklisted (
                	id INT NOT NULL AUTO_INCREMENT,
                	player_id INT NOT NULL,
                	warp_id INT NOT NULL,
                	date BIGINT,
                	PRIMARY KEY (id)
                );
        """);
    }

    @Override
    public void loadOrUpdate(Player player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT name FROM axplayerwarps_players WHERE uuid = ?",
                player.getUniqueId().toString())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("name").equals(player.getName())) return;
                    execute("UPDATE axplayerwarps_players SET name = ? WHERE uuid = ?", player.getName(), player.getUniqueId().toString());
                } else {
                    insert("INSERT INTO axplayerwarps_players (uuid, name) VALUES (?, ?)", player.getUniqueId().toString(), player.getName());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void execute(String sql, Object... obj) {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int n = 1;
            for (Object o : obj) stmt.setObject(n++, o);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private PreparedStatement createStatement(Connection conn, String sql, Object... obj) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        int n = 1;
        for (Object o : obj) stmt.setObject(n++, o);
        return stmt;
    }

    private int insert(String sql, Object... obj) {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int n = 1;
            for (Object o : obj) stmt.setObject(n++, o);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public int getPlayerId(OfflinePlayer offlinePlayer) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        return getPlayerId(offlinePlayer.getUniqueId());
    }

    public int getPlayerId(UUID uuid) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_players WHERE uuid = ?",
                uuid.toString())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                else {
                    OfflinePlayer pl = Bukkit.getOfflinePlayer(uuid);
                    return insert("INSERT INTO axplayerwarps_players (uuid, name) VALUES (?, ?)", uuid.toString(), pl.getName());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Player not found!");
    }

    @Override
    public String getPlayerName(UUID uuid) {
        String cached = userNameCache.inverse().get(uuid);
        if (cached != null) return cached;
        OfflinePlayer pl = Bukkit.getOfflinePlayer(uuid);
        if (pl.getName() != null) return pl.getName();

        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT name FROM axplayerwarps_players WHERE uuid = ?",
                uuid.toString())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString(1);
                    userNameCache.put(name, uuid);
                    return name;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "---";
    }

    @Nullable
    @Override
    public UUID getUUIDFromName(String name) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT uuid FROM axplayerwarps_players WHERE UPPER(name) = UPPER(?)",
                name)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public UUID getUUIDFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT uuid FROM axplayerwarps_players WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Pair<UUID, String> getUUIDAndNameFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT uuid, name FROM axplayerwarps_players WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new Pair<>(UUID.fromString(rs.getString(1)), rs.getString(2));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public int getWorldId(String world) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        return getWorldId(Bukkit.getWorld(world));
    }

    public int getWorldId(World world) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        if (world == null) return -1;

        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_worlds WHERE world = ?",
                world.getName())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                else {
                    return insert("INSERT INTO axplayerwarps_worlds (world) VALUES (?)", world.getName());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("World not found!");
    }

    @Nullable
    public String getWorldFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT world FROM axplayerwarps_worlds WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public int getCategoryId(String category) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_categories WHERE category = ?",
                category)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                else {
                    return insert("INSERT INTO axplayerwarps_categories (category) VALUES (?)", category);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Category not found!");
    }

    @Nullable
    public Category getCategoryFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT category FROM axplayerwarps_categories WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return CategoryManager.getCategories().get(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public int getCurrencyId(String currency) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_currencies WHERE currency = ?",
                currency)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                else {
                    return insert("INSERT INTO axplayerwarps_currencies (currency) VALUES (?)", currency);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Currency not found!");
    }

    @Nullable
    public CurrencyHook getCurrencyFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT currency FROM axplayerwarps_currencies WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return HookManager.getCurrencyHook(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public int getMaterialId(Material material) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        return getMaterialId(material.name());
    }

    @Override
    public int getMaterialId(String material) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_materials WHERE material = ?",
                material)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                else {
                    return insert("INSERT INTO axplayerwarps_materials (material) VALUES (?)", material);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Currency not found!");
    }

    @Nullable
    public Material getMaterialFromId(int id) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT material FROM axplayerwarps_materials WHERE id = ?",
                id)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Material.valueOf(rs.getString(1));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public int createWarp(OfflinePlayer player, Location l, String warpName) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        return insert("""
                INSERT INTO axplayerwarps_warps
                (owner_id, world_id, x, y, z, yaw, pitch, name, created)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """,
                getPlayerId(player),
                getWorldId(l.getWorld()),
                l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(),
                warpName,
                System.currentTimeMillis());
    }

    @Override
    public void updateWarp(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");

        int worldId = getWorldId(warp.getLocation().getWorld());
        if (worldId == -1) return;

        execute("""
                UPDATE axplayerwarps_warps SET
                owner_id = ?,
                world_id = ?,
                x = ?,
                y = ?,
                z = ?,
                yaw = ?,
                pitch = ?,
                name = ?,
                description = ?,
                category_id = ?,
                icon_id = ?,
                currency_id = ?,
                price = ?,
                earned_money = ?,
                access = ?
                WHERE id = ?
                """,
                getPlayerId(warp.getOwner()),
                worldId,
                warp.getLocation().getX(),
                warp.getLocation().getY(),
                warp.getLocation().getZ(),
                warp.getLocation().getYaw(),
                warp.getLocation().getPitch(),
                warp.getName(),
                warp.getDescription(),
                warp.getCategory() == null ? null : getCategoryId(warp.getCategory().raw()),
                warp.getIcon() == null ? null : getMaterialId(warp.getIcon()),
                warp.getCurrency() == null ? null : getCurrencyId(warp.getCurrency().getName()),
                warp.getTeleportPrice(),
                warp.getEarnedMoney(),
                warp.getAccess().ordinal(),
                warp.getId()
        );
    }

    @Override
    public void deleteWarp(Warp warp) {
        for (WarpUser user : Users.getPlayers().values()) {
            user.getFavorites().removeIf(w -> w.equals(warp));
        }

        execute("DELETE FROM axplayerwarps_warps WHERE id = ?;", warp.getId());
        execute("DELETE FROM axplayerwarps_visits WHERE warp_id = ?;", warp.getId());
        execute("DELETE FROM axplayerwarps_ratings WHERE warp_id = ?;", warp.getId());
        execute("DELETE FROM axplayerwarps_favorites WHERE warp_id = ?;", warp.getId());
        execute("DELETE FROM axplayerwarps_whitelisted WHERE warp_id = ?;", warp.getId());
        execute("DELETE FROM axplayerwarps_blacklisted WHERE warp_id = ?;", warp.getId());

        WarpManager.getWarps().remove(warp);
    }

    @Override
    public void setRating(Player player, Warp warp, int stars) {
        removeRating(player, warp);
        warp.getAllRatings().put(player.getUniqueId(), stars);
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("INSERT INTO axplayerwarps_ratings (reviewer_id, warp_id, stars, date) VALUES (?, ?, ?, ?);",
                getPlayerId(player), warp.getId(), stars, System.currentTimeMillis());
    }

    @Override
    public void removeRating(Player player, Warp warp) {
        warp.getAllRatings().remove(player.getUniqueId());
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("DELETE FROM axplayerwarps_ratings WHERE reviewer_id = ? AND warp_id = ?;",
                getPlayerId(player), warp.getId());
    }

    @Nullable
    @Override
    public Integer getRating(Player player, Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT stars FROM axplayerwarps_ratings WHERE reviewer_id = ? AND warp_id = ?;",
                getPlayerId(player), warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Pair<Integer, Float> getRatings(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT count(reviewer_id), avg(stars) FROM axplayerwarps_ratings WHERE warp_id = ?;",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new Pair<>(rs.getInt(1), rs.getFloat(2));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new Pair<>(0, 0f);
    }

    @Override
    public HashMap<UUID, Integer> getAllRatings(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        HashMap<UUID, Integer> ratings = new HashMap<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT axplayerwarps_players.uuid, axplayerwarps_ratings.stars FROM axplayerwarps_ratings INNER JOIN axplayerwarps_players ON axplayerwarps_ratings.reviewer_id = axplayerwarps_players.id WHERE axplayerwarps_ratings.warp_id = ?;",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ratings.put(UUID.fromString(rs.getString(1)), rs.getInt(2));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ratings;
    }

    @Override
    public void addToFavorites(Player player, Warp warp) {
        removeFromFavorites(player, warp);
        Users.get(player).getFavorites().add(warp);
        warp.setFavorites(warp.getFavorites() + 1);
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("INSERT INTO axplayerwarps_favorites (player_id, warp_id, date) VALUES (?, ?, ?);",
                getPlayerId(player), warp.getId(), System.currentTimeMillis());
    }

    @Override
    public void removeFromFavorites(Player player, Warp warp) {
        if (Users.get(player).getFavorites().remove(warp)) {
            warp.setFavorites(warp.getFavorites() - 1);
        }
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("DELETE FROM axplayerwarps_favorites WHERE player_id = ? AND warp_id = ?;",
                getPlayerId(player), warp.getId());
    }

    @Override
    public void removeAllFavorites(Player player) {
        for (Warp warp : Users.get(player).getFavorites()) {
            warp.setFavorites(warp.getFavorites() - 1);
        }
        Users.get(player).getFavorites().clear();
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("DELETE FROM axplayerwarps_favorites WHERE player_id = ?;",
                getPlayerId(player));
    }

    @Override
    public int getFavorites(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT count(*) FROM axplayerwarps_favorites WHERE warp_id = ?;",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getFavorites(Player player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT count(*) FROM axplayerwarps_favorites WHERE player_id = ?;",
                getPlayerId(player))
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<Warp> getFavoriteWarps(Player player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        List<Warp> warps = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT warp_id FROM axplayerwarps_favorites WHERE player_id = ?;",
                getPlayerId(player))
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    WarpManager.getWarps()
                            .stream().filter(warp -> warp.getId() == id)
                            .findAny().ifPresent(warps::add);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return warps;
    }

    @Override
    public List<Warp> getRecentWarps(Player player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        List<Warp> warps = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT DISTINCT warp_id FROM axplayerwarps_visits WHERE visitor_id = ? ORDER BY date DESC;",
                getPlayerId(player))
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    WarpManager.getWarps()
                            .stream().filter(warp -> warp.getId() == id)
                            .findAny().ifPresent(warps::add);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return warps;
    }

    @Override
    public boolean isFavorite(Player player, Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_favorites WHERE player_id = ? AND warp_id = ? LIMIT 1;",
                getPlayerId(player), warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void addVisit(Player player, Warp warp) {
        warp.setVisits(warp.getVisits() + 1);
        warp.getVisitors().add(player.getUniqueId());
        ThreadUtils.checkNotMain("This method can only be called async!");
        execute("INSERT INTO axplayerwarps_visits (visitor_id, warp_id, date) VALUES (?, ?, ?);",
                getPlayerId(player), warp.getId(), System.currentTimeMillis());
    }

    @Override
    public int getVisits(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT count(*) FROM axplayerwarps_visits WHERE warp_id = ?;",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public HashSet<UUID> getVisitors(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        HashSet<UUID> visitors = new HashSet<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT DISTINCT axplayerwarps_players.uuid FROM axplayerwarps_visits INNER JOIN axplayerwarps_players ON axplayerwarps_visits.visitor_id = axplayerwarps_players.id WHERE warp_id = ?;",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    visitors.add(UUID.fromString(rs.getString(1)));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return visitors;
    }

    @Override
    public int getUniqueVisits(Warp warp) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT count(*) FROM (SELECT DISTINCT visitor_id FROM axplayerwarps_visits WHERE warp_id = ?);",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean warpExists(String name) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM axplayerwarps_warps WHERE UPPER(name) = UPPER(?)",
                name)
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void addToList(Warp warp, AccessList al, OfflinePlayer player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        removeFromList(warp, AccessList.BLACKLIST, player);
        removeFromList(warp, AccessList.WHITELIST, player);

        long time = System.currentTimeMillis();
        AccessPlayer accessPlayer = new AccessPlayer(player, time, getPlayerName(player.getUniqueId()));
        switch (al) {
            case WHITELIST -> warp.getWhitelisted().add(accessPlayer);
            case BLACKLIST -> warp.getBlacklisted().add(accessPlayer);
        }
        execute("INSERT INTO " + al.getTable() + " (player_id, warp_id, date) VALUES (?, ?, ?);",
                getPlayerId(player), warp.getId(), time);
    }

    @Override
    public void removeFromList(Warp warp, AccessList al, OfflinePlayer player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        switch (al) {
            case WHITELIST -> warp.getWhitelisted().removeIf(ap -> ap.player().getUniqueId().equals(player.getUniqueId()));
            case BLACKLIST -> warp.getBlacklisted().removeIf(ap -> ap.player().getUniqueId().equals(player.getUniqueId()));
        }
        execute("DELETE FROM " + al.getTable() + " WHERE player_id = ? AND warp_id = ?;",
                getPlayerId(player), warp.getId());
    }

    @Override
    public void clearList(Warp warp, AccessList al) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        switch (al) {
            case WHITELIST -> warp.getWhitelisted().clear();
            case BLACKLIST -> warp.getBlacklisted().clear();
        }
        execute("DELETE FROM " + al.getTable() + " WHERE warp_id = ?;",
                warp.getId());
    }

    @Override
    public boolean isOnList(Warp warp, AccessList al, OfflinePlayer player) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT id FROM " + al.getTable() + " WHERE warp_id = ? AND player_id = (SELECT id FROM axplayerwarps_players WHERE uuid = ? LIMIT 1) LIMIT 1",
                warp.getId(), player.getUniqueId().toString())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public List<AccessPlayer> getAccessList(Warp warp, AccessList al) {
        ThreadUtils.checkNotMain("This method can only be called async!");
        List<AccessPlayer> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT axplayerwarps_players.uuid, axplayerwarps_players.name, " + al.getTable() + ".date FROM axplayerwarps_players INNER JOIN " + al.getTable() + " ON axplayerwarps_players.id = " + al.getTable() + ".player_id WHERE " + al.getTable() + ".warp_id = ?",
                warp.getId())
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AccessPlayer ap = new AccessPlayer(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString(1))), rs.getLong(3), rs.getString(2));
                    list.add(ap);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public record AccessPlayer(OfflinePlayer player, long added, String name) {}

    @Override
    public void loadWarps() {
        ThreadUtils.checkNotMain("This method can only be called async!");
        try (Connection conn = getConnection(); PreparedStatement stmt = createStatement(conn,
                "SELECT * FROM axplayerwarps_warps;")
        ) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String world = getWorldFromId(rs.getInt("world_id"));
                    Location loc = new Location(
                            Bukkit.getWorld(world),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );

                    Category category = null;
                    if (rs.getString("category_id") != null) {
                        category = getCategoryFromId(rs.getInt("category_id"));
                    }

                    CurrencyHook currencyHook = null;
                    if (rs.getString("currency_id") != null) {
                        currencyHook = getCurrencyFromId(rs.getInt("currency_id"));
                    }

                    Material material = null;
                    if (rs.getString("icon_id") != null) {
                        material = getMaterialFromId(rs.getInt("icon_id"));
                    }

                    Pair<UUID, String> player = getUUIDAndNameFromId(rs.getInt("owner_id"));
                    if (player == null) continue;

                    Warp warp = new Warp(
                            rs.getInt("id"),
                            rs.getLong("created"),
                            rs.getString("description"),
                            rs.getString("name"),
                            loc,
                            world,
                            category,
                            player.getKey(),
                            player.getValue(),
                            Access.values()[rs.getInt("access")],
                            currencyHook,
                            rs.getDouble("price"),
                            rs.getDouble("earned_money"),
                            material
                    );

                    WarpManager.getWarps().add(warp);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void disable() {
    }
}
