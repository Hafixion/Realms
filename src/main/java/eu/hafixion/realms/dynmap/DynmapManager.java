package eu.hafixion.realms.dynmap;

import eu.hafixion.realms.towny.block.ClaimChunk;
import eu.hafixion.realms.towny.nations.Nation;
import eu.hafixion.realms.towny.nations.NationDatabase;
import eu.hafixion.realms.towny.towns.Town;
import eu.hafixion.realms.towny.towns.TownDatabase;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.dynmap.markers.*;

import java.util.*;

public class DynmapManager {
    AreaStyle defstyle;
    Map<String, AreaStyle> cusstyle;
    MarkerSet set;
    static MarkerAPI api;

    enum direction {XPLUS, ZPLUS, XMINUS, ZMINUS}

    public DynmapManager(MarkerSet set, MarkerAPI api) {
        this.set = set;
        defstyle = new AreaStyle();
        cusstyle = new HashMap<>();
        this.api = api;
    }

    int blocksize = 16;
    private Map<String, AreaMarker> resareas = new HashMap<>();
    private Map<String, Marker> resmark = new HashMap<>();

    private static class AreaStyle {
        String strokecolor = "#FF0000";
        double strokeopacity = 0.8;
        int strokeweight = 3;
        String fillcolor = "#FF0000";
        double fillopacity = 0.35;
        boolean boost = false;
    }

