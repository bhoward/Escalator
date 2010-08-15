@echo off

REM Change these as necessary
SET JAVAEXE=java.exe
SET ESCHOME=.
SET ESCLIB=%ESCHOME%\lib

REM Should not need to touch this
%JAVAEXE% -cp %ESCHOME%\escalator.jar;%ESCLIB%\js.jar;%ESCLIB%\scala-compiler.jar;%ESCLIB%\scala-library.jar;%ESCLIB%\scala-swing.jar;%ESCLIB%\scalacheck.jar;%ESCLIB%\specs.jar edu.depauw.escalator.GUIMain %*
