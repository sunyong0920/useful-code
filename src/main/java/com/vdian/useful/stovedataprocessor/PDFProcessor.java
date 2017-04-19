package com.vdian.useful.stovedataprocessor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.vdian.useful.stovedataprocessor.baidu.TransApi;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author jifang
 * @since 2017/3/17 下午8:52.
 */
public class PDFProcessor {

    private static final TransApi api = new TransApi("20170317000042491", "wFBf3mWy5ZPwsCFx72lb");

    private static final Map<String, String> translateMap = ThesaurusProcess.process("/Users/jifang/IdeaProjects/setrain/src/main/resources/thesaurus.txt");

    public static void main(String[] args) throws IOException {
        args = new String[]{
                "/Users/jifang/Downloads/管式炉实验数据GCMS",
                "/Users/jifang/Downloads/output"
        };

        long begin = System.currentTimeMillis();

        File fromParentDir = new File(args[0]);
        File toParentDir = new File(args[1]);
        if (!toParentDir.exists()) {
            toParentDir.mkdir();
        }
        if (fromParentDir.isDirectory()) {
            File[] files = fromParentDir.listFiles();
            assert files != null;
            for (File dir : files) {
                if (dir.isDirectory() && !dir.getName().equals(".") && !dir.getName().equals("..")) {
                    String fromDirName = dir.getName();
                    String toDirName = toParentDir.getAbsolutePath() + File.separator + fromDirName;
                    new File(toDirName).mkdirs();
                    processDir(dir, toDirName);
                }
            }
        }

        System.out.println("total cost: " + ((System.currentTimeMillis() - begin) / 1000) + " s");
    }

    private static void processDir(File fromDir, String toDirName) {
        for (File fromFile : fromDir.listFiles()) {
            String fromFileName = fromFile.getName();
            String nakedFileName = needProcess(fromFileName);
            if (!Strings.isNullOrEmpty(nakedFileName)) {
                String toFileName = toDirName + File.separator + nakedFileName + ".xls";
                try {
                    processFile(fromFile.getAbsolutePath(), toFileName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    private static void processFile(String fromAbsoluteFileName, String toFileName) throws IOException {
        long start = System.currentTimeMillis();
        PdfReader reader = new PdfReader(fromAbsoluteFileName);

        SortedMap<Integer, Domain> map = new TreeMap<>();
        List<String> enNames = new ArrayList<>();
        for (int pageNumber = 1; pageNumber <= reader.getNumberOfPages(); ++pageNumber) {

            // 一页
            Iterator<String> iterator = Splitter.on("\n").split(PdfTextExtractor.getTextFromPage(reader, pageNumber)).iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.startsWith(" 峰号")) {
                    processMap(map, enNames, iterator);
                } else if (line.startsWith("行号#:")) {
                    int number = Integer.valueOf(line.split("   ")[0].split(":")[1]);
                    map.get(number).formula = getFormula(iterator);
                }
            }
        }

        List<String> zhNames = getZhName(enNames, translateMap);
        for (Map.Entry<Integer, Domain> entry : map.entrySet()) {
            entry.getValue().chName = zhNames.get(entry.getKey() - 1);
        }

        writeExcel(map, toFileName);

        System.out.println("process file [" + fromAbsoluteFileName + " ] cost: " + (System.currentTimeMillis() - start) + " ms");
    }

    private static void writeExcel(SortedMap<Integer, Domain> map, String toFileName) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(toFileName)) {
            Sheet sheet = workbook.createSheet();
            Row headlineRow = sheet.createRow(0);
            createTileLine(headlineRow);

            for (SortedMap.Entry<Integer, Domain> entry : map.entrySet()) {
                Row row = sheet.createRow(entry.getKey());

                Domain domain = entry.getValue();
                row.createCell(0, CellType.NUMERIC).setCellValue(domain.number);
                row.createCell(1, CellType.NUMERIC).setCellValue(domain.areaRate);
                row.createCell(2, CellType.STRING).setCellValue(domain.enName);
                row.createCell(3, CellType.STRING).setCellValue(domain.chName);
                row.createCell(4, CellType.STRING).setCellValue(domain.formula);
            }

            workbook.write(out);
        }
    }

    private static void createTileLine(Row row) {
        String[] titles = new String[]{
                "编号",
                "峰面积%",
                "英文名称",
                "中文名称",
                "分子式",
                "备注"
        };
        for (int i = 0; i < titles.length; ++i) {
            row.createCell(i, CellType.STRING).setCellValue(titles[i]);
        }
    }

    private static String needProcess(String fileName) {
        List<String> strings = Splitter.on(".").splitToList(fileName);
        if (strings.size() == 2
                && !Strings.isNullOrEmpty(strings.get(1))
                && strings.get(1).equals("pdf")
                && !fileName.equals("2Qualjxh4.D.pdf")) {
            return strings.get(0);
        } else {
            return "";
        }
    }

    private static List<String> getZhName(List<String> names, Map<String, String> translateMap) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); ++i) {
            if (i != 0) {
                sb.append("\n");
            }
            sb.append(names.get(i));
        }


        String result = api.getTransResult(sb.toString(), "en", "zh");
        JSONArray array = JSONObject.parseObject(result).getJSONArray("trans_result");

        List<String> zhNames = new ArrayList<>(names.size());
        for (int i = 0; i < array.size(); ++i) {
            String dst = array.getJSONObject(i).getString("dst");
            zhNames.add(translate(dst, translateMap));
        }

        return zhNames;
    }

    private static String translate(String name, Map<String, String> translateMap) {
        for (Map.Entry<String, String> entry : translateMap.entrySet()) {
            String key = entry.getKey();

            //StringUtils.containsIgnoreCase();
            //StringUtils.replace
            if (name.contains(key)) {
                name = name.replace(key, entry.getValue());
            }
        }

        return name;
    }

    private static void processMap(SortedMap<Integer, Domain> map, List<String> enNames, Iterator<String> iter) {
        while (iter.hasNext()) {
            String line = iter.next();
            List<String> word = Splitter.on("  ").omitEmptyStrings().trimResults().splitToList(line);
            if (word.size() == 10 || word.size() == 11) {
                int number = Integer.valueOf(word.get(0));
                String areaRate = word.get(5);
                String enName;
                String ninthCol = word.get(9);
                if (StringUtils.equals(ninthCol, "V")) {
                    enName = word.get(10);
                } else {
                    enName = ninthCol;
                }


                Domain domain = new Domain();
                domain.number = number;
                domain.areaRate = areaRate;
                domain.enName = enName;
                map.put(number, domain);
                enNames.add(enName);
            } else {
                return;
            }
        }
    }

    private static String getFormula(Iterator<String> iterator) {
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.startsWith("命中#:1")) {
                String nextLine = iterator.next();
                return nextLine.split("  ")[1].split(":")[1];
            }
        }
        return "";
    }

    @Test
    public void test() throws IOException {
    }

    private static final class Domain {
        int number;
        String areaRate;
        String enName;
        String chName;
        String formula;
    }
}
