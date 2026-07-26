/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ibm.watsonx.ai.batch.BatchCancelRequest;
import com.ibm.watsonx.ai.batch.BatchCreateRequest;
import com.ibm.watsonx.ai.batch.BatchListRequest;
import com.ibm.watsonx.ai.batch.BatchRetrieveRequest;
import com.ibm.watsonx.ai.chat.ChatModeration;
import com.ibm.watsonx.ai.chat.model.ChatParameters;
import com.ibm.watsonx.ai.chat.model.schema.ArraySchema;
import com.ibm.watsonx.ai.chat.model.schema.BooleanSchema;
import com.ibm.watsonx.ai.chat.model.schema.ConstantSchema;
import com.ibm.watsonx.ai.chat.model.schema.EnumSchema;
import com.ibm.watsonx.ai.chat.model.schema.IntegerSchema;
import com.ibm.watsonx.ai.chat.model.schema.JsonSchema;
import com.ibm.watsonx.ai.chat.model.schema.NumberSchema;
import com.ibm.watsonx.ai.chat.model.schema.ObjectSchema;
import com.ibm.watsonx.ai.chat.model.schema.RequiredSchema;
import com.ibm.watsonx.ai.chat.model.schema.StringSchema;
import com.ibm.watsonx.ai.deployment.FindByIdRequest;
import com.ibm.watsonx.ai.detection.DetectionTextRequest;
import com.ibm.watsonx.ai.detection.detector.GraniteGuardian;
import com.ibm.watsonx.ai.detection.detector.Hap;
import com.ibm.watsonx.ai.detection.detector.Pii;
import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.file.FileDeleteRequest;
import com.ibm.watsonx.ai.file.FileListRequest;
import com.ibm.watsonx.ai.file.FileRetrieveRequest;
import com.ibm.watsonx.ai.rerank.RerankParameters;
import com.ibm.watsonx.ai.textgeneration.TextGenerationParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaFetchParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.create.CreateSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaFetchParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.improve.ImproveSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaFetchParameters;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaParameters;
import com.ibm.watsonx.ai.textprocessing.schema.merge.MergeSchemaSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationFetchParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationParameters;
import com.ibm.watsonx.ai.textprocessing.textclassification.TextClassificationSemanticConfig;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionDeleteParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionFetchParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionParameters;
import com.ibm.watsonx.ai.textprocessing.textextraction.TextExtractionSemanticConfig;
import com.ibm.watsonx.ai.timeseries.TimeSeriesParameters;
import com.ibm.watsonx.ai.tokenization.TokenizationParameters;

/**
 * Verifies that toString() implementations across the hierarchy include both inherited and own fields.
 */
public class ToStringTest {

    // -------------------------------------------------------------------------
    // WatsonxParameters hierarchy
    // -------------------------------------------------------------------------

