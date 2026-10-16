package ml.mypals.litematicapass.render;

import java.util.Map;
import java.util.TreeMap;

/**
 * Tracks pipeline execution counts for `/litematicapass debug status`,
 * making it easier to diagnose OptiFine hook failures and missing framebuffer composites.
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
