// Generated from org/apache/metron/profiler/client/window/generated/Window.g4 by ANTLR 4.5
package org.apache.metron.profiler.client.window.generated;

//CHECKSTYLE:OFF
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a process tree produced by
 * {@link WindowParser}.
 */
public interface WindowListener extends ParseTreeListener {
	/**
	 * Enter a process tree produced by {@link WindowParser#window}.
	 * @param ctx the process tree
	 */
	void enterWindow(WindowParser.WindowContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#window}.
	 * @param ctx the process tree
	 */
	void exitWindow(WindowParser.WindowContext ctx);
	/**
	 * Enter a process tree produced by the {@code NonRepeatingWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void enterNonRepeatingWindow(WindowParser.NonRepeatingWindowContext ctx);
	/**
	 * Exit a process tree produced by the {@code NonRepeatingWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void exitNonRepeatingWindow(WindowParser.NonRepeatingWindowContext ctx);
	/**
	 * Enter a process tree produced by the {@code RepeatingWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void enterRepeatingWindow(WindowParser.RepeatingWindowContext ctx);
	/**
	 * Exit a process tree produced by the {@code RepeatingWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void exitRepeatingWindow(WindowParser.RepeatingWindowContext ctx);
	/**
	 * Enter a process tree produced by the {@code DenseWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void enterDenseWindow(WindowParser.DenseWindowContext ctx);
	/**
	 * Exit a process tree produced by the {@code DenseWindow}
	 * labeled alternative in {@link WindowParser#window_expression}.
	 * @param ctx the process tree
	 */
	void exitDenseWindow(WindowParser.DenseWindowContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#excluding_specifier}.
	 * @param ctx the process tree
	 */
	void enterExcluding_specifier(WindowParser.Excluding_specifierContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#excluding_specifier}.
	 * @param ctx the process tree
	 */
	void exitExcluding_specifier(WindowParser.Excluding_specifierContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#including_specifier}.
	 * @param ctx the process tree
	 */
	void enterIncluding_specifier(WindowParser.Including_specifierContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#including_specifier}.
	 * @param ctx the process tree
	 */
	void exitIncluding_specifier(WindowParser.Including_specifierContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#specifier}.
	 * @param ctx the process tree
	 */
	void enterSpecifier(WindowParser.SpecifierContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#specifier}.
	 * @param ctx the process tree
	 */
	void exitSpecifier(WindowParser.SpecifierContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#specifier_arg_list}.
	 * @param ctx the process tree
	 */
	void enterSpecifier_arg_list(WindowParser.Specifier_arg_listContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#specifier_arg_list}.
	 * @param ctx the process tree
	 */
	void exitSpecifier_arg_list(WindowParser.Specifier_arg_listContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#day_specifier}.
	 * @param ctx the process tree
	 */
	void enterDay_specifier(WindowParser.Day_specifierContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#day_specifier}.
	 * @param ctx the process tree
	 */
	void exitDay_specifier(WindowParser.Day_specifierContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#identifier}.
	 * @param ctx the process tree
	 */
	void enterIdentifier(WindowParser.IdentifierContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#identifier}.
	 * @param ctx the process tree
	 */
	void exitIdentifier(WindowParser.IdentifierContext ctx);
	/**
	 * Enter a process tree produced by {@link WindowParser#specifier_list}.
	 * @param ctx the process tree
	 */
	void enterSpecifier_list(WindowParser.Specifier_listContext ctx);
	/**
	 * Exit a process tree produced by {@link WindowParser#specifier_list}.
	 * @param ctx the process tree
	 */
	void exitSpecifier_list(WindowParser.Specifier_listContext ctx);
	/**
	 * Enter a process tree produced by the {@code FromToDuration}
	 * labeled alternative in {@link WindowParser#duration}.
	 * @param ctx the process tree
	 */
	void enterFromToDuration(WindowParser.FromToDurationContext ctx);
	/**
	 * Exit a process tree produced by the {@code FromToDuration}
	 * labeled alternative in {@link WindowParser#duration}.
	 * @param ctx the process tree
	 */
	void exitFromToDuration(WindowParser.FromToDurationContext ctx);
	/**
	 * Enter a process tree produced by the {@code FromDuration}
	 * labeled alternative in {@link WindowParser#duration}.
	 * @param ctx the process tree
	 */
	void enterFromDuration(WindowParser.FromDurationContext ctx);
	/**
	 * Exit a process tree produced by the {@code FromDuration}
	 * labeled alternative in {@link WindowParser#duration}.
	 * @param ctx the process tree
	 */
	void exitFromDuration(WindowParser.FromDurationContext ctx);
	/**
	 * Enter a process tree produced by the {@code SkipDistance}
	 * labeled alternative in {@link WindowParser#skip_distance}.
	 * @param ctx the process tree
	 */
	void enterSkipDistance(WindowParser.SkipDistanceContext ctx);
	/**
	 * Exit a process tree produced by the {@code SkipDistance}
	 * labeled alternative in {@link WindowParser#skip_distance}.
	 * @param ctx the process tree
	 */
	void exitSkipDistance(WindowParser.SkipDistanceContext ctx);
	/**
	 * Enter a process tree produced by the {@code WindowWidth}
	 * labeled alternative in {@link WindowParser#window_width}.
	 * @param ctx the process tree
	 */
	void enterWindowWidth(WindowParser.WindowWidthContext ctx);
	/**
	 * Exit a process tree produced by the {@code WindowWidth}
	 * labeled alternative in {@link WindowParser#window_width}.
	 * @param ctx the process tree
	 */
	void exitWindowWidth(WindowParser.WindowWidthContext ctx);
	/**
	 * Enter a process tree produced by the {@code TimeInterval}
	 * labeled alternative in {@link WindowParser#time_interval}.
	 * @param ctx the process tree
	 */
	void enterTimeInterval(WindowParser.TimeIntervalContext ctx);
	/**
	 * Exit a process tree produced by the {@code TimeInterval}
	 * labeled alternative in {@link WindowParser#time_interval}.
	 * @param ctx the process tree
	 */
	void exitTimeInterval(WindowParser.TimeIntervalContext ctx);
	/**
	 * Enter a process tree produced by the {@code TimeAmount}
	 * labeled alternative in {@link WindowParser#time_amount}.
	 * @param ctx the process tree
	 */
	void enterTimeAmount(WindowParser.TimeAmountContext ctx);
	/**
	 * Exit a process tree produced by the {@code TimeAmount}
	 * labeled alternative in {@link WindowParser#time_amount}.
	 * @param ctx the process tree
	 */
	void exitTimeAmount(WindowParser.TimeAmountContext ctx);
	/**
	 * Enter a process tree produced by the {@code TimeUnit}
	 * labeled alternative in {@link WindowParser#time_unit}.
	 * @param ctx the process tree
	 */
	void enterTimeUnit(WindowParser.TimeUnitContext ctx);
	/**
	 * Exit a process tree produced by the {@code TimeUnit}
	 * labeled alternative in {@link WindowParser#time_unit}.
	 * @param ctx the process tree
	 */
	void exitTimeUnit(WindowParser.TimeUnitContext ctx);
}