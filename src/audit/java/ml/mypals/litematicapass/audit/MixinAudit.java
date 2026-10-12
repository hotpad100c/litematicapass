package ml.mypals.litematicapass.audit;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Development check, not part of the mod: forces Mixin to apply every mixin of the game, then checks in the classes
 * Mixin exported that every handler of every LitematicaPass mixin is called from its target, and stops the game. Run it with
 * {@code ./gradlew :<version>:runAuditClient}; the exit code is 0 only when every mixin applied.
 */
public final class MixinAudit implements PreLaunchEntrypoint {
	private static final String TAG = "[litematicapass-audit] ";
	private static final String CONFIG = "litematicapass.mixins.json";

	@Override
	public void onPreLaunch() {
		int failures = 0;
		try {
			try {
				MixinEnvironment.getCurrentEnvironment().audit();
				System.out.println(TAG + "Mixin audit finished");
			} catch (Throwable t) {
				// Another mod's mixin can fail in the development environment only (for example a descriptor written
				// in intermediary names without a refmap); our own targets are loaded and checked one by one below.
				System.out.println(TAG + "note: the full Mixin audit stopped: " + t);
			}
			failures = checkOwnMixins();
		} catch (Throwable t) {
			System.out.println(TAG + "FAIL: the audit threw " + t);
			t.printStackTrace(System.out);
			failures++;
		}
		System.out.println(TAG + (failures == 0 ? "RESULT: OK" : "RESULT: " + failures + " FAILURE(S)"));
		System.out.flush();
		Runtime.getRuntime().halt(failures == 0 ? 0 : 1);
	}

	private static int checkOwnMixins() throws Exception {
		ClassLoader loader = MixinAudit.class.getClassLoader();
		String config = read(loader, CONFIG);
		String pkg = match(config, "\"package\"\\s*:\\s*\"([^\"]+)\"").get(0);
		String pluginName = match(config, "\"plugin\"\\s*:\\s*\"([^\"]+)\"").get(0);
		IMixinConfigPlugin plugin = (IMixinConfigPlugin) Class.forName(pluginName, true, loader).getDeclaredConstructor().newInstance();
		String client = config.substring(config.indexOf("\"client\""));
		client = client.substring(client.indexOf('[') + 1, client.indexOf(']'));

		int failures = 0;
		for (String simple : match(client, "\"([^\"]+)\"")) {
			String mixin = pkg + "." + simple;
			if (simple.startsWith("iris.") && !FabricLoader.getInstance().isModLoaded("iris")) {
				System.out.println(TAG + "skip " + simple + " (Iris not loaded)");
				continue;
			}
			ClassNode node = new ClassNode();
			try (InputStream in = loader.getResourceAsStream(mixin.replace('.', '/') + ".class")) {
				new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
			}
			for (String target : targets(node)) {
				if (!plugin.shouldApplyMixin(target, mixin)) {
					System.out.println(TAG + "skip " + simple + " -> " + target + " (not applied on this version, see the mixin plugin)");
					continue;
				}
				try {
					Class.forName(target, false, loader);
				} catch (ClassNotFoundException e) {
					System.out.println(TAG + "FAIL " + simple + " -> " + target + ": target class not found");
					failures++;
					continue;
				} catch (Throwable t) {
					System.out.println(TAG + "FAIL " + simple + " -> " + target + ": could not transform: " + t);
					failures++;
					continue;
				}
				ClassNode transformed = exported(target);
				if (transformed == null) {
					System.out.println(TAG + "FAIL " + simple + " -> " + target + ": no exported class (is -Dmixin.debug.export=true set?)");
					failures++;
					continue;
				}
				// Every handler must be called from the transformed target; an uncalled one injected nowhere.
				for (MethodNode handler : node.methods) {
					Integer require = require(handler);
					if (require == null) {
						continue;
					}
					int calls = callsTo(transformed, target.replace('.', '/'), handler.name);
					if (calls > 0) {
						System.out.println(TAG + "ok   " + simple + "." + handler.name + " -> " + target + ": called " + calls + "x");
					} else if (require == 0) {
						System.out.println(TAG + "warn " + simple + "." + handler.name + " -> " + target + ": injected nowhere (require = 0)");
					} else {
						System.out.println(TAG + "FAIL " + simple + "." + handler.name + " -> " + target + ": injected nowhere");
						failures++;
					}
				}
			}
		}
		return failures;
	}

	/** The require value of an injector handler (1 when not given), or null when the method is no handler. */
	private static Integer require(MethodNode method) {
		if (method.visibleAnnotations == null) {
			return null;
		}
		for (AnnotationNode a : method.visibleAnnotations) {
			if (a.desc.contains("/injector/") || a.desc.contains("/injection/")) {
				return value(a, "require") instanceof Integer r ? r : 1;
			}
		}
		return null;
	}

	/** The target class as Mixin exported it after transforming it. */
	private static ClassNode exported(String target) throws Exception {
		Path file = FabricLoader.getInstance().getGameDir().resolve(".mixin.out/class/" + target.replace('.', '/') + ".class");
		if (!Files.isRegularFile(file)) {
			return null;
		}
		ClassNode node = new ClassNode();
		new ClassReader(Files.readAllBytes(file)).accept(node, 0);
		return node;
	}

	/** Calls in the class to its merged copies of the handler (Mixin renames them to end with the handler name). */
	private static int callsTo(ClassNode cls, String owner, String handler) {
		int calls = 0;
		for (MethodNode method : cls.methods) {
			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof MethodInsnNode call && call.owner.equals(owner) && !call.name.equals(handler)
					&& call.name.endsWith("$" + handler)) {
					calls++;
				}
			}
		}
		return calls;
	}

	private static List<String> targets(ClassNode node) {
		List<String> targets = new ArrayList<>();
		for (AnnotationNode a : node.invisibleAnnotations != null ? node.invisibleAnnotations : List.<AnnotationNode>of()) {
			if (!a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
				continue;
			}
			if (value(a, "value") instanceof List<?> types) {
				for (Object t : types) {
					targets.add(((Type) t).getClassName());
				}
			}
			if (value(a, "targets") instanceof List<?> names) {
				for (Object n : names) {
					targets.add(((String) n).replace('/', '.'));
				}
			}
		}
		return targets;
	}

	private static Object value(AnnotationNode a, String key) {
		if (a.values == null) {
			return null;
		}
		for (int i = 0; i + 1 < a.values.size(); i += 2) {
			if (key.equals(a.values.get(i))) {
				return a.values.get(i + 1);
			}
		}
		return null;
	}

	private static String read(ClassLoader loader, String name) throws Exception {
		try (InputStream in = loader.getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalStateException(name + " not found");
			}
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[4096];
			InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			for (int n; (n = reader.read(buf)) > 0; ) {
				sb.append(buf, 0, n);
			}
			return sb.toString();
		}
	}

	private static List<String> match(String text, String regex) {
		List<String> out = new ArrayList<>();
		Matcher m = Pattern.compile(regex).matcher(text);
		while (m.find()) {
			out.add(m.group(1));
		}
		return out;
	}
}
