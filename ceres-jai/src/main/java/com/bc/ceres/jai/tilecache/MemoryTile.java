/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.jai.tilecache;

import javax.media.jai.CachedTile;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.Image;
import java.lang.ref.WeakReference;
import java.math.BigInteger;


/**
 * Information associated with a cached tile.
 * <p/>
 * <p> This class is used by {@link SwappingTileCache} to create an object that
 * includes all the information associated with a tile, and is put
 * into the tile cache.
 * <p/>
 * The implementation is based on the Sun Microsystems' implementation of
 * the <code>javax.media.jai.CachedTile</code> interface.
 *
 * @author Sun Microsystems
 * @author Norman Fomferra
 */
public final class MemoryTile implements CachedTile {

    // Soft or Weak references need to be used, or the objects
    // never get garbage collected.   The OpImage finalize
    // method calls removeTiles().  It was suggested, that
    // the owner be a weak reference.
    Raster tile;                // the tile to be cached
    WeakReference owner;        // the RenderedImage this tile belongs to

    int tileX;            // tile X index
    int tileY;            // tile Y index

    Object tileCacheMetric;     // Metric for weighting tile computation cost
    long timeStamp;        // the last time this tile is accessed

    Object key;            // the key used to hash this tile
    long tileSize;        // the memory used by this tile in bytes

    MemoryTile previous;    // the SunCachedTile before this tile
    MemoryTile next;        // the SunCachedTile after this tile

    int action = 0;             // add, remove, update from tile cache

    MemoryTile(RenderedImage owner,
               int tileX,
               int tileY,
               Raster tile,
               Object tileCacheMetric) {
        this.owner = new WeakReference<RenderedImage>(owner);
        this.tile = tile;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileCacheMetric = tileCacheMetric;  // may be null
        this.key = hashKey(owner, tileX, tileY);
        DataBuffer db = tile.getDataBuffer();
        this.tileSize = DataBuffer.getDataTypeSize(db.getDataType()) / 8L *
                db.getSize() * db.getNumBanks();
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public Object getKey() {
        return key;
    }

    /*
     * Returns the hash table "key" as a <code>Object</code> for this
     * tile.  For <code>PlanarImage</code> and
     * <code>SerializableRenderedImage</code>, the key is generated by
     * the method <code>ImageUtilgenerateID(Object) </code>.  For the
     * other cases, a <code>Long</code> object is returned.
     * The upper 32 bits for this <code>Long</code> is the tile owner's
     * hash code, and the lower 32 bits is the tile's index.
     */
    public static Object hashKey(RenderedImage owner,
                                 int tileX,
                                 int tileY) {
// todo - added new (but unsafe) key for better readability (swapped file names)
        Object imageId = owner.getProperty("imageId");
        if (imageId == null  || Image.UndefinedProperty.equals(imageId)) {
            return  Integer.toHexString(owner.hashCode()) +
                    "_" + tileX +
                    "_" + tileY +
                    "_" + owner.getClass().getName().replace('.', '_');
        } else {
            return  Integer.toHexString(owner.hashCode()) +
                    "_" + tileX +
                    "_" + tileY +
                    "_" + imageId;
        }
// todo - restore original Sun code
/*
        long idx = tileY * (long) owner.getNumXTiles() + tileX;

        BigInteger imageID = null;
        if (owner instanceof PlanarImage)
            imageID = (BigInteger) ((PlanarImage) owner).getImageID();
        else if (owner instanceof SerializableRenderedImage)
            imageID = (BigInteger) ((SerializableRenderedImage) owner).getImageID();

        if (imageID != null) {
            byte[] buf = imageID.toByteArray();
            int length = buf.length;
            byte[] buf1 = new byte[length + 8];
            System.arraycopy(buf, 0, buf1, 0, length);
            for (int i = 7, j = 0; i >= 0; i--, j += 8)
                buf1[length++] = (byte) (idx >> j);
            return new BigInteger(buf1);
        }

        idx = idx & 0x00000000ffffffffL;
        return ((long) owner.hashCode() << 32) | idx;
*/
    }

    /**
     *  Special version of hashKey for use in SunTileCache.removeTiles().
     *  Minimizes the overhead of repeated calls to
     *  hashCode and getNumTiles(). Note that this causes a
     *  linkage between the CachedTile and SunTileCache classes
     *  in that SunTileCache now has to understand how the
     *  tileIndex is calculated.
     */
/*
    static Long hashKey(int ownerHashCode,
                        int tileIndex) {
        long idx = (long)tileIndex;
        idx = idx & 0x00000000ffffffffL;
        return new Long(((long)ownerHashCode << 32) | idx);
    }
*/
    /** Returns the owner's hash code. */
    /*    static long getOwnerHashCode(Long key) {
            return key.longValue() >>> 32;
        }
    */
    /**
     * Returns a string representation of the class object.
     */
    public String toString() {
        RenderedImage o = getOwner();
        String ostring = o == null ? "null" : o.toString();

        Raster t = getTile();
        String tstring = t == null ? "null" : t.toString();

        return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
                ": owner = " + ostring +
                " tileX = " + Integer.toString(tileX) +
                " tileY = " + Integer.toString(tileY) +
                " tile = " + tstring +
                " key = " + getKeyAsString() +
                " tileSize = " + Long.toString(tileSize) +
                " timeStamp = " + Long.toString(timeStamp);
    }

    public String getKeyAsString() {
        if (key instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) key;
            return bigInteger.toString(16);
        }
        return ((key instanceof Long) ? Long.toHexString((Long) key) : key.toString());
    }

    /**
     * Returns the cached tile.
     */
    public Raster getTile() {
        return tile;
    }

    /**
     * Returns the owner of the cached tile.
     */
    public RenderedImage getOwner() {
        return (RenderedImage) owner.get();
    }

    /**
     * Returns the current time stamp
     */
    public long getTileTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns the tileCacheMetric object
     */
    public Object getTileCacheMetric() {
        return tileCacheMetric;
    }

    /**
     * Returns the tile memory size
     */
    public long getTileSize() {
        return tileSize;
    }

    /**
     * Returns information about the method that
     * triggered the notification event.
     */
    public int getAction() {
        return action;
    }
}