package com.cq.maintenance.knowledge.similarity;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TfIdfCalculatorTest {
    final TfIdfCalculator calculator=new TfIdfCalculator();
    final TextPreprocessor preprocessor=new TextPreprocessor(Set.of("设备","出现"));
    @Test void termFrequencyUsesTermCountOverTotal(){assertEquals(2D/3D,calculator.termFrequency(List.of("轴承","轴承","过热")).get("轴承"),1e-9);}
    @Test void documentFrequencyCountsDocumentsNotOccurrences(){assertEquals(2,calculator.documentFrequency(List.of(List.of("轴承","轴承"),List.of("轴承","过热"))).get("轴承"));}
    @Test void smoothedIdfMatchesFormula(){assertEquals(Math.log(4D/2D)+1D,calculator.inverseDocumentFrequency(3,1),1e-9);}
    @Test void tfIdfCombinesTfAndIdf(){assertEquals(0.5D,calculator.vector(List.of("轴承","过热"),Map.of("轴承",2),2).get("轴承"),1e-9);}
    @Test void identicalTextHasUnitSimilarity(){var v=Map.of("主轴",0.5D,"异响",0.5D);assertEquals(1D,calculator.cosine(v,v),1e-9);}
    @Test void disjointTextHasZeroSimilarity(){assertEquals(0D,calculator.cosine(Map.of("主轴",1D),Map.of("漏油",1D)),1e-9);}
    @Test void partialOverlapIsBetweenZeroAndOne(){double value=calculator.cosine(Map.of("主轴",1D,"异响",1D),Map.of("主轴",1D,"过热",1D));assertTrue(value>0D&&value<1D);}
    @Test void emptyTextIsSafe(){assertTrue(preprocessor.tokens("  ，。 ").isEmpty());assertTrue(calculator.termFrequency(List.of()).isEmpty());}
    @Test void zeroVectorIsSafe(){assertEquals(0D,calculator.cosine(Map.of(),Map.of("主轴",1D)));}
    @Test void preprocessingIsStableAndPreservesDomainBigrams(){assertEquals(preprocessor.tokens("设备出现主轴轴承异响"),preprocessor.tokens("设备出现主轴轴承异响"));assertTrue(preprocessor.tokens("设备出现主轴轴承异响").contains("轴承"));}
}