    @Test
    void batch_cancel_request_toString_includes_parent_fields() {
        var s = BatchCancelRequest.builder().projectId("p1").spaceId("s1").transactionId("t1").batchId("b1").build().toString();
        assertTrue(s.contains("BatchCancelRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("spaceId=s1"), s);
        assertTrue(s.contains("transactionId=t1"), s);
        assertTrue(s.contains("batchId=b1"), s);
    }

    @Test
    void batch_retrieve_request_toString_includes_parent_fields() {
        var s = BatchRetrieveRequest.builder().projectId("p1").batchId("b1").build().toString();
        assertTrue(s.contains("BatchRetrieveRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("batchId=b1"), s);
    }

    @Test
    void batch_list_request_toString_includes_parent_fields() {
        var s = BatchListRequest.builder().projectId("p1").limit(5).build().toString();
        assertTrue(s.contains("BatchListRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("limit=5"), s);
    }

    @Test
    void batch_create_request_toString_includes_parent_fields() {
        var s = BatchCreateRequest.builder().projectId("p1").inputFileId("f1").build().toString();
        assertTrue(s.contains("BatchCreateRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("inputFileId=f1"), s);
    }

    @Test
    void detection_text_request_toString_includes_parent_fields() {
        var s = DetectionTextRequest.builder().projectId("p1").input("hello").detectors(Pii.ofDefaults()).build().toString();
        assertTrue(s.contains("DetectionTextRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("input=hello"), s);
    }

    @Test
    void find_by_id_request_toString_includes_parent_fields() {
        var s = FindByIdRequest.builder().projectId("p1").deploymentId("d1").build().toString();
        assertTrue(s.contains("FindByIdRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("deploymentId=d1"), s);
    }

    @Test
    void embedding_parameters_toString_includes_parent_fields() {
        var s = EmbeddingParameters.builder().projectId("p1").modelId("m1").build().toString();
        assertTrue(s.contains("EmbeddingParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
    }

    @Test
    void file_delete_request_toString_includes_parent_fields() {
        var s = FileDeleteRequest.builder().projectId("p1").fileId("f1").build().toString();
        assertTrue(s.contains("FileDeleteRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("fileId=f1"), s);
    }

    @Test
    void file_retrieve_request_toString_includes_parent_fields() {
        var s = FileRetrieveRequest.builder().projectId("p1").fileId("f1").build().toString();
        assertTrue(s.contains("FileRetrieveRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("fileId=f1"), s);
    }

    @Test
    void file_list_request_toString_includes_parent_fields() {
        var s = FileListRequest.builder().projectId("p1").limit(10).build().toString();
        assertTrue(s.contains("FileListRequest"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("limit=10"), s);
    }

    @Test
    void rerank_parameters_toString_includes_parent_fields() {
        var s = RerankParameters.builder().projectId("p1").modelId("m1").topN(3).build().toString();
        assertTrue(s.contains("RerankParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
        assertTrue(s.contains("topN=3"), s);
    }

    @Test
    void time_series_parameters_toString_includes_parent_fields() {
        var s = TimeSeriesParameters.builder().projectId("p1").modelId("m1").predictionLength(12).build().toString();
        assertTrue(s.contains("TimeSeriesParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
        assertTrue(s.contains("predictionLength=12"), s);
    }

    @Test
    void tokenization_parameters_toString_includes_parent_fields() {
        var s = TokenizationParameters.builder().projectId("p1").modelId("m1").crypto("key1").returnTokens(true).build().toString();
        assertTrue(s.contains("TokenizationParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
        assertTrue(s.contains("crypto=key1"), s);
        assertTrue(s.contains("returnTokens=true"), s);
    }

    @Test
    void chat_parameters_toString_includes_parent_fields() {
        var s = ChatParameters.builder().projectId("p1").modelId("m1").temperature(0.7).build().toString();
        assertTrue(s.contains("ChatParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
        assertTrue(s.contains("temperature=0.7"), s);
    }

    @Test
    void text_generation_parameters_toString_includes_parent_fields() {
        var s = TextGenerationParameters.builder().projectId("p1").modelId("m1").maxNewTokens(512).build().toString();
        assertTrue(s.contains("TextGenerationParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("modelId=m1"), s);
        assertTrue(s.contains("maxNewTokens=512"), s);
    }

    // -------------------------------------------------------------------------
    // TextProcessing parameters hierarchy
    // -------------------------------------------------------------------------

    @Test
    void text_extraction_fetch_parameters_toString_includes_parent_fields() {
        var s = TextExtractionFetchParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("TextExtractionFetchParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void text_extraction_delete_parameters_toString_includes_parent_fields() {
        var s = TextExtractionDeleteParameters.builder().projectId("p1").hardDelete(true).build().toString();
        assertTrue(s.contains("TextExtractionDeleteParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("hardDelete=Optional[true]"), s);
    }

    @Test
    void text_extraction_parameters_toString_includes_parent_fields() {
        var s = TextExtractionParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("TextExtractionParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void text_classification_fetch_parameters_toString_includes_parent_fields() {
        var s = TextClassificationFetchParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("TextClassificationFetchParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void text_classification_delete_parameters_toString_includes_parent_fields() {
        var s = TextClassificationDeleteParameters.builder().projectId("p1").hardDelete(true).build().toString();
        assertTrue(s.contains("TextClassificationDeleteParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("hardDelete=Optional[true]"), s);
    }

    @Test
    void text_classification_parameters_toString_includes_parent_fields() {
        var s = TextClassificationParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("TextClassificationParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void create_schema_fetch_parameters_toString_includes_parent_fields() {
        var s = CreateSchemaFetchParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("CreateSchemaFetchParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void create_schema_delete_parameters_toString_includes_parent_fields() {
        var s = CreateSchemaDeleteParameters.builder().projectId("p1").hardDelete(true).build().toString();
        assertTrue(s.contains("CreateSchemaDeleteParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("hardDelete=Optional[true]"), s);
    }

    @Test
    void create_schema_parameters_toString_includes_parent_fields() {
        var s = CreateSchemaParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("CreateSchemaParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void improve_schema_fetch_parameters_toString_includes_parent_fields() {
        var s = ImproveSchemaFetchParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("ImproveSchemaFetchParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void improve_schema_delete_parameters_toString_includes_parent_fields() {
        var s = ImproveSchemaDeleteParameters.builder().projectId("p1").hardDelete(true).build().toString();
        assertTrue(s.contains("ImproveSchemaDeleteParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("hardDelete=Optional[true]"), s);
    }

    @Test
    void improve_schema_parameters_toString_includes_parent_fields() {
        var s = ImproveSchemaParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("ImproveSchemaParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void merge_schema_fetch_parameters_toString_includes_parent_fields() {
        var s = MergeSchemaFetchParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("MergeSchemaFetchParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    @Test
    void merge_schema_delete_parameters_toString_includes_parent_fields() {
        var s = MergeSchemaDeleteParameters.builder().projectId("p1").hardDelete(true).build().toString();
        assertTrue(s.contains("MergeSchemaDeleteParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
        assertTrue(s.contains("hardDelete=Optional[true]"), s);
    }

    @Test
    void merge_schema_parameters_toString_includes_parent_fields() {
        var s = MergeSchemaParameters.builder().projectId("p1").build().toString();
        assertTrue(s.contains("MergeSchemaParameters"), s);
        assertTrue(s.contains("projectId=p1"), s);
    }

    // -------------------------------------------------------------------------
    // SemanticConfig hierarchy
    // -------------------------------------------------------------------------

    @Test
    void create_schema_semantic_config_toString_includes_parent_fields() {
        var s = CreateSchemaSemanticConfig.builder().defaultModelName("my-model").build().toString();
        assertTrue(s.contains("CreateSchemaSemanticConfig"), s);
        assertTrue(s.contains("defaultModelName=my-model"), s);
    }

    @Test
    void improve_schema_semantic_config_toString_includes_parent_fields() {
        var s = ImproveSchemaSemanticConfig.builder().defaultModelName("my-model").build().toString();
        assertTrue(s.contains("ImproveSchemaSemanticConfig"), s);
        assertTrue(s.contains("defaultModelName=my-model"), s);
    }

    @Test
    void merge_schema_semantic_config_toString_includes_parent_fields() {
        var s = MergeSchemaSemanticConfig.builder().defaultModelName("my-model").build().toString();
        assertTrue(s.contains("MergeSchemaSemanticConfig"), s);
        assertTrue(s.contains("defaultModelName=my-model"), s);
    }

    @Test
    void text_extraction_semantic_config_toString_includes_parent_fields() {
        var s = TextExtractionSemanticConfig.builder().defaultModelName("my-model").forceSchemaName("invoice").build().toString();
        assertTrue(s.contains("TextExtractionSemanticConfig"), s);
        assertTrue(s.contains("defaultModelName=my-model"), s);
        assertTrue(s.contains("forceSchemaName=invoice"), s);
    }

    @Test
    void text_classification_semantic_config_toString_includes_parent_fields() {
        var s = TextClassificationSemanticConfig.builder().defaultModelName("my-model").forceSchemaName("receipt").build().toString();
        assertTrue(s.contains("TextClassificationSemanticConfig"), s);
        assertTrue(s.contains("defaultModelName=my-model"), s);
        assertTrue(s.contains("forceSchemaName=receipt"), s);
    }

    // -------------------------------------------------------------------------
    // BaseDetector hierarchy
    // -------------------------------------------------------------------------

    @Test
    void pii_toString_includes_name_and_properties() {
        var s = Pii.builder().build().toString();
        assertTrue(s.contains("Pii"), s);
        assertTrue(s.contains("name=pii"), s);
        assertTrue(s.contains("properties"), s);
    }

    @Test
    void hap_toString_includes_name_and_properties() {
        var s = Hap.builder().threshold(0.5).build().toString();
        assertTrue(s.contains("Hap"), s);
        assertTrue(s.contains("name=hap"), s);
        assertTrue(s.contains("threshold"), s);
    }

    @Test
    void granite_guardian_toString_includes_name_and_properties() {
        var s = GraniteGuardian.builder().threshold(0.6).build().toString();
        assertTrue(s.contains("GraniteGuardian"), s);
        assertTrue(s.contains("name=granite_guardian"), s);
        assertTrue(s.contains("threshold"), s);
    }

    // -------------------------------------------------------------------------
    // ChatModeration hierarchy
    // -------------------------------------------------------------------------

    @Test
    void chat_moderation_toString_includes_configured_detectors() {
        var s = ChatModeration.builder()
            .hap(h -> h.output(0.9f))
            .pii(p -> p.output(true))
            .build()
            .toString();
        assertTrue(s.contains("ChatModeration"), s);
        assertTrue(s.contains("hap="), s);
        assertTrue(s.contains("pii="), s);
    }

    @Test
    void chat_moderation_hap_toString_includes_class_name_and_properties() {
        var moderation = ChatModeration.builder().hap(h -> h.output(0.9f).mask(true)).build();
        var s = moderation.hap().toString();
        assertTrue(s.contains("Hap"), s);
        assertTrue(s.contains("properties"), s);
        assertTrue(s.contains("output"), s);
        assertTrue(s.contains("mask"), s);
    }

    @Test
    void chat_moderation_pii_toString_includes_class_name_and_properties() {
        var moderation = ChatModeration.builder().pii(p -> p.output(true)).build();
        var s = moderation.pii().toString();
        assertTrue(s.contains("Pii"), s);
        assertTrue(s.contains("properties"), s);
        assertTrue(s.contains("output"), s);
    }

    @Test
    void chat_moderation_granite_guardian_toString_includes_class_name_and_properties() {
        var moderation = ChatModeration.builder().graniteGuardian(g -> g.input(0.85f)).build();
        var s = moderation.graniteGuardian().toString();
        assertTrue(s.contains("GraniteGuardian"), s);
        assertTrue(s.contains("properties"), s);
        assertTrue(s.contains("input"), s);
    }

    // -------------------------------------------------------------------------
    // JsonSchema hierarchy
    // -------------------------------------------------------------------------

    @Test
    void boolean_schema_toString_includes_base_fields() {
        var s = JsonSchema.bool().description("active").build().toString();
        assertTrue(s.contains("BooleanSchema"), s);
        assertTrue(s.contains("description=active"), s);
        assertTrue(s.contains("type=boolean"), s);
    }

    @Test
    void string_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.string().description("name").maxLength(50).build().toString();
        assertTrue(s.contains("StringSchema"), s);
        assertTrue(s.contains("description=name"), s);
        assertTrue(s.contains("maxLength=50"), s);
    }

    @Test
    void number_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.number().description("price").minimum(0).build().toString();
        assertTrue(s.contains("NumberSchema"), s);
        assertTrue(s.contains("description=price"), s);
        assertTrue(s.contains("minimum=0"), s);
    }

    @Test
    void integer_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.integer().description("age").minimum(18).build().toString();
        assertTrue(s.contains("IntegerSchema"), s);
        assertTrue(s.contains("description=age"), s);
        assertTrue(s.contains("minimum=18"), s);
    }

    @Test
    void enum_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.enumeration("A", "B").description("status").build().toString();
        assertTrue(s.contains("EnumSchema"), s);
        assertTrue(s.contains("description=status"), s);
        assertTrue(s.contains("enumValues"), s);
    }

    @Test
    void constant_schema_toString_includes_own_fields() {
        var s = JsonSchema.constant(42).build().toString();
        assertTrue(s.contains("ConstantSchema"), s);
        assertTrue(s.contains("constant=42"), s);
    }

    @Test
    void required_schema_toString_includes_own_fields() {
        var s = JsonSchema.required("id", "name").build().toString();
        assertTrue(s.contains("RequiredSchema"), s);
        assertTrue(s.contains("required"), s);
    }

    @Test
    void array_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.array().description("tags").items(JsonSchema.string()).build().toString();
        assertTrue(s.contains("ArraySchema"), s);
        assertTrue(s.contains("description=tags"), s);
        assertTrue(s.contains("items"), s);
    }

    @Test
    void object_schema_toString_includes_base_and_own_fields() {
        var s = JsonSchema.object().description("user").property("id", JsonSchema.string()).build().toString();
        assertTrue(s.contains("ObjectSchema"), s);
        assertTrue(s.contains("description=user"), s);
        assertTrue(s.contains("properties"), s);
    }

    // Verify that concrete types returned from the builders are the right subtype
    @Test
    void string_schema_builder_returns_string_schema() {
        assertTrue(JsonSchema.string().build() instanceof StringSchema);
    }

    @Test
    void number_schema_builder_returns_number_schema() {
        assertTrue(JsonSchema.number().build() instanceof NumberSchema);
    }

    @Test
    void integer_schema_builder_returns_integer_schema() {
        assertTrue(JsonSchema.integer().build() instanceof IntegerSchema);
    }

    @Test
    void boolean_schema_builder_returns_boolean_schema() {
        assertTrue(JsonSchema.bool().build() instanceof BooleanSchema);
    }

    @Test
    void enum_schema_builder_returns_enum_schema() {
        assertTrue(JsonSchema.enumeration("X").build() instanceof EnumSchema);
    }

    @Test
    void constant_schema_builder_returns_constant_schema() {
        assertTrue(JsonSchema.constant("v").build() instanceof ConstantSchema);
    }

    @Test
    void required_schema_builder_returns_required_schema() {
        assertTrue(JsonSchema.required("f").build() instanceof RequiredSchema);
    }

    @Test
    void object_schema_builder_returns_object_schema() {
        assertTrue(JsonSchema.object().property("x", JsonSchema.string()).build() instanceof ObjectSchema);
    }

    @Test
    void array_schema_builder_returns_array_schema() {
        assertTrue(JsonSchema.array().items(JsonSchema.string()).build() instanceof ArraySchema);
    }

    @Test
    void nullable_schema_includes_null_in_type() {
        var s = JsonSchema.string().nullable().build().toString();
        assertTrue(s.contains("null"), s);
    }

    @Test
    void one_of_schema_is_included_in_toString() {
        var s = JsonSchema.object()
            .property("age", JsonSchema.integer())
            .oneOf(JsonSchema.required("age"), JsonSchema.required("dob"))
            .build().toString();
        assertTrue(s.contains("oneOf"), s);
    }

    @Test
    void watsonx_parameters_base_toString_contains_all_three_base_fields() {
        var s = BatchCancelRequest.builder()
            .projectId("proj").spaceId("space").transactionId("txn").batchId("b1").build().toString();
        assertTrue(s.contains("projectId=proj"), s);
        assertTrue(s.contains("spaceId=space"), s);
        assertTrue(s.contains("transactionId=txn"), s);
    }

    @Test
    void watsonx_model_parameters_chain_includes_model_id() {
        var s = RerankParameters.builder().projectId("p").modelId("ibm/model").build().toString();
        assertTrue(s.contains("projectId=p"), s);
        assertTrue(s.contains("modelId=ibm/model"), s);
    }

    @Test
    void watsonx_crypto_parameters_chain_includes_crypto() {
        var s = TokenizationParameters.builder().projectId("p").modelId("m").crypto("crn:key:1").returnTokens(true).build().toString();
        assertTrue(s.contains("projectId=p"), s);
        assertTrue(s.contains("modelId=m"), s);
        assertTrue(s.contains("crypto=crn:key:1"), s);
    }
}
