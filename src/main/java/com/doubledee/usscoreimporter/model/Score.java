package com.doubledee.usscoreimporter.model;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

public class Score {
    private final String artist;
    private final String title;
    private final String player;
    private final int difficulty;
    private final int score;
    private final int timesPlayed;
    private final Date date;

    public Score(ResultSet resultSet) throws SQLException {
        this.artist = dehex(resultSet.getString(1));
        this.title = dehex(resultSet.getString(2));
        this.player = dehex(resultSet.getString(3));
        this.difficulty = resultSet.getInt(4);
        this.score = resultSet.getInt(5);
        this.timesPlayed = resultSet.getInt(6);
        this.date = new Date(resultSet.getInt(7) * 1000L);
    }

    public String dehex(String text) {
        try {
            byte[] bytes = Hex.decodeHex(text.toCharArray());
            return new String(trim(bytes), StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            return null;
        }
    }

    private static byte[] trim(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getPlayer() {
        return player;
    }

    public int getScore() {
        return score;
    }

    public Date getDate() {
        return date;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    @Override
    public String toString() {
        return getArtist() + " - " + getTitle() + ": " + getScore() + " " + getDate();
    }
}
