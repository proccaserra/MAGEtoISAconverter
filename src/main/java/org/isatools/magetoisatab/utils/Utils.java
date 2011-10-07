package org.isatools.magetoisatab.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Utils {

    public static List<Column> createColumnOrderList(String[] columnNames) {
        LinkedList<Column> columnOrders = new LinkedList<Column>();
        for (int index = 0; index < columnNames.length; index++) {
            if (!columnNames[index].trim().equals("")) {
                columnOrders.add(new Column(index, columnNames[index]));
            }
        }

        return columnOrders;
    }

    public static int getIndexForValue(String value, List<Column> columnOrders) {
        int count = 0;

        for (Column column : columnOrders) {
            if (column.getLabel().equalsIgnoreCase(value)) {
                return count;
            }
            count++;
        }

        return -1;
    }

    public static int[] createIndexArray(List<Column> columnOrders) {
        int[] columnOrder = new int[columnOrders.size()];

        int count = 0;
        for (Column column : columnOrders) {
            columnOrder[count] = column.getIndex();
            count++;
        }

        return columnOrder;

    }

    public static List<String[]> cleanInput(List<String[]> spreadsheet) {

        List<String[]> cleanedData = new ArrayList<String[]>();

        for(String[] line : spreadsheet) {
            if(line.length > 0) {
                if(!line[0].trim().equals("")) {
                    cleanedData.add(line);
                }
            }
        }

        return cleanedData;
    }


}
