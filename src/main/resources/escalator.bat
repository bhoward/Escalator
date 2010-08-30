@echo off


REM Change these as necessary

SET JAVAEXE=java.exe


REM Should not need to touch this

%JAVAEXE% -cp .\escalator_2.8.0-2.0.jar;.\js-1.7R2.jar;.\scala-compiler.jar;.\scala-library.jar;.\scala-swing-2.8.0.jar;.\scalacheck_2.8.0-1.7.jar;.\specs_2.8.0-1.6.5.jar;.\jlfgr-1_0.jar;. edu.depauw.escalator.Main %*
