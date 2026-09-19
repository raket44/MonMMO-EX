import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * One page with every imported species exactly as the mod ships it: front/back, normal/shiny,
 * the party icon pair, and the source each side came from - so the owner previews in one place
 * instead of chasing packs. Reads the staged mod zip and the staging reports; writes
 * build/sprite-preview/index.html next to the extracted GIFs.
 *
 *   java SpritePreview.java <mod.zip> <sprite-pack.csv> <strings_en.xml> <outDir>
 */
public class SpritePreview {
    public static void main(String[] a) throws Exception {
        Path zip = Path.of(a[0]), report = Path.of(a[1]), strings = Path.of(a[2]), out = Path.of(a[3]);
        Files.createDirectories(out);
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                if (!e.getName().startsWith("sprites/battlesprites/") && !e.getName().startsWith("sprites/monstericons/")) continue;
                Path target = out.resolve(e.getName());
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Map<Integer, String> names = new HashMap<>();
        Matcher m = Pattern.compile("<string id=\"15(\\d{4})\">([^<]*)").matcher(Files.readString(strings, StandardCharsets.UTF_8));
        while (m.find()) names.put(Integer.parseInt(m.group(1)), m.group(2));

        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(report)) {
            if (line.startsWith("symbol,")) continue;
            rows.add(line.split(","));
        }
        rows.sort(Comparator.comparingInt(r -> Integer.parseInt(r[1])));

        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><meta charset=utf-8><title>MonMMO sprite preview</title><style>")
         .append("body{background:#14161a;color:#ddd;font:14px sans-serif;margin:16px}")
         .append("table{border-collapse:collapse}td,th{padding:4px 8px;border-bottom:1px solid #333;text-align:center;vertical-align:middle}")
         .append("img{image-rendering:pixelated}.gif{width:96px;height:96px}.icon{width:32px;height:32px}")
         .append(".src{font-size:11px;color:#8ac}.expansion{color:#e88}.showdown{color:#cc8}.pack{color:#8c8}")
         .append(".showdown-ani,.showdown-ani-recoloured,.ebs-bw-animated{color:#8cf}.monmmo-custom{color:#f8c}")
         .append("#f{position:sticky;top:0;background:#14161a;padding:8px 0}</style>")
         .append("<div id=f><b>").append(rows.size()).append(" imported species</b> &nbsp; filter: ")
         .append("<select onchange=\"flt(this.value)\"><option value=''>all</option><option value='showdown-ani'>animated (gen5ani)</option>")
         .append("<option value='showdown'>still (Showdown)</option><option value='pack'>still (your packs)</option><option value='expansion'>Expansion art</option></select>")
         .append(" &nbsp; <input placeholder='name' oninput=\"nm(this.value)\"></div>")
         .append("<table><tr><th>wire</th><th>name</th><th>front</th><th>front shiny</th><th>back</th><th>back shiny</th><th>icons</th><th>front source</th><th>back source</th></tr>");
        for (String[] r : rows) {
            int wire = Integer.parseInt(r[1]);
            String name = names.getOrDefault(wire, r[0].replace("SPECIES_", ""));
            String base = "sprites/battlesprites/" + wire;
            h.append("<tr data-src='").append(r[2]).append("' data-name='").append(name.toLowerCase()).append("'>")
             .append("<td>").append(wire).append("</td><td>").append(name).append("<br><small>").append(r[0]).append("</small></td>");
            for (String side : new String[] {"front-n", "front-s", "back-n", "back-s"})
                h.append("<td><img class=gif src='").append(base).append("-").append(side).append(".gif'></td>");
            h.append("<td>");
            for (String ic : new String[] {"-0", "-0-s"})
                h.append("<img class=icon src='sprites/monstericons/").append(wire).append(ic).append(".png'>");
            h.append("</td><td class='src ").append(r[2]).append("'>").append(r[2]).append("</td><td class='src ").append(r[4]).append("'>").append(r[4]).append("</td></tr>");
        }
        h.append("</table><script>")
         .append("let s='',n='';function apply(){for(const tr of document.querySelectorAll('tr[data-src]')){")
         .append("tr.style.display=((s===''||tr.dataset.src===s)&&tr.dataset.name.includes(n))?'':'none'}}")
         .append("function flt(v){s=v;apply()}function nm(v){n=v.toLowerCase();apply()}</script>");
        Files.writeString(out.resolve("index.html"), h.toString(), StandardCharsets.UTF_8);
        System.out.println("wrote " + out.resolve("index.html") + " (" + rows.size() + " species)");
    }
}
