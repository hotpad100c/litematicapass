package ml.mypals.litematicapass.render;

import java.util.Map;
import java.util.TreeMap;

/**
 * Counts how often each step of the pipeline ran, for {@code /litematicapass debug status}: in a normal instance nothing
 * else shows whether a hook into OptiFine fired or why nothing was merged.
 */
public final class Stats {
	private static final Map<String, Long> COUNTS = new TreeMap<>();

	private Stats() {}

	public static void count(String step) {
		COUNTS.merge(step, 1L, Long::sum);
	}

	public static Map<String, Long> counts() {
		return COUNTS;
	}

	public static void reset() {
		COUNTS.clear();
	}
}
