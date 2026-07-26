package com.meetingiq.platform.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    private static final Function<String, String> IDENTITY = s -> s;

    @Test
    void itemRankedInBothListsOutranksAnItemInOnlyOneList() {
        List<String> a = List.of("x", "y", "z");
        List<String> b = List.of("y", "z", "x");
        // "y" is top-1 in b and 2nd in a; "x" is top-1 in a but last in b — "y" should win.
        List<String> fused = ReciprocalRankFusion.fuse(a, b, IDENTITY);
        assertThat(fused.get(0)).isEqualTo("y");
    }

    @Test
    void emptyRankingsProduceEmptyResult() {
        assertThat(ReciprocalRankFusion.fuse(List.of(), List.of(), IDENTITY)).isEmpty();
    }

    @Test
    void oneEmptyRankingDegradesToTheOtherRankingsOrder() {
        List<String> a = List.of("p", "q", "r");
        assertThat(ReciprocalRankFusion.fuse(a, List.of(), IDENTITY)).containsExactly("p", "q", "r");
    }

    @Test
    void disjointRankingsIncludeAllItemsFromBoth() {
        List<String> a = List.of("a1", "a2");
        List<String> b = List.of("b1", "b2");
        assertThat(ReciprocalRankFusion.fuse(a, b, IDENTITY)).containsExactlyInAnyOrder("a1", "a2", "b1", "b2");
    }

    @Test
    void topRankInBothListsBeatsTopRankInOnlyOneList() {
        List<String> a = List.of("shared", "onlyA");
        List<String> b = List.of("shared", "onlyB");
        List<String> fused = ReciprocalRankFusion.fuse(a, b, IDENTITY);
        assertThat(fused.get(0)).isEqualTo("shared");
        assertThat(fused).containsExactlyInAnyOrder("shared", "onlyA", "onlyB");
    }

    @Test
    void duplicateItemAcrossRankingsIsNotDoubleCounted() {
        List<String> a = List.of("only");
        List<String> b = List.of("only");
        List<String> fused = ReciprocalRankFusion.fuse(a, b, IDENTITY);
        assertThat(fused).containsExactly("only");
    }
}
