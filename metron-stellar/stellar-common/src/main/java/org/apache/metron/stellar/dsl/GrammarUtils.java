/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.dsl;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class GrammarUtils {

  public static String toSyntaxTree(ParseTree tree) {
    return new AST(tree).toString();
  }

  /**
   * This is a utility class to walk the parse tree for an antlr ParseTree
   */
  private static class AST {

    private final Object payload;
    private final List<AST> children;

    public AST(ParseTree tree) {
      this(null, tree);
    }

    private AST(AST ast, ParseTree tree) {
      this(ast, tree, new ArrayList<>());
    }

    private AST(AST parent, ParseTree tree, List<AST> children) {
      this.payload = getPayload(tree);
      this.children = children;
      if (parent == null) {
        walk(tree, this);
      }
      else {
        parent.children.add(this);
      }
    }

    private Object getPayload(ParseTree tree) {
      if (tree.getChildCount() == 0) {
        return tree.getPayload();
      }
      else {
        String ruleName = tree.getClass().getSimpleName().replace("Context", "");
        return Character.toLowerCase(ruleName.charAt(0)) + ruleName.substring(1);
      }
    }

    private static void walk(ParseTree tree, AST ast) {
      if (tree.getChildCount() == 0) {
        new AST(ast, tree);
      }
      else if (tree.getChildCount() == 1) {
        walk(tree.getChild(0), ast);
      }
      else if (tree.getChildCount() > 1) {
        for (int i = 0; i < tree.getChildCount(); i++) {
          AST temp = new AST(ast, tree.getChild(i));
          if (!(temp.payload instanceof Token)) {
            walk(tree.getChild(i), temp);
          }
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      AST ast = this;
      List<AST> firstStack = new ArrayList<>();
      firstStack.add(ast);
      List<List<AST>> childListStack = new ArrayList<>();
      childListStack.add(firstStack);
      while (!childListStack.isEmpty()) {
        List<AST> childStack = childListStack.get(childListStack.size() - 1);
        if (childStack.isEmpty()) {
          childListStack.remove(childListStack.size() - 1);
        }
        else {
          ast = childStack.remove(0);
          String caption;
          if (ast.payload instanceof Token) {
            Token token = (Token) ast.payload;
            caption = String.format("TOKEN[type: %s, text: %s]",
                    token.getType(), token.getText().replace("\n", "\\n"));
          }
          else {
            caption = String.valueOf(ast.payload);
          }
          String indent = "";
          for (int i = 0; i < childListStack.size() - 1; i++) {
            indent += (childListStack.get(i).size() > 0) ? "|  " : "   ";
          }
          builder.append(indent)
                  .append(childStack.isEmpty() ? "'- " : "|- ")
                  .append(caption)
                  .append("\n");

          if (ast.children.size() > 0) {
            List<AST> children = new ArrayList<>();
            for (int i = 0; i < ast.children.size(); i++) {
              children.add(ast.children.get(i));
            }
            childListStack.add(children);
          }
        }
      }
      return builder.toString();
    }
  }
}
