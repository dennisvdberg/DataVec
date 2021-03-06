/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.api.transform.transform.column;

import org.datavec.api.util.StringUtils;
import org.nd4j.shade.jackson.annotation.JsonIgnoreProperties;
import org.nd4j.shade.jackson.annotation.JsonProperty;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.transform.BaseTransform;
import org.datavec.api.transform.metadata.ColumnMetaData;
import org.datavec.api.writable.Writable;

import java.util.*;

/**
 * Remove the specified columns from the data.
 * To specify only the columns to keep, use {@link RemoveAllColumnsExceptForTransform}
 *
 * @author Alex Black
 */
@JsonIgnoreProperties({"inputSchema", "columnsToRemoveIdx", "indicesToRemove"})
public class RemoveColumnsTransform extends BaseTransform {

    private int[] columnsToRemoveIdx;
    private String[] columnsToRemove;
    private Set<Integer> indicesToRemove;

    public RemoveColumnsTransform(@JsonProperty("columnsToRemove") String... columnsToRemove) {
        this.columnsToRemove = columnsToRemove;
    }

    @Override
    public void setInputSchema(Schema schema) {
        super.setInputSchema(schema);

        indicesToRemove = new HashSet<>();

        int i = 0;
        columnsToRemoveIdx = new int[columnsToRemove.length];
        for (String s : columnsToRemove) {
            int idx = schema.getIndexOfColumn(s);
            if (idx < 0) throw new RuntimeException("Column \"" + s + "\" not found");
            columnsToRemoveIdx[i++] = idx;
            indicesToRemove.add(idx);
        }
    }

    @Override
    public Schema transform(Schema schema) {
        int nToRemove = columnsToRemove.length;
        int newNumColumns = schema.numColumns() - nToRemove;
        if (newNumColumns <= 0)
            throw new IllegalStateException("Number of columns after executing operation is " + newNumColumns + " (is <= 0). " +
                    "origColumns = " + schema.getColumnNames() + ", toRemove = " + Arrays.toString(columnsToRemove));

        List<String> origNames = schema.getColumnNames();
        List<ColumnMetaData> origMeta = schema.getColumnMetaData();

        Set<String> set = new HashSet<>();
        Collections.addAll(set, columnsToRemove);


        List<ColumnMetaData> newMeta = new ArrayList<>(newNumColumns);

        Iterator<String> namesIter = origNames.iterator();
        Iterator<ColumnMetaData> metaIter = origMeta.iterator();

        while (namesIter.hasNext()) {
            String n = namesIter.next();
            ColumnMetaData t = metaIter.next();
            if (!set.contains(n)) {
                newMeta.add(t);
            }
        }

        return schema.newSchema(newMeta);
    }

    @Override
    public List<Writable> map(List<Writable> writables) {
        if (writables.size() != inputSchema.numColumns()) {
            List<String> list = new ArrayList<>();
            for(Writable w : writables)
                list.add(w.toString());
            String toString = StringUtils.join(",",list);
            throw new IllegalStateException("Cannot execute transform: input writables list length (" + writables.size() + ") does not " +
                    "match expected number of elements (schema: " + inputSchema.numColumns() + "). Transform = " + toString() + " and record " + toString);
        }

        List<Writable> outList = new ArrayList<>(writables.size() - columnsToRemove.length);

        int i = 0;
        for (Writable w : writables) {
            if (indicesToRemove.contains(i++)) continue;
            outList.add(w);
        }
        return outList;
    }

    @Override
    public String toString() {
        return "RemoveColumnsTransform(" + Arrays.toString(columnsToRemove) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoveColumnsTransform o2 = (RemoveColumnsTransform) o;

        return Arrays.equals(columnsToRemove, o2.columnsToRemove);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(columnsToRemove);
    }
}
