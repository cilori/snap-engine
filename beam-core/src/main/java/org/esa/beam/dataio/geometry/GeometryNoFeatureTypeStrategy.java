/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class GeometryNoFeatureTypeStrategy extends AbstractInterpretationStrategy {

    private FeatureIdCreator idCreator = new FeatureIdCreator();
    private GeoCoding geoCoding;
    private String geometryName;

    public GeometryNoFeatureTypeStrategy(GeoCoding geoCoding, String geometryName) {
        this.geoCoding = geoCoding;
        this.geometryName = geometryName;
    }

    @Override
    public void transformGeoPosToPixelPos(SimpleFeature simpleFeature) {
        Geometry defaultGeometry = (Geometry) simpleFeature.getDefaultGeometry();
        defaultGeometry.apply(new GeoPosToPixelPosFilter(defaultGeometry.getCoordinates().length, geoCoding));
    }

    @Override
    public void setDefaultGeometry(SimpleFeatureTypeBuilder builder) {
        builder.setDefaultGeometry(geometryName);
    }

    @Override
    public void setName(SimpleFeatureTypeBuilder builder) {
        builder.setName(
                builder.getDefaultGeometry() + "_" +
                new SimpleDateFormat("dd-MMM-yyyy'T'HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    @Override
    public int getExpectedTokenCount(int attributeCount) {
        return attributeCount;
    }

    @Override
    public String getFeatureId(String[] tokens) {
        return idCreator.createFeatureId();
    }

    @Override
    public void interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException {
        for (int attributeIndex = 0; attributeIndex < tokens.length; attributeIndex++) {
            String token = tokens[attributeIndex];
            setAttributeValue(builder, simpleFeatureType, attributeIndex, token);
        }
    }

    @Override
    public int getStartColumn() {
        return 0;
    }

    private static class FeatureIdCreator {

        private static int count = 0;

        private String createFeatureId() {
            return "ID" + String.format("%08d", count++);
        }
    }
}
