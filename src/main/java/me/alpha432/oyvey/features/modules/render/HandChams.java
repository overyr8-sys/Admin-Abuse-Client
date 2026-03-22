package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class HandChams extends Module {

    private static HandChams INSTANCE;

    public enum RenderMode { Solid, Wireframe }

    public final Setting<RenderMode> mode  = mode("Mode",  RenderMode.Solid);
    public final Setting<Integer>    red   = num("Red",   255, 0, 255);
    public final Setting<Integer>    green = num("Green", 0,   0, 255);
    public final Setting<Integer>    blue  = num("Blue",  0,   0, 255);
    public final Setting<Integer>    alpha = num("Alpha", 240, 0, 255);

    public HandChams() {
        super("HandChams", "Changes your hand color", Category.RENDER);
        INSTANCE = this;
    }

    public static HandChams getInstance() {
        return INSTANCE;
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }
}