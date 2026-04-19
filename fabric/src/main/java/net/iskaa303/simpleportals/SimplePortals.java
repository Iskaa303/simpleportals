package net.iskaa303.simpleportals;

import net.iskaa303.simpleportals.CommonClass;
import net.iskaa303.simpleportals.Constants;

import net.fabricmc.api.ModInitializer;

public class SimplePortals implements ModInitializer {
    
    @Override
    public void onInitialize() {
        
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Constants.LOG.info("Hello Fabric world!");
        CommonClass.init();
    }
}
