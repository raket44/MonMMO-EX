package monmmo;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Post-load fixups applied to the client's live species registry.
 *
 * Ships inside the classpath overlay and is invoked by a one-instruction bytecode hook at the end
 * of the data.pak load, once both the ROM loader and every data.pak section have run. Working on
 * the live objects is the whole point: replacing a species record wholesale discarded everything
 * the ROM loader had attached - Jigglypuff lost its evolution the moment a fresh record retyped it.
 *
 * Everything is reached by reflection against the obfuscated names, verified by the build against
 * the installed client. Pure Java 17 with no dependencies: nothing else exists on the client's
 * classpath, and its bundled runtime rejects newer class files.
 *
 * Instructions come from a jar resource written by the build:
 *
 * <pre>
 * retype:&lt;wireId&gt;:&lt;type1&gt;:&lt;type2&gt;
 * movetype:&lt;moveId&gt;:&lt;typeOrdinal&gt;
 * moveanim:&lt;newMoveId&gt;:&lt;donorMoveId&gt;
 * evo:&lt;fromWire&gt;:&lt;methodId&gt;:&lt;param&gt;:&lt;toWire&gt;
 * dump:&lt;wireId&gt;
 * </pre>
 *
 * Evolutions replicate the ROM loader exactly: the method byte goes through the client's own
 * value-to-enum map, item-based methods get the same +5000 param shift, the entry lands in the
 * pre-evolution's list, and the target's pre-evolution pointer is set for the chain walk.
 * {@code dump} writes a species' live evolution entries and the enum's value map to the diagnostic
 * log - the ground truth that calibrates the build's method table.
 */
public final class DexPatch {
  private DexPatch() {}

  /** The loader hook can fire again on a resync; evolutions must not be appended twice. */
  private static boolean applied;

  /** movevfx/moveanim lines, cached for re-application on every registry rebuild. */
  private static java.util.List<String> animLines;

