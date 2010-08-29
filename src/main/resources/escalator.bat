@echo off

REM Change these as necessary
SET JAVAEXE=java.exe
SET ESCHOME=.

REM Should not need to touch this
%JAVAEXE% -cp %ESCLIB%\escalator_2.8.0-2.0.jar;%ESCHOME%\js-1.7R2.jar;%ESCHOME%\scala-compiler.jar;%ESCHOME%\scala-library.jar;%ESCHOME%\scala-swing-2.8.0.jar;%ESCHOME%\scalacheck_2.8.0-1.7.jar;%ESCHOME%\specs_2.8.0-1.6.5.jar;%ESCHOME%\jlfgr-1_0.jar;%ESCHOME\test-interface-0.3.jar;%ESCHOME% edu.depauw.escalator.Main %*
