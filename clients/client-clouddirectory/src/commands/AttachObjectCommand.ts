// smithy-typescript generated code
import { EndpointParameterInstructions, getEndpointPlugin } from "@aws-sdk/middleware-endpoint";
import { getSerdePlugin } from "@aws-sdk/middleware-serde";
import { HttpRequest as __HttpRequest, HttpResponse as __HttpResponse } from "@aws-sdk/protocol-http";
import { Command as $Command } from "@aws-sdk/smithy-client";
import {
  FinalizeHandlerArguments,
  Handler,
  HandlerExecutionContext,
  HttpHandlerOptions as __HttpHandlerOptions,
  MetadataBearer as __MetadataBearer,
  MiddlewareStack,
  SerdeContext as __SerdeContext,
} from "@aws-sdk/types";

import { CloudDirectoryClientResolvedConfig, ServiceInputTypes, ServiceOutputTypes } from "../CloudDirectoryClient";
import {
  AttachObjectRequest,
  AttachObjectRequestFilterSensitiveLog,
  AttachObjectResponse,
  AttachObjectResponseFilterSensitiveLog,
} from "../models/models_0";
import {
  deserializeAws_restJson1AttachObjectCommand,
  serializeAws_restJson1AttachObjectCommand,
} from "../protocols/Aws_restJson1";

export interface AttachObjectCommandInput extends AttachObjectRequest {}
export interface AttachObjectCommandOutput extends AttachObjectResponse, __MetadataBearer {}

/**
 * <p>Attaches an existing object to another object. An object can be accessed in two
 *       ways:</p>
 *          <ol>
 *             <li>
 *                <p>Using the path</p>
 *             </li>
 *             <li>
 *                <p>Using <code>ObjectIdentifier</code>
 *                </p>
 *             </li>
 *          </ol>
 * @example
 * Use a bare-bones client and the command you need to make an API call.
 * ```javascript
 * import { CloudDirectoryClient, AttachObjectCommand } from "@aws-sdk/client-clouddirectory"; // ES Modules import
 * // const { CloudDirectoryClient, AttachObjectCommand } = require("@aws-sdk/client-clouddirectory"); // CommonJS import
 * const client = new CloudDirectoryClient(config);
 * const command = new AttachObjectCommand(input);
 * const response = await client.send(command);
 * ```
 *
 * @see {@link AttachObjectCommandInput} for command's `input` shape.
 * @see {@link AttachObjectCommandOutput} for command's `response` shape.
 * @see {@link CloudDirectoryClientResolvedConfig | config} for CloudDirectoryClient's `config` shape.
 *
 */
export class AttachObjectCommand extends $Command<
  AttachObjectCommandInput,
  AttachObjectCommandOutput,
  CloudDirectoryClientResolvedConfig
> {
  // Start section: command_properties
  // End section: command_properties

  public static getEndpointParameterInstructions(): EndpointParameterInstructions {
    return {
      UseFIPS: { type: "builtInParams", name: "useFipsEndpoint" },
      Endpoint: { type: "builtInParams", name: "endpoint" },
      Region: { type: "builtInParams", name: "region" },
      UseDualStack: { type: "builtInParams", name: "useDualstackEndpoint" },
    };
  }

  constructor(readonly input: AttachObjectCommandInput) {
    // Start section: command_constructor
    super();
    // End section: command_constructor
  }

  /**
   * @internal
   */
  resolveMiddleware(
    clientStack: MiddlewareStack<ServiceInputTypes, ServiceOutputTypes>,
    configuration: CloudDirectoryClientResolvedConfig,
    options?: __HttpHandlerOptions
  ): Handler<AttachObjectCommandInput, AttachObjectCommandOutput> {
    this.middlewareStack.use(getSerdePlugin(configuration, this.serialize, this.deserialize));
    this.middlewareStack.use(getEndpointPlugin(configuration, AttachObjectCommand.getEndpointParameterInstructions()));

    const stack = clientStack.concat(this.middlewareStack);

    const { logger } = configuration;
    const clientName = "CloudDirectoryClient";
    const commandName = "AttachObjectCommand";
    const handlerExecutionContext: HandlerExecutionContext = {
      logger,
      clientName,
      commandName,
      inputFilterSensitiveLog: AttachObjectRequestFilterSensitiveLog,
      outputFilterSensitiveLog: AttachObjectResponseFilterSensitiveLog,
    };
    const { requestHandler } = configuration;
    return stack.resolve(
      (request: FinalizeHandlerArguments<any>) =>
        requestHandler.handle(request.request as __HttpRequest, options || {}),
      handlerExecutionContext
    );
  }

  private serialize(input: AttachObjectCommandInput, context: __SerdeContext): Promise<__HttpRequest> {
    return serializeAws_restJson1AttachObjectCommand(input, context);
  }

  private deserialize(output: __HttpResponse, context: __SerdeContext): Promise<AttachObjectCommandOutput> {
    return deserializeAws_restJson1AttachObjectCommand(output, context);
  }

  // Start section: command_body_extra
  // End section: command_body_extra
}
