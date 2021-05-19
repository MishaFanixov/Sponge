package org.spongepowered.common.adventure;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.minecraft.client.KeyMapping;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.common.accessor.client.KeyMappingAccessor;
import org.spongepowered.common.launch.Launch;

final class ComponentFlattenerProvider {
    private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");
    static final ComponentFlattener INSTANCE;

    static {
        final ComponentFlattener.Builder builder = ComponentFlattener.basic().toBuilder();

        if (!Launch.getInstance().isDedicatedServer()) {
            builder.mapper(KeybindComponent.class, ComponentFlattenerProvider::resolveKeybind);
        }

        builder.complexMapper(TranslatableComponent.class, (component, consumer) -> {
            final String key = component.key();
            for (final Translator registry : GlobalTranslator.get().sources()) {
                if (registry instanceof TranslationRegistry && ((TranslationRegistry) registry).contains(key)) {
                    consumer.accept(GlobalTranslator.render(component, Locale.getDefault()));
                    return;
                }
            }

            final /* @NonNull */ String translated = Language.getInstance().getOrDefault(key);
            final Matcher matcher = ComponentFlattenerProvider.LOCALIZATION_PATTERN.matcher(translated);
            final List<Component> args = component.args();
            int argPosition = 0;
            int lastIdx = 0;
            while (matcher.find()) {
                // append prior
                if (lastIdx < matcher.start()) {
                    consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
                }
                lastIdx = matcher.end();

                final /* @Nullable */ String argIdx = matcher.group(1);
                // calculate argument position
                if (argIdx != null) {
                    try {
                        final int idx = Integer.parseInt(argIdx);
                        if (idx < args.size()) {
                            consumer.accept(args.get(idx));
                        }
                    } catch (final NumberFormatException ex) {
                        // ignore, drop the format placeholder
                    }
                } else {
                    final int idx = argPosition++;
                    if (idx < args.size()) {
                        consumer.accept(args.get(idx));
                    }
                }
            }

            // append tail
            if (lastIdx < translated.length()) {
                consumer.accept(Component.text(translated.substring(lastIdx)));
            }
        });

        INSTANCE = builder.build();
    }

    @OnlyIn(Dist.CLIENT)
    private static String resolveKeybind(final KeybindComponent component) {
        final KeyMapping mapping = KeyMappingAccessor.accessor$ALL().get(component.keybind());
        if (mapping != null) {
            return mapping.getTranslatedKeyMessage().getString();
        }
        return component.keybind();
    }
}
