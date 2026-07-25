package com.diagna.logic.api.dto;

import java.util.List;

/** Retrieval provenance for one chat answer — which chunks were retrieved and whether the full transcript was used instead. */
public record RetrievalInfoDto(List<String> chunkIds, double topScore, boolean usedFullTranscript) {
}
