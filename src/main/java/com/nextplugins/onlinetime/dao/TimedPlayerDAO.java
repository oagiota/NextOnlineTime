package com.nextplugins.onlinetime.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.henryfabio.sqlprovider.executor.SQLExecutor;
import com.nextplugins.onlinetime.api.player.TimedPlayer;
import com.nextplugins.onlinetime.dao.adapter.TimedPlayerAdapter;

import java.util.Set;

@Singleton
public final class TimedPlayerDAO {

    private static final String TABLE = "onlinetime_players";

    @Inject private SQLExecutor sqlExecutor;
    @Inject private String duplicateEntry;

    public void createTable() {

        this.sqlExecutor.updateQuery("CREATE TABLE IF NOT EXISTS " + TABLE + "(" +
                "name VARCHAR(16) NOT NULL PRIMARY KEY UNIQUE," +
                "time INTEGER(8)," +
                "collectedRewards TEXT" +
                ");");

    }

    public TimedPlayer selectOne(String name) {

        return this.sqlExecutor.resultOneQuery(
                "SELECT * FROM " + TABLE + " WHERE name = ?",
                statment -> statment.set(1, name),
                TimedPlayerAdapter.class
        );

    }

    public Set<TimedPlayer> selectAll(String preferences) {

        return this.sqlExecutor.resultManyQuery(
                "SELECT * FROM " + TABLE + " " + preferences,
                simpleStatement -> {
                },
                TimedPlayerAdapter.class
        );

    }

    public void insertOne(TimedPlayer timedPlayer) {

        this.sqlExecutor.updateQuery(
                String.format(duplicateEntry, TABLE),
                statment -> {

                    String rewards = String.join(",", timedPlayer.getCollectedRewards());

                    statment.set(1, timedPlayer.getName());
                    statment.set(2, timedPlayer.getTimeInServer());
                    statment.set(3, rewards);

                    if (duplicateEntry.contains("ON")) {

                        statment.set(4, timedPlayer.getTimeInServer());
                        statment.set(5, rewards);

                    }

                }
        );

    }

    public void saveOne(TimedPlayer timedPlayer) {

        this.sqlExecutor.updateQuery(
                "UPDATE " + TABLE + " SET time = ?, collectedRewards = ? WHERE name = ?",
                statment -> {
                    statment.set(1, timedPlayer.getTimeInServer());
                    statment.set(2, String.join(",", timedPlayer.getCollectedRewards()));
                    statment.set(3, timedPlayer.getName());
                }
        );

    }

}
