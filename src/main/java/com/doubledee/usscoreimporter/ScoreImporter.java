package com.doubledee.usscoreimporter;

import com.doubledee.usscoreimporter.db.SqLiteDb;
import com.doubledee.usscoreimporter.model.Score;

import java.io.File;
import java.util.List;

public class ScoreImporter {
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: java -jar UltrastarScoreImporter.jar <%SOURCE_DIR%> <%TARGET_DIR%>\n" +
                    "e. g. java -jar UltrastarScoreImporter-0.1.jar \"C:\\Program Files (x86)\\UltraStar WorldParty\" " +
                                       "\"C:\\Program Files (x86)\\UltraStar Deluxe\"");
            return;
        }
        String sourceDb = checkPath(args[0]);
        if (sourceDb == null) {
            System.out.println("Source " + args[0] + " does not exist");
        }
        String targetDb = checkPath(args[1]);
        if (targetDb == null) {
            System.out.println("Target " + args[1] + " does not exist");
        }
        SqLiteDb importer = new SqLiteDb();
        List<Score> currentScores = importer.fetchSongScores(targetDb);
        System.out.println("There are already " + currentScores.size() + " score(s) in the target score database");
        List<Score> newScores = importer.migrateScores(currentScores, sourceDb, targetDb);
        System.out.println("Finished importing " + newScores.size() + " score(s)");
    }
    private static String checkPath(String path) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File ultrastarFile = new File(path + "Ultrastar.db");
        if (ultrastarFile.exists()) {
            return ultrastarFile.getAbsolutePath();
        }
        ultrastarFile = new File(path + "ultrastar.db");
        if (ultrastarFile.exists()) {
            return ultrastarFile.getAbsolutePath();
        }
        return null;
    }
}
