package io.hops.kmon.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class RrdtoolCommand {

    private static final Logger logger = Logger.getLogger(RrdtoolCommand.class.getName());
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 150;
    private static final int DEFAULT_LOWERLIMIT = 0;
    private static final boolean SHOW_DETAILS = false;
    private List<String> cmds;
    private List<String> graphCommands;
    private String hostId;
    private String plugin;
    private String pluginInstance;
    private int start;
    private int end;
    private int width;
    private int height;
    private int lowerLimit;
    private String watermark;
    private String title;
    private String verticalLabel;
    private String filePath;
    private String fileExtension;

    public enum ChartType {

        LINE, AREA, AREA_STACK, LINES, SUM_LINE, AVG_LINE
    }

    public RrdtoolCommand(String filePath, String fileExtension, String hostId, String plugin, String pluginInstance,
            int start, int end) {
        this.filePath = filePath;
        this.fileExtension = fileExtension;
        this.hostId = hostId;
        this.plugin = plugin;
        this.pluginInstance = pluginInstance;
        this.start = start;
        this.end = end;
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
        this.lowerLimit = DEFAULT_LOWERLIMIT;
        cmds = new ArrayList<String>();
        graphCommands = new ArrayList<String>();
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public void setPlugin(String plugin, String pluginInstance) {
        this.plugin = plugin;
        this.pluginInstance = pluginInstance;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVerticalLabel(String verticalLabel) {
        this.verticalLabel = verticalLabel;
    }

    public void setLowerLimit(int lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public void setGraphSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public List<String> getCommands() {
        cmds = new ArrayList<String>();
        cmds.add("rrdtool");
        cmds.add("graph");
        cmds.add("");
        cmds.add("--slope-mode");
        cmds.add("--imgformat=PNG");
        cmds.add("--start=" + start);
        cmds.add("--end=" + end);
        cmds.add("--rigid");
        cmds.add("--height=" + height);
        cmds.add("--width=" + width);
        cmds.add("--lower-limit=" + lowerLimit);
        if (title != null) {
            cmds.add("--title=" + title);
        }
        if (verticalLabel != null) {
            cmds.add("--vertical-label=" + verticalLabel);
        }
        if (watermark != null) {
            cmds.add("--watermark=" + watermark);
        }
        cmds.add("TEXTALIGN:left");
        cmds.addAll(graphCommands);
        return cmds;
    }

    public void drawSumLine(List<String> hosts, String type, String typeInstance, String ds, String label, String color, String detailsFormat) {
        List<String> vars = new ArrayList<String>();
        String cmd;
        for (String h : hosts) {
            String var = h.replace(".", "-") + "-" + type + "-" + typeInstance + ds;
            String rrdFile = getRrdFileName(h, plugin, pluginInstance, type, typeInstance);
            graphCommands.add("DEF:" + var + "=" + rrdFile + ":" + ds + ":AVERAGE");
            vars.add(var);
        }
        String sumVar = "sum" + type + "-" + typeInstance + ds;
        cmd = "CDEF:" + sumVar + "=" + vars.get(0);
        for (String v : vars.subList(1, vars.size())) {
            cmd += "," + v + ",+";
        }
        graphCommands.add(cmd);
        graphCommands.add("LINE1:" + sumVar + "#" + color + ":" + label);
    }
    
    public void drawAverageLine(List<String> hosts, String type, String typeInstance, String ds, String label, String color, String detailsFormat) {
        List<String> vars = new ArrayList<String>();
        String cmd;
        for (String h : hosts) {
            String var = h.replace(".", "-") + "-" + type + "-" + typeInstance + ds;
            String rrdFile = getRrdFileName(h, plugin, pluginInstance, type, typeInstance);
            graphCommands.add("DEF:" + var + "=" + rrdFile + ":" + ds + ":AVERAGE");
            vars.add(var);
        }
        String sumVar = "sum" + type + "-" + typeInstance + ds;
        cmd = "CDEF:" + sumVar + "=" + vars.get(0);
        for (String v : vars.subList(1, vars.size())) {
            cmd += "," + v + ",+";
        }
        cmd += "," + hosts.size() + ",/"; // Average
        graphCommands.add(cmd);
        graphCommands.add("LINE1:" + sumVar + "#" + color + ":" + label);
    }    

    public void drawLine(String type, String typeInstance, String ds, String label, String color, String detailsFormat) {
        addGraph(ChartType.LINE, type, typeInstance, ds, label, color, detailsFormat);
    }

    public void drawArea(String type, String typeInstance, String ds, String label, String color, String detailsFormat) {
        addGraph(ChartType.AREA, type, typeInstance, ds, label, color, detailsFormat);
    }

    public void stackArea(String type, String typeInstance, String ds, String label, String color, String detailsFormat) {
        addGraph(ChartType.AREA_STACK, type, typeInstance, ds, label, color, detailsFormat);
    }

    private void addGraph(ChartType chartType, String type, String typeInstance, String ds, String label, String color,
            String detailsFormat) {
        String var = type + "-" + typeInstance + ds;
        String rrdFile = getRrdFileName(hostId, plugin, pluginInstance, type, typeInstance);
        Color c = Color.decode("0x" + color).brighter().brighter();
        String brightColor = toHex(c);
        graphCommands.add("DEF:" + var + "=" + rrdFile + ":" + ds + ":AVERAGE");
        switch (chartType) {
            case LINE:
                graphCommands.add("LINE1:" + var + "#" + color + ":" + label);
                break;
            case AREA:
                graphCommands.add("AREA:" + var + "#" + brightColor + ":" + label);
                break;
            case AREA_STACK:
                graphCommands.add("AREA:" + var + "#" + brightColor + ":" + label + ":STACK");
                break;
        }
        addDetails(var, detailsFormat);
    }

    private void addDetails(String var, String detailsFormat) {
        if (SHOW_DETAILS && detailsFormat != null) {
            graphCommands.add("GPRINT:" + var + ":AVERAGE:Avg\\:" + detailsFormat);
            graphCommands.add("GPRINT:" + var + ":MIN:Min\\:" + detailsFormat);
            graphCommands.add("GPRINT:" + var + ":MAX:Max\\:" + detailsFormat + "\\l");
        }
    }

    private String getRrdFileName(String hostId, String plugin, String pluginInstance, String type, String typeInstance) {

        String rrdFile = filePath + hostId + "/" + plugin;
        if (pluginInstance != null && !pluginInstance.equals("")) {
            rrdFile += "-" + pluginInstance;
        }
        rrdFile += "/" + type;
        if (typeInstance != null && !typeInstance.equals("")) {
            rrdFile += "-" + typeInstance;
        }
        rrdFile += fileExtension;
        return rrdFile;
    }

    private String toHex(Color c) {
        String hex = Integer.toHexString(c.getRGB() & 0xffffff);
        if (hex.length() < 6) {
            hex = "0" + hex;
        }
        return hex;
    }
}
