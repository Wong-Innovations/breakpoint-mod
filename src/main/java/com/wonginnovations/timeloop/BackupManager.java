package com.wonginnovations.timeloop;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.ZipperUtil;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {

    public static boolean HAS_BACKED_UP = false;
    public static final Map<Integer, File> BACKUPS = new HashMap<>();
    public static final Map<Integer, File> BACKUP_SOURCES = new HashMap<>();
    public static boolean RESTORE_ON_CLOSE = false;

    private static File backupDir;
    private static File saveRoot;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getBackupsDir() {
        File backupDir = new File(DimensionManager.getCurrentSaveRootDirectory(), "timeloop/backups");
        if (!backupDir.exists())
            backupDir.mkdirs();
        return backupDir;
    }

    public static void backupDimensions() {
        backupDir = getBackupsDir();
        saveRoot = DimensionManager.getCurrentSaveRootDirectory();
        if (saveRoot == null) return;
        HAS_BACKED_UP = true;
        for (WorldServer worldServer : DimensionManager.getWorlds()) {
            if (worldServer.provider.getSaveFolder() != null && Timeloop.CONFIG.hasDimensionConfig(worldServer.provider.getDimension())) {
                try {
                    File newBackup = new File(backupDir, worldServer.provider.getSaveFolder() + "_backup.zip");
                    File source = new File(saveRoot, worldServer.provider.getSaveFolder());
                    if (!newBackup.exists())
                        zip(source, newBackup);
                    BACKUPS.put(worldServer.provider.getDimension(), newBackup);
                    BACKUP_SOURCES.put(worldServer.provider.getDimension(), source);
                } catch (FileNotFoundException e) {
                    HAS_BACKED_UP = false;
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (worldServer.provider.getDimension() == 0 && Timeloop.CONFIG.hasDimensionConfig(0)) {
                try {
                    File backup1 = new File(backupDir, "region_backup.zip");
                    File backup2 = new File(backupDir, "data_backup.zip");
                    File backup3 = new File(backupDir, "forcedchunks_dat_backup.zip");
                    File source1 = new File(saveRoot, "region");
                    File source2 = new File(saveRoot, "data");
                    File source3 = new File(saveRoot, "forcedchunks.dat");
                    if (!backup1.exists()) zip(source1, backup1);
                    if (!backup2.exists()) zip(source2, backup2);
                    if (!backup3.exists()) zip(source3, backup3);
                    BACKUPS.put(0, backupDir);
                    BACKUP_SOURCES.put(0, saveRoot);
                } catch (FileNotFoundException e) {
                    HAS_BACKED_UP = false;
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            File backup1 = new File(backupDir, "playerdata_backup.zip");
            File backup2 = new File(backupDir, "level_dat_backup.zip");
            File backup3 = new File(backupDir, "level_dat_old_backup.zip");
            File source1 = new File(saveRoot, "playerdata");
            File source2 = new File(saveRoot, "level.dat");
            File source3 = new File(saveRoot, "level.dat_old");
            if (!backup1.exists()) zip(source1, backup1);
            if (!backup2.exists()) zip(source2, backup2);
            if (!backup3.exists()) zip(source3, backup3);
        } catch (FileNotFoundException e) {
            HAS_BACKED_UP = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void restoreDimensions() {
        BACKUPS.keySet().forEach(id -> {
            if (Timeloop.CONFIG.hasDimensionConfig(id) && Timeloop.CONFIG.getDimensionConfig(id).doLoop) {
                try {
                    if (id != 0) {
                        FileUtils.cleanDirectory(BACKUP_SOURCES.get(id));
                        unzip(BACKUPS.get(id), BACKUP_SOURCES.get(id));
                    } else {
                        FileUtils.cleanDirectory(new File(BACKUP_SOURCES.get(id), "region"));
                        FileUtils.cleanDirectory(new File(BACKUP_SOURCES.get(id), "data"));
                        FileUtils.forceDelete(new File(BACKUP_SOURCES.get(id), "forcedchunks.dat"));
                        unzip(new File(backupDir, "region_backup.zip"), new File(saveRoot , "region"));
                        unzip(new File(backupDir, "data_backup.zip"), new File(saveRoot , "data"));
                        unzip(new File(backupDir, "forcedchunks_dat_backup.zip"), saveRoot);
                    }
                    if (saveRoot != null && Timeloop.CONFIG.keepInventory) {
                        FileUtils.cleanDirectory(new File(saveRoot, "playerdata"));
                        FileUtils.forceDelete(new File(saveRoot, "level.dat"));
                        FileUtils.forceDelete(new File(saveRoot, "level.dat_old"));
                        unzip(new File(backupDir, "playerdata_backup.zip"), new File(saveRoot, "playerdata"));
                        unzip(new File(backupDir, "level_dat_backup.zip"), saveRoot);
                        unzip(new File(backupDir, "level_dat_old_backup.zip"), saveRoot);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void zip(File fileToZip, File zipFile) throws IOException {
        if (fileToZip.isDirectory()) {
            // zip directory recursively
            ZipperUtil.zip(fileToZip, zipFile);
        } else {
            // zip single file
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }

            zipOut.close();
            fis.close();
            fos.close();
        }
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            Timeloop.LOGGER.info("Restoring: " + newFile.getAbsolutePath());
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

}
