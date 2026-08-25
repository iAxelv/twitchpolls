package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Sound;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSoundAction implements ActionStrategy {
    @Override
    @SuppressWarnings("removal")
    public void execute(ActionContext context) {
        List<String> soundNames = context.config().getStringList("sounds");
        if (soundNames == null || soundNames.isEmpty()) {
            context.plugin().getLogger().warning("RANDOM_SOUND no tiene sonidos configurados en " + context.config().getName());
            return;
        }

        String configuredName = soundNames.get(ThreadLocalRandom.current().nextInt(soundNames.size()));
        String soundName = configuredName.trim()
                .replaceFirst("(?i)^minecraft:", "")
                .toUpperCase(Locale.ROOT);
        try {
            Sound sound = Sound.valueOf(soundName);
            context.world().playSound(context.location(), sound, 1.0f, 1.0f);
            context.plugin().getLogger().info("RANDOM_SOUND reprodujo " + soundName + " para " + context.player().getName());
        } catch (IllegalArgumentException exception) {
            context.plugin().getLogger().warning("RANDOM_SOUND desconocido en "
                    + context.config().getName() + ": " + configuredName);
        }
    }
}