package com.vdian.useful.stovedataprocessor;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author jifang
 * @since 2017/3/17 下午3:59.
 */
public class ThesaurusProcess {

    public static Map<String, String> process(String fileName) {
        final Map<String, String> thesaurusMap = new HashMap<>();
        try {
            Files.readLines(new File(fileName), Charset.forName("UTF-8"), new LineProcessor<Map<String, Set<String>>>() {

                @Override
                public boolean processLine(String line) throws IOException {
                    Iterator<String> iterator = Splitter.on("：").omitEmptyStrings().trimResults().split(line).iterator();
                    thesaurusMap.put(iterator.next(), iterator.next());

                    return true;
                }

                @Override
                public Map<String, Set<String>> getResult() {
                    return null;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return thesaurusMap;
    }
}
