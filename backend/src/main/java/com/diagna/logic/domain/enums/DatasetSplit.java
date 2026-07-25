package com.diagna.logic.domain.enums;

/**
 * The train/validation/test split a meeting or dialog belongs to in the
 * original MISeD release. Splits are meeting-disjoint (verified: no meeting
 * id appears in more than one split), so this is safe to denormalise onto
 * both {@code meetings} and {@code dialogs} documents without risk of the
 * two disagreeing.
 *
 * <p>Stored in Mongo as the uppercase enum name ("TRAIN"/"VALIDATION"/"TEST")
 * by the ingest CLI, so Spring Data's default enum conversion (by
 * {@code name()}) round-trips it without a custom converter.
 */
public enum DatasetSplit {
    TRAIN,
    VALIDATION,
    TEST
}
