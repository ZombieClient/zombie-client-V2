package dev.zombie;

import dev.zombie.gui.ZombieScreen;
import dev.zombie.hack.Hack;
import dev.zombie.hack.HackManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ZombieClient implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("zombieclient", "main"));

    private static HackManager hacks;
    private static KeyBinding menuKey;

    public static HackManager hacks() { return hacks; }

    @Override
    public void onInitializeClient() {
        hacks = new HackManager();
        Config.load(hacks);

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zombieclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        // One rebindable key per module (most default to unbound).
        for (Hack h : hacks.all()) {
            h.binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.zombieclient." + h.name.toLowerCase(), InputUtil.Type.KEYSYM, h.key, CATEGORY));
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            while (menuKey.wasPressed()) mc.setScreen(new ZombieScreen());
            hacks.tick();
        });

        HudElementRegistry.addLast(Identifier.of("zombieclient", "hud"), (ctx, tickCounter) -> drawHud(ctx));
    }

    private static void drawHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || hacks == null) return;
        int w = mc.getWindow().getScaledWidth();
        ctx.drawTextWithShadow(mc.textRenderer, "Zombie Client", 4, 4, 0xFF7BE05C);
        int y = 4;
        for (Hack h : hacks.all()) {
            if (!h.isEnabled()) continue;
            ctx.drawTextWithShadow(mc.textRenderer, h.name, w - mc.textRenderer.getWidth(h.name) - 4, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
