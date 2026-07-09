package net.iskaa303.simpleportals.client.gui;

import net.iskaa303.simpleportals.Constants;
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

public class SimplePortalsConfigScreen extends Screen {

    private static final ResourceLocation HEADER_SEP =
            ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
    private static final ResourceLocation FOOTER_SEP =
            ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");

    private final Screen last;
    private Button overlayBtn;
    private EditBox intField;
    private Button intUp, intDown;

    // Layout
    private int listLeft, listRight, listTop, listBottom;
    private int rowLeft, rowWidth, overlayY, precisionY;

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
        intUp = Button.builder(Component.literal("▲"), b -> tickInt(1))
                .bounds(arrowX, precisionY + 4, 14, 10).build();
        intDown = Button.builder(Component.literal("▼"), b -> tickInt(-1))
                .bounds(arrowX, precisionY + 14, 14, 10).build();

        // All interactive widgets registered as renderable+child for focus & click
        addRenderableWidget(overlayBtn);
        addRenderableWidget(intField);
        addRenderableWidget(intUp);
        addRenderableWidget(intDown);

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - w / 2, by, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
                    SimplePortalsConfig.reset();
                    intField.setValue(String.valueOf(SimplePortalsConfig.dotPrecision));
                    overlayBtn.setMessage(Component.literal(
                            SimplePortalsConfig.overlayPosition.getName()));
                }).bounds(cx + 2, by, w / 2 - 2, 20).build());
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
        // Step 1: background blur (via super)
        super.render(gui, mouseX, mouseY, delta);

        // Step 2: title
        gui.drawCenteredString(font,
                Component.literal("§dSimple Portals§r  §aConfig§r"),
                width / 2, 16, 0xFFFFFF);

        // Step 3: list area elements
        gui.blit(HEADER_SEP, listLeft, listTop - 2, 0, 0,
                listRight - listLeft, 2, 32, 2);
        gui.blit(FOOTER_SEP, listLeft, listBottom, 0, 0,
                listRight - listLeft, 2, 32, 2);

        gui.drawCenteredString(font, Component.literal("§lGeneral"),
                (listLeft + listRight) / 2, listTop + 8, 0xFFFFFF);

        // OverlayPosition row
        gui.fill(rowLeft, overlayY, rowLeft + rowWidth, overlayY + 28, 0x66000000);
        gui.drawString(font, Component.literal("Overlay Position"),
                rowLeft + 12, overlayY + 6, 0xFFFFFF);

        // DotPrecision row
        gui.fill(rowLeft, precisionY, rowLeft + rowWidth, precisionY + 28, 0x66000000);
        gui.drawString(font, Component.literal("Dot Precision"),
                rowLeft + 12, precisionY + 6, 0xFFFFFF);

        // Step 4: redraw list-area widgets on top of fills (they were drawn
        // by super.render() under the fills, so we need them on top)
        overlayBtn.render(gui, mouseX, mouseY, delta);
        intField.render(gui, mouseX, mouseY, delta);
        intUp.render(gui, mouseX, mouseY, delta);
        intDown.render(gui, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        SimplePortalsConfig.save();
        minecraft.setScreen(last);
    }
}
