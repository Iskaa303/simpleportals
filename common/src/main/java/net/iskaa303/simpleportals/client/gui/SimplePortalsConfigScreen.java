package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.Constants;
import net.iskaa303.simpleportals.client.keybinds.SimplePortalsKeybinds;
import net.iskaa303.simpleportals.config.OverlayPosition;
import net.iskaa303.simpleportals.config.SimplePortalsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class SimplePortalsConfigScreen extends Screen {

    private static final ResourceLocation HEADER_SEP =
            ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
    private static final ResourceLocation FOOTER_SEP =
            ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");

    private final Screen last;
    private Button overlayBtn;
    private EditBox intField;
    private Button intUp, intDown;

    // Keybinding buttons (6 total)
    private Button keyModeWheelBtn, keySnapGridBtn, keySnapPointBtn, keyToggleGridBtn;
    private Button keyCopySurfaceBtn, keyConnectSurfaceBtn;
    private int listeningForKey = -1; // -1 = not listening, 0..5 = which keybinding


    // Layout
    private int listLeft, listRight, listTop, listBottom;
    private int rowLeft, rowWidth, overlayY, precisionY;
    private int keySectionY;

    public SimplePortalsConfigScreen(Screen last) {
        super(Component.literal(Constants.MOD_NAME + " Config"));
        this.last = last;
    }

    @Override
    protected void init() {
        super.init();

        int w = Math.min(420, width - 40);
        int cx = width / 2;
        int by = height - 28;

        listLeft = (width - w) / 2;
        listRight = listLeft + w;
        listTop = 38;
        listBottom = height - 56;
        rowWidth = w - 20;
        rowLeft = listLeft + (w - rowWidth) / 2;

        overlayY = listTop + 24;
        precisionY = overlayY + 28;
        keySectionY = precisionY + 36;

        int btnX = rowLeft + rowWidth - 130 - 12;

        overlayBtn = Button.builder(
                Component.literal(SimplePortalsConfig.overlayPosition.getName()),
                btn -> {
                    OverlayPosition[] vals = OverlayPosition.values();
                    int next = (SimplePortalsConfig.overlayPosition.ordinal() + 1) % vals.length;
                    SimplePortalsConfig.overlayPosition = vals[next];
                    SimplePortalsConfig.save();
                    btn.setMessage(Component.literal(vals[next].getName()));
                }
        ).bounds(btnX, overlayY + 4, 130, 20).build();
        overlayBtn.setTooltip(Tooltip.create(Component.literal(
                "Options: top_left, top_center, top_right,\nmid_left, mid_center, mid_right,\nbottom_left, bottom_center, bottom_right")));

        int fieldW = 130 - 16;
        intField = new EditBox(font, btnX + 1, precisionY + 5, fieldW, 18, Component.literal(""));
        intField.setValue(String.valueOf(SimplePortalsConfig.dotPrecision));
        intField.setFilter(s -> s.matches("-?[0-9]*"));
        intField.setResponder(s -> {
            try {
                SimplePortalsConfig.dotPrecision = s.isBlank() ? 0 : Integer.parseInt(s);
                SimplePortalsConfig.save();
            } catch (NumberFormatException ignored) {}
        });
        intField.setCanLoseFocus(true);
        intField.setTooltip(Tooltip.create(Component.literal(
                "Decimal places for coordinates (0-10)")));

        int arrowX = btnX + fieldW + 3;
        intUp = Button.builder(Component.literal("\u25B2"), b -> tickInt(1))
                .bounds(arrowX, precisionY + 4, 14, 10).build();
        intDown = Button.builder(Component.literal("\u25BC"), b -> tickInt(-1))
                .bounds(arrowX, precisionY + 14, 14, 10).build();

        // 6 keybinding buttons
        keyModeWheelBtn = makeKeyButton(SimplePortalsKeybinds.getModeWheel(),
                keySectionY + 24, btnX, v -> SimplePortalsKeybinds.setModeWheel(v));
        keySnapGridBtn = makeKeyButton(SimplePortalsKeybinds.getSnapGrid(),
                keySectionY + 50, btnX, v -> SimplePortalsKeybinds.setSnapGrid(v));
        keySnapPointBtn = makeKeyButton(SimplePortalsKeybinds.getSnapPoint(),
                keySectionY + 76, btnX, v -> SimplePortalsKeybinds.setSnapPoint(v));
        keyToggleGridBtn = makeKeyButton(SimplePortalsKeybinds.getToggleGrid(),
                keySectionY + 102, btnX, v -> SimplePortalsKeybinds.setToggleGrid(v));
        keyCopySurfaceBtn = makeKeyButton(SimplePortalsKeybinds.getCopySurface(),
                keySectionY + 128, btnX, v -> SimplePortalsKeybinds.setCopySurface(v));
        keyConnectSurfaceBtn = makeKeyButton(SimplePortalsKeybinds.getConnectSurface(),
                keySectionY + 154, btnX, v -> SimplePortalsKeybinds.setConnectSurface(v));

        addRenderableWidget(overlayBtn);
        addRenderableWidget(intField);
        addRenderableWidget(intUp);
        addRenderableWidget(intDown);
        addRenderableWidget(keyModeWheelBtn);
        addRenderableWidget(keySnapGridBtn);
        addRenderableWidget(keySnapPointBtn);
        addRenderableWidget(keyToggleGridBtn);
        addRenderableWidget(keyCopySurfaceBtn);
        addRenderableWidget(keyConnectSurfaceBtn);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - w / 2, by, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
                    SimplePortalsConfig.reset();
                    intField.setValue(String.valueOf(SimplePortalsConfig.dotPrecision));
                    overlayBtn.setMessage(Component.literal(
                            SimplePortalsConfig.overlayPosition.getName()));
                    updateKeyButtons();
                }).bounds(cx + 2, by, w / 2 - 2, 20).build());
    }

    private Button makeKeyButton(int initialKey, int y, int btnX, Consumer<Integer> setter) {
        return Button.builder(
                SimplePortalsKeybinds.getKeyName(initialKey),
                btn -> {
                    if (listeningForKey == -1) {
                        listeningForKey = getListeningIndex(btn);
                        btn.setMessage(Component.literal("..."));
                    }
                }
        ).bounds(btnX, y, 130, 20).build();
    }

    private int getListeningIndex(Button btn) {
        if (btn == keyModeWheelBtn) return 0;
        if (btn == keySnapGridBtn) return 1;
        if (btn == keySnapPointBtn) return 2;
        if (btn == keyToggleGridBtn) return 3;
        if (btn == keyCopySurfaceBtn) return 4;
        if (btn == keyConnectSurfaceBtn) return 5;
        return -1;
    }

    private void applyKeyBinding(int index, int keyCode) {
        switch (index) {
            case 0 -> SimplePortalsKeybinds.setModeWheel(keyCode);
            case 1 -> SimplePortalsKeybinds.setSnapGrid(keyCode);
            case 2 -> SimplePortalsKeybinds.setSnapPoint(keyCode);
            case 3 -> SimplePortalsKeybinds.setToggleGrid(keyCode);
            case 4 -> SimplePortalsKeybinds.setCopySurface(keyCode);
            case 5 -> SimplePortalsKeybinds.setConnectSurface(keyCode);
        }
        SimplePortalsConfig.save();
        updateKeyButtons();
        listeningForKey = -1;
    }

    private void updateKeyButtons() {
        keyModeWheelBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getModeWheel()));
        keySnapGridBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getSnapGrid()));
        keySnapPointBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getSnapPoint()));
        keyToggleGridBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getToggleGrid()));
        keyCopySurfaceBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getCopySurface()));
        keyConnectSurfaceBtn.setMessage(SimplePortalsKeybinds.getKeyName(SimplePortalsKeybinds.getConnectSurface()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningForKey >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = -1;
                updateKeyButtons();
                return true;
            }
            applyKeyBinding(listeningForKey, keyCode);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void tickInt(int dir) {
        int inc = hasShiftDown() ? 10 : hasControlDown() ? 5 : 1;
        int v = Math.max(0, Math.min(10, SimplePortalsConfig.dotPrecision + dir * inc));
        SimplePortalsConfig.dotPrecision = v;
        SimplePortalsConfig.save();
        intField.setValue(String.valueOf(v));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);

        gui.drawCenteredString(font,
                Component.literal("\u00a7dSimple Portals\u00a7r  \u00a7aConfig\u00a7r"),
                width / 2, 16, 0xFFFFFF);

        gui.blit(HEADER_SEP, listLeft, listTop - 2, 0, 0,
                listRight - listLeft, 2, 32, 2);
        gui.blit(FOOTER_SEP, listLeft, listBottom, 0, 0,
                listRight - listLeft, 2, 32, 2);

        gui.drawCenteredString(font, Component.literal("\u00a7lGeneral"),
                (listLeft + listRight) / 2, listTop + 8, 0xFFFFFF);

        // OverlayPosition
        gui.fill(rowLeft, overlayY, rowLeft + rowWidth, overlayY + 28, 0x66000000);
        gui.drawString(font, Component.literal("Overlay Position"),
                rowLeft + 12, overlayY + 6, 0xFFFFFF);

        // DotPrecision
        gui.fill(rowLeft, precisionY, rowLeft + rowWidth, precisionY + 28, 0x66000000);
        gui.drawString(font, Component.literal("Dot Precision"),
                rowLeft + 12, precisionY + 6, 0xFFFFFF);

        // Keybindings section
        gui.drawCenteredString(font, Component.literal("\u00a7lKeybindings"),
                (listLeft + listRight) / 2, keySectionY, 0xFFFFFF);

        gui.fill(rowLeft, keySectionY + 18, rowLeft + rowWidth, keySectionY + 182, 0x66000000);
        gui.drawString(font, Component.translatable("config.simpleportals.keyModeWheel"),
                rowLeft + 12, keySectionY + 28, 0xFFFFFF);
        gui.drawString(font, Component.translatable("config.simpleportals.keySnapGrid"),
                rowLeft + 12, keySectionY + 54, 0xFFFFFF);
        gui.drawString(font, Component.translatable("config.simpleportals.keySnapPoint"),
                rowLeft + 12, keySectionY + 80, 0xFFFFFF);
        gui.drawString(font, Component.translatable("config.simpleportals.keyToggleGrid"),
                rowLeft + 12, keySectionY + 106, 0xFFFFFF);
        gui.drawString(font, Component.translatable("config.simpleportals.keyCopySurface"),
                rowLeft + 12, keySectionY + 132, 0xFFFFFF);
        gui.drawString(font, Component.translatable("config.simpleportals.keyConnectSurface"),
                rowLeft + 12, keySectionY + 158, 0xFFFFFF);

        // Redraw widgets on top
        overlayBtn.render(gui, mouseX, mouseY, delta);
        intField.render(gui, mouseX, mouseY, delta);
        intUp.render(gui, mouseX, mouseY, delta);
        intDown.render(gui, mouseX, mouseY, delta);
        keyModeWheelBtn.render(gui, mouseX, mouseY, delta);
        keySnapGridBtn.render(gui, mouseX, mouseY, delta);
        keySnapPointBtn.render(gui, mouseX, mouseY, delta);
        keyToggleGridBtn.render(gui, mouseX, mouseY, delta);
        keyCopySurfaceBtn.render(gui, mouseX, mouseY, delta);
        keyConnectSurfaceBtn.render(gui, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        SimplePortalsConfig.save();
        minecraft.setScreen(last);
    }
}
