package pl.touk.nussknacker.engine.graph

import org.apache.commons.lang3.ClassUtils
import pl.touk.nussknacker.engine.api.typed.ClazzRef
import pl.touk.nussknacker.engine.definition.DefinitionExtractor
import pl.touk.nussknacker.engine.graph.evaluatedparam.Parameter
import sink.SinkRef
import pl.touk.nussknacker.engine.graph.expression.Expression
import pl.touk.nussknacker.engine.graph.node.SubprocessInputDefinition.SubprocessParameter
import pl.touk.nussknacker.engine.graph.service.ServiceRef
import pl.touk.nussknacker.engine.graph.source.SourceRef
import pl.touk.nussknacker.engine.graph.subprocess.SubprocessRef
import pl.touk.nussknacker.engine.graph.variable.Field

import scala.reflect.ClassTag
import scala.util.Try

object node {

  sealed trait Node {
    def data: NodeData
    def id: String = data.id
  }

  sealed trait OneOutputNode extends Node {
    def next: SubsequentNode
  }

  case class SourceNode(data: StartingNodeData, next: SubsequentNode) extends OneOutputNode

  sealed trait SubsequentNode extends Node

  case class OneOutputSubsequentNode(data: OneOutputSubsequentNodeData, next: SubsequentNode) extends OneOutputNode with SubsequentNode

  case class FilterNode(data: Filter, nextTrue: SubsequentNode, nextFalse: Option[SubsequentNode] = None) extends SubsequentNode

  //this should never occur in process to be run (unresolved)
  case class SubprocessNode(data: SubprocessInput, nexts: Map[String, SubsequentNode]) extends SubsequentNode

  case class SwitchNode(data: Switch, nexts: List[Case], defaultNext: Option[SubsequentNode] = None) extends SubsequentNode

  case class SplitNode(data: Split, nextParts: List[SubsequentNode]) extends SubsequentNode

  case class Case(expression: Expression, node: SubsequentNode)

  case class EndingNode(data: EndingNodeData) extends SubsequentNode

  trait UserDefinedAdditionalNodeFields
  sealed trait NodeData {
    def id: String
    def additionalFields: Option[UserDefinedAdditionalNodeFields]
  }

  trait Disableable {

    def isDisabled: Option[Boolean]
  }

  trait WithComponent {
    def componentId: String
  }

  sealed trait OneOutputSubsequentNodeData extends NodeData

  sealed trait EndingNodeData extends NodeData

  sealed trait StartingNodeData extends NodeData

  case class Source(id: String, ref: SourceRef, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends StartingNodeData with WithComponent {
    override val componentId = ref.typ
  }

  case class Filter(id: String, expression: Expression, isDisabled: Option[Boolean] = None,
                    additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends NodeData with Disableable

  case class Switch(id: String, expression: Expression, exprVal: String, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends NodeData

  case class VariableBuilder(id: String, varName: String, fields: List[Field], additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends OneOutputSubsequentNodeData

  case class Variable(id: String, varName: String, value: Expression, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends OneOutputSubsequentNodeData

  case class Enricher(id: String, service: ServiceRef, output: String, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends OneOutputSubsequentNodeData with WithComponent {
    override val componentId = service.id
  }

  case class CustomNode(id: String, outputVar: Option[String], nodeType: String, parameters: List[Parameter], additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends OneOutputSubsequentNodeData with WithComponent {
    override val componentId = nodeType
  }

  case class Split(id: String, isDisabled: Boolean = false, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends NodeData

  case class Processor(id: String, service: ServiceRef, isDisabled: Option[Boolean] = None, additionalFields: Option[UserDefinedAdditionalNodeFields] = None) extends OneOutputSubsequentNodeData with EndingNodeData with Disableable with WithComponent {
    override val componentId = service.id
  }

  case class Sink(
                   id: String,
                   ref: SinkRef,
                   endResult: Option[Expression] = None,
                   isDisabled: Option[Boolean] = None,
                   additionalFields: Option[UserDefinedAdditionalNodeFields] = None
                 ) extends EndingNodeData with WithComponent with Disableable {
    override val componentId = ref.typ
  }

  case class SubprocessInput(id: String, ref: SubprocessRef,
                             additionalFields: Option[UserDefinedAdditionalNodeFields] = None, isDisabled: Option[Boolean] = None) extends OneOutputSubsequentNodeData with EndingNodeData with WithComponent  with Disableable{
    override val componentId = ref.id
  }


  //this is used after resolving subprocess, used for detecting when subprocess ends and context should change
  case class SubprocessOutput(id: String, outputName: String, additionalFields: Option[UserDefinedAdditionalNodeFields] = None)
    extends OneOutputSubsequentNodeData

  //this is used only in subprocess definition
  case class SubprocessInputDefinition(id: String,
                                       parameters: List[SubprocessParameter],
                                       additionalFields: Option[UserDefinedAdditionalNodeFields] = None)
    extends StartingNodeData

  //this is used only in subprocess definition
  case class SubprocessOutputDefinition(id: String, outputName: String, additionalFields: Option[UserDefinedAdditionalNodeFields] = None)
    extends EndingNodeData

  //we don't use DefinitionExtractor.Parameter here, because this class should be serializable to json and Parameter has ClazzRef which has *real* class inside  
  //TODO: probably should be able to handle class parameters or typed maps
  //shape of this data should probably change, currently we leave it for backward compatibility
  object SubprocessInputDefinition {

    case class SubprocessParameter(name: String, typ: SubprocessClazzRef)

    object SubprocessClazzRef {

      def apply[T:ClassTag]: SubprocessClazzRef = SubprocessClazzRef(ClazzRef[T].refClazzName)

    }

    case class SubprocessClazzRef(refClazzName: String) {

      def toClazzRef(classLoader: ClassLoader) = Try(ClazzRef(ClassUtils.getClass(classLoader, refClazzName)))

    }

  }

  //it has to be here, otherwise it won't compile...
  def prefixNodeId[T<:NodeData](prefix: List[String], nodeData: T) : T = {
    import pl.touk.nussknacker.engine.util.copySyntax._
    nodeData.asInstanceOf[NodeData].copy(id = (prefix :+ nodeData.id).mkString("-")).asInstanceOf[T]
  }
}
