// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package xbc

import scala.annotation.tailrec

object Interpret { // basic interpreter for Lang

  import Lang._

  type Value = Long

  def standard(program: Program): Value = {
    program match {
      case Program(defs, main) =>
        def nested_evalFrame = evalFrame _

        @tailrec
        def evalFrame(actuals: Vector[Value], body: Exp): Value = {

          def eval(exp: Exp): Value =
            exp match {
              case Num(x) => x
              case Builtin(binOp, e1, e2) => applyBinOp(binOp, eval(e1), eval(e2))
              case IfNot0(e1, e2, e3) => if (eval(e1) != 0) eval(e2) else eval(e3)
              case Arg(i) => actuals(i)
              case FnCall(fnName, args) =>
                val arity = args.length
                defs.get(fnName, arity) match {
                  case None => sys.error(s"FnCall: $fnName/$arity")
                  case Some(body) =>
                    val vs = args.map(eval(_)).toVector
                    nested_evalFrame(vs, body)
                }
            }

          body match {
            case IfNot0(e1, e2, e3) =>
              if (eval(e1) != 0) evalFrame(actuals, e2) else evalFrame(actuals, e3)
            case FnCall(fnName, args) =>
              val arity = args.length
              defs.get(fnName, arity) match {
                case None => sys.error(s"FnCall: $fnName/$arity")
                case Some(body) =>
                  val vs = args.map(eval(_)).toVector
                  evalFrame(vs, body) //tailcall
              }
            case _ =>
              eval(body)
          }
        }

        evalFrame(Vector(), main)
    }
  }

  def applyBinOp(binOp: BinOp, v1: Value, v2: Value): Value = {
    binOp match {
      case MulOp => v1 * v2
      case AddOp => v1 + v2
      case SubOp => v1 - v2
      case LessOp => if (v1 < v2) 1 else 0
    }
  }

}
