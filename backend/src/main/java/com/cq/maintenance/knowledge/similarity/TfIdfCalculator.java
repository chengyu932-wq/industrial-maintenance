package com.cq.maintenance.knowledge.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TfIdfCalculator {
    public Map<String,Double> termFrequency(List<String> tokens){
        if(tokens.isEmpty())return Map.of();Map<String,Double> result=new HashMap<>();
        tokens.forEach(token->result.merge(token,1D,Double::sum));
        result.replaceAll((key,count)->count/tokens.size());return Map.copyOf(result);
    }
    public Map<String,Integer> documentFrequency(List<List<String>> documents){
        Map<String,Integer> result=new HashMap<>();
        for(List<String> document:documents)for(String token:new HashSet<>(document))result.merge(token,1,Integer::sum);
        return Map.copyOf(result);
    }
    public double inverseDocumentFrequency(int documentCount,int documentFrequency){
        return Math.log((documentCount+1D)/(documentFrequency+1D))+1D;
    }
    public Map<String,Double> vector(List<String> tokens,Map<String,Integer> frequencies,int documentCount){
        Map<String,Double> result=new HashMap<>();
        termFrequency(tokens).forEach((token,tf)->{Integer df=frequencies.get(token);if(df!=null)result.put(token,tf*inverseDocumentFrequency(documentCount,df));});
        return Map.copyOf(result);
    }
    public double cosine(Map<String,Double> left,Map<String,Double> right){
        if(left.isEmpty()||right.isEmpty())return 0D;double dot=0D,leftNorm=0D,rightNorm=0D;
        for(double value:left.values())leftNorm+=value*value;
        for(double value:right.values())rightNorm+=value*value;
        for(Map.Entry<String,Double> item:left.entrySet())dot+=item.getValue()*right.getOrDefault(item.getKey(),0D);
        if(leftNorm==0D||rightNorm==0D)return 0D;
        return Math.max(0D,Math.min(1D,dot/(Math.sqrt(leftNorm)*Math.sqrt(rightNorm))));
    }
}
