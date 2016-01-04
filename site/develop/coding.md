---
layout: page
title: Coding Guidelines
---

## General rules

* All files must have an Apache copyright header at the top of the file.
* Code should be removed rather than commented out.
* All public functions should have javadoc comments.
* Always use braces to surround branches.
* try-finally should be avoided.

## Formatting

* All files must have an 80 character maximum line length.
* Indentation should be 2 spaces.
* Files should use spaces instead of tabs.
* Wrapping lines
  * Break after a comma.
  * Break before an operator.
  * Prefer higher-level breaks to lower-level breaks.
  * Align the new line with beginning of the expression at the same level
    on the previous line.
  * If the above rules lead to confusing code, just indent 8 spaces.
* One variable declaration per a line.

## Naming

* Packages should be all lowercase.
  * Java code should be in `org.apache.,metron`, except for compatibility classes 
* Classes should be in mixed case.
* Variables should be in camel case.
* Constants should be in upper case.
