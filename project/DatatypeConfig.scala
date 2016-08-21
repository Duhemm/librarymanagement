import sbt.datatype.TpeRef
import sbt.datatype.CodecCodeGen

object DatatypeConfig {

  /** Extract the only type parameter from a TpeRef */
  def oneArg(tpe: TpeRef): TpeRef = {
    import scala.util.matching.Regex
    val pat = s"""${CodecCodeGen.removeTypeParameters(tpe.name)}<(.+?)>""".r
    val pat(arg0) = tpe.name
    TpeRef(arg0, false, false, false)
  }

  /** Extract the two type parameters from a TpeRef */
  def twoArgs(tpe: TpeRef): List[TpeRef] = {
    import scala.util.matching.Regex
    val pat = s"""${CodecCodeGen.removeTypeParameters(tpe.name)}<(.+?), (.+?)>""".r
    val pat(arg0, arg1) = tpe.name
    TpeRef(arg0, false, false, false) :: TpeRef(arg1, false, false, false) :: Nil
  }

  /** Codecs that were manually written. */
  val myCodecs: PartialFunction[String, TpeRef => List[String]] = {
    case "xsbti.Maybe" =>
      { tpe => "sbt.internal.librarymanagement.formats.MaybeFormat" :: getFormats(oneArg(tpe)) }
    case "xsbti.T2" =>
      { tpe => "sbt.internal.librarymanagement.formats.T2Format" :: twoArgs(tpe).flatMap(getFormats) }
    case "xsbti.F1" =>
      { tpe => "sbt.internal.librarymanagement.formats.F1Format" :: twoArgs(tpe).flatMap(getFormats) }
    case "xsbti.GlobalLock" =>
      { tpe => "sbt.internal.librarymanagement.formats.GlobalLockFormat" :: Nil }
    case "xsbti.Logger" =>
      { tpe => "sbt.internal.librarymanagement.formats.LoggerFormat" :: Nil}

    case "scala.xml.NodeSeq" =>
      { tpe => "sbt.internal.librarymanagement.formats.NodeSeqFormat" :: Nil }

    case "java.lang.Boolean" =>
      { tpe => "sbt.internal.librarymanagement.formats.MyBooleanFormat" :: Nil }
    case "java.lang.Integer" =>
      { tpe => "sbt.internal.librarymanagement.formats.MyIntegerFormat" :: Nil }
    case "java.util.Date" =>
      { tpe => "sbt.internal.librarymanagement.formats.DateFormat" :: Nil }
    case "java.util.Map" =>
      { tpe => "sbt.internal.librarymanagement.formats.JavaMapFormat" :: twoArgs(tpe).flatMap(getFormats) }
    case "java.util.Set" =>
      { tpe => "sbt.internal.librarymanagement.formats.JavaSetFormat" :: getFormats(oneArg(tpe)) }

    case "sbt.librarymanagement.UpdateOptions" =>
      { tpe => "sbt.internal.librarymanagement.formats.UpdateOptionsFormat" :: Nil }

    case "org.apache.ivy.plugins.resolver.DependencyResolver" =>
      { tpe => "sbt.internal.librarymanagement.formats.DependencyResolverFormat" :: Nil }
  }

  /** Types for which we don't include the format -- they're just aliases to InclExclRule */
  val excluded = Set(
    "sbt.librarymanagement.InclusionRule",
    "sbt.librarymanagement.ExclusionRule")

  /** Returns the list of formats required to encode the given `TpeRef`. */
  val getFormats: TpeRef => List[String] =
    CodecCodeGen.extensibleFormatsForType {
      case TpeRef("sbt.internal.librarymanagement.RetrieveConfiguration", false, false, false) =>
        "sbt.librarymanagement.RetrieveConfigurationFormats" :: Nil
      case tpe @ TpeRef(name, _, _, _) if myCodecs isDefinedAt CodecCodeGen.removeTypeParameters(name) =>
        myCodecs(CodecCodeGen.removeTypeParameters(name))(tpe)
      case TpeRef(name, _, _, _) if excluded contains CodecCodeGen.removeTypeParameters(name) =>
        Nil
      case other =>
        CodecCodeGen.formatsForType(other)
    }

}