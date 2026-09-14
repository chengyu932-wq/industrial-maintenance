package com.cq.maintenance.knowledge.similarity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TextPreprocessor {
    private static final Pattern SEGMENT=Pattern.compile("[\\p{IsHan}]+|[a-z0-9]+(?:[-_.][a-z0-9]+)*");
    private final Set<String> stopwords;

    public TextPreprocessor(){this(loadStopwords());}
    TextPreprocessor(Set<String> stopwords){this.stopwords=Set.copyOf(stopwords);}

    public List<String> tokens(String source){
        if(source==null||source.isBlank())return List.of();
        String normalized=Normalizer.normalize(source,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        for(String word:stopwords)normalized=normalized.replace(word," ");
        List<String> result=new ArrayList<>();Matcher matcher=SEGMENT.matcher(normalized);
        while(matcher.find()){
            String segment=matcher.group();
            if(segment.codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN)){
                int[] chars=segment.codePoints().toArray();
                if(chars.length==1)result.add(segment);
                else for(int i=0;i<chars.length-1;i++)result.add(new String(chars,i,2));
            }else result.add(segment);
        }
        return List.copyOf(result);
    }

    private static Set<String> loadStopwords(){
        Set<String> result=new LinkedHashSet<>();ClassPathResource resource=new ClassPathResource("knowledge-stopwords.txt");
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(resource.getInputStream(),StandardCharsets.UTF_8))){
            reader.lines().map(String::trim).filter(line->!line.isEmpty()&&!line.startsWith("#")).forEach(result::add);
        }catch(IOException e){throw new IllegalStateException("知识库停用词加载失败",e);}
        return result;
    }
}