    void addStyle(String resid, AreaMarker m) {
        AreaStyle as = cusstyle.get(resid);
        if (as == null) {
            as = defstyle;
        }
        int sc = 0xFF0000;
        int fc = 0xFF0000;
        try {
            sc = Integer.parseInt(as.strokecolor.substring(1), 16);
            fc = Integer.parseInt(as.fillcolor.substring(1), 16);
        } catch (NumberFormatException ignored) {
        }
        m.setLineStyle(as.strokeweight, as.strokeopacity, sc);
        m.setFillStyle(as.fillopacity, fc);
        m.setBoostFlag(as.boost);
    }

    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    int floodFillTarget(TileFlags src, TileFlags dest, int x, int y) {
        int cnt = 0;
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if (src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                cnt++;
                if (src.getFlag(x + 1, y))
                    stack.push(new int[]{x + 1, y});
                if (src.getFlag(x - 1, y))
                    stack.push(new int[]{x - 1, y});
                if (src.getFlag(x, y + 1))
                    stack.push(new int[]{x, y + 1});
                if (src.getFlag(x, y - 1))
                    stack.push(new int[]{x, y - 1});
            }
        }
        return cnt;
    }

    /* Handle specific faction on specific world */
    public void handleTownOnWorld(String factname, Town town, String world, LinkedList<ClaimChunk> blocks, Map<String, AreaMarker> newmap, Map<String, Marker> newmark) {
        double[] x;
        double[] z;
        int poly_index = 0; /* Index of polygon for given faction */

        /* Build popup */
        String desc = town.getDynmapMenu();

        /* Handle areas */
        if (blocks.isEmpty())
            return;
        LinkedList<ClaimChunk> nodevals = new LinkedList<>();
        TileFlags curblks = new TileFlags();
        /* Loop through blocks: set flags on blockmaps */
        for (ClaimChunk b : blocks) {
            curblks.setFlag(b.getChunkPos().getX(), b.getChunkPos().getZ(), true); /* Set flag for block */
            nodevals.addLast(b);
        }
        /* Loop through until we don't find more areas */
        while (nodevals != null) {
            LinkedList<ClaimChunk> ournodes = null;
            LinkedList<ClaimChunk> newlist = null;
            TileFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for (ClaimChunk node : nodevals) {
                int nodex = node.getChunkPos().getX();
                int nodez = node.getChunkPos().getZ();
                /* If we need to start shape, and this block is not part of one yet */
                if ((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new TileFlags();  /* Create map for shape */
                    ournodes = new LinkedList<>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex;
                    minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if ((ourblks != null) && ourblks.getFlag(nodex, nodez)) {
                    ournodes.add(node);
                    if (nodex < minx) {
                        minx = nodex;
                        minz = nodez;
                    } else if ((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                } else {  /* Else, keep it in the list for the next polygon */
                    if (newlist == null) newlist = new LinkedList<>();
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if (ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<>();
                linelist.add(new int[]{minx, minz}); // Add start point
                while ((cur_x != minx) || (cur_z != minz) || (dir != direction.ZMINUS)) {
                    switch (dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if (!ourblks.getFlag(cur_x + 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z - 1)) {  /* Straight? */
                                cur_x++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++;
                                cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if (!ourblks.getFlag(cur_x, cur_z + 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z + 1)) {  /* Straight? */
                                cur_z++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++;
                                cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if (!ourblks.getFlag(cur_x - 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z + 1)) {  /* Straight? */
                                cur_x--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--;
                                cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if (!ourblks.getFlag(cur_x, cur_z - 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z - 1)) {  /* Straight? */
                                cur_z--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--;
                                cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = factname + "__" + world + "__" + poly_index;
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for (int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double) line[0] * (double) blocksize;
                    z[i] = (double) line[1] * (double) blocksize;
                }
                /* Find existing one */
                AreaMarker m = resareas.remove(polyid); /* Existing area? */
                if (m == null) {
                    m = set.createAreaMarker(polyid, factname, false, world, x, z, false);
                    m.setRangeY(40, 40);
                    if (m == null) {
                        System.out.println("error adding area marker " + polyid);
                        return;
                    }
                } else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(factname);   /* Update label */
                }
                m.setDescription(desc); /* Set popup */

                /* Set line and fill properties */
                addStyle(factname, m);

                if (town.getNation() != null) {
                    m.setFillStyle(0.30, Integer.parseInt(town.getNation().getColor().substring(1), 16));
                    m.setLineStyle(2, 1, Integer.parseInt(town.getNation().getColor().substring(1), 16));
                }

                /* Add to map */
                newmap.put(polyid, m);
                poly_index++;
            }
        }

        // create Home Marker
        Block homeblock = town.getHomeBlock();
        MarkerIcon icon = town.getNation() != null ? api.getMarkerIcon("blueflag") : api.getMarkerIcon("redflag");
        Marker homeMarker = set.createMarker("home." + town.getName(), town.getName(), "world", homeblock.getX(), homeblock.getY(), homeblock.getZ(), icon, false);

        if (town.getStockpileChest() != null) {
            Location stockpile = town.getStockpileChest();
            Marker stockpileMarker = set.createMarker("stockpile." + town.getName(), "Stockpile", "world", stockpile.getX(), stockpile.getY(), stockpile.getZ(), api.getMarkerIcon("chest"), false);
        }

    }

    public void handleNationOnWorld(String factname, Nation nation, String world, LinkedList<ClaimChunk> blocks, Map<String, AreaMarker> newmap, Map<String, Marker> newmark) {
        double[] x;
        double[] z;
        int poly_index = 0; /* Index of polygon for given faction */

        /* Build popup */
        String desc = nation.getDynmapLabel();

        /* Handle areas */
        if(blocks.isEmpty())
            return;
        LinkedList<ClaimChunk> nodevals = new LinkedList<>();
        TileFlags curblks = new TileFlags();
        /* Loop through blocks: set flags on blockmaps */
        for(ClaimChunk b : blocks) {
            curblks.setFlag(b.getChunkPos().getX(), b.getChunkPos().getZ(), true); /* Set flag for block */
            nodevals.addLast(b);
        }
        /* Loop through until we don't find more areas */
        while (nodevals != null) {
            LinkedList<ClaimChunk> ournodes = null;
            LinkedList<ClaimChunk> newlist = null;
            TileFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(ClaimChunk node : nodevals) {
                int nodex = node.getChunkPos().getX();
                int nodez = node.getChunkPos().getZ();
                /* If we need to start shape, and this block is not part of one yet */
                if((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new TileFlags();  /* Create map for shape */
                    ournodes = new LinkedList<>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourblks != null) && ourblks.getFlag(nodex, nodez)) {
                    ournodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newlist == null) newlist = new LinkedList<>();
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if(ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<>();
                linelist.add(new int[] {minx, minz} ); // Add start point
                while((cur_x != minx) || (cur_z != minz) || (dir != direction.ZMINUS)) {
                    switch(dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if(!ourblks.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++; cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if(!ourblks.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++; cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if(!ourblks.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--; cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if(!ourblks.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = factname + "__" + world + "__" + poly_index;
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double) blocksize;
                    z[i] = (double)line[1] * (double) blocksize;
                }
                /* Find existing one */
                AreaMarker m = resareas.remove(polyid); /* Existing area? */
                if(m == null) {
                    m = set.createAreaMarker(polyid, factname, false, world, x, z, false);
                    m.setRangeY(40, 40);
                    if(m == null) {
                        System.out.println("error adding area marker " + polyid);
                        return;
                    }
                }
                else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(factname);   /* Update label */
                }
                m.setDescription(desc); /* Set popup */

                /* Set line and fill properties */
                addStyle(factname, m);
                m.setFillStyle(0.25, Integer.parseInt(nation.getColor().substring(1), 16));
                m.setLineStyle(2, 1, Integer.parseInt(nation.getColor().substring(1), 16));

                /* Add to map */
                newmap.put(polyid, m);
                poly_index++;
            }
        }
    }

    /* Update Factions information */
    public void updateTowns() {
        Map<String,AreaMarker> newmap = new HashMap<>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<>(); /* Build new map */

        /* Loop through factions */
        for(Town fact : TownDatabase.INSTANCE.getTowns()) {
            String factname = fact.getName();
            String fid = fact.getName() + "_" + fact.getUuid().toString();

            Set<ClaimChunk> claims = fact.getClaims().keySet();
            LinkedList<ClaimChunk> list = new LinkedList<>(claims);
            handleTownOnWorld(factname, fact, "world", list, newmap, newmark);
        }

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : resareas.values()) {
            oldm.deleteMarker();
        }
        for(Marker oldm : resmark.values()) {
            oldm.deleteMarker();
        }

        /* And replace with new map */
        resareas = newmap;
        resmark = newmark;

    }


    public void updateNations() {
        Map<String,AreaMarker> newmap = new HashMap<>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<>(); /* Build new map */

        /* Loop through factions */
        for(Nation fact : NationDatabase.INSTANCE.getNations()) {
            String factname = fact.getName();
            String fid = fact.getName() + "_" + fact.getUuid().toString();

            LinkedList<ClaimChunk> list = new LinkedList<>(fact.getAllClaims());
            handleNationOnWorld(factname, fact, "world", list, newmap, newmark);
        }
        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : resareas.values()) {
            oldm.deleteMarker();
        }
        for(Marker oldm : resmark.values()) {
            oldm.deleteMarker();
        }

        /* And replace with new map */
        resareas = newmap;
        resmark = newmark;

    }
}
