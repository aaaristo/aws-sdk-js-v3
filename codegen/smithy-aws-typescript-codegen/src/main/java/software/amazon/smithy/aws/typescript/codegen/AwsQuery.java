/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.typescript.codegen;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait.Format;
import software.amazon.smithy.typescript.codegen.TypeScriptWriter;
import software.amazon.smithy.typescript.codegen.integration.HttpRpcProtocolGenerator;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Handles generating the aws.query protocol for services. It handles reading and
 * writing from document bodies, including generating any functions needed for
 * performing serde.
 *
 * This builds on the foundations of the {@link HttpRpcProtocolGenerator} to handle
 * standard components of the HTTP requests and responses.
 *
 * @see QueryShapeSerVisitor
 * @see XmlShapeDeserVisitor
 * @see QueryMemberSerVisitor
 * @see XmlMemberDeserVisitor
 * @see AwsProtocolUtils
 * @see <a href="https://smithy.io/2.0/spec/protocol-traits.html#xml-bindings">Smithy XML traits.</a>
 */
@SmithyInternalApi
final class AwsQuery extends HttpRpcProtocolGenerator {

    AwsQuery() {
        // AWS Query protocols will attempt to parse error codes from response bodies.
        super(true);
    }

    @Override
    protected String getOperationPath(GenerationContext context, OperationShape operationShape) {
        return "/";
    }

    @Override
    public ShapeId getProtocol() {
        return AwsQueryTrait.ID;
    }

    @Override
    public String getName() {
        return "aws.query";
    }

    @Override
    protected String getDocumentContentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    protected void generateDocumentBodyShapeSerializers(GenerationContext context, Set<Shape> shapes) {
        AwsProtocolUtils.generateDocumentBodyShapeSerde(context, shapes, new QueryShapeSerVisitor(context));
    }

    @Override
    protected void generateDocumentBodyShapeDeserializers(GenerationContext context, Set<Shape> shapes) {
        AwsProtocolUtils.generateDocumentBodyShapeSerde(context, shapes, new XmlShapeDeserVisitor(context));
    }

    @Override
    public void generateSharedComponents(GenerationContext context) {
        super.generateSharedComponents(context);
        AwsProtocolUtils.generateXmlParseBody(context);
        AwsProtocolUtils.generateXmlParseErrorBody(context);
        AwsProtocolUtils.generateBuildFormUrlencodedString(context);
        AwsProtocolUtils.addItempotencyAutofillImport(context);

        TypeScriptWriter writer = context.getWriter();

        // Generate a function that handles the complex rules around deserializing
        // an error code from an xml error body.
        SymbolReference responseType = getApplicationProtocol().getResponseType();
        writer.openBlock("const loadQueryErrorCode = (\n"
                       + "  output: $T,\n"
                       + "  data: any\n"
                       + "): string | undefined => {", "};", responseType, () -> {
            // Attempt to fetch the error code from the specific location.
            String errorCodeCheckLocation = getErrorBodyLocation(context, "data") + "?.Code";
            String errorCodeAccessLocation = getErrorBodyLocation(context, "data") + ".Code";
            writer.openBlock("if ($L !== undefined) {", "}", errorCodeCheckLocation, () -> {
                writer.write("return $L;", errorCodeAccessLocation);
            });

            // Default a 404 status code to the NotFound code.
            writer.openBlock("if (output.statusCode == 404) {", "}", () -> writer.write("return 'NotFound';"));
        });
        writer.write("");
    }

    @Override
    protected String getErrorBodyLocation(GenerationContext context, String outputLocation) {
        return outputLocation + ".Error";
    }

    @Override
    protected void writeDefaultHeaders(GenerationContext context, OperationShape operation) {
        super.writeDefaultHeaders(context, operation);
        AwsProtocolUtils.generateUnsignedPayloadSigV4Header(context, operation);
    }

    @Override
    protected void serializeInputDocument(
            GenerationContext context,
            OperationShape operation,
            StructureShape inputStructure
    ) {
        TypeScriptWriter writer = context.getWriter();

        // Set the form encoded string.
        writer.openBlock("body = buildFormUrlencodedString({", "});", () -> {
            // Gather all the explicit input entries.
            writer.write("...$L,",
                    inputStructure.accept(new QueryMemberSerVisitor(context, "input", Format.DATE_TIME)));
            // Set the protocol required values.
            ServiceShape serviceShape = context.getService();
            writer.write("Action: $S,", operation.getId().getName(serviceShape));
            writer.write("Version: $S,", serviceShape.getVersion());
        });
    }

    @Override
    protected boolean writeUndefinedInputBody(GenerationContext context, OperationShape operation) {
        return AwsProtocolUtils.generateUndefinedQueryInputBody(context, operation);
    }

    @Override
    protected void writeErrorCodeParser(GenerationContext context) {
        TypeScriptWriter writer = context.getWriter();

        // Outsource error code parsing since it's complex for this protocol.
        writer.write("const errorCode = loadQueryErrorCode(output, parsedOutput.body);");
    }

    @Override
    protected void deserializeOutputDocument(
            GenerationContext context,
            OperationShape operation,
            StructureShape outputStructure
    ) {
        TypeScriptWriter writer = context.getWriter();

        String dataSource = "data." + operation.getId().getName(context.getService()) + "Result";
        writer.write("contents = $L;",
                outputStructure.accept(new XmlMemberDeserVisitor(context, dataSource, Format.DATE_TIME)));
    }

    @Override
    public void generateProtocolTests(GenerationContext context) {
        AwsProtocolUtils.generateProtocolTests(this, context);
    }

    @Override
    public Map<String, ShapeId> getOperationErrors(GenerationContext context, OperationShape operation) {
        Map<String, ShapeId> errors = new TreeMap<>();

        operation.getErrors().forEach(shapeId -> {
            Shape errorShape = context.getModel().expectShape(shapeId);
            String errorName = shapeId.getName(context.getService());

            Optional<AwsQueryErrorTrait> errorShapeTrait = errorShape.getTrait(AwsQueryErrorTrait.class);
            if (errorShapeTrait.isPresent()) {
                errors.put(errorShapeTrait.get().getCode(), shapeId);
            } else {
                errors.put(errorName, shapeId);
            }
        });

        return errors;
    }
}
