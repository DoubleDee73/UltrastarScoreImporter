package com.doubledee.usscoreimporter.db;

import com.doubledee.usscoreimporter.model.Score;
import org.apache.commons.codec.binary.Hex;

import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqLiteDb {
    public List<Score> fetchSongScores(String dbFile) {
        Connection connection;
        List<Score> scores = new ArrayList<>();
        // create a database connection
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            String sql = "select hex(s.artist), hex(s.title), hex(sc.player), " +
                    "sc.difficulty, sc.score, s.TimesPlayed, sc.date from us_songs s " +
                    "inner join us_scores sc on s.id = sc.SongId";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                scores.add(new Score(resultSet));
            }
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return scores;
    }

    public List<Score> migrateScores(List<Score> ultrastarScores, String legacyDb, String currentDb) {
        List<Score> legacyScores = fetchSongScores(legacyDb);
        List<Score> migratedScores = new ArrayList<>();
        for (Score score : legacyScores) {
            if (ultrastarScores.stream().anyMatch(it -> scoresAreEqual(it, score))) {
                System.out.println(score + " was already found in the target DB. Skipping the score");
                continue;
            }
            migratedScores.add(persistLegacyScore(score, currentDb));
        }
        return migratedScores;
    }

    private Score persistLegacyScore(Score score, String currentDb) {
        try {
            Connection currentConnection = DriverManager.getConnection("jdbc:sqlite:" + currentDb);
            String sql = "select * from us_songs where Hex(Artist)=? and Hex(Title)=?";
            PreparedStatement statement = currentConnection.prepareStatement(sql);
            statement.setString(1, enhex(score.getArtist()));
            statement.setString(2, enhex(score.getTitle()));
            ResultSet resultSet = statement.executeQuery();
            int songId;
            if (resultSet.next()) {
                songId = resultSet.getInt(1);
            } else {
                statement = currentConnection.prepareStatement("INSERT INTO us_songs (Artist, Title, TimesPlayed) " +
                                                                       "VALUES (?, ?, ?)");
                statement.setCharacterStream(1, createCharStream(score.getArtist()), score.getArtist().length() + 1);
                statement.setCharacterStream(2, createCharStream(score.getTitle()), score.getTitle().length() + 1);
                statement.setInt(3, score.getTimesPlayed());
                statement.execute();
                resultSet = currentConnection.createStatement().executeQuery("SELECT Max(id) from us_songs");
                resultSet.next();
                songId = resultSet.getInt(1);
            }
            statement = currentConnection.prepareStatement("INSERT INTO us_scores " +
                                                                   "(SongId, Difficulty, Player, Score, Date) VALUES " +
                                                                   "(?, ?, ?, ?, ?)");
            statement.setInt(1, songId);
            statement.setInt(2, score.getDifficulty());
            statement.setCharacterStream(3, createCharStream(score.getPlayer()), score.getPlayer().length() + 1);
            statement.setInt(4, score.getScore());
            statement.setLong(5, score.getDate().getTime() / 1000);
            statement.execute();
            currentConnection.close();
            System.out.println(score + " was successfully imported into the new score database.");
        } catch (SQLException e) {
            System.out.println("Something went wrong while trying to persist score " + score);
            throw new RuntimeException(e);
        }
        return score;
    }

    private String enhex(String text) {
        return Hex.encodeHexString(text.getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    private boolean scoresAreEqual(Score newScore, Score oldScore) {
        return oldScore.toString().equalsIgnoreCase(newScore.toString());
    }

    private Reader createCharStream(String text) {
        return new StringReader(text + "\0");
    }
}
