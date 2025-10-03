/*
 * Copyright 2021 Haven King
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.hephaestus.atmosfera;

import com.google.gson.*;
import dev.hephaestus.atmosfera.client.sound.AtmosphericSoundDefinition;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class AtmosferaConfig {
	private static final TreeMap<Identifier, Integer> VOLUME_MODIFIERS = new TreeMap<>(Comparator.comparing(id -> I18n.translate(id.toString())));
	private static final TreeMap<Identifier, Boolean> SUBTITLE_MODIFIERS = new TreeMap<>(Comparator.comparing(id -> I18n.translate(id.toString())));
	private static boolean printDebugMessages = false;
	private static boolean enableCustomMusic = true;
	private static float customMusicWeightScale = 2.5f;

	static {
		read();
	}

	private static void read() {
		try (InputStream fi = new FileInputStream("config" + File.separator + "atmosfera.json")) {
			JsonObject json = (JsonObject) JsonParser.parseReader(new InputStreamReader(fi));

			if (json.has("general")) {
				JsonObject general = json.getAsJsonObject("general");

				if (general.has("enable_custom_music")) {
					enableCustomMusic = general.get("enable_custom_music").getAsBoolean();
				}

				if (general.has("custom_music_weight_scale")) {
					customMusicWeightScale = general.get("custom_music_weight_scale").getAsFloat();
				}
			}

			if (json.has("volumes")) {
				for (Map.Entry<String, JsonElement> element : json.get("volumes").getAsJsonObject().entrySet()) {
					if (element.getValue().isJsonPrimitive()) {
						VOLUME_MODIFIERS.put(new Identifier(element.getKey()), element.getValue().getAsInt());
					}
				}
			}

			if (json.has("subtitles")) {
				for (Map.Entry<String, JsonElement> element : json.get("subtitles").getAsJsonObject().entrySet()) {
					if (element.getValue().isJsonPrimitive()) {
						SUBTITLE_MODIFIERS.put(new Identifier(element.getKey()), element.getValue().getAsBoolean());
					}
				}
			}

			if (json.has("debug")) {
				JsonObject debug = json.getAsJsonObject("debug");

				if (debug.has("print_debug_messages")) {
					printDebugMessages = debug.get("print_debug_messages").getAsBoolean();
				}
			}
		} catch (FileNotFoundException e) {
			Atmosfera.log("The user config file was not found. Creating the default config...");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			for (AtmosphericSoundDefinition sound : Atmosfera.SOUND_DEFINITIONS.values()) {
				VOLUME_MODIFIERS.putIfAbsent(sound.id(), sound.defaultVolume());
				SUBTITLE_MODIFIERS.putIfAbsent(sound.id(), sound.hasSubtitleByDefault());
			}

			for (AtmosphericSoundDefinition sound : Atmosfera.MUSIC_DEFINITIONS.values()) {
				VOLUME_MODIFIERS.putIfAbsent(sound.id(), sound.defaultVolume());
			}

			write();
		}
	}

	private static void write() {
		File configFile = new File("config" + File.separator + "atmosfera.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			JsonObject jsonObject = new JsonObject();

			JsonObject general = new JsonObject();
			general.addProperty("enable_custom_music", enableCustomMusic);
			general.addProperty("custom_music_weight_scale", customMusicWeightScale);

			jsonObject.add("general", general);
			jsonObject.add("volumes", gson.toJsonTree(VOLUME_MODIFIERS));
			jsonObject.add("subtitles", gson.toJsonTree(SUBTITLE_MODIFIERS));

			JsonObject debug = new JsonObject();
			debug.addProperty("print_debug_messages", printDebugMessages);

			jsonObject.add("debug", debug);

			writer.write(gson.toJson(jsonObject));
		} catch (IOException e) {
			Atmosfera.warn("Failed to save the config to the file.");
		}
	}

	public static float volumeModifier(Identifier soundId) {
		try {
			return (VOLUME_MODIFIERS.getOrDefault(
					soundId, Atmosfera.SOUND_DEFINITIONS.getOrDefault(
							soundId, Atmosfera.MUSIC_DEFINITIONS.get(soundId)).defaultVolume())) / 100F;
		} catch (NullPointerException e) {
			Atmosfera.warn("Unknown sound: {}", soundId);
			throw e;
		}
	}

	public static boolean showSubtitle(Identifier soundId) {
		return SUBTITLE_MODIFIERS.getOrDefault(soundId, Atmosfera.SOUND_DEFINITIONS.get(soundId).hasSubtitleByDefault());
	}

	public static Screen getScreen(Screen parent) {
		read();

		ConfigBuilder builder = ConfigBuilder.create().setTitle(Text.literal(Atmosfera.MOD_NAME));
		builder.setParentScreen(parent);
		builder.setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/light_blue_stained_glass.png"));
		ConfigEntryBuilder entryBuilder = builder.entryBuilder()
				.setResetButtonKey(Text.translatable("text.cloth-config.reset_value"));

		ConfigCategory generalCategory = builder.getOrCreateCategory(Text.translatable("config.category.atmosfera.general"));
		ConfigCategory volumesCategory = builder.getOrCreateCategory(Text.translatable("config.category.atmosfera.volumes"));
		ConfigCategory subtitlesCategory = builder.getOrCreateCategory(Text.translatable("config.category.atmosfera.subtitles"));

		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			ConfigCategory debugCategory = builder.getOrCreateCategory(Text.translatable("config.category.atmosfera.debug"));
			debugCategory.addEntry(entryBuilder
					.startBooleanToggle(Text.translatable("config.value.atmosfera.print_debug_messages"), printDebugMessages)
					.setSaveConsumer(b -> printDebugMessages = b)
					.setDefaultValue(false)
					.build()
			);
		}

		SubCategoryBuilder soundSubcategory = entryBuilder
				.startSubCategory(Text.translatable("config.subcategory.atmosfera.ambient_sound"))
				.setExpanded(true);
		SubCategoryBuilder musicSubcategory = entryBuilder
				.startSubCategory(Text.translatable("config.subcategory.atmosfera.music"))
				.setExpanded(true);

		generalCategory.addEntry(
				entryBuilder.startBooleanToggle(Text.translatable("config.value.atmosfera.enable_custom_music"), enableCustomMusic)
						.setTooltip(Text.translatable("config.value.atmosfera.enable_custom_music.@Tooltip"))
						.setSaveConsumer(b -> enableCustomMusic = b)
						.setDefaultValue(true)
						.build()
		);

		generalCategory.addEntry(
				entryBuilder.startLongSlider(Text.translatable("config.value.atmosfera.custom_music_weight_scale"), (long)(customMusicWeightScale * 100), 1, 1000)
						.setSaveConsumer(v -> customMusicWeightScale = v / 100f)
						.setTextGetter(v -> Text.literal(v + "%"))
						.setDefaultValue(250)
						.build()
		);

		generalCategory.addEntry(
				entryBuilder.startTextDescription(Text.translatable("config.value.atmosfera.custom_music_weight_scale_explanation"))
						.setTooltip(Text.translatable("config.value.atmosfera.custom_music_weight_scale_explanation.@Tooltip"))
						.build()
		);

		for (Map.Entry<Identifier, Integer> sound : VOLUME_MODIFIERS.entrySet()) {
			Map<Identifier, AtmosphericSoundDefinition> soundType;

			if (Atmosfera.SOUND_DEFINITIONS.containsKey(sound.getKey())) {
				soundType = Atmosfera.SOUND_DEFINITIONS;

				// Prevents the crash caused by additional and missing elements.
				if (soundType.containsKey(sound.getKey())) {

					// Replaces the "colon" with a "dot" as the ID separator to utilize the language file.
					String soundLangID = String.join(".", sound.getKey().toString().split(":"));

					MutableText tooltip = Text.literal(soundLangID + "\n");
					tooltip.append(Text.translatable("subtitle." + soundLangID));
					tooltip.append("\n");
					tooltip.append(Text.translatable("config.value.atmosfera.sound_tip.@Tooltip"));

					soundSubcategory.add(
							entryBuilder.startIntSlider(Text.translatable(soundLangID), sound.getValue(), 0, 200)
									.setDefaultValue(soundType.get(sound.getKey()).defaultVolume())
									.setTooltip(tooltip.formatted(Formatting.GRAY))
									.setTextGetter(integer -> Text.literal(integer + "%"))
									.setSaveConsumer(volume -> VOLUME_MODIFIERS.put(sound.getKey(), volume))
									.build()
					);
				}
			} else {
				soundType = Atmosfera.MUSIC_DEFINITIONS;
				if (soundType.containsKey(sound.getKey())) {
					String soundLangID = String.join(".", sound.getKey().toString().split(":"));

					MutableText tooltip = Text.literal(soundLangID);
					tooltip.append("\n");
					tooltip.append(Text.translatable("config.value.atmosfera.sound_tip.@Tooltip"));

					musicSubcategory.add(
							entryBuilder.startIntSlider(Text.translatable(soundLangID), sound.getValue(), 0, 200)
									.setDefaultValue(soundType.get(sound.getKey()).defaultVolume())
									.setTooltip(tooltip.formatted(Formatting.GRAY))
									.setTextGetter(integer -> Text.literal(integer + "%"))
									.setSaveConsumer(volume -> VOLUME_MODIFIERS.put(sound.getKey(), volume))
									.build()
					);
				}
			}
		}

		volumesCategory.addEntry(soundSubcategory.build());
		volumesCategory.addEntry(musicSubcategory.build());

		for (Map.Entry<Identifier, Boolean> sound : SUBTITLE_MODIFIERS.entrySet()) {
			if (Atmosfera.SOUND_DEFINITIONS.containsKey(sound.getKey())) {
				String soundLangID = String.join(".", sound.getKey().toString().split(":"));

				MutableText tooltipText = Text.literal(soundLangID + "\n");
				tooltipText.append(Text.translatable("subtitle." + soundLangID));

				subtitlesCategory.addEntry(
						entryBuilder.startBooleanToggle(Text.translatable(soundLangID), sound.getValue())
								.setDefaultValue(Atmosfera.SOUND_DEFINITIONS.get(sound.getKey()).hasSubtitleByDefault())
								.setTooltip(tooltipText.formatted(Formatting.GRAY))
								.setSaveConsumer(subtitle -> SUBTITLE_MODIFIERS.put(sound.getKey(), subtitle))
								.build()
				);
			}
		}

		if (soundSubcategory.size() + musicSubcategory.size() == 0) {
			subtitlesCategory.removeCategory();
			volumesCategory.addEntry(
					entryBuilder.startTextDescription(Text.translatable("config.atmosfera.resource_pack_warning").formatted(Formatting.RED))
							.build()
			);
		}

		builder.setSavingRunnable(AtmosferaConfig::write);

		return builder.build();
	}

	public static boolean printDebugMessages() {
		return printDebugMessages;
	}

	public static boolean enableCustomMusic() {
		return enableCustomMusic;
	}

	public static float customMusicWeightScale() {
		return customMusicWeightScale;
	}
}
