package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.platform.Services;

public class SimplePortalsMod {
    public static void init() {
        if (Services.PLATFORM.isModLoaded(Constants.MOD_ID)) {
            Constants.LOG.info("{} loaded!", Constants.MOD_NAME);
        }
    }
}