package com.foxy.twitchPolls.actions;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSoundAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        List<String> soundNames = context.config().getStringList("sounds");
        if (soundNames == null || soundNames.isEmpty()) return;
        String soundName = soundNames.get(ThreadLocalRandom.current().nextInt(soundNames.size()));
        try {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT)));
            if (sound != null) context.world().playSound(context.location(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
        }
    }
}