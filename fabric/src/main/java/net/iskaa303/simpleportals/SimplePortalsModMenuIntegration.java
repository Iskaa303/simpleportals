package net.iskaa303.simpleportals;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.iskaa303.simpleportals.client.gui.SimplePortalsConfigScreen;

public class SimplePortalsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SimplePortalsConfigScreen::new;
    }
}