  /**
   * Called from the patched tail of the animation registry's builder (m7): the battle scene
   * constructs a FRESH registry per battle, which discarded every runtime entry - this re-applies
   * the movevfx and moveanim fixups onto the instance being built. Runs during class
   * initialization too, so it must stay self-contained.
   */
  public static void animRegistryRebuilt(Object registry) {
    try {
      if (animLines == null) {
        java.util.List<String> collected = new ArrayList<>();
        InputStream stream = DexPatch.class.getResourceAsStream("/monmmo/fixups.txt");
        if (stream == null) {
          return;
        }
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("movevfx:") || line.startsWith("moveanim:")) {
              collected.add(line);
            }
          }
        }
        animLines = collected;
      }
      Object animMap = registry.getClass().getField("Kp1").get(registry);
      Method animGet = animMap.getClass().getMethod("gi0", short.class);
      Method animPut = animMap.getClass().getMethod("nuL", short.class, Object.class);
      java.lang.reflect.Constructor<?> vfxCtor =
          Class.forName("monmmo.VfxAnim")
              .getConstructor(Class.forName("f.QL1"), short.class);
      int vfx = 0;
      int aliases = 0;
      for (String line : animLines) {
        String[] parts = line.split(":");
        if (parts[0].equals("movevfx")) {
          final short moveId = Short.parseShort(parts[1]);
          final java.lang.reflect.Constructor<?> ctor = vfxCtor;
          java.util.function.Function<Object, Object> factory =
              attacker -> {
                try {
                  return ctor.newInstance(attacker, moveId);
                } catch (Exception failed) {
                  throw new RuntimeException(failed);
                }
              };
          animPut.invoke(animMap, moveId, factory);
          vfx++;
        } else {
          Object donor = animGet.invoke(animMap, Short.parseShort(parts[2]));
          if (donor != null) {
            animPut.invoke(animMap, Short.parseShort(parts[1]), donor);
            aliases++;
          }
        }
      }
      log("[monmmo] anim registry rebuilt: vfx=" + vfx + " aliases=" + aliases);
    } catch (Throwable failure) {
      log("[monmmo] anim registry re-apply failed: " + failure);
    }
  }

  /** Moves some existing tool already teaches, built once from the live registry. */
  private static java.util.Set<Short> toolTeaches;

  /** A genuine TM item to copy field-for-field, found once by its machine flag and name. */
  private static Object tmDonor;

  /** Never let a fixup failure take the client down; the log line is the diagnostic. */
  public static void apply() {
    if (applied) {
      return;
    }
    applied = true;
    try {
      run();
    } catch (Throwable error) {
      log("[monmmo] dex fixups failed: " + error);
    }
  }

  private static void run() throws Exception {
    InputStream stream = DexPatch.class.getResourceAsStream("/monmmo/fixups.txt");
    if (stream == null) {
      return;
    }

    Class<?> registryClass = Class.forName("f.Fq1");
    Object registry = registryClass.getMethod("NuL").invoke(null);
    @SuppressWarnings("unchecked")
    Map<Short, Object> species = (Map<Short, Object>) registryClass.getField("aX1").get(registry);

    Class<?> speciesClass = Class.forName("f.zK0");
    Field primaryType = speciesClass.getField("mx");
    Field secondaryType = speciesClass.getField("dz0");
    Field evolutionList = speciesClass.getField("sv0");
    Field preEvolution = speciesClass.getField("Yt1");
    Method typeByOrdinal = Class.forName("f.rK1").getMethod("wo0", byte.class);

    Class<?> methodEnum = Class.forName("f.kx");
    Object methodByValue = methodEnum.getField("w0").get(null);
    Method lookupByValue = methodByValue.getClass().getMethod("t70", byte.class);
    Class<?> evolutionClass = Class.forName("f.KQ1");
    Constructor<?> evolution =
        evolutionClass.getConstructor(methodEnum, int.class, short.class);
    Field evolutionTarget = evolutionClass.getField("Dy");

    // The move registry, for movetype fixups: f.mj.aF1() is the singleton, le0(short) the lookup,
    // and f.sC.fy1 the type the section-4 loader itself assigns (flag 0x008).
    Class<?> moveRegistryClass = Class.forName("f.mj");
    Object moveRegistry = moveRegistryClass.getMethod("aF1").invoke(null);
    Method moveById = moveRegistryClass.getMethod("le0", short.class);
    Field moveTypeField = Class.forName("f.sC").getField("fy1");

    // The battle-animation registry, for moveanim fixups: f.Rj1.aP0 (its clinit builds the
    // per-move factory map Kp1, one lambda per canonical move; ux1 falls back to a generic
    // animation for unknown ids). Aliasing a new move id to a donor's factory gives it that
    // donor's full animation.
    Class<?> animRegistryClass = Class.forName("f.Rj1");
    Object animRegistry = animRegistryClass.getField("aP0").get(null);
    Object animMap = animRegistryClass.getField("Kp1").get(animRegistry);
    Method animGet = animMap.getClass().getMethod("gi0", short.class);
    Method animPut = animMap.getClass().getMethod("nuL", short.class, Object.class);

    // The vfx-playing animation class for movevfx fixups, resolved lazily so a build without it
    // degrades to the donor aliases instead of failing every fixup.
    java.lang.reflect.Constructor<?> vfxCtor = null;
    try {
      vfxCtor =
          Class.forName("monmmo.VfxAnim")
              .getConstructor(Class.forName("f.QL1"), short.class);
    } catch (Throwable missing) {
      log("[monmmo] VfxAnim unavailable: " + missing);
    }

    int retypes = 0;
    int moveVfx = 0;
    int moveAnims = 0;
    int moveTypes = 0;
    int evolutions = 0;
    int tools = 0;
    int evoItems = 0;
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line.trim());
      }
    }

    for (String line : lines) {
      String[] parts = line.split(":");
      switch (parts[0]) {
        case "retype" -> {
          Object target = species.get(Short.parseShort(parts[1]));
          if (target == null) {
            continue;
          }
          primaryType.set(target, typeByOrdinal.invoke(null, Byte.parseByte(parts[2])));
          secondaryType.set(target, typeByOrdinal.invoke(null, Byte.parseByte(parts[3])));
          retypes++;
        }
        case "moveanim" -> {
          Object donor = animGet.invoke(animMap, Short.parseShort(parts[2]));
          if (donor == null) {
            continue;
          }
          animPut.invoke(animMap, Short.parseShort(parts[1]), donor);
          moveAnims++;
        }
        case "movevfx" -> {
          // The move's own shipped particle effect (particle/auto/<id>.vfx), played through the
          // VfxAnim translation of the client's generic timeline. Beats any donor alias.
          if (vfxCtor == null) {
            continue;
          }
          final short vfxMoveId = Short.parseShort(parts[1]);
          final java.lang.reflect.Constructor<?> ctor = vfxCtor;
          java.util.function.Function<Object, Object> factory =
              attacker -> {
                try {
                  return ctor.newInstance(attacker, vfxMoveId);
                } catch (Exception failed) {
                  throw new RuntimeException(failed);
                }
              };
          animPut.invoke(animMap, vfxMoveId, factory);
          moveVfx++;
        }
        case "movetype" -> {
          Object move = moveById.invoke(moveRegistry, Short.parseShort(parts[1]));
          if (move == null) {
            continue;
          }
          moveTypeField.set(move, typeByOrdinal.invoke(null, Byte.parseByte(parts[2])));
          moveTypes++;
        }
        case "evo" -> {
          Object from = species.get(Short.parseShort(parts[1]));
          Object to = species.get(Short.parseShort(parts[4]));
          if (from == null || to == null) {
            continue;
          }
          Object method = lookupByValue.invoke(methodByValue, Byte.parseByte(parts[2]));
          if (method == null) {
            continue;
          }
          int param = Integer.parseInt(parts[3]);
          // The ROM loader shifts item ids into PokeMMO's item space for these method
          // ordinals; injected entries take the identical path.
          int ordinal = ((Enum<?>) method).ordinal();
          if (ordinal == 6 || ordinal == 8 || (ordinal >= 17 && ordinal <= 20)) {
            param += 5000;
          }
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>) evolutionList.get(from);
          list.add(evolution.newInstance(method, param, Short.parseShort(parts[4])));
          preEvolution.set(to, from);
          evolutions++;
        }
        case "tmitem" -> {
          // A TM item for a move no existing tool teaches. The donor is a real TM - found by
          // its machine flag and a digit-bearing name - so kind, pocket and behavior fields are
          // all genuine; only the move, the name and the id are ours. The id space continues
          // above the tools the client ships.
          short moveId = Short.parseShort(parts[1]);
          int nameId = Integer.parseInt(parts[2]);
          short itemId = Short.parseShort(parts[3]);
          Object toolRegistry = Class.forName("f.YY0").getField("Mk1").get(null);
          @SuppressWarnings("unchecked")
          java.util.TreeMap<Object, Object> toolMap =
              (java.util.TreeMap<Object, Object>)
                  toolRegistry.getClass().getField("xy").get(toolRegistry);
          if (toolTeaches == null) {
            toolTeaches = new java.util.HashSet<>();
            for (Object tool : toolMap.values()) {
              short taught = tool.getClass().getField("m30").getShort(tool);
              if (taught > 0) {
                toolTeaches.add(taught);
              }
              if (tmDonor == null) {
                // Measured on the live client: a genuine TM is named like "TM29" and its BP1
                // flag is FALSE - requiring true here is why zero items were created.
                String name = (String) tool.getClass().getMethod("getName").invoke(tool);
                if (name != null && name.matches("TM[0-9]+")) {
                  tmDonor = tool;
                }
              }
            }
          }
          if (toolTeaches.contains(moveId) || tmDonor == null) {
            continue;
          }
          Object made = tmDonor.getClass().getConstructor().newInstance();
          for (Field field : tmDonor.getClass().getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
              field.set(made, field.get(tmDonor));
            }
          }
          made.getClass().getField("m30").setShort(made, moveId);
          made.getClass().getField("fb").setInt(made, nameId);
          // The item must BE its number, not just sit at it: field-copying left every clone
          // claiming the donor's internal id, so anything resolving by number found the donor.
          made.getClass().getField("ky0").setShort(made, itemId);
          // Registered through the client's own fA1, which files the item in BOTH registry maps.
          // A bare put into xy satisfied the dex but not the bag, whose lookups go through MH0
          // and answered with the "client does not support this item" placeholder.
          toolRegistry.getClass().getMethod("fA1", made.getClass()).invoke(toolRegistry, made);
          toolTeaches.add(moveId);
          tools++;
        }
        case "evoitem" -> {
          // An evolution item the client never shipped - Ice Stone, Sachet, the apples and the
          // rest. The donor is named by id: a real evolution stone, so kind, pocket, icon and
          // use-on-a-monster behavior are all genuine; only the name and the identity are ours.
          short itemId = Short.parseShort(parts[1]);
          int nameId = Integer.parseInt(parts[2]);
          short donorId = Short.parseShort(parts[3]);
          Object itemRegistry = Class.forName("f.YY0").getField("Mk1").get(null);
          @SuppressWarnings("unchecked")
          java.util.TreeMap<Object, Object> itemMap =
              (java.util.TreeMap<Object, Object>)
                  itemRegistry.getClass().getField("xy").get(itemRegistry);
          // Evolution item params are shifted +5000 into PokeMMO's custom block, so the stone
          // evolutions actually resolve may live at donorId+5000; take whichever exists.
          Object donor = itemMap.get((short) (donorId + 5000));
          if (donor == null) {
            donor = itemMap.get(donorId);
          }
          if (donor == null || itemMap.containsKey(itemId)) {
            continue;
          }
          Object made = donor.getClass().getConstructor().newInstance();
          for (Field field : donor.getClass().getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
              field.set(made, field.get(donor));
            }
          }
          made.getClass().getField("fb").setInt(made, nameId);
          made.getClass().getField("ky0").setShort(made, itemId);
          if (parts.length > 4) {
            // XV is the description string id - PE1 reads it - so the bag shows the item's own
            // text instead of the donor stone's.
            made.getClass().getField("XV").setInt(made, Integer.parseInt(parts[4]));
          }
          // Through fA1, so the bag's MH0 map knows the item too - see the tmitem case.
          itemRegistry.getClass().getMethod("fA1", made.getClass()).invoke(itemRegistry, made);
          evoItems++;
        }
        case "locations" -> {
          // The dex's Wild Locations tab reads one registry, and the 31914 row format has no
          // season field - so a season IS a whole table. The overlay ships one table per season
          // and this installs the one matching today, using the same weekly rotation the server
          // runs its encounters on, so what the dex promises is what actually spawns.
          String season = currentSeason();
          byte[] table = resource("monmmo/locations-" + season + ".bin");
          if (table == null) {
            log("[monmmo] locations: no table for " + season);
            continue;
          }
          installLocations(table);
          log("[monmmo] locations installed for " + season + " (" + table.length + " bytes)");
        }
        case "dumpitems" -> {
          // Calibration for the full item import: every item the client actually holds, as
          // id;name lines. The set of Expansion items missing from the client is computed by
          // diffing names against this file, not by guessing which generations PokeMMO covers.
          Object itemRegistry = Class.forName("f.YY0").getField("Mk1").get(null);
          java.util.TreeMap<?, ?> itemMap =
              (java.util.TreeMap<?, ?>) itemRegistry.getClass().getField("xy").get(itemRegistry);
          try (FileWriter writer = new FileWriter("item-names.csv", false)) {
            for (java.util.Map.Entry<?, ?> mapping : itemMap.entrySet()) {
              Object item = mapping.getValue();
              String name = (String) item.getClass().getMethod("getName").invoke(item);
              writer.write(mapping.getKey() + ";" + (name == null ? "" : name) + "\n");
            }
          } catch (Exception error) {
            log("[monmmo] dumpitems failed: " + error);
          }
        }
        case "dumpspecies" -> {
          // What the client ACTUALLY holds for a species after every section and fixup has run:
          // the dex renders from these fields, so an empty one here is the missing line on
          // screen. Types are mx/dz0, egg groups Cq1/ww (their iI0 byte names the group string
          // at 181000+id), the category line comes from qH1() and the name from DZ(true).
          Object target = species.get(Short.parseShort(parts[1]));
          if (target == null) {
            log("[monmmo] dumpspecies " + parts[1] + " NOT REGISTERED");
            continue;
          }
          StringBuilder out = new StringBuilder("[monmmo] species " + parts[1]);
          try {
            out.append(" name=").append(speciesClass.getMethod("DZ", boolean.class).invoke(target, true));
            out.append(" category=").append(speciesClass.getMethod("qH1").invoke(target));
            out.append(" type1=").append(primaryType.get(target));
            out.append(" type2=").append(secondaryType.get(target));
            Object egg1 = speciesClass.getField("Cq1").get(target);
            Object egg2 = speciesClass.getField("ww").get(target);
            out.append(" egg1=").append(egg1 == null ? "null" : egg1.toString());
            out.append(" egg2=").append(egg2 == null ? "null" : egg2.toString());
            out.append(" forms=").append(speciesClass.getField("pq").getByte(target));
            Object moves = speciesClass.getField("Bg").get(target);
            out.append(" levelupList=").append(moves == null ? "null" : ((List<?>) moves).size());
          } catch (Throwable probeError) {
            out.append(" PROBE FAILED: ").append(probeError);
          }
          log(out.toString());
        }
        case "dumptools" -> {
          Object toolRegistry = Class.forName("f.YY0").getField("Mk1").get(null);
          java.util.TreeMap<?, ?> toolMap =
              (java.util.TreeMap<?, ?>) toolRegistry.getClass().getField("xy").get(toolRegistry);
          StringBuilder text = new StringBuilder("[monmmo] tools=" + toolMap.size());
          for (Object tool : toolMap.values()) {
            short taught = tool.getClass().getField("m30").getShort(tool);
            if (taught == 94 || taught == 85) {
              text.append(" [move=").append(taught)
                  .append(" name=").append(tool.getClass().getMethod("getName").invoke(tool))
                  .append(" fb=").append(tool.getClass().getField("fb").getInt(tool))
                  .append(" BP1=").append(tool.getClass().getMethod("BP1").invoke(tool))
                  .append(" ky0=").append(tool.getClass().getField("ky0").getShort(tool))
                  .append(" y70=").append(tool.getClass().getField("y70").getByte(tool))
                  .append("]");
            }
          }
          if (!toolMap.isEmpty()) {
            Object k = toolMap.lastKey();
            Object v = toolMap.lastEntry().getValue();
            text.append(" keyClass=").append(k.getClass().getSimpleName())
                .append(" lastKey=").append(k)
                .append(" lastMove=").append(v.getClass().getField("m30").getShort(v))
                .append(" lastName=").append(v.getClass().getField("fb").getInt(v));
          }
          log(text.toString());
          // The full table as a file, so the build's RetailTools calibration can be verified
          // against the live client instead of trusted: itemId;fb;moveId;name per tool.
          try (java.io.PrintWriter out =
              new java.io.PrintWriter("monmmo-tools.csv", StandardCharsets.UTF_8)) {
            for (Object tool : toolMap.values()) {
              out.println(
                  tool.getClass().getField("ky0").getShort(tool)
                      + ";" + tool.getClass().getField("fb").getInt(tool)
                      + ";" + tool.getClass().getField("m30").getShort(tool)
                      + ";" + tool.getClass().getMethod("getName").invoke(tool));
            }
          } catch (Exception writeError) {
            log("[monmmo] tool dump file failed: " + writeError);
          }
        }
                case "dump" -> {
          short id = Short.parseShort(parts[1]);
          Object target = species.get(id);
          if (target == null) {
            continue;
          }
          StringBuilder text = new StringBuilder("[monmmo] dump id=" + id + " evos=");
          for (Object entry : (List<?>) evolutionList.get(target)) {
            Object method = null;
            for (Field field : evolutionClass.getFields()) {
              if (field.getType() == methodEnum) {
                method = field.get(entry);
              }
            }
            text.append("(")
                .append(method == null ? "?" : ((Enum<?>) method).name())
                .append("#")
                .append(method == null ? -1 : ((Enum<?>) method).ordinal())
                .append("->")
                .append(evolutionTarget.get(entry))
                .append(")");
          }
          text.append(" valueMap=");
          for (byte value = 1; value <= 26; value++) {
            Object method = lookupByValue.invoke(methodByValue, value);
            if (method != null) {
              text.append(value).append("=").append(((Enum<?>) method).ordinal()).append(",");
            }
          }
          log(text.toString());
        }
        default -> {}
      }
    }
    log("[monmmo] dex fixups applied: retypes=" + retypes + " moveTypes=" + moveTypes + " moveVfx=" + moveVfx + " moveAnims=" + moveAnims + " evolutions=" + evolutions + " tools=" + tools + " evoItems=" + evoItems);
    if (tools > 0) {
      // Read back what was just created, through the same accessors the dex uses: how many
      // tools now teach imported moves, and what one of them says its name is. A broken name
      // lookup or a registry mismatch shows up here instead of as another blank launch.
      try {
        Object toolRegistry = Class.forName("f.YY0").getField("Mk1").get(null);
        java.util.TreeMap<?, ?> toolMap =
            (java.util.TreeMap<?, ?>) toolRegistry.getClass().getField("xy").get(toolRegistry);
        int high = 0;
        String sample = null;
        for (Object tool : toolMap.values()) {
          if (tool.getClass().getField("m30").getShort(tool) > 559) {
            high++;
            if (sample == null) {
              sample = (String) tool.getClass().getMethod("getName").invoke(tool);
            }
          }
        }
        Object iceStone = toolMap.get((short) 21000);
        String iceStoneName =
            iceStone == null
                ? "MISSING"
                : (String) iceStone.getClass().getMethod("getName").invoke(iceStone);
        // The use-item packet carries a u16 that is not ky0 - a real click sent 0x4A08 for the
        // Ice Stone. Logging the candidate id fields of the donor stone and our clone shows
        // which field that number lives in.
        for (short probeId : new short[] {5084, 5017, 21000}) {
          Object probe = toolMap.get(probeId);
          if (probe == null) {
            continue;
          }
          StringBuilder fields = new StringBuilder("[monmmo] itemfields id=" + probeId);
          for (String name : new String[] {"Gv", "CT1", "lO0", "lW0", "os", "Pd1", "cg", "cr1", "b7"}) {
            try {
              fields.append(" ").append(name).append("=")
                  .append(probe.getClass().getField(name).get(probe));
            } catch (Throwable ignored) {
            }
          }
          log(fields.toString());
        }
        log(
            "[monmmo] verify: highMoveTools="
                + high
                + " sampleName="
                + sample
                + " item21000="
                + iceStoneName);
      } catch (Throwable error) {
        log("[monmmo] verify failed: " + error);
      }
    }
  }

  /** The season the world is in: ISO week modulo four, matching the server's WorldClock. */
  private static String currentSeason() {
    int week =
        java.time.LocalDate.now()
            .get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    return switch (week % 4) {
      case 0 -> "spring";
      case 1 -> "summer";
      case 2 -> "autumn";
      default -> "winter";
    };
  }

  private static byte[] resource(String name) {
    try (java.io.InputStream stream =
        DexPatch.class.getClassLoader().getResourceAsStream(name)) {
      return stream == null ? null : stream.readAllBytes();
    } catch (Exception error) {
      return null;
    }
  }

  /**
   * Replaces the client's wild-location registry with a season's table.
   *
   * The payload is the same shape as data.pak section 5, so one builder serves both: `u16
   * speciesCount`, then per species `u16 id, u16 rowCount` and 12-byte rows. Rows become f/Kk1
   * objects through the constructor the ROM loader itself uses, filed under f/Fa1 entries in
   * f/X21's map - the exact structures the dex screen already renders.
   */
  private static void installLocations(byte[] table) throws Exception {
    Object registry = Class.forName("f.X21").getField("ap1").get(null);
    Object map = registry.getClass().getField("vf").get(registry);
    Class<?> zoneClass = Class.forName("f.Fa1");
    Class<?> rowClass = Class.forName("f.Kk1");
    Class<?> typeClass = Class.forName("f.bL0");
    Object typeLookup = typeClass.getField("T90").get(null);
    java.lang.reflect.Method byOrdinal = typeLookup.getClass().getMethod("t70", byte.class);
    java.lang.reflect.Constructor<?> zoneCtor = zoneClass.getConstructor(short.class);
    java.lang.reflect.Constructor<?> rowCtor =
        rowClass.getConstructor(
            byte.class, byte.class, typeClass, byte.class, byte.class, byte.class, short.class,
            byte.class, byte.class);
    java.lang.reflect.Field rowList = zoneClass.getField("bN");
    java.lang.reflect.Method put = map.getClass().getMethod("nuL", short.class, Object.class);

    java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(table);
    buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    int speciesCount = buffer.getShort() & 0xFFFF;
    int installed = 0;
    for (int i = 0; i < speciesCount; i++) {
      short speciesId = buffer.getShort();
      int rows = buffer.getShort() & 0xFFFF;
      Object zone = zoneCtor.newInstance(speciesId);
      @SuppressWarnings("unchecked")
      java.util.List<Object> list = (java.util.List<Object>) rowList.get(zone);
      for (int r = 0; r < rows; r++) {
        byte region = buffer.get();
        buffer.getShort(); // consumed and discarded by the client's own loader
        byte locationIndex = buffer.get();
        byte type = buffer.get();
        byte variant = buffer.get();
        byte timeMask = buffer.get();
        byte regionFilter = buffer.get();
        short rarityMask = buffer.getShort();
        byte minLevel = buffer.get();
        byte maxLevel = buffer.get();
        list.add(
            rowCtor.newInstance(
                region,
                locationIndex,
                byOrdinal.invoke(typeLookup, type),
                variant,
                timeMask,
                regionFilter,
                rarityMask,
                minLevel,
                maxLevel));
      }
      put.invoke(map, speciesId, zone);
      installed++;
    }
    log("[monmmo] locations: " + installed + " species tables installed");
  }

  private static void log(String message) {
    try (FileWriter writer = new FileWriter("dex-diagnostic.log", true)) {
      writer.write(message + System.lineSeparator());
    } catch (Exception ignored) {
      // The log is best-effort; the fixups themselves must never depend on it.
    }
  }
}
