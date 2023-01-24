package com.wonginnovations.timeloop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {

    public boolean keepInventory = false;
    public String serverDisconnectMsg = "";

    public Map<Integer, DimensionConfig> dimensions = new HashMap<>();

    public ModConfig() {
    }

    public boolean hasDimensionConfig(int id) {
        return dimensions.containsKey(id);
    }

    public DimensionConfig getDimensionConfig(int id) {
        return dimensions.get(id);
    }

    public void loadConfig(File cfg) {
        try {
            if (cfg.createNewFile()) {
                writeDefaultConfig(cfg);
            } else {
                try (Reader reader = new FileReader(cfg)) {
                    ModConfig fromFile = new Gson().fromJson(reader, ModConfig.class);
                    this.keepInventory = fromFile.keepInventory;
                    this.serverDisconnectMsg = fromFile.serverDisconnectMsg;
                    this.dimensions = fromFile.dimensions;
                }
           }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeDefaultConfig(File cfg) {
        this.keepInventory = false;
        this.serverDisconnectMsg = "End of loop reached. World has been reset.";
        this.dimensions.put(0, new DimensionConfig(0, 0, 0, false));
        try (Writer writer = new FileWriter(cfg)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class DimensionConfig {

        public int id;
        public int loopStart;
        public int loopEnd;
        public boolean doLoop;

        public DimensionConfig(int id, int loopStart, int loopEnd, boolean doLoop) {
            this.id = id;
            this.loopStart = loopStart;
            this.loopEnd = loopEnd;
            this.doLoop = doLoop;
        }

    }

}
