package net.iskaa303.simpleportals.registry;

import java.util.function.Supplier;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.item.PortalStick;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class SimplePortalsItems {
    public static final RegistrationProvider<Item> ITEM_REGISTRY = RegistrationProvider.get(BuiltInRegistries.ITEM, Constants.MOD_ID);

    public static final RegistryObject<PortalStick> PORTAL_STICK = register("portal_stick", () -> new PortalStick(new Item.Properties().stacksTo(1)));

    private SimplePortalsItems() {}

    public static void bootstrap() {}

    public static <T extends Item> RegistryObject<T> register(String name, final Supplier<T> item) {
        return ITEM_REGISTRY.register(name, item);
    }
}
