package net.craftcitizen.imagemaps;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

@SerializableAs("ImageMaps.Map")
public class ImageMap implements ConfigurationSerializable {
    private final String filename;
    private final int x;
    private final int y;
    private final double scale;

    public ImageMap(final String filename, final int x, final int y, final double scale) {
        this.filename = filename;
        this.x = x;
        this.y = y;
        this.scale = scale;
    }

    public ImageMap(final Map<?, ?> map) {
        this.filename = map.get("image").toString();
        this.x = (int) map.get("x");
        this.y = (int) map.get("y");
        this.scale = (double) map.get("scale");
    }

    @Override
    public Map<String, Object> serialize() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("image", this.filename);
        map.put("x", this.x);
        map.put("y", this.y);
        map.put("scale", this.scale);
        return map;
    }

    public String getFilename() { return this.filename; }

    public int getX() { return this.x; }

    public int getY() { return this.y; }

    public double getScale() { return this.scale; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.filename == null) ? 0 : this.filename.hashCode());
        final long temp = Double.doubleToLongBits(this.scale);
        result = prime * result + (int) (temp ^ temp >>> 32);
        result = prime * result + this.x;
        result = prime * result + this.y;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final ImageMap other)) {
            return false;
        }
        if (this.filename == null) {
            if (other.filename != null) {
                return false;
            }
        } else if (!this.filename.equals(other.filename)) {
            return false;
        }
        return Double.doubleToLongBits(this.scale) == Double.doubleToLongBits(other.scale) && this.x == other.x && this.y == other.y;
    }
}
