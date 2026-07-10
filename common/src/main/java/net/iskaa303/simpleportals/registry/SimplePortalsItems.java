package net.iskaa303.simpleportals.registry;

import java.util.function.Supplier;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.item.PointStick;
import net.iskaa303.simpleportals.item.ConnectionStick;
import net.iskaa303.simpleportals.item.SurfaceStick;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class SimplePortalsItems {
    public static final RegistrationProvider<Item> ITEM_REGISTRY = RegistrationProvider.get(BuiltInRegistries.ITEM, Constants.MOD_ID);

    public static final RegistryObject<PointStick> POINT_STICK = register("point_stick", () -> new PointStick(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ConnectionStick> CONNECTION_STICK = register("connection_stick", () -> new ConnectionStick(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<SurfaceStick> SURFACE_STICK = register("surface_stick", () -> new SurfaceStick(new Item.Properties().stacksTo(1)));
    private SimplePortalsItems() {}

    public static void bootstrap() {}

    public static <T extends Item> RegistryObject<T> register(String name, final Supplier<T> item) {
        return ITEM_REGISTRY.register(name, item);
    }
}
