package dev.potato.fps;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.network.chat.Component;

/**
 * Potato FPS: auto-tunes render distance to hold a target FPS
 * and offers a one-command low-settings preset.
 *
 * Commands (client-side):
 *   /potato            - apply the Potato preset
 *   /potato auto       - toggle dynamic render distance
 *   /potato target N   - set target FPS (default 90)
 *
 * Uses Mojang mappings (names are the same across 1.21.x and 26.x).
 */
public class PotatoFps implements ClientModInitializer {
    private static final int MIN_DISTANCE = 2;

    private boolean auto = true;
    private int targetFps = 90;

    private int ceiling = -1;   // max render distance the tuner may restore to
    private int lastSet = -1;   // value we set last, to detect manual changes
    private int ticks = 0;
    private int cooldown = 0;
    private final int[] samples = new int[3];
    private int sampleIdx = 0;
    private int sampleCount = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        // Give the original distance back when leaving a world.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> restore(client));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("potato")
                .executes(ctx -> {
                    applyPreset(Minecraft.getInstance());
                    ctx.getSource().sendFeedback(Component.literal("Potato preset applied."));
                    return 1;
                })
                .then(ClientCommandManager.literal("auto").executes(ctx -> {
                    auto = !auto;
                    ctx.getSource().sendFeedback(Component.literal("Auto render distance: " + (auto ? "ON" : "OFF")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("target")
                    .then(ClientCommandManager.argument("fps", IntegerArgumentType.integer(20, 500))
                        .executes(ctx -> {
                            targetFps = IntegerArgumentType.getInteger(ctx, "fps");
                            ctx.getSource().sendFeedback(Component.literal("Target FPS: " + targetFps));
                            return 1;
                        })))));
    }

    private void onTick(Minecraft mc) {
        if (!auto || mc.level == null) return;
        if (++ticks % 20 != 0) return; // once per second

        samples[sampleIdx] = mc.getFps();
        sampleIdx = (sampleIdx + 1) % samples.length;
        sampleCount = Math.min(sampleCount + 1, samples.length);
        if (cooldown > 0) { cooldown--; return; }
        if (sampleCount < samples.length) return;

        int sum = 0;
        for (int s : samples) sum += s;
        int avg = sum / samples.length;

        int current = mc.options.renderDistance().get();
        if (ceiling < 0 || (lastSet > 0 && current != lastSet)) {
            ceiling = current; // first run, or the player changed it manually
        }

        if (avg < targetFps - 10 && current > MIN_DISTANCE) {
            setDistance(mc, current - 1);
            cooldown = 4;
        } else if (avg > targetFps + 30 && current < ceiling) {
            setDistance(mc, current + 1);
            cooldown = 8;
        }
    }

    private void setDistance(Minecraft mc, int value) {
        mc.options.renderDistance().set(value);
        lastSet = value;
    }

    private void restore(Minecraft mc) {
        if (ceiling > 0) {
            mc.options.renderDistance().set(ceiling);
        }
        ceiling = -1;
        lastSet = -1;
        sampleCount = 0;
        ticks = 0;
        cooldown = 0;
    }

    private void applyPreset(Minecraft mc) {
        Options o = mc.options;
        o.renderDistance().set(4);
        o.simulationDistance().set(5);
        o.particles().set(ParticleStatus.MINIMAL);
        o.cloudStatus().set(CloudStatus.OFF);
        o.entityDistanceScaling().set(0.5);
        o.biomeBlendRadius().set(0);
        o.entityShadows().set(false);
        o.bobView().set(false);
        o.enableVsync().set(false);
        o.save();
        ceiling = 6; // the auto tuner may raise distance up to 6 if FPS allows
        lastSet = 4;
    }
}
