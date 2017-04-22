package com.vdian.useful.stovedataprocessor;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang
 * @since 2017/4/19 下午5:08.
 */
public class ExcelDataProcessor {

    public static Map<String, Pair<Integer, Integer>> processExcel(String fileName) {

        Map<String, Pair<Integer, Integer>> result = new HashMap<>();

        File dirFile = new File(fileName);
        for (File file : dirFile.listFiles()) {
            if (file.getName().equals(".")
                    || file.getName().equals("..")
                    || file.getName().startsWith(".")) {
                continue;
            }

            Workbook workbook;
            try {
                workbook = WorkbookFactory.create(file);
            } catch (IOException | InvalidFormatException e) {
                throw new RuntimeException(e);
            }
            Sheet sheet = workbook.iterator().next();
            for (int i = 1; i <= sheet.getLastRowNum(); ++i) {
                Row row = sheet.getRow(i);

                if (row != null && row.getLastCellNum() >= 7) {
                    Cell englishNameCell = row.getCell(2);// 英文名称
                    Cell gCell = row.getCell(6); //G
                    Cell hCell = row.getCell(7);

                    if (!isNull(englishNameCell, gCell, hCell)) {
                        String name = englishNameCell.getStringCellValue();
                        int gValue = (int) gCell.getNumericCellValue();
                        int hValue = (int) hCell.getNumericCellValue();

                        result.put(name, Pair.of(gValue, hValue));
                    }
                } else {
                    System.out.println(i);
                }
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException, InvalidFormatException {
        Map<String, Pair<Integer, Integer>> stringPairMap = processExcel("/Users/jifang/Downloads/jxh1201-1__2016121_2处理.xls");
        System.out.println(stringPairMap);
    }

    public static boolean isNull(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                return true;
            }
        }

        return false;
    }

}
