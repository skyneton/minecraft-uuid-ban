package net.mpoisv.ban.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class YamlConfiguration {
    private final HashMap<String, Object> properties;
    public YamlConfiguration() {
        properties = new HashMap<>();
    }

    private YamlConfiguration(String path, HashMap<String, Object> values) {
        properties = values;
    }

    private YamlConfiguration(HashMap<String, Object> values) {
        properties = values;
    }

    private YamlConfiguration(String path) {
        properties = new HashMap<>();
    }

    public void save(String path) throws IOException {
        var file = new File(path);
        file.getParentFile().mkdirs();
        try(var fos = new FileOutputStream(file)) {
            fos.write(toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public Collection<String> keys() {
        var list = new ArrayList<String>();
        keys(list, "");
        return list;
    }

    private void keys(ArrayList<String> list, String parent) {
        for(var kv : properties.entrySet()) {
            if(kv.getValue() instanceof YamlConfiguration) {
                ((YamlConfiguration) kv.getValue()).keys(list, parent + "." + kv.getKey());
            }
            else list.add(parent + "." + kv.getKey());
        }
    }

    public HashMap<String, Object> keyValues() {
        var map = new HashMap<String, Object>();
        keyValues(map, "");
        return map;
    }

    private void keyValues(HashMap<String, Object> map, String parent) {
        for(var kv : properties.entrySet()) {
            if(kv.getValue() instanceof YamlConfiguration) {
                ((YamlConfiguration) kv.getValue()).keyValues(map, parent + "." + kv.getKey());
            }
            else map.put(parent + "." + kv.getKey(), kv.getValue());
        }
    }

    public Integer getInt(String key) {
        var value = get(key);
        return value instanceof Integer ? (Integer) value : null;
    }

    public Long getLong(String key) {
        var value = get(key);
        return value instanceof Long ? (Long) value : null;
    }

    public String getString(String key) {
        var value = get(key);
        return value instanceof String ? (String) value : null;
    }

    public Double getDouble(String key) {
        var value = get(key);
        return value instanceof Double ? (Double) value : null;
    }

    public Boolean getBoolean(String key) {
        var value = get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public Object[] getList(String key) {
        var value = get(key);
        if(value instanceof Collection<?>) return ((Collection<?>) value).toArray();
        return value.getClass().isArray() ? (Object[]) value : null;
    }

    public YamlConfiguration set(String key, Object value) {
        if(key.startsWith(".")) key = key.substring(1);
        var path = key.split("\\.");
        var target = this;
        for(int i = 0, l = path.length - 1; i <= l; i++) {
            if(i == l) {
                if(value == null) {
                    target.properties.remove(path[i]);
                    return this;
                }
                target.properties.put(path[i], value);
                return this;
            }
            var child = target.properties.get(path[i]);
            if(!(child instanceof YamlConfiguration)) {
                child = new YamlConfiguration();
                target.properties.put(path[i], child);
            }
            target = (YamlConfiguration) child;
        }
        return this;
    }

    public Object get(String key) {
        if(key.startsWith(".")) key = key.substring(1);
        var path = key.split("\\.");
        var target = this;
        for(int i = 0, l = path.length - 1; i <= l; i++) {
            if(i == l)
                return target.properties.get(path[i]);
            var child = target.properties.get(path[i]);
            if(!(child instanceof YamlConfiguration)) return null;
            target = (YamlConfiguration) child;
        }
        return null;
    }

    public boolean contains(String key) {
        if(key.startsWith(".")) key = key.substring(1);
        var path = key.split("\\.");
        var target = this;
        for(int i = 0, l = path.length - 1; i <= l; i++) {
            if(i == l)
                return target.properties.get(path[i]) != null;
            var child = target.properties.get(path[i]);
            if(!(child instanceof YamlConfiguration)) return false;
            target = (YamlConfiguration) child;
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringBuilder("", "  ").toString();
    }

    public String toString(String sep) {
        return toStringBuilder("", sep).toString();
    }

    private StringBuilder toStringBuilder(String pad, String sep) {
        var builder = new StringBuilder();
        for (var kv : properties.entrySet()) {
            if(kv.getValue() == null) continue;
            builder.append(pad).append(kv.getKey()).append(": ");
            var value = kv.getValue();
            if(value instanceof YamlConfiguration)
                builder.append('\n').append(((YamlConfiguration) value).toStringBuilder(pad + sep, sep));
            else if(value instanceof Boolean || value instanceof Double
                    || value instanceof Integer || value instanceof Float || value instanceof Long)
                builder.append(value);
            else {
                if(value.getClass().isArray())
                    builder.append(pad).append(sep).append('\n').append(listToString((Collection<?>) value, pad, sep));
                else {
                    var str = value.toString();
                    if(str.contains("#") || str.contains("\\") || containsEscapeSequence(str)) {
                        builder.append('"')
                                .append(removeEscapeSequence(str.replace("\\", "\\\\"))
                                        .replace("\n", "\\n")
                                        .replace("\"", "\"\""))
                                .append('"');
                    }else if(str.contains("\n"))
                        builder.append("|\n").append(pad).append(sep).append(str.replace("\n", "\n" + pad + sep));
                    else builder.append(str);
                }
            }
            builder.append('\n');
        }
        return builder;
    }

    private StringBuilder listToString(Collection<?> values, String pad, String sep) {
        var builder = new StringBuilder();
        var array = values.toArray();
        for(int k = 0, count = array.length - 1; k <= count; k++) {
            var value = array[k];
            builder.append(pad).append("- ");
            if(value instanceof YamlConfiguration)
                builder.append('\n').append(((YamlConfiguration) value).toStringBuilder(pad + sep, sep));
            else if(value instanceof Boolean || value instanceof Double
                    || value instanceof Integer || value instanceof Float || value instanceof Long)
                builder.append(value);
            else {
                if(value.getClass().isArray())
                    builder.append(pad).append(sep).append('\n').append(listToString((Collection<?>) value, pad + sep, sep));
                else {
                    var str = value.toString();
                    if(str.contains("#") || str.contains("\\") || containsEscapeSequence(str)) {
                        builder.append('"')
                                .append(removeEscapeSequence(str.replace("\\", "\\\\"))
                                        .replace("\n", "\\n")
                                        .replace("\"", "\"\""))
                                .append('"');
                    }else if(str.contains("\n"))
                        builder.append("|\n").append(pad).append(sep).append(str.replace("\n", "\n" + pad + sep));
                    else builder.append(str);
                }
            }
            if(k < count) builder.append('\n');
        }
        return builder;
    }

    public static YamlConfiguration loadConfiguration(String path) throws IOException {
        var file = new File(path);
        if(file.exists()) {
            try(var fis = new FileInputStream(file)) {
                return parse(new String(fis.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return new YamlConfiguration();
    }

    public static YamlConfiguration loadFromText(String text) {
        return parse(text);
    }

    private static YamlConfiguration parse(String text) {
        var parseData = new ParseData();
        return parse(text, parseData);
    }

    private static YamlConfiguration parse(String text, ParseData data) {
        var map = new HashMap<String, Object>();
        var info =
    }

    private static String removeEscapeSequence(String str) {
        return str.replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static boolean containsEscapeSequence(String str) {
        return str.contains("\b") || str.contains("\f") || str.contains("\r") || str.contains("\t");
    }
}

class ParseData {
    public int index = 0;
    public int lineIndex = 0;
}
