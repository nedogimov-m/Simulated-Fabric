package dev.simulated_team.simulated.client.sections;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatedSectionManager {
	private static final Map<Identifier, SimulatedSection> SECTIONS = new HashMap<>();
	private static final Map<SimulatedSection, Identifier> BY_SECTION = new HashMap<>();
	private static List<SimulatedSection> sortedSections = new ArrayList<>();

	public static SimulatedSection getSection(final Identifier id) {
		return SECTIONS.get(id);
	}

	public static Identifier getId(final SimulatedSection section) {
		return BY_SECTION.get(section);
	}

	public static List<SimulatedSection> getSections() {
		return sortedSections;
	}

	public static class ReloadListener extends SimpleJsonResourceReloadListener<SimulatedSection> {

		private static final Gson GSON = new Gson();
		public ReloadListener() {
			super(SimulatedSection.CODEC, net.minecraft.resources.FileToIdConverter.json("simulated_sections"));
		}

		@Override
		protected void apply(final Map<Identifier, SimulatedSection> map, final ResourceManager resourceManager, final ProfilerFiller profilerFiller) {
			SECTIONS.clear();
			BY_SECTION.clear();
            SECTIONS.putAll(map);
            map.forEach((id,section)->BY_SECTION.put(section,id));

			sortedSections = SECTIONS.values().stream().sorted().toList();
		}
	}
}
