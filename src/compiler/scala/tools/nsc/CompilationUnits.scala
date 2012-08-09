/* NSC -- new Scala compiler
 * Copyright 2005-2012 LAMP/EPFL
 * @author  Martin Odersky
 */

package scala.tools.nsc

import util.FreshNameCreator
import scala.reflect.internal.util.{ Position, NoPosition, BatchSourceFile, SourceFile, NoSourceFile }
import scala.collection.mutable
import scala.collection.mutable.{ LinkedHashSet, ListBuffer }

trait CompilationUnits { self: Global =>

  /** An object representing a missing compilation unit.
   */
  object NoCompilationUnit extends CompilationUnit(NoSourceFile) {
    override lazy val isJava = false
    override def exists = false
    override def toString() = "NoCompilationUnit"
  }

  /** One unit of compilation that has been submitted to the compiler.
    * It typically corresponds to a single file of source code.  It includes
    * error-reporting hooks.  */
  class CompilationUnit(val source: SourceFile) {

    /** the fresh name creator */
    var fresh: FreshNameCreator = new FreshNameCreator.Default

    def freshTermName(prefix: String): TermName = newTermName(fresh.newName(prefix))
    def freshTypeName(prefix: String): TypeName = newTypeName(fresh.newName(prefix))

    /** the content of the compilation unit in tree form */
    var body: Tree = EmptyTree

    def exists = source != NoSourceFile && source != null

//    def parseSettings() = {
//      val argsmarker = "SCALAC_ARGS"
//      if(comments nonEmpty) {
//        val pragmas = comments find (_.text.startsWith("//#")) // only parse first one
//        pragmas foreach { p =>
//          val i = p.text.indexOf(argsmarker)
//          if(i > 0)
//        }
//      }
//    }
    /** Note: depends now contains toplevel classes.
     *  To get their sourcefiles, you need to dereference with .sourcefile
     */
    val depends = mutable.HashSet[Symbol]()

    /** so we can relink
     */
    val defined = mutable.HashSet[Symbol]()

    /** Synthetic definitions generated by namer, eliminated by typer.
     */
    val synthetics = mutable.HashMap[Symbol, Tree]()

    /** things to check at end of compilation unit */
    val toCheck = new ListBuffer[() => Unit]

    /** The features that were already checked for this unit */
    var checkedFeatures = Set[Symbol]()

    def position(pos: Int) = source.position(pos)

    /** The position of a targeted type check
     *  If this is different from NoPosition, the type checking
     *  will stop once a tree that contains this position range
     *  is fully attributed.
     */
    def targetPos: Position = NoPosition

    /** The icode representation of classes in this compilation unit.
     *  It is empty up to phase 'icode'.
     */
    val icode: LinkedHashSet[icodes.IClass] = new LinkedHashSet

    def echo(pos: Position, msg: String) =
      reporter.echo(pos, msg)

    def error(pos: Position, msg: String) =
      reporter.error(pos, msg)

    def warning(pos: Position, msg: String) =
      reporter.warning(pos, msg)

    def deprecationWarning(pos: Position, msg: String) =
      currentRun.deprecationWarnings0.warn(pos, msg)

    def uncheckedWarning(pos: Position, msg: String) =
      currentRun.uncheckedWarnings0.warn(pos, msg)

    def inlinerWarning(pos: Position, msg: String) =
      currentRun.inlinerWarnings.warn(pos, msg)

    def incompleteInputError(pos: Position, msg:String) =
      reporter.incompleteInputError(pos, msg)

    def comment(pos: Position, msg: String) =
      reporter.comment(pos, msg)

    /** Is this about a .java source file? */
    lazy val isJava = source.file.name.endsWith(".java")

    override def toString() = source.toString()

    def clear() {
      fresh = new FreshNameCreator.Default
      body = EmptyTree
      depends.clear()
      defined.clear()
      synthetics.clear()
      toCheck.clear()
      checkedFeatures = Set()
      icode.clear()
    }
  }
}


