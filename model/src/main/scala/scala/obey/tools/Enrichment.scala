package scala.obey.tools

import scala.meta.syntactic.ast._
import scala.obey.model._
import scala.language.implicitConversions

import scala.annotation.StaticAnnotation

object Enrichment {

  implicit class DefnExtractor(tree: Defn) {

    def isAbstract: Boolean = true //tree.mods.contains(Mod.Abstract)

    def isMain: Boolean = tree match {
      case Defn.Def(_, Term.Name("main"), _, _, _, _) => true
      case _ => false
    }

    def isValueParameter: Boolean = tree.parent.map(p => p.isInstanceOf[Member.Method]).getOrElse(false)

    /*TODO find how to do that*/
    def isConstructorArg: Boolean = true //tree.parent.map(p => p.isInstanceOf[Member.Ctor]).getOrElse(false)

  }

}